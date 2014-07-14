package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.hybrid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.CategoryBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.CategoryJaccardBasedRec;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.AbstractStrategyJob;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;

public class CommonHybridCrossSourceStrategyJob extends AbstractHybridCrossSourceStrategyJob implements Callable<List<MetricsExporter>>{

	protected List<String> users;
	protected int orderNumber = 0;
	
	private static Map<StrategyType, Double> definedStrategyWeights;
	private Map<StrategyType, Double> evaluatingStrategyWeights;
	
	static {
		definedStrategyWeights = new HashMap<StrategyType, Double>();
		definedStrategyWeights.put(StrategyType.CF_Review, 								0.0197);
		definedStrategyWeights.put(StrategyType.CF_Market_Seller_Jaccard, 				0.0239);
		definedStrategyWeights.put(StrategyType.CF_Market_Seller_CN, 					0.0197);
		definedStrategyWeights.put(StrategyType.CF_Market_Seller_Total, 				0.0159);
		definedStrategyWeights.put(StrategyType.CF_Market_Seller_Total, 				0.0159);
		definedStrategyWeights.put(StrategyType.CF_Market_Seller_Total, 				0.0159);
		definedStrategyWeights.put(StrategyType.CF_Market_Seller_Total, 				0.0159);
		
		definedStrategyWeights.put(StrategyType.CF_Categories_CN, 						0.0034);
		definedStrategyWeights.put(StrategyType.CF_Categories_Jacc, 					0.0059);
		definedStrategyWeights.put(StrategyType.CF_Categories_Total, 					0.0017);
		
		// Pure Social Weights
//		definedStrategyWeights.put(StrategyType.CF_Review, 								0.0026);
//		definedStrategyWeights.put(StrategyType.CF_Market_Seller_Jaccard, 				0.0028);
//		definedStrategyWeights.put(StrategyType.CF_Market_Seller_CN, 					0.0028);
//		definedStrategyWeights.put(StrategyType.CF_Market_Seller_Total, 				0.0029);
		
		// Pure Location Weights
//		definedStrategyWeights.put(StrategyType.CF_Review, 								0.0028);
//		definedStrategyWeights.put(StrategyType.CF_Market_Seller_Jaccard, 				0.0033);
//		definedStrategyWeights.put(StrategyType.CF_Market_Seller_CN, 					0.0033);
//		definedStrategyWeights.put(StrategyType.CF_Market_Seller_Total, 				0.0031);
		
		definedStrategyWeights.put(StrategyType.CF_Soc_Group_Jaccard, 					0.0077);
		definedStrategyWeights.put(StrategyType.UB_WithOutMLTGroups, 					0.0074);
		definedStrategyWeights.put(StrategyType.CF_Soc_Group_Total, 					0.0018);
		definedStrategyWeights.put(StrategyType.CF_Soc_Interests_Jaccard, 				0.0008);
		definedStrategyWeights.put(StrategyType.UB_WithOutMLTInterests, 				0.0005);
		definedStrategyWeights.put(StrategyType.CF_Soc_Interests_Total, 				0.0011);
		
		definedStrategyWeights.put(StrategyType.CF_Social_Likes, 						0.0053);
		definedStrategyWeights.put(StrategyType.CF_Social_Comments, 					0.0063);
		definedStrategyWeights.put(StrategyType.WallPostInteraction, 					0.0066);
		definedStrategyWeights.put(StrategyType.CF_Social, 								0.0879);
		
		definedStrategyWeights.put(StrategyType.CF_Soc_Network_NeighOverlap, 			0.1710);
		definedStrategyWeights.put(StrategyType.CF_Soc_Network_Jaccard, 				0.1490);
		definedStrategyWeights.put(StrategyType.CF_Soc_Network_AdamicAdar, 				0.1171);
		definedStrategyWeights.put(StrategyType.CF_Soc_Network_CN, 						0.1366);
		definedStrategyWeights.put(StrategyType.CF_Soc_Network_PrefAttachment, 			0.0355);
		
		definedStrategyWeights.put(StrategyType.CF_Loc_Picks_CN, 						0.0045);
		definedStrategyWeights.put(StrategyType.CF_Loc_Picks_Jaccard, 					0.0045);
		definedStrategyWeights.put(StrategyType.CF_Loc_Picks_Total, 					0.0038);
		
		definedStrategyWeights.put(StrategyType.CF_Loc_Common_Regions, 					0.0030);
		definedStrategyWeights.put(StrategyType.CF_Loc_Common_Regions_Jaccard, 			0.0028);
		definedStrategyWeights.put(StrategyType.CF_Loc_Total_Regions, 					0.0015);
		
		definedStrategyWeights.put(StrategyType.CF_Loc_Shared_Regions_Common, 			0.0015);
		definedStrategyWeights.put(StrategyType.CF_Loc_Shared_Regions_Jaccard, 			0.0018);
		definedStrategyWeights.put(StrategyType.CF_Loc_Shared_Regions_Total, 			0.0007);
		
		definedStrategyWeights.put(StrategyType.CF_Loc_Days_Seen_In_Region, 			0.0016);
		definedStrategyWeights.put(StrategyType.CF_Loc_Physical_Distance_in_Region, 	0.0025);
		
		definedStrategyWeights.put(StrategyType.CF_Region_Network_Coocurred_CN, 		0.0023);
		definedStrategyWeights.put(StrategyType.CF_Region_Network_Coocurred_Jaccard, 	0.0034);
		definedStrategyWeights.put(StrategyType.CF_Region_Network_Coocurred_Overlap, 	0.0023);
		definedStrategyWeights.put(StrategyType.CF_Region_Network_Coocurred_Adar, 		0.0003);
		definedStrategyWeights.put(StrategyType.CF_Region_Network_Coocurred_PrefAtt, 	0.0005);
		
		// Pure Social Weights
		
//		definedStrategyWeights.put(StrategyType.CF_Loc_Picks_CN, 						0.0054);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Picks_Jaccard, 					0.0054);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Picks_Total, 					0.0055);
//		
//		definedStrategyWeights.put(StrategyType.CF_Loc_Common_Regions, 					0.0062);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Common_Regions_Jaccard, 			0.0062);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Total_Regions, 					0.0048);
//		
//		definedStrategyWeights.put(StrategyType.CF_Loc_Shared_Regions_Common, 			0.0046);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Shared_Regions_Jaccard, 			0.0052);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Shared_Regions_Total, 			0.0055);
//		
//		definedStrategyWeights.put(StrategyType.CF_Loc_Days_Seen_In_Region, 			0.0054);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Physical_Distance_in_Region, 	0.0111);
		
//		definedStrategyWeights.put(StrategyType.CF_Region_Network_Coocurred_CN, 		0.0134);
//		definedStrategyWeights.put(StrategyType.CF_Region_Network_Coocurred_Jaccard, 	0.0134);
//		definedStrategyWeights.put(StrategyType.CF_Region_Network_Coocurred_Overlap, 	0.0102);
//		definedStrategyWeights.put(StrategyType.CF_Region_Network_Coocurred_Adar, 		0.0102);
//		definedStrategyWeights.put(StrategyType.CF_Region_Network_Coocurred_PrefAtt, 	0.0100);
		
		// Pure Location Weights
//		definedStrategyWeights.put(StrategyType.CF_Loc_Picks_CN, 						0.0024);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Picks_Jaccard, 					0.0023);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Picks_Total, 					0.0024);
//		
//		definedStrategyWeights.put(StrategyType.CF_Loc_Common_Regions, 					0.0011);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Common_Regions_Jaccard, 			0.0011);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Total_Regions, 					0.0012);
//		
//		definedStrategyWeights.put(StrategyType.CF_Loc_Shared_Regions_Common, 			0.0036);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Shared_Regions_Jaccard, 			0.0037);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Shared_Regions_Total, 			0.0025);
//		
//		definedStrategyWeights.put(StrategyType.CF_Loc_Days_Seen_In_Region, 			0.0024);
//		definedStrategyWeights.put(StrategyType.CF_Loc_Physical_Distance_in_Region, 	0.0034);
	}

	public CommonHybridCrossSourceStrategyJob(
			List<String> users, String jobDescription, int evalOrder, StrategyType... strategies) {
		this.users = users;
		this.jobDescription = jobDescription;
		this.orderNumber = evalOrder;
		
		evaluatingStrategyWeights = new HashMap<StrategyType, Double>();
		for (StrategyType strategy : strategies) {
			evaluatingStrategyWeights.put(strategy, definedStrategyWeights.get(strategy));
		}
	}

	@Override
	public List<MetricsExporter> call() throws Exception {
		ContentFilter cf = new ContentFilter();
		List<MetricsExporter> metricsCalcs = initMetricCalcs(orderNumber + "_" + jobDescription);
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate(cf, metricsCalcs, evaluatingStrategyWeights);
		
		return metricsCalcs;
	}

	@Override
	protected List<String> getUsers() {
		return users;
	}

}
