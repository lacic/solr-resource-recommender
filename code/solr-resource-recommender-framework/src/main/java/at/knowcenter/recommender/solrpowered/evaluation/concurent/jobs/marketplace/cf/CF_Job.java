package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.marketplace.cf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class CF_Job extends RecommenderEvaluator implements Callable<List<MetricsExporter>>{

	private List<String> users;

	public CF_Job(List<String> users, String jobDescription) {
		this.users = users;
		this.jobDescription = jobDescription;
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
			mCalc.setAlgorithmName("6_CF_stress");
			metricsCalcs.add(mCalc);
		}
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate_CF(users, cf, metricsCalcs);
		return metricsCalcs;
	}
	
	protected void evaluate_CF_Occurrence(List<String> users, ContentFilter cf, List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		int userSize = users.size();
		for (String userID : users) {
			
			long getRecommStartTime = System.nanoTime();
			
			List<String> recommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CollaborativeFiltering));

			long duaration = System.nanoTime() - getRecommStartTime;
			
			for (int n = 1; n <= resultSize; n++) {
				MetricsExporter mCalc = metricsCalcs.get(n - 1);
				if (recommendations.size() > n) {
					appendMetrics(mCalc, n, userID, recommendations.subList(0, n),
							recommendStrategies.get(StrategyType.CollaborativeFiltering).getAlreadyBoughtProducts());
				} else {
					appendMetrics(mCalc, n, userID, recommendations,
							recommendStrategies.get(StrategyType.CollaborativeFiltering).getAlreadyBoughtProducts());
				}
				mCalc.appendDuaration(duaration);
			}
			currentEvaluatingUser++;
			System.out.println("CF_" + jobDescription +": Evaluation progress: " + ((currentEvaluatingUser) / (double)userSize) * 100 + " % done");
		}
//		for (int n = 1; n <= resultSize; n++) {
//			MetricsExporter mCalc = metricsCalcs.get(n - 1);
//			mCalc.exportCalculatedMetricsAverage("6_CF", users.size());
//		}
	}
	
	

}
