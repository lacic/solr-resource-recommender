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
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.hybrid.CommonTTWeightedSumHybridJob;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.hybrid.CF_CFcat_C_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.hybrid.UB_InterestsGroups_Social_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.marketplace.MP_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.marketplace.cb.C_Description_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.marketplace.cb.C_Name_Description_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.marketplace.cb.C_Name_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.marketplace.cf.CF_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.social.UB_CustomerGroups_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.social.UB_Interests_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.social.cb.SocialStream_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.social.cf.CF_Social_Comments_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.social.cf.CF_Social_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.social.cf.CF_Social_Likes_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.social.combined.CFPurchWithSocCommonNeighborhoodRecommender_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.social.combined.CFPurchWithSocCommonNeighborhoodReplacedRecommender_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.social.combined.CFPurchWithSocCommonNeighborhoodSummedRecommender_Job;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.cleaner.DataFetcher;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendService;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemService;
import at.knowcenter.recommender.solrpowered.services.impl.user.UserService;

public class TTSetEvaluator {

	public static void main(String... args) {
		TTSetEvaluator evaluator = new TTSetEvaluator();
		evaluator.evaluate();
	}
	
	
	private List<String> users;
	
	public void evaluate() {
		long startTime = System.nanoTime();
		
		initEvaluation();
		
		ExecutorService executor = Executors.newFixedThreadPool(29);
		
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

			/*
			jobs.add(new CommonTTStrategyJob(userPartition, "MP_"+ i, StrategyType.MostPopular_Review, 0));
			jobs.add(new CommonTTStrategyJob(userPartition, "CF_Rev_" + i, StrategyType.CF_Review, 1));
//			jobs.add(new CommonTTStrategyJob(userPartition, "Cat_" + i, StrategyType.CF_Categories, 3));

			jobs.add(new CommonTTStrategyJob(userPartition, "SellerCN" + i, StrategyType.CF_Market_Seller_CN, 2));
			jobs.add(new CommonTTStrategyJob(userPartition, "SellerJaccard" + i, StrategyType.CF_Market_Seller_Jaccard, 3));
			jobs.add(new CommonTTStrategyJob(userPartition, "SellerTotal" + i, StrategyType.CF_Market_Seller_Total, 4));
			
			jobs.add(new CommonTTStrategyJob(userPartition, "Cat_CN" + i, StrategyType.CF_Categories_CN, 5));
			jobs.add(new CommonTTStrategyJob(userPartition, "Cat_Jacc" + i, StrategyType.CF_Categories_Jacc, 6));
			jobs.add(new CommonTTStrategyJob(userPartition, "Cat_Tot" + i, StrategyType.CF_Categories_Total, 7));
			
			jobs.add(new UB_Interests_Job(userPartition, "interests_" + i));
			jobs.add(new CommonTTStrategyJob(userPartition, "InterestsJaccard" + i, StrategyType.CF_Soc_Interests_Jaccard, 8));
			jobs.add(new CommonTTStrategyJob(userPartition, "InterestTotal" + i, StrategyType.CF_Soc_Interests_Total, 9));
			
			jobs.add(new UB_CustomerGroups_Job(userPartition, "groups_" + i));
			jobs.add(new CommonTTStrategyJob(userPartition, "GroupJaccard" + i, StrategyType.CF_Soc_Group_Jaccard, 10));
			jobs.add(new CommonTTStrategyJob(userPartition, "GroupTotal" + i, StrategyType.CF_Soc_Group_Total, 11));

			jobs.add(new CommonTTStrategyJob(userPartition, "Soc", StrategyType.CF_Social, 12));

			jobs.add(new CommonTTStrategyJob(userPartition, "CommonN" + i, StrategyType.CF_Soc_Network_CN, 13));
			jobs.add(new CommonTTStrategyJob(userPartition, "Jaccard" + i, StrategyType.CF_Soc_Network_Jaccard, 14));
			jobs.add(new CommonTTStrategyJob(userPartition, "NeighOv" + i, StrategyType.CF_Soc_Network_NeighOverlap, 15));
			jobs.add(new CommonTTStrategyJob(userPartition, "AdamicAdar" + i, StrategyType.CF_Soc_Network_AdamicAdar, 16));
			jobs.add(new CommonTTStrategyJob(userPartition, "PrefAttach" + i, StrategyType.CF_Soc_Network_PrefAttachment, 17));
			

			jobs.add(new CommonTTStrategyJob(userPartition, "Picks" + i, StrategyType.CF_Loc_Picks_CN, 18));
			jobs.add(new CommonTTStrategyJob(userPartition, "PicksJaccard" + i, StrategyType.CF_Loc_Picks_Jaccard, 19));
			jobs.add(new CommonTTStrategyJob(userPartition, "PicksTotal" + i, StrategyType.CF_Loc_Picks_Total, 20));

			jobs.add(new CommonTTStrategyJob(userPartition, "SharedRegTotal" + i, StrategyType.CF_Loc_Shared_Regions_Total, 21));
			jobs.add(new CommonTTStrategyJob(userPartition, "SharedRegJaccard" + i, StrategyType.CF_Loc_Shared_Regions_Jaccard, 22));
			jobs.add(new CommonTTStrategyJob(userPartition, "SharedRegCommon" + i, StrategyType.CF_Loc_Shared_Regions_Common, 23));

			jobs.add(new CommonTTStrategyJob(userPartition, "CommonRegion" + i, StrategyType.CF_Loc_Common_Regions, 24));
			jobs.add(new CommonTTStrategyJob(userPartition, "CommonRegionJacc" + i, StrategyType.CF_Loc_Common_Regions_Jaccard, 25));
			jobs.add(new CommonTTStrategyJob(userPartition, "RegionTotal" + i, StrategyType.CF_Loc_Total_Regions, 26));

			jobs.add(new CommonTTStrategyJob(userPartition, "RegCoCN" + i, StrategyType.CF_Region_Network_Coocurred_CN, 27));
			jobs.add(new CommonTTStrategyJob(userPartition, "RegCoJaccard" + i, StrategyType.CF_Region_Network_Coocurred_Jaccard, 28));
			jobs.add(new CommonTTStrategyJob(userPartition, "RegCoAdar" + i, StrategyType.CF_Region_Network_Coocurred_Adar, 29));
			jobs.add(new CommonTTStrategyJob(userPartition, "RegCoOverlap" + i, StrategyType.CF_Region_Network_Coocurred_Overlap, 30));
			jobs.add(new CommonTTStrategyJob(userPartition, "RegCoPrefAtt" + i, StrategyType.CF_Region_Network_Coocurred_PrefAtt, 31));
			*/

//			jobs.add(new CommonTTStrategyJob(userPartition, "Wallposts_" + i, StrategyType.WallPostInteraction, 6));
//			jobs.add(new CommonTTStrategyJob(userPartition, "Snapshots_" + i, StrategyType.SnapshotInteraction, 7));

//
//			jobs.add(new CommonTTStrategyJob(userPartition, "GroupOverlap" + i, StrategyType.CF_Soc_Group_Overlap, 15));
//			jobs.add(new CommonTTStrategyJob(userPartition, "GroupAdar" + i, StrategyType.CF_Soc_Group_AdemicAdar, 16));
//			jobs.add(new CommonTTStrategyJob(userPartition, "GroupPrefAttach" + i, StrategyType.CF_Soc_Group_PrefAttach, 17));
//
//			jobs.add(new CommonTTStrategyJob(userPartition, "InterestsOverlap" + i, StrategyType.CF_Soc_Interests_Overlap, 19));
//			jobs.add(new CommonTTStrategyJob(userPartition, "InterestsAdar" + i, StrategyType.CF_Soc_Interests_AdemicAdar, 20));
//			jobs.add(new CommonTTStrategyJob(userPartition, "InterestsPrefAttach" + i, StrategyType.CF_Soc_Interests_PrefAttach, 21));
			
//			jobs.add(new CommonTTStrategyJob(userPartition, "SellerPrefAtt" + i, StrategyType.CF_Market_Seller_PrefAtt, 22));
//			jobs.add(new CommonTTStrategyJob(userPartition, "SellerOverlap" + i, StrategyType.CF_Market_Seller_Overlap, 23));
//			jobs.add(new CommonTTStrategyJob(userPartition, "SellerAdar" + i, StrategyType.CF_Market_Seller_AdamicAdar, 24));
//			jobs.add(new CommonTTStrategyJob(userPartition, "SellerSummed" + i, StrategyType.CF_Market_Seller_Summed, 25));

//			jobs.add(new CommonTTStrategyJob(userPartition, "LocCN" + i, StrategyType.CF_Location_Network_All_CN, 28));
//			jobs.add(new CommonTTStrategyJob(userPartition, "LocCoCN" + i, StrategyType.CF_Location_Network_Coocured_CN, 29));
//			jobs.add(new CommonTTStrategyJob(userPartition, "RegCN" + i, StrategyType.CF_Region_Network_All_CN, 30));
//
//			jobs.add(new CommonTTStrategyJob(userPartition, "LocJaccard" + i, StrategyType.CF_Location_Network_All_Jaccard, 32));
//			jobs.add(new CommonTTStrategyJob(userPartition, "LocCoJaccard" + i, StrategyType.CF_Location_Network_Coocured_Jaccard, 33));
//			jobs.add(new CommonTTStrategyJob(userPartition, "RegJaccard" + i, StrategyType.CF_Region_Network_All_Jaccard, 34));
//			
//			jobs.add(new CommonTTStrategyJob(userPartition, "LocAdar" + i, StrategyType.CF_Location_Network_All_Adar, 36));
//			jobs.add(new CommonTTStrategyJob(userPartition, "LocCoAdar" + i, StrategyType.CF_Location_Network_Coocured_Adar, 37));
//			jobs.add(new CommonTTStrategyJob(userPartition, "RegAdar" + i, StrategyType.CF_Region_Network_All_Adar, 38));
//			
//			jobs.add(new CommonTTStrategyJob(userPartition, "RegOverlap" + i, StrategyType.CF_Region_Network_All_Overlap, 42));
//			jobs.add(new CommonTTStrategyJob(userPartition, "RegPrefAtt" + i, StrategyType.CF_Region_Network_All_PrefAtt, 43));
//			jobs.add(new CommonTTStrategyJob(userPartition, "LocOverlap" + i, StrategyType.CF_Location_Network_All_Overlap, 44));
//			jobs.add(new CommonTTStrategyJob(userPartition, "LocPrefAtt" + i, StrategyType.CF_Location_Network_All_PrefAtt, 45));

			
			// Market Hybrid
			jobs.add(new CommonTTWeightedSumHybridJob(userPartition, "WS_Hyb_Market", 59, new StrategyType[]{
					StrategyType.CF_Market_Seller_Jaccard, StrategyType.CF_Market_Seller_CN, 
					StrategyType.CF_Review, StrategyType.CF_Market_Seller_Total,
					StrategyType.CF_Categories_CN, StrategyType.CF_Categories_Jacc, StrategyType.CF_Categories_Total}));
			
//			jobs.add(new CommonMixedHybridStrategyJob(userPartition, "Mixed_Hyb_Market_" + i, 72, new StrategyType[]{
//					StrategyType.CF_Market_Seller_Jaccard, StrategyType.CF_Market_Seller_CN, 
//					StrategyType.CF_Review, StrategyType.CF_Market_Seller_Total,
//					StrategyType.CF_Categories_CN,StrategyType.CF_Categories_Jacc,StrategyType.CF_Categories_Total}));
//			
//			jobs.add(new CommonHybridCrossSourceStrategyJob(userPartition, "CS_Hyb_Market", 83, new StrategyType[]{
//					StrategyType.CF_Market_Seller_Jaccard, StrategyType.CF_Market_Seller_CN, 
//					StrategyType.CF_Review, StrategyType.CF_Market_Seller_Total,
//					StrategyType.CF_Categories_CN, StrategyType.CF_Categories_Jacc, StrategyType.CF_Categories_Total}));
//			
//			jobs.add(new CommonMixedHybridStrategyJob(userPartition, "Mixed_Hyb_Market", 72, new StrategyType[]{
//					StrategyType.CF_Market_Seller_Jaccard, StrategyType.CF_Market_Seller_CN, StrategyType.CF_Review, StrategyType.CF_Market_Seller_Total}));
//			
//			jobs.add(new CommonHybridCrossSourceStrategyJob(userPartition, "CS_Hyb_Market", 83, new StrategyType[]{
//					StrategyType.CF_Market_Seller_Jaccard, StrategyType.CF_Market_Seller_CN, 
//					StrategyType.CF_Review, StrategyType.CF_Market_Seller_Total,
//					StrategyType.CF_Categories_CN, StrategyType.CF_Categories_Jacc, StrategyType.CF_Categories_Total}));
			
//			// Social Content Hybrid
//			
			jobs.add(new CommonTTWeightedSumHybridJob(userPartition, "WS_Hyb_Social_Content", 61, new StrategyType[]{
					StrategyType.CF_Soc_Group_Jaccard, StrategyType.UB_WithOutMLTGroups, StrategyType.CF_Soc_Group_Total,
					StrategyType.CF_Soc_Interests_Jaccard, StrategyType.UB_WithOutMLTInterests, StrategyType.CF_Soc_Interests_Total}));
//			
//			jobs.add(new CommonMixedHybridStrategyJob(userPartition, "Mixed_Hyb_Social_Content", 72, new StrategyType[]{
//					StrategyType.CF_Soc_Group_Jaccard, StrategyType.UB_WithOutMLTGroups, StrategyType.CF_Soc_Group_Total,
//					StrategyType.CF_Soc_Interests_Jaccard, StrategyType.UB_WithOutMLTInterests, StrategyType.CF_Soc_Interests_Total}));
//			
//			jobs.add(new CommonHybridCrossSourceStrategyJob(userPartition, "CS_Hyb_Social_Content", 84, new StrategyType[]{
//					StrategyType.CF_Soc_Group_Jaccard, StrategyType.UB_WithOutMLTGroups, StrategyType.CF_Soc_Group_Total,
//					StrategyType.CF_Soc_Interests_Jaccard, StrategyType.UB_WithOutMLTInterests, StrategyType.CF_Soc_Interests_Total}));
//			
//			// Social Network Hybrid
//			
			jobs.add(new CommonTTWeightedSumHybridJob(userPartition, "WS_Hyb_Social_Network", 62, new StrategyType[]{
					StrategyType.CF_Soc_Network_NeighOverlap, StrategyType.CF_Soc_Network_Jaccard, StrategyType.CF_Soc_Network_AdamicAdar, StrategyType.CF_Soc_Network_CN,
					StrategyType.CF_Social, StrategyType.CF_Soc_Network_PrefAttachment, StrategyType.CF_Social_Likes, StrategyType.CF_Social_Comments, StrategyType.WallPostInteraction}));
//			
//			jobs.add(new CommonMixedHybridStrategyJob(userPartition, "Mixed_Hyb_Social_Network", 73, new StrategyType[]{
//					StrategyType.CF_Soc_Network_NeighOverlap, StrategyType.CF_Soc_Network_Jaccard, StrategyType.CF_Soc_Network_AdamicAdar, StrategyType.CF_Soc_Network_CN,
//					StrategyType.CF_Social, StrategyType.CF_Soc_Network_PrefAttachment, StrategyType.CF_Social_Likes, StrategyType.CF_Social_Comments, StrategyType.WallPostInteraction}));
//			
//			jobs.add(new CommonHybridCrossSourceStrategyJob(userPartition, "CS_Hyb_Social_Network", 85, new StrategyType[]{
//					StrategyType.CF_Soc_Network_NeighOverlap, StrategyType.CF_Soc_Network_Jaccard, StrategyType.CF_Soc_Network_AdamicAdar, StrategyType.CF_Soc_Network_CN,
//					StrategyType.CF_Social, StrategyType.CF_Soc_Network_PrefAttachment, StrategyType.CF_Social_Likes, StrategyType.CF_Social_Comments, StrategyType.WallPostInteraction}));
//			
//			// Social Hybrid
//			
			jobs.add(new CommonTTWeightedSumHybridJob(userPartition, "WS_Hyb_Social", 66, new StrategyType[]{
					StrategyType.CF_Soc_Network_NeighOverlap, StrategyType.CF_Soc_Network_Jaccard, StrategyType.CF_Soc_Network_AdamicAdar, StrategyType.CF_Soc_Network_CN,
					StrategyType.CF_Social, StrategyType.CF_Soc_Group_Jaccard, StrategyType.UB_WithOutMLTGroups, StrategyType.CF_Soc_Group_Total,
					StrategyType.CF_Soc_Network_PrefAttachment, StrategyType.CF_Social_Likes, StrategyType.CF_Social_Comments, StrategyType.WallPostInteraction,
					StrategyType.CF_Soc_Interests_Jaccard, StrategyType.UB_WithOutMLTInterests, StrategyType.CF_Soc_Interests_Total}));
//			
//			jobs.add(new CommonMixedHybridStrategyJob(userPartition, "Mixed_Hyb_Social", 77, new StrategyType[]{
//					StrategyType.CF_Soc_Network_NeighOverlap, StrategyType.CF_Soc_Network_Jaccard, StrategyType.CF_Soc_Network_AdamicAdar, StrategyType.CF_Soc_Network_CN,
//					StrategyType.CF_Social, StrategyType.CF_Soc_Group_Jaccard, StrategyType.UB_WithOutMLTGroups, StrategyType.CF_Soc_Group_Total,
//					StrategyType.CF_Soc_Network_PrefAttachment, StrategyType.CF_Social_Likes, StrategyType.CF_Social_Comments, StrategyType.WallPostInteraction,
//					StrategyType.CF_Soc_Interests_Jaccard, StrategyType.UB_WithOutMLTInterests, StrategyType.CF_Soc_Interests_Total}));
//			
//			jobs.add(new CommonHybridCrossSourceStrategyJob(userPartition, "CS_Hyb_Social", 89, new StrategyType[]{
//					StrategyType.CF_Soc_Network_NeighOverlap, StrategyType.CF_Soc_Network_Jaccard, StrategyType.CF_Soc_Network_AdamicAdar, StrategyType.CF_Soc_Network_CN,
//					StrategyType.CF_Social, StrategyType.CF_Soc_Group_Jaccard, StrategyType.UB_WithOutMLTGroups, StrategyType.CF_Soc_Group_Total,
//					StrategyType.CF_Soc_Network_PrefAttachment, StrategyType.CF_Social_Likes, StrategyType.CF_Social_Comments, StrategyType.WallPostInteraction,
//					StrategyType.CF_Soc_Interests_Jaccard, StrategyType.UB_WithOutMLTInterests, StrategyType.CF_Soc_Interests_Total}));
			
			
			// Location Content Hybrid
			
//			jobs.add(new CommonHybridWeightedSumStrategyJob(userPartition, "WS_Hyb_Location_Regions", 63, new StrategyType[]{
//					StrategyType.CF_Loc_Picks_Jaccard, StrategyType.CF_Loc_Common_Regions_Jaccard, StrategyType.CF_Loc_Shared_Regions_Jaccard}));
//			
//			jobs.add(new CommonHybridWeightedSumStrategyJob(userPartition, "WS_Hyb_Location_DistanceSeen", 64, new StrategyType[]{
//					StrategyType.CF_Loc_Days_Seen_In_Region, StrategyType.CF_Loc_Physical_Distance_in_Region}));
			
			jobs.add(new CommonTTWeightedSumHybridJob(userPartition, "WS_Hyb_Location_Content", 65, new StrategyType[]{
					StrategyType.CF_Loc_Picks_CN, StrategyType.CF_Loc_Picks_Jaccard, StrategyType.CF_Loc_Picks_Total,
					StrategyType.CF_Loc_Common_Regions, StrategyType.CF_Loc_Common_Regions_Jaccard, StrategyType.CF_Loc_Physical_Distance_in_Region,
					StrategyType.CF_Loc_Shared_Regions_Jaccard, StrategyType.CF_Loc_Days_Seen_In_Region, StrategyType.CF_Loc_Total_Regions,
					StrategyType.CF_Loc_Shared_Regions_Common, StrategyType.CF_Loc_Shared_Regions_Total}));
//			
//			jobs.add(new CommonMixedHybridStrategyJob(userPartition, "Mixed_Hyb_Location_Content", 76, new StrategyType[]{
//					StrategyType.CF_Loc_Picks_CN, StrategyType.CF_Loc_Picks_Jaccard, StrategyType.CF_Loc_Picks_Total,
//					StrategyType.CF_Loc_Common_Regions, StrategyType.CF_Loc_Common_Regions_Jaccard, StrategyType.CF_Loc_Physical_Distance_in_Region,
//					StrategyType.CF_Loc_Shared_Regions_Jaccard, StrategyType.CF_Loc_Days_Seen_In_Region, StrategyType.CF_Loc_Total_Regions,
//					StrategyType.CF_Loc_Shared_Regions_Common, StrategyType.CF_Loc_Shared_Regions_Total}));
//			
//			
//			jobs.add(new CommonHybridCrossSourceStrategyJob(userPartition, "CS_Hyb_Location_Content", 78, new StrategyType[]{
//					StrategyType.CF_Loc_Picks_CN, StrategyType.CF_Loc_Picks_Jaccard, StrategyType.CF_Loc_Picks_Total,
//					StrategyType.CF_Loc_Common_Regions, StrategyType.CF_Loc_Common_Regions_Jaccard, StrategyType.CF_Loc_Physical_Distance_in_Region,
//					StrategyType.CF_Loc_Shared_Regions_Jaccard, StrategyType.CF_Loc_Days_Seen_In_Region, StrategyType.CF_Loc_Total_Regions,
//					StrategyType.CF_Loc_Shared_Regions_Common, StrategyType.CF_Loc_Shared_Regions_Total}));
//			
//			// Location Network Hybrid
//			
			jobs.add(new CommonTTWeightedSumHybridJob(userPartition, "WS_Hyb_Location_Network", 67, new StrategyType[]{
					StrategyType.CF_Region_Network_Coocurred_Jaccard, StrategyType.CF_Region_Network_Coocurred_Overlap, StrategyType.CF_Region_Network_Coocurred_CN, 
					StrategyType.CF_Region_Network_Coocurred_PrefAtt, StrategyType.CF_Region_Network_Coocurred_Adar}));
//			
//			jobs.add(new CommonMixedHybridStrategyJob(userPartition, "Mixed_Hyb_Location_Network", 67, new StrategyType[]{
//					StrategyType.CF_Region_Network_Coocurred_Jaccard, StrategyType.CF_Region_Network_Coocurred_Overlap, StrategyType.CF_Region_Network_Coocurred_CN, 
//					StrategyType.CF_Region_Network_Coocurred_PrefAtt, StrategyType.CF_Region_Network_Coocurred_Adar}));
//			
//			jobs.add(new CommonHybridCrossSourceStrategyJob(userPartition, "CS_Hyb_Location_Network", 67, new StrategyType[]{
//					StrategyType.CF_Region_Network_Coocurred_Jaccard, StrategyType.CF_Region_Network_Coocurred_Overlap, StrategyType.CF_Region_Network_Coocurred_CN, 
//					StrategyType.CF_Region_Network_Coocurred_PrefAtt, StrategyType.CF_Region_Network_Coocurred_Adar}));
			
			
			// Location Hybrid
			
			
			jobs.add(new CommonTTWeightedSumHybridJob(userPartition, "WS_Hyb_Location", 67, new StrategyType[]{
					StrategyType.CF_Loc_Picks_CN, StrategyType.CF_Loc_Picks_Jaccard, StrategyType.CF_Loc_Picks_Total,
					StrategyType.CF_Region_Network_Coocurred_Jaccard, StrategyType.CF_Loc_Common_Regions, StrategyType.CF_Loc_Common_Regions_Jaccard, 
					StrategyType.CF_Loc_Physical_Distance_in_Region, StrategyType.CF_Region_Network_Coocurred_Overlap, StrategyType.CF_Region_Network_Coocurred_CN,
					StrategyType.CF_Loc_Shared_Regions_Jaccard, StrategyType.CF_Loc_Days_Seen_In_Region, StrategyType.CF_Loc_Total_Regions,
					StrategyType.CF_Loc_Shared_Regions_Common, StrategyType.CF_Loc_Shared_Regions_Total,
					StrategyType.CF_Region_Network_Coocurred_PrefAtt, StrategyType.CF_Region_Network_Coocurred_Adar}));
//			
//			jobs.add(new CommonMixedHybridStrategyJob(userPartition, "Mixed_Hyb_Location", 88, new StrategyType[]{
//					StrategyType.CF_Loc_Picks_CN, StrategyType.CF_Loc_Picks_Jaccard, StrategyType.CF_Loc_Picks_Total,
//					StrategyType.CF_Region_Network_Coocurred_Jaccard, StrategyType.CF_Loc_Common_Regions, StrategyType.CF_Loc_Common_Regions_Jaccard, 
//					StrategyType.CF_Loc_Physical_Distance_in_Region, StrategyType.CF_Region_Network_Coocurred_Overlap, StrategyType.CF_Region_Network_Coocurred_CN,
//					StrategyType.CF_Loc_Shared_Regions_Jaccard, StrategyType.CF_Loc_Days_Seen_In_Region, StrategyType.CF_Loc_Total_Regions,
//					StrategyType.CF_Loc_Shared_Regions_Common, StrategyType.CF_Loc_Shared_Regions_Total,
//					StrategyType.CF_Region_Network_Coocurred_PrefAtt, StrategyType.CF_Region_Network_Coocurred_Adar}));
//			
//			jobs.add(new CommonHybridCrossSourceStrategyJob(userPartition, "CS_Hyb_Location", 90, new StrategyType[]{
//					StrategyType.CF_Loc_Picks_CN, StrategyType.CF_Loc_Picks_Jaccard, StrategyType.CF_Loc_Picks_Total,
//					StrategyType.CF_Region_Network_Coocurred_Jaccard, StrategyType.CF_Loc_Common_Regions, StrategyType.CF_Loc_Common_Regions_Jaccard, 
//					StrategyType.CF_Loc_Physical_Distance_in_Region, StrategyType.CF_Region_Network_Coocurred_Overlap, StrategyType.CF_Region_Network_Coocurred_CN,
//					StrategyType.CF_Loc_Shared_Regions_Jaccard, StrategyType.CF_Loc_Days_Seen_In_Region, StrategyType.CF_Loc_Total_Regions,
//					StrategyType.CF_Loc_Shared_Regions_Common, StrategyType.CF_Loc_Shared_Regions_Total,
//					StrategyType.CF_Region_Network_Coocurred_PrefAtt, StrategyType.CF_Region_Network_Coocurred_Adar}));
//			
//			
			
			// All
			/*
			jobs.add(new CommonHybridWeightedSumStrategyJob(userPartition, "WS_Hyb_Social_Location_Market", 71, new StrategyType[]{
					StrategyType.CF_Soc_Network_NeighOverlap, StrategyType.CF_Soc_Network_Jaccard, StrategyType.CF_Soc_Network_AdamicAdar, 
					StrategyType.CF_Soc_Network_CN, StrategyType.CF_Social, StrategyType.CF_Market_Seller_Jaccard, 
					StrategyType.CF_Market_Seller_CN, StrategyType.CF_Review, StrategyType.CF_Market_Seller_Total,
					StrategyType.CF_Soc_Group_Jaccard, StrategyType.UB_WithOutMLTGroups, StrategyType.CF_Soc_Group_Total, 
					StrategyType.CF_Soc_Network_PrefAttachment, StrategyType.CF_Social_Likes, StrategyType.CF_Social_Comments, StrategyType.WallPostInteraction,
					StrategyType.CF_Soc_Interests_Jaccard, StrategyType.UB_WithOutMLTInterests, StrategyType.CF_Soc_Interests_Total,
					StrategyType.CF_Loc_Picks_CN, StrategyType.CF_Loc_Picks_Jaccard, StrategyType.CF_Loc_Picks_Total,
					StrategyType.CF_Region_Network_Coocurred_Jaccard, StrategyType.CF_Loc_Common_Regions, StrategyType.CF_Loc_Common_Regions_Jaccard, 
					StrategyType.CF_Loc_Physical_Distance_in_Region, StrategyType.CF_Region_Network_Coocurred_Overlap, StrategyType.CF_Region_Network_Coocurred_CN,
					StrategyType.CF_Loc_Shared_Regions_Jaccard, StrategyType.CF_Loc_Days_Seen_In_Region, StrategyType.CF_Loc_Total_Regions,
					StrategyType.CF_Loc_Shared_Regions_Common, StrategyType.CF_Loc_Shared_Regions_Total,
					StrategyType.CF_Region_Network_Coocurred_PrefAtt, StrategyType.CF_Region_Network_Coocurred_Adar,
					StrategyType.CF_Categories_CN, StrategyType.CF_Categories_Jacc, StrategyType.CF_Categories_Total}));
			
			jobs.add(new CommonMixedHybridStrategyJob(userPartition, "Mixed_Hyb_Social_Location_Market", 82, new StrategyType[]{
					StrategyType.CF_Soc_Network_NeighOverlap, StrategyType.CF_Soc_Network_Jaccard, StrategyType.CF_Soc_Network_AdamicAdar, 
					StrategyType.CF_Soc_Network_CN, StrategyType.CF_Social, StrategyType.CF_Market_Seller_Jaccard, 
					StrategyType.CF_Market_Seller_CN, StrategyType.CF_Review, StrategyType.CF_Market_Seller_Total,
					StrategyType.CF_Soc_Group_Jaccard, StrategyType.UB_WithOutMLTGroups, StrategyType.CF_Soc_Group_Total, 
					StrategyType.CF_Soc_Network_PrefAttachment, StrategyType.CF_Social_Likes, StrategyType.CF_Social_Comments, StrategyType.WallPostInteraction,
					StrategyType.CF_Soc_Interests_Jaccard, StrategyType.UB_WithOutMLTInterests, StrategyType.CF_Soc_Interests_Total,
					StrategyType.CF_Loc_Picks_CN, StrategyType.CF_Loc_Picks_Jaccard, StrategyType.CF_Loc_Picks_Total,
					StrategyType.CF_Region_Network_Coocurred_Jaccard, StrategyType.CF_Loc_Common_Regions, StrategyType.CF_Loc_Common_Regions_Jaccard, 
					StrategyType.CF_Loc_Physical_Distance_in_Region, StrategyType.CF_Region_Network_Coocurred_Overlap, StrategyType.CF_Region_Network_Coocurred_CN,
					StrategyType.CF_Loc_Shared_Regions_Jaccard, StrategyType.CF_Loc_Days_Seen_In_Region, StrategyType.CF_Loc_Total_Regions,
					StrategyType.CF_Loc_Shared_Regions_Common, StrategyType.CF_Loc_Shared_Regions_Total,
					StrategyType.CF_Region_Network_Coocurred_PrefAtt, StrategyType.CF_Region_Network_Coocurred_Adar}));
			
			jobs.add(new CommonHybridCrossSourceStrategyJob(userPartition, "CS_Hyb_Social_Location_Market", 94, new StrategyType[]{
					StrategyType.CF_Soc_Network_NeighOverlap, StrategyType.CF_Soc_Network_Jaccard, StrategyType.CF_Soc_Network_AdamicAdar, 
					StrategyType.CF_Soc_Network_CN, StrategyType.CF_Social, StrategyType.CF_Market_Seller_Jaccard, 
					StrategyType.CF_Market_Seller_CN, StrategyType.CF_Review, StrategyType.CF_Market_Seller_Total,
					StrategyType.CF_Soc_Group_Jaccard, StrategyType.UB_WithOutMLTGroups, StrategyType.CF_Soc_Group_Total, 
					StrategyType.CF_Soc_Network_PrefAttachment, StrategyType.CF_Social_Likes, StrategyType.CF_Social_Comments, StrategyType.WallPostInteraction,
					StrategyType.CF_Soc_Interests_Jaccard, StrategyType.UB_WithOutMLTInterests, StrategyType.CF_Soc_Interests_Total,
					StrategyType.CF_Loc_Picks_CN, StrategyType.CF_Loc_Picks_Jaccard, StrategyType.CF_Loc_Picks_Total,
					StrategyType.CF_Region_Network_Coocurred_Jaccard, StrategyType.CF_Loc_Common_Regions, StrategyType.CF_Loc_Common_Regions_Jaccard, 
					StrategyType.CF_Loc_Physical_Distance_in_Region, StrategyType.CF_Region_Network_Coocurred_Overlap, StrategyType.CF_Region_Network_Coocurred_CN,
					StrategyType.CF_Loc_Shared_Regions_Jaccard, StrategyType.CF_Loc_Days_Seen_In_Region, StrategyType.CF_Loc_Total_Regions,
					StrategyType.CF_Loc_Shared_Regions_Common, StrategyType.CF_Loc_Shared_Regions_Total,
					StrategyType.CF_Region_Network_Coocurred_PrefAtt, StrategyType.CF_Region_Network_Coocurred_Adar,
					StrategyType.CF_Categories_CN, StrategyType.CF_Categories_Jacc, StrategyType.CF_Categories_Total}));
			*/
//			jobs.add(new CommonHybridWeightedSumStrategyJob(userPartition, "WS_Hyb_Social_Location_Market_Best3", 95, new StrategyType[]{
//					StrategyType.CF_Soc_Network_NeighOverlap, StrategyType.CF_Loc_Picks_Jaccard,StrategyType.CF_Market_Seller_Jaccard}));
			
//			jobs.add(new CommonMixedHybridStrategyJob(userPartition, "Mixed_Hyb_Social_Location_Market_Best3", 96, new StrategyType[]{
//			StrategyType.CF_Soc_Network_NeighOverlap, StrategyType.CF_Loc_Picks_Jaccard,StrategyType.CF_Market_Seller_Jaccard}));
	
//			jobs.add(new CommonHybridCrossSourceStrategyJob(userPartition, "CS_Hyb_Social_Location_Market_Best3", 97, new StrategyType[]{
//			StrategyType.CF_Soc_Network_NeighOverlap, StrategyType.CF_Loc_Picks_Jaccard,StrategyType.CF_Market_Seller_Jaccard}));
			
			// Mixed Hybrid
			
		
		
//			jobs.add(new CommonMixedHybridStrategyJob(userPartition, "Mixed_Hyb_Location_Regions", 74, new StrategyType[]{
//					StrategyType.CF_Loc_Picks_Jaccard, StrategyType.CF_Loc_Common_Regions_Jaccard, StrategyType.CF_Loc_Shared_Regions_Jaccard}));
//			
//			jobs.add(new CommonMixedHybridStrategyJob(userPartition, "Mixed_Hyb_Location_DistanceSeen", 75, new StrategyType[]{
//					StrategyType.CF_Loc_Days_Seen_In_Region, StrategyType.CF_Loc_Physical_Distance_in_Region}));
			
			
			

			// Cross-source Hybrid
			
			
			
//			jobs.add(new CommonHybridCrossSourceStrategyJob(userPartition, "CS_Hyb_Location_Regions", 86, new StrategyType[]{
//					StrategyType.CF_Loc_Picks_Jaccard, StrategyType.CF_Loc_Common_Regions_Jaccard, StrategyType.CF_Loc_Shared_Regions_Jaccard}));
//			
//			jobs.add(new CommonHybridCrossSourceStrategyJob(userPartition, "CS_Hyb_Location_DistanceSeen", 87, new StrategyType[]{
//					StrategyType.CF_Loc_Days_Seen_In_Region, StrategyType.CF_Loc_Physical_Distance_in_Region}));
			
			
			

			
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
	    	// for k = 5 and k = 10
	    	metrics.get(4).exportCalculatedMetricsFoAll(users.size(), 5);
	    	metrics.get(9).exportCalculatedMetricsFoAll(users.size(), 10);

	    	for (int n = 1; n <= 10; n++) {
				MetricsExporter mCalc = metrics.get(n-1);
				mCalc.exportCalculatedMetricsAverage(users.size());
			}
	    }
	    
	    long totalTime = System.nanoTime() - startTime;
	    System.out.println("Finished all threads in " + (totalTime / 1000000000) + " seconds");
	}

	private void initEvaluation() {
		users = DataFetcher.getReviewingUsers(SolrServiceContainer.getInstance().getTestReviewService());
		System.out.println("User Size " + users.size());
	}
	
}
