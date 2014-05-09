package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.marketplace.cb;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.SolrServer;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public abstract class C_Strategy_Job extends RecommenderEvaluator{

	protected abstract List<String> getUsers();
	
	protected void evaluate_C_Weighted(ContentFilter cf, List<MetricsExporter> metricsCalcs, StrategyType strategy) {
		int currentEvaluatingUser = 0;
		List<String> users = getUsers();
		int userSize = users.size();
		
		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			
			List<String> recommendations = 
					getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(strategy));
			
			long duaration = System.nanoTime() - getRecommStartTime;
			
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

			currentEvaluatingUser++;
			System.out.println("C-" + strategy.name() + ": Evaluation progress: " + ((currentEvaluatingUser) / (double)userSize) * 100 + " % done");
		}
		
//
//		for (int n = 1; n <= resultSize; n++) {
//			MetricsExporter mCalc = metricsCalcs.get(n - 1);
//			mCalc.exportCalculatedMetricsAverage("7_C-" + strategy.name(), users.size());
//		}
	}

}
