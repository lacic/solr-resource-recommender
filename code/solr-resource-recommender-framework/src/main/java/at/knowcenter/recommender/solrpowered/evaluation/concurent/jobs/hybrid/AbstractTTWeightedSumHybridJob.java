package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.hybrid;

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
import at.knowcenter.recommender.solrpowered.evaluation.TTEvalEngine;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public abstract class AbstractTTWeightedSumHybridJob extends TTEvalEngine{

	protected abstract List<String> getUsers();
	
	protected void evaluate(ContentFilter cf, List<MetricsExporter> metricsCalcs, 
			Map<StrategyType, Double> strategyWeights) {
		int currentEvaluatingUser = 0;
		List<String> users = getUsers();
		int userSize = users.size();
		
		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			
			List<String> recommendations = new ArrayList<String>();

			Map<String, Double> weightMap = new HashMap<String, Double>();

			for (StrategyType strategy : strategyWeights.keySet()) {
				Double strategyWeight = strategyWeights.get(strategy);
				List<String> strategyRecs = 
						getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(strategy));
				
				RecommendationQueryUtils.fillWeightedMap(weightMap, strategyRecs, strategyWeight);
			}
			
			List<String> sortedAndTrimedRecommendations = 
					RecommendationQueryUtils.extractCrossRankedProducts(weightMap);
			RecommendationQueryUtils.appendDifferentProducts(
					resultSize, recommendations, sortedAndTrimedRecommendations);

			
			
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
