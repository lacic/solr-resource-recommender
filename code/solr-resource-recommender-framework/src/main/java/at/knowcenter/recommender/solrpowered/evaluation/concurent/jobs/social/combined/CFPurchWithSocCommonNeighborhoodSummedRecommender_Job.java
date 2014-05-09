package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.combined;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.SolrServer;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.AbstractStrategyJob;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class CFPurchWithSocCommonNeighborhoodSummedRecommender_Job extends AbstractStrategyJob implements Callable<List<MetricsExporter>>{

	private List<String> users;


	public CFPurchWithSocCommonNeighborhoodSummedRecommender_Job(List<String> users, String jobDescription) {
		this.users = users;
		this.jobDescription = jobDescription;
	}

	@Override
	public List<MetricsExporter> call() throws Exception {
		ContentFilter cf = new ContentFilter();
		List<MetricsExporter> metricsCalcs = initMetricCalcs("20_" + StrategyType.CFPurchWithSocCommonNeighborhoodSummedRecommender.name());
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate(cf, metricsCalcs, StrategyType.CFPurchWithSocCommonNeighborhoodSummedRecommender);
		
		return metricsCalcs;
	}

	@Override
	protected List<String> getUsers() {
		return users;
	}
	
}
