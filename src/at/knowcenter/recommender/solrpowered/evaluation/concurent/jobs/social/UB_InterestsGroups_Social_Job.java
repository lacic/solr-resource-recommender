package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.SolrServer;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class UB_InterestsGroups_Social_Job extends RecommenderEvaluator implements Callable<List<MetricsExporter>>{

	private List<String> users;


	public UB_InterestsGroups_Social_Job(List<String> users, String jobDescription) {
		super(SolrServiceContainer.getInstance().getUserService().getSolrServer());
		this.users = users;
		this.jobDescription = jobDescription;
	}

	@Override
	public List<MetricsExporter> call() throws Exception {
		ContentFilter cf = new ContentFilter();
		List<MetricsExporter> metricsCalcs = initMetricCalcs("14_interests+groups+soc");
		
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
			
			Double ubWeight = 0.0103;
			Double socWeight = 0.0214;
			
			List<String> ubRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.UB_InterestsWithOutMLT));
			List<String> socialRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CF_Social));
			
			Map<String, Double> occurencesMap = new HashMap<String, Double>();
			
			RecommendationQueryUtils.fillWeightedMap(occurencesMap, ubRecommendations, ubWeight);
			RecommendationQueryUtils.fillWeightedMap(occurencesMap, socialRecommendations, socWeight);
			
			List<String> sortedAndTrimedRecommendations = RecommendationQueryUtils.extractCrossRankedProducts(occurencesMap);
			List<String> recommendations = new ArrayList<String>();

			RecommendationQueryUtils.appendDifferentProducts(resultSize, recommendations, sortedAndTrimedRecommendations);
			
			
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
