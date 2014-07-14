package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs;

import java.util.List;
import java.util.concurrent.Callable;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.AbstractStrategyJob;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;

public class CommonTTStrategyJob extends AbstractTTStrategyJob implements Callable<List<MetricsExporter>>{

	protected List<String> users;
	protected int orderNumber = 0;
	protected StrategyType evaluatingStrategy;

	public CommonTTStrategyJob(List<String> users, String jobDescription, StrategyType strategy, int evalOrder) {
		this.users = users;
		this.jobDescription = jobDescription;
		this.evaluatingStrategy = strategy;
		this.orderNumber = evalOrder;
	}

	@Override
	public List<MetricsExporter> call() throws Exception {
		ContentFilter cf = new ContentFilter();
		List<MetricsExporter> metricsCalcs = initMetricCalcs(orderNumber + "_" + evaluatingStrategy.name());
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate(cf, metricsCalcs, evaluatingStrategy);
		
		return metricsCalcs;
	}

	@Override
	protected List<String> getUsers() {
		return users;
	}

}
