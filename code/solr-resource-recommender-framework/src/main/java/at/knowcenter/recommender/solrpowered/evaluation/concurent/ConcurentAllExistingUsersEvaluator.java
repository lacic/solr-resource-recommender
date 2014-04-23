package at.knowcenter.recommender.solrpowered.evaluation.concurent;

import java.util.ArrayList;
import java.util.Collections;
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
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.hybrid.CF_CFcat_C_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.hybrid.UB_InterestsGroups_Social_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.marketplace.MP_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.marketplace.cb.C_Description_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.marketplace.cb.C_Name_Description_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.marketplace.cb.C_Name_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.marketplace.cf.CF_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.UB_CustomerGroups_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.UB_Interests_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.cb.SocialStream_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.cf.CF_Social_Comments_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.cf.CF_Social_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.cf.CF_Social_Likes_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.combined.CFPurchWithSocCommonNeighborhoodRecommender_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.combined.CFPurchWithSocCommonNeighborhoodReplacedRecommender_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.combined.CFPurchWithSocCommonNeighborhoodSummedRecommender_Job;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.cleaner.DataFetcher;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendService;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemService;
import at.knowcenter.recommender.solrpowered.services.impl.user.UserService;

public class ConcurentAllExistingUsersEvaluator {

	public static void main(String... args) {
		ConcurentAllExistingUsersEvaluator evaluator = new ConcurentAllExistingUsersEvaluator();
		evaluator.evaluate();
	}
	
	
	private List<String> users;
	
	public void evaluate() {
		long startTime = System.nanoTime();
		
		initEvaluation();
		
		ExecutorService executor = Executors.newFixedThreadPool(5);
		
		List<Callable<List<MetricsExporter>>> jobs = new ArrayList<Callable<List<MetricsExporter>>>();
		
		int fullUserSize = users.size();
		
		int partitionCount = 150;
		
		for (int i = 0; i < partitionCount; i++) {
			
			int partitionSize = fullUserSize / partitionCount;
			int offset = (i + 1) * partitionSize;
			
			if (i == partitionCount - 1) {
				offset = fullUserSize;
			}
			
			List<String> userPartition= users.subList(i * partitionSize, offset);

//			jobs.add(new CommonStrategyJob(userPartition, "CF_Rev", StrategyType.CF_Review, 0));
//			jobs.add(new CommonStrategyJob(userPartition, "MP", StrategyType.MostPopular_Review, 1));
//			jobs.add(new CommonStrategyJob(userPartition, "Soc", StrategyType.CF_Social, 2));
//			jobs.add(new C_Name_Job(userPartition));
//			jobs.add(new C_Description_Job(userPartition));
//			jobs.add(new CommonStrategyJob(userPartition, "Cat_" + i, StrategyType.CF_Categories, 3));

//			jobs.add(new UB_Interests_Job(userPartition, "interests_" + i));
//			jobs.add(new UB_CustomerGroups_Job(userPartition, "groups_" + i));
//
//			jobs.add(new CommonStrategyJob(userPartition, "Bio_" + i, StrategyType.BiographyBasedMLT, 4));
//			jobs.add(new CommonStrategyJob(userPartition, "Real_Bio" + i, StrategyType.RealBiographyBasedMLT, 5));
//			jobs.add(new SocialStream_Job(userPartition, "soc_stream_job_" + i));
//			jobs.add(new CF_Social_Likes_Job(userPartition, "likes_"+i));
//			jobs.add(new CF_Social_Comments_Job(userPartition, "comments_"+i));
//			jobs.add(new CommonStrategyJob(userPartition, "Wallposts_" + i, StrategyType.WallPostInteraction, 6));
//			jobs.add(new CommonStrategyJob(userPartition, "Snapshots_" + i, StrategyType.SnapshotInteraction, 7));

//			jobs.add(new CommonStrategyJob(userPartition, "InterestsC" + i, StrategyType.UB_Interests_MLT, 8));
//			jobs.add(new CommonStrategyJob(userPartition, "CommonN" + i, StrategyType.CF_Soc_Network_CN, 9));
//			jobs.add(new CommonStrategyJob(userPartition, "Jaccard" + i, StrategyType.CF_Soc_Network_Jaccard, 10));
//			jobs.add(new CommonStrategyJob(userPartition, "NeighOv" + i, StrategyType.CF_Soc_Network_NeighOverlap, 11));
//			jobs.add(new CommonStrategyJob(userPartition, "AdamicAdar" + i, StrategyType.CF_Soc_Network_AdamicAdar, 12));
//			jobs.add(new CommonStrategyJob(userPartition, "PrefAttach" + i, StrategyType.CF_Soc_Network_PrefAttachment, 13));

//			jobs.add(new CommonStrategyJob(userPartition, "GroupJaccard" + i, StrategyType.CF_Soc_Group_Jaccard, 14));
//			jobs.add(new CommonStrategyJob(userPartition, "GroupOverlap" + i, StrategyType.CF_Soc_Group_Overlap, 15));
//			jobs.add(new CommonStrategyJob(userPartition, "GroupAdar" + i, StrategyType.CF_Soc_Group_AdemicAdar, 16));
//			jobs.add(new CommonStrategyJob(userPartition, "GroupPrefAttach" + i, StrategyType.CF_Soc_Group_PrefAttach, 17));

//			jobs.add(new CommonStrategyJob(userPartition, "InterestsJaccard" + i, StrategyType.CF_Soc_Interests_Jaccard, 18));
//			jobs.add(new CommonStrategyJob(userPartition, "InterestsOverlap" + i, StrategyType.CF_Soc_Interests_Overlap, 19));
//			jobs.add(new CommonStrategyJob(userPartition, "InterestsAdar" + i, StrategyType.CF_Soc_Interests_AdemicAdar, 20));
//			jobs.add(new CommonStrategyJob(userPartition, "InterestsPrefAttach" + i, StrategyType.CF_Soc_Interests_PrefAttach, 21));
			
//			jobs.add(new CommonStrategyJob(userPartition, "SellerCN" + i, StrategyType.CF_Market_Seller_CN, 20));
//			jobs.add(new CommonStrategyJob(userPartition, "SellerJaccard" + i, StrategyType.CF_Market_Seller_Jaccard, 21));
//			jobs.add(new CommonStrategyJob(userPartition, "SellerPrefAtt" + i, StrategyType.CF_Market_Seller_PrefAtt, 22));
//			jobs.add(new CommonStrategyJob(userPartition, "SellerOverlap" + i, StrategyType.CF_Market_Seller_Overlap, 23));
//			jobs.add(new CommonStrategyJob(userPartition, "SellerAdar" + i, StrategyType.CF_Market_Seller_AdamicAdar, 24));
//			jobs.add(new CommonStrategyJob(userPartition, "SellerSummed" + i, StrategyType.CF_Market_Seller_Summed, 25));
//			jobs.add(new CommonStrategyJob(userPartition, "Picks" + i, StrategyType.CF_Loc_Picks_CN, 26));
//			jobs.add(new CommonStrategyJob(userPartition, "PicksJaccard" + i, StrategyType.CF_L[oc_Picks_Jaccard, 27));

//			jobs.add(new CommonStrategyJob(userPartition, "LocCN" + i, StrategyType.CF_Location_Network_All_CN, 28));
//			jobs.add(new CommonStrategyJob(userPartition, "LocCoCN" + i, StrategyType.CF_Location_Network_Coocured_CN, 29));
//			jobs.add(new CommonStrategyJob(userPartition, "RegCN" + i, StrategyType.CF_Region_Network_All_CN, 30));
//			jobs.add(new CommonStrategyJob(userPartition, "RegCoCN" + i, StrategyType.CF_Region_Network_Coocurred_CN, 31));

//			jobs.add(new CommonStrategyJob(userPartition, "LocJaccard" + i, StrategyType.CF_Location_Network_All_Jaccard, 32));
//			jobs.add(new CommonStrategyJob(userPartition, "LocCoJaccard" + i, StrategyType.CF_Location_Network_Coocured_Jaccard, 33));
//			jobs.add(new CommonStrategyJob(userPartition, "RegJaccard" + i, StrategyType.CF_Region_Network_All_Jaccard, 34));
//			jobs.add(new CommonStrategyJob(userPartition, "RegCoJaccard" + i, StrategyType.CF_Region_Network_Coocurred_Jaccard, 35));
			
//			jobs.add(new CommonStrategyJob(userPartition, "LocAdar" + i, StrategyType.CF_Location_Network_All_Adar, 36));
//			jobs.add(new CommonStrategyJob(userPartition, "LocCoAdar" + i, StrategyType.CF_Location_Network_Coocured_Adar, 37));
//			jobs.add(new CommonStrategyJob(userPartition, "RegAdar" + i, StrategyType.CF_Region_Network_All_Adar, 38));
//			jobs.add(new CommonStrategyJob(userPartition, "RegCoAdar" + i, StrategyType.CF_Region_Network_Coocurred_Adar, 39));
			
//			jobs.add(new CommonStrategyJob(userPartition, "RegCoOverlap" + i, StrategyType.CF_Region_Network_Coocurred_Overlap, 40));
//			jobs.add(new CommonStrategyJob(userPartition, "RegCoPrefAtt" + i, StrategyType.CF_Region_Network_Coocurred_PrefAtt, 41));
			
//			jobs.add(new CommonStrategyJob(userPartition, "RegOverlap" + i, StrategyType.CF_Region_Network_All_Overlap, 42));
//			jobs.add(new CommonStrategyJob(userPartition, "RegPrefAtt" + i, StrategyType.CF_Region_Network_All_PrefAtt, 43));
//			jobs.add(new CommonStrategyJob(userPartition, "LocOverlap" + i, StrategyType.CF_Location_Network_All_Overlap, 44));
//			jobs.add(new CommonStrategyJob(userPartition, "LocPrefAtt" + i, StrategyType.CF_Location_Network_All_PrefAtt, 45));
			
//			jobs.add(new CommonStrategyJob(userPartition, "CommonRegion" + i, StrategyType.CF_Loc_Common_Regions, 46));
//			jobs.add(new CommonStrategyJob(userPartition, "CommonRegionJacc" + i, StrategyType.CF_Loc_Common_Regions_Jaccard, 47));
			
//			jobs.add(new CommonStrategyJob(userPartition, "SharedRegTotal" + i, StrategyType.CF_Loc_Shared_Regions_Total, 48));
//			jobs.add(new CommonStrategyJob(userPartition, "SharedRegJaccard" + i, StrategyType.CF_Loc_Shared_Regions_Jaccard, 49));
//			jobs.add(new CommonStrategyJob(userPartition, "SharedRegCommon" + i, StrategyType.CF_Loc_Shared_Regions_Common, 50));
//			jobs.add(new CommonStrategyJob(userPartition, "SellerTotal" + i, StrategyType.CF_Market_Seller_Total, 51));
//			jobs.add(new CommonStrategyJob(userPartition, "InterestTotal" + i, StrategyType.CF_Soc_Interests_Total, 52));
//			jobs.add(new CommonStrategyJob(userPartition, "GroupTotal" + i, StrategyType.CF_Soc_Group_Total, 53));
//			jobs.add(new CommonStrategyJob(userPartition, "PicksTotal" + i, StrategyType.CF_Loc_Picks_Total, 54));
//			jobs.add(new CommonStrategyJob(userPartition, "RegionTotal" + i, StrategyType.CF_Loc_Total_Regions, 55));
			
//			jobs.add(new CommonStrategyJob(userPartition, "DaysSeen" + i, StrategyType.CF_Loc_Days_Seen_In_Region, 56));
//			jobs.add(new CommonStrategyJob(userPartition, "PhysDistance" + i, StrategyType.CF_Loc_Physical_Distance_in_Region, 57));
			jobs.add(new CommonStrategyJob(userPartition, "PhysDistance3D" + i, StrategyType.CF_Loc_Physical_Distance_3D_in_Region, 58));

			//			jobs.add(new CF_C_UB_Soc_MP_Job(userPartition, "all_" + i, 0));
			
//			jobs.add(new CF_CFcat_C_Job(userPartition, "all_" + i));
//			jobs.add(new UB_InterestsGroups_Social_Job(userPartition, "all_" + i));
//			
//			jobs.add(new MP_Job(userPartition, "mp_" + i));
//			jobs.add(new CF_Job(userPartition,"job_" + i));
//			jobs.add(new CF_Social_Job(userPartition, "soc_int_job_" + i));
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
				MetricsExporter mCalc = metrics.get(n-1);
				mCalc.exportCalculatedMetricsAverage(users.size());
			}
	    }
	    
	    long totalTime = System.nanoTime() - startTime;
	    System.out.println("Finished all threads in " + (totalTime / 1000000000) + " seconds");
	}

	private void initEvaluation() {
//		RecommenderEvaluator recommenderEval = new RecommenderEvaluator();
//		DataFetcher.getReviewingUsers();
		
		users = DataFetcher.getReviewingUsers();

//		List<String> socialUsers = recommenderEval.getAllSocialUsers();
//		
//		socialUsers.retainAll(allUsers);
//		allUsers.removeAll(socialUsers);
//
//		System.out.println(allUsers.size());
//		System.out.println(socialUsers.size());
//		
//		Collections.shuffle(socialUsers);
//		Collections.shuffle(allUsers);
//
//		int toRemove = (int) (socialUsers.size() * 0.0);
//		
//		users = socialUsers.subList(0, socialUsers.size() - toRemove);
//		users.addAll(allUsers.subList(0, toRemove));
		
		System.out.println("User Size " + users.size());
	}
	
}
