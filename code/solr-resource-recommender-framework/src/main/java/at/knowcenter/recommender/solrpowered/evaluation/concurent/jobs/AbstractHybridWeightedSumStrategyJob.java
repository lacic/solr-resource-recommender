package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.SolrServer;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public abstract class AbstractHybridWeightedSumStrategyJob extends RecommenderEvaluator{

	protected abstract List<String> getUsers();
	
	protected void evaluate(ContentFilter cf, List<MetricsExporter> metricsCalcs, 
			Map<StrategyType, Double> strategyWeights) {
		int currentEvaluatingUser = 0;
		List<String> users = getUsers();
		int userSize = users.size();
		
		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			
			List<String> recommendations = new ArrayList<String>();

			final Map<String, Double> weightMap = new HashMap<String, Double>();

			for (StrategyType strategy : strategyWeights.keySet()) {
				Double strategyWeight = strategyWeights.get(strategy);
				
				List<String> strategyRecs = 
						getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(strategy));
				
				for (String recommendation : strategyRecs) {
					int positionScore = strategyRecs.size() - strategyRecs.indexOf(recommendation);
					
					Double recWeight = weightMap.get(recommendation);
					double itemWeight = positionScore * strategyWeight;
					
					if (recWeight == null) {
						recWeight = itemWeight;
					} else if (recWeight <itemWeight){
						recWeight = itemWeight;
					}
					weightMap.put(recommendation, recWeight);
				}
				
			}
			
			Comparator<String> interactionCountComparator = new Comparator<String>() {

				@Override
				public int compare(String a, String b) {
					if (weightMap.get(a) >= weightMap.get(b)) {
			            return -1;
			        } else {
			            return 1;
			        }
				}
				
			};
			
	        TreeMap<String,Double> sortedMap = new TreeMap<String,Double>(interactionCountComparator);
	        sortedMap.putAll(weightMap);
	        
	        for (String recommendedItem : sortedMap.keySet()) {
	        	recommendations.add(recommendedItem);
	        	if (recommendations.size() >= resultSize) {
	        		break;
	        	}
	        }
			
			long duaration = System.nanoTime() - getRecommStartTime;
			
			appendMetrics(metricsCalcs, userID, recommendations, duaration);

			currentEvaluatingUser++;
			System.out.println(jobDescription + ": Evaluation progress: " + ((currentEvaluatingUser) / (double)userSize) * 100 + " % done");
		}
		
	}

	private void appendMetrics(List<MetricsExporter> metricsCalcs, String userID, List<String> recommendations, long duaration) {
		for (int n = 1; n <= resultSize; n++) {
			MetricsExporter mCalc = metricsCalcs.get(n - 1);
			if (recommendations.size() > n) {
				appendMetrics(mCalc, n, userID, recommendations.subList(0, n),
						recommendStrategies.get(StrategyType.ContentBased).getAlreadyBoughtProducts());
			} else {
				appendMetrics(mCalc, n, userID, recommendations,
						recommendStrategies.get(StrategyType.ContentBased).getAlreadyBoughtProducts());
			}
			mCalc.appendDuaration(duaration);
		}
	}

}
