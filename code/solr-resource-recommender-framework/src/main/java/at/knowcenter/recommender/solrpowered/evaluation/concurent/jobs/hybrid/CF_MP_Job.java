package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.hybrid;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class CF_MP_Job extends RecommenderEvaluator implements Callable<List<MetricsExporter>>{

	private List<String> users;

	public CF_MP_Job(List<String> users, String jobDescription) {
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
			mCalc.setAlgorithmName("4_CF-MP_stress");
			metricsCalcs.add(mCalc);
		}
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate_CF_MP(users, cf, metricsCalcs);
		return metricsCalcs;
	}
	
	

}
