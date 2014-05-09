package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.marketplace.cb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class C_Job extends RecommenderEvaluator implements Callable<List<MetricsExporter>>{

	private List<String> users;

	public C_Job(List<String> users, String jobDescription) {
		this.jobDescription = jobDescription;
		this.users = users;
	}
	
	@Override
	public List<MetricsExporter> call() throws Exception {
		return evaluate();
	}
	
	@Override
	public List<MetricsExporter> evaluate() {
		ContentFilter cf = new ContentFilter();
		List<MetricsExporter> metricsCalcs = initMetricCalcs("7_CN");
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate_C_Weighted(cf, metricsCalcs);
		
		return metricsCalcs;
	}
	
	protected void evaluate_C_Weighted(ContentFilter cf, List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		int userSize = users.size();

		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			
			List<String> recommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CN_WeightNameDescription));
			
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
		}

		currentEvaluatingUser++;
		System.out.println("C_+" + jobDescription +": Evaluation progress: " + ((currentEvaluatingUser) / (double)userSize) * 100 + " % done");

//
//		for (int n = 1; n <= resultSize; n++) {
//			MetricsExporter mCalc = metricsCalcs.get(n - 1);
//			mCalc.exportCalculatedMetricsAverage("7_CN", users.size());
//		}
	}

}
