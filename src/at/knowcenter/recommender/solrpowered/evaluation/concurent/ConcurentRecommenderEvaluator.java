package at.knowcenter.recommender.solrpowered.evaluation.concurent;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.*;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.contentbased.C_Name_Description_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.SocialStream_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.UB_InterestsGroups_Social_Job;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendService;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemService;
import at.knowcenter.recommender.solrpowered.services.impl.social.SocialActionService;
import at.knowcenter.recommender.solrpowered.services.impl.social.reversed.OwnSocialActionService;
import at.knowcenter.recommender.solrpowered.services.impl.socialstream.SocialStreamService;
import at.knowcenter.recommender.solrpowered.services.impl.user.UserService;

public class ConcurentRecommenderEvaluator {

	private List<String> users;
	
	public void evaluate() {
		long startTime = System.nanoTime();
		
		initEvaluation();
		
		ExecutorService executor = Executors.newFixedThreadPool(5);
		
		List<Callable<List<MetricsExporter>>> jobs = new ArrayList<Callable<List<MetricsExporter>>>();
		
		int fullUserSize = users.size();
		int partitionCount = 200;
		
		for (int i = 0; i < partitionCount; i++) {
			
			int partitionSize = fullUserSize / partitionCount;
			int offset = (i + 1) * partitionSize;
			
			if (i == partitionCount - 1) {
				offset = fullUserSize;
			}
			
			List<String> userPartition= users.subList(i * partitionSize, offset);
			jobs.add(new SocialStream_Job(userPartition, "job_" + i));
		}
		
		Map<String, List<MetricsExporter>> metricExporterMap = new HashMap<String, List<MetricsExporter>>();
		
		
	    List<Future<List<MetricsExporter>>> list = new ArrayList<Future<List<MetricsExporter>>>();
	    
	    for (Callable<List<MetricsExporter>> job : jobs) {
			Future<List<MetricsExporter>> submit = executor.submit(job);
			list.add(submit);
		}

	    for (Future<List<MetricsExporter>> future : list) {
	    	try {
	    		List<MetricsExporter> metrics = future.get();
	    		
	    		if (metricExporterMap.containsKey(metrics.get(0).getAlgorithmName())) {
	    			List<MetricsExporter> previousMetrics = metricExporterMap.get(metrics.get(0).getAlgorithmName());
	    			for (int i = 0; i < previousMetrics.size(); i++) {
	    				previousMetrics.get(i).appendMetrics(metrics.get(i));
	    			}
	    			
	    			metricExporterMap.put(metrics.get(0).getAlgorithmName(), previousMetrics);
	    		} else {
	    			metricExporterMap.put(metrics.get(0).getAlgorithmName(), metrics);
	    		}
	    	} catch (InterruptedException e) {
	    		e.printStackTrace();
	    	} catch (ExecutionException e) {
	    		e.printStackTrace();
	    	}
	    }
	    executor.shutdown();
	    
	    for (List<MetricsExporter> metrics : metricExporterMap.values()) {
	    	for (int n = 1; n <= 10; n++) {
				MetricsExporter mCalc = metrics.get(n - 1);
				mCalc.exportCalculatedMetricsAverage(users.size());
			}
	    }
	    
	    long totalTime = System.nanoTime() - startTime;
	    System.out.println("Finished all threads in " + (totalTime / 1000000000) + " seconds");
	}

	private void initEvaluation() {
		int port = 8984;
		String address = "localhost";
		RecommenderEvaluator recommenderEval = new RecommenderEvaluator();
		
		UserService userService = new UserService(address, port, "collection3");
		RecommendService recService = new RecommendService(address, port, "collection2");
		ItemService itemService = new ItemService(address, port, "collection1");
		
		SocialActionService socialService = new SocialActionService(address, port, "bn_social_action");
		OwnSocialActionService ownSocialService = new OwnSocialActionService(address, port, "bn_own_social_action");
		SocialStreamService socialStreamService = new SocialStreamService(address, port, "bn_social_stream");
		
		SolrServiceContainer.getInstance().setUserService(userService);
		SolrServiceContainer.getInstance().setRecommendService(recService);
		SolrServiceContainer.getInstance().setItemService(itemService);
		SolrServiceContainer.getInstance().setSocialActionService(socialService);
		SolrServiceContainer.getInstance().setOwnSocialActionService(ownSocialService);
		SolrServiceContainer.getInstance().setSocialStreamService(socialStreamService);
		
		users = recommenderEval.getAllSocialStreamUsers();
		
		List<String> allUsers = recommenderEval.getAllUsers();
		
		users.retainAll(allUsers);
		
//		System.out.println("All User Size " + allUsers.size());	
		
		System.out.println("User Size " + users.size());
	}
	
}
