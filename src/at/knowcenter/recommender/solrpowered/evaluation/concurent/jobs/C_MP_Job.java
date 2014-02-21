package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class C_MP_Job extends RecommenderEvaluator implements Callable<List<MetricsExporter>>{

	private List<String> users;

	public C_MP_Job(List<String> users, String jobDescription) {
		super(SolrServiceContainer.getInstance().getRecommendService().getSolrServer());
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
		List<MetricsExporter> metricsCalcs = new ArrayList<MetricsExporter>();
		
		for (int n = 1; n <= resultSize; n++) {
			MetricsExporter mCalc = new MetricsExporter("./evaluation_data/");
			mCalc.setAlgorithmName("5_C-MP_stress");
			metricsCalcs.add(mCalc);
		}
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate_C_MP(users, cf, metricsCalcs);
		return metricsCalcs;
	}
	
	

}
