package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.impl.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.CustomerAction.ACTION;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class UpdateTest_Job extends RecommenderEvaluator implements Callable<List<MetricsExporter>>{

	private int updateCount ;

	public UpdateTest_Job(String jobDescription, int updateCount) {
		this.jobDescription = jobDescription;
		this.updateCount = updateCount;
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
			mCalc.setAlgorithmName("16_Updates_updateTest");
			metricsCalcs.add(mCalc);
		}
		
		System.out.println("Evaluation over " + updateCount + " updates");
		
		evaluate_CF_C_MP(cf, metricsCalcs);
		return metricsCalcs;
	}
	
	protected void evaluate_CF_C_MP(ContentFilter cf, List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		
		for (int i = 0; i < updateCount; i++) {
			
			long updateStart = System.nanoTime();
		
			CustomerAction ca = new CustomerAction();
			ca.setItemId("i_update_id_" + (int) (Math.random() * 10000));
			ArrayList<String> customers = new ArrayList<String>();
			customers.add("c_update_id");
			ca.setCustomerIds(customers);
			ca.setAction(ACTION.PURCHASED);
			SolrServiceContainer.getInstance().getRecommendService().updateDocument(ca);		
			
			for (int n = 1; n <= resultSize; n++) {
				MetricsExporter mCalc = metricsCalcs.get(n - 1);
				mCalc.appendDuaration(System.nanoTime() - updateStart);
			}
			
			currentEvaluatingUser++;
			double progress = ((double) currentEvaluatingUser) / updateCount;
			System.out.println("Updates_+" + jobDescription +": Evaluation progress: " + progress * 100 + " % done");
		}
	}
	
	
	

}
