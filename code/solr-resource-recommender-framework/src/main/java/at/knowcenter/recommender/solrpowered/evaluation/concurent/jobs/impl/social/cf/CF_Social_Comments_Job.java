package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.social.cf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class CF_Social_Comments_Job extends RecommenderEvaluator implements Callable<List<MetricsExporter>>{

	private List<String> users;

	public CF_Social_Comments_Job(List<String> users, String jobDescription) {
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
			mCalc.setAlgorithmName("9_CF_Social_Comment");
			metricsCalcs.add(mCalc);
		}
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate_CF_Social(users, cf, metricsCalcs);
		return metricsCalcs;
	}
	
	protected void evaluate_CF_Social(List<String> users, ContentFilter cf, List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		int userSize = users.size();
		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			List<String> recommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CF_Social_Comments));
			long duaration = System.nanoTime() - getRecommStartTime;
			
			for (int n = 1; n <= resultSize; n++) {
				MetricsExporter mCalc = metricsCalcs.get(n - 1);
				if (recommendations.size() > n) {
					appendMetrics(mCalc, n, userID, recommendations.subList(0, n),
							recommendStrategies.get(StrategyType.CF_Social).getAlreadyBoughtProducts());
				} else {
					appendMetrics(mCalc, n, userID, recommendations,
							recommendStrategies.get(StrategyType.CF_Social).getAlreadyBoughtProducts());
				}
				mCalc.appendDuaration(duaration);
			}
			currentEvaluatingUser++;
			System.out.println("CF_Social_Comment_+" + jobDescription +": Evaluation progress: " + ((currentEvaluatingUser) / (double)userSize) * 100 + " % done");
		}
	}
	
	

}
