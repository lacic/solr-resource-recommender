package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.SolrServer;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class UB_Interests_Job extends UB_Strategy_Job implements Callable<List<MetricsExporter>>{

	private List<String> users;


	public UB_Interests_Job(List<String> users, String jobDescription) {
		super(SolrServiceContainer.getInstance().getUserService().getSolrServer());
		this.users = users;
		this.jobDescription = jobDescription;
	}

	@Override
	public List<MetricsExporter> call() throws Exception {
		ContentFilter cf = new ContentFilter();
		List<MetricsExporter> metricsCalcs = initMetricCalcs("10_" + StrategyType.UB_Interests.name());
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate(cf, metricsCalcs, StrategyType.UB_Interests);
		
		return metricsCalcs;
	}

	@Override
	protected List<String> getUsers() {
		return users;
	}
	
}
