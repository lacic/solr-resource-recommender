package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.marketplace.cf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.SolrServer;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class CF_Category_Job extends RecommenderEvaluator implements Callable<List<MetricsExporter>>{

	private List<String> users;


	public CF_Category_Job(List<String> users, String jobDescription) {
		this.users = users;
		this.jobDescription = jobDescription;
	}

	@Override
	public List<MetricsExporter> call() throws Exception {
		ContentFilter cf = new ContentFilter();
		List<MetricsExporter> metricsCalcs = initMetricCalcs("16_cf_category");
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate(cf, metricsCalcs);
		
		return metricsCalcs;
	}

	protected List<String> getUsers() {
		return users;
	}
	
	protected void evaluate(ContentFilter cf, List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		List<String> users = getUsers();
		int userSize = users.size();
		
		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			
			List<String> recommendations = 
					getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CF_Categories));
			
			
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
