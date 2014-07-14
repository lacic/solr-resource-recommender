package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.hybrid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.AbstractStrategyJob;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;

public class CommonHybridStrategyJob extends AbstractHybridStrategyJob implements Callable<List<MetricsExporter>>{

	protected List<String> users;
	protected int orderNumber = 0;
	
	private static Map<StrategyType, Double> definedStrategyWeights;
	private Map<StrategyType, Double> evaluatingStrategyWeights;
	
	static {
		definedStrategyWeights = new HashMap<StrategyType, Double>();
		definedStrategyWeights.put(StrategyType.CF_Review, 								0.0197);
		definedStrategyWeights.put(StrategyType.CF_Market_Seller_Jaccard, 				0.0239);
		definedStrategyWeights.put(StrategyType.CF_Soc_Group_Jaccard, 					0.0077);
		definedStrategyWeights.put(StrategyType.CF_Soc_Interests_Jaccard, 				0.0008);
		definedStrategyWeights.put(StrategyType.CF_Social, 								0.0879);
		definedStrategyWeights.put(StrategyType.CF_Soc_Network_NeighOverlap, 			0.1710);
		definedStrategyWeights.put(StrategyType.CF_Loc_Picks_Jaccard, 					0.0045);
		definedStrategyWeights.put(StrategyType.CF_Loc_Common_Regions_Jaccard, 			0.0028);
		definedStrategyWeights.put(StrategyType.CF_Loc_Shared_Regions_Jaccard, 			0.0018);
		definedStrategyWeights.put(StrategyType.CF_Loc_Days_Seen_In_Region, 			0.0016);
		definedStrategyWeights.put(StrategyType.CF_Loc_Physical_Distance_in_Region, 	0.0025);
		definedStrategyWeights.put(StrategyType.CF_Region_Network_Coocurred_Jaccard, 	0.0034);
	}

	public CommonHybridStrategyJob(
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
		
		evaluate(cf, orderNumber, evaluatingStrategyWeights);
		
		return metricsCalcs;
	}

	@Override
	protected List<String> getUsers() {
		return users;
	}

}
