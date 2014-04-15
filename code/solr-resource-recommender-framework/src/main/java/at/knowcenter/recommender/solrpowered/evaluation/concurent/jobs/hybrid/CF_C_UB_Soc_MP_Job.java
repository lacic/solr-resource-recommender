package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.hybrid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.CustomerAction.ACTION;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class CF_C_UB_Soc_MP_Job extends RecommenderEvaluator implements Callable<List<MetricsExporter>>{

	private List<String> users;

	public CF_C_UB_Soc_MP_Job(List<String> users, String jobDescription, int simulatedUpdateCount) {
		this.jobDescription = jobDescription;
		this.users = users;
//		this.simulatedUpdatesCount = simulatedUpdateCount;
	}
	
	@Override
	public List<MetricsExporter> call() throws Exception {
		return evaluate();
	}
	
	@Override
	public List<MetricsExporter> evaluate() {
		ContentFilter cf = new ContentFilter();
		List<MetricsExporter> metricsCalcs = new ArrayList<MetricsExporter>();
		
		for (int n = 1; n <= resultSize; n++) {
			MetricsExporter mCalc = new MetricsExporter("./evaluation_data/");
			mCalc.setAlgorithmName("15_CF-C-UB-Soc-MP");
			metricsCalcs.add(mCalc);
		}
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate_CF_C_MP(users, cf, metricsCalcs);
		return metricsCalcs;
	}
	
	protected void evaluate_CF_C_MP(List<String> users, ContentFilter cf, List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		int userSize = users.size();
		
//		int updateCounter = 0 + simulatedUpdatesCount;
//		int currentUserIndex = 1;
		
		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			
//			if (updateCounter > 0 && currentUserIndex % simulatedUpdatesCount == 0) {
//				Thread t = new Thread(new Runnable() {
//					@Override
//					public void run() {
//						long updateStart = System.nanoTime();
//						CustomerAction ca = new CustomerAction();
//						ca.setItemId("i_eval_id_" + (int) Math.random() * 10000);
//						ArrayList<String> customers = new ArrayList<String>();
//						customers.add("c_eval_id_" + (int) Math.random() * 10000);
//						ca.setCustomerIds(customers);
//						ca.setAction(ACTION.PURCHASED);
//						SolrServiceContainer.getInstance().getRecommendService().updateDocument(ca);		
//						
//						File file = new File("./evaluation_data/" + jobDescription + "_updateTime.txt");
//						FileWriter writer;
//						try {
//							writer = new FileWriter(file, true);
//							BufferedWriter bw = new BufferedWriter(writer);
//							bw.write(((System.nanoTime() - updateStart) / 1000000) + "\n");
//							bw.close();
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
//					}
//				});
//				updateCounter--;
//				t.start();
//			}
//			
//			currentUserIndex++;
			
			Double cfWeight = 0.0236;
			Double cfCatWeight = 0.0105;
			Double cbWeight = 0.0055;
			Double ubWeight = 0.0103;
			Double socWeight = 0.0214;
			Double mpWeight = 0.0054;
			Double socStreamWeight = 0.0011;

			
			List<String> cfRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CollaborativeFiltering));
			List<String> cfCatRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CF_Categories));
			List<String> cbRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.ContentBased));
			List<String> ubRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.UB_WithOutMLT));
			List<String> socialRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CF_Social));
			List<String> socialStreamRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.SocialStream));
			List<String> mpRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.MostPopular));
			
			Map<String, Double> occurencesMap = new HashMap<String, Double>();
			
			RecommendationQueryUtils.fillWeightedMap(occurencesMap, cfRecommendations, cfWeight);
			RecommendationQueryUtils.fillWeightedMap(occurencesMap, cfCatRecommendations, cfCatWeight);
			RecommendationQueryUtils.fillWeightedMap(occurencesMap, cbRecommendations, cbWeight);
			RecommendationQueryUtils.fillWeightedMap(occurencesMap, ubRecommendations, ubWeight);
			RecommendationQueryUtils.fillWeightedMap(occurencesMap, socialRecommendations, socWeight);
			RecommendationQueryUtils.fillWeightedMap(occurencesMap, socialStreamRecommendations, socStreamWeight);
			RecommendationQueryUtils.fillWeightedMap(occurencesMap, mpRecommendations, mpWeight);
			
			List<String> sortedAndTrimedRecommendations = RecommendationQueryUtils.extractCrossRankedProducts(occurencesMap);
			List<String> recommendations = new ArrayList<String>();

			RecommendationQueryUtils.appendDifferentProducts(resultSize, recommendations, sortedAndTrimedRecommendations);
			
			long duaration = System.nanoTime() - getRecommStartTime;
			
			for (int n = 1; n <= resultSize; n++) {
				MetricsExporter mCalc = metricsCalcs.get(n - 1);
				if (recommendations.size() > n) {
					appendMetrics(mCalc, n, userID, recommendations.subList(0, n), 
							recommendStrategies.get(StrategyType.CN_WeightNameDescription).getAlreadyBoughtProducts());
				} else {
					appendMetrics(mCalc, n, userID, recommendations,
							recommendStrategies.get(StrategyType.CN_WeightNameDescription).getAlreadyBoughtProducts());
				}
				mCalc.appendDuaration(duaration);
			}
			currentEvaluatingUser++;
			double progress = ((double) currentEvaluatingUser) / userSize;
			System.out.println("CF_CFcat_C_UB_Soc_MP_+" + jobDescription +": Evaluation progress: " + progress * 100 + " % done");
		}
	}
	
	
	

}
