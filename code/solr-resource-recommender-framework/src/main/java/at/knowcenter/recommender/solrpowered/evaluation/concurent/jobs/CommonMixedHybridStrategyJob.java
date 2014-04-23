package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.AbstractStrategyJob;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;

public class CommonMixedHybridStrategyJob extends AbstractMixedHybridStrategyJob implements Callable<List<MetricsExporter>>{

	protected List<String> users;
	protected int orderNumber = 0;
	protected List<StrategyType> evaluatingStrategies;

	public CommonMixedHybridStrategyJob(
			List<String> users, String jobDescription, int evalOrder, StrategyType... strategies) {
		this.users = users;
		this.jobDescription = jobDescription;
		this.evaluatingStrategies = Arrays.asList(strategies);
		this.orderNumber = evalOrder;
	}

	@Override
	public List<MetricsExporter> call() throws Exception {
		ContentFilter cf = new ContentFilter();
		List<MetricsExporter> metricsCalcs = initMetricCalcs(orderNumber + "_" + jobDescription);
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate(cf, metricsCalcs, evaluatingStrategies);
		
		return metricsCalcs;
	}

	@Override
	protected List<String> getUsers() {
		return users;
	}

}
