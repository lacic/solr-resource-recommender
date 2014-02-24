package at.knowcenter.recommender.solrpowered.evaluation.metrics;

import java.util.ArrayList;
import java.util.List;

public class PredictionCalculator{

	private String userID;
	private List<String> realData;
	private List<String> predictionData;
	
	private double numFoundRelevantDocs;
	
	public PredictionCalculator(String userID, List<String> realData, List<String> predictionData, int k) {
		this.userID = userID;
		this.realData = realData;
		if (k == 0 || predictionData.size() < k) {
			this.predictionData = predictionData;
		} else {
			this.predictionData = predictionData.subList(0, k);
		}
		
		determineRelevantDocs();
 	}
	
	public double getRecall() {
		if (this.realData.size() != 0) {
			return this.numFoundRelevantDocs / this.realData.size();
		}
		return 0.0;
	}
	
	public double getPrecision() {
		if (this.predictionData.size() != 0) {
			return this.numFoundRelevantDocs / this.predictionData.size();
		}
		return 0.0;
	}
	
	public double getFMeasure() {
		if (getPrecision() + getRecall() != 0) {
			return 2.0 * (getPrecision() * getRecall() / (getPrecision() + getRecall()));
		}
		return 0.0;
	}
	
	public double getMRR() {
		return MRR.calculateMRR(realData, predictionData);
	}
	
	public double getMAP() {
		if (this.predictionData.size() != 0 && this.realData.size() != 0 && this.numFoundRelevantDocs != 0) {
			double sum = 0.0;
			for (int i = 1; i <= this.predictionData.size(); i++) {
				sum += (getPrecisionK(i) * isCorrect(i - 1));
			}
			return sum / this.realData.size();
		}
		return 0.0;
	}
	
	/**
	* Compute the normalized discounted cumulative gain (NDCG) of a list of ranked items.
	*
	* @return the NDCG for the given data
	*/
	public double getNDCG() {
		return NDCG.calculateNDCG(realData, predictionData);
	}
	

	private double getPrecisionK(int k) {
		if (k != 0 && k <= this.predictionData.size()) {
			List<String> foundRelevantDocs = new ArrayList<String>(this.realData);
			foundRelevantDocs.retainAll(this.predictionData.subList(0, k));
			double numFoundRelevantDocs = foundRelevantDocs.size();
			return numFoundRelevantDocs / k;
		}
		return 0.0;
	}
	
	private double isCorrect(int n) {
		if (this.predictionData.size() > n && this.realData.contains(this.predictionData.get(n))) {
			return 1.0;
		}
		return 0.0;
	}
	
	private void determineRelevantDocs() {
		List<String> foundRelevantDocs = new ArrayList<String>(this.realData);
		foundRelevantDocs.retainAll(this.predictionData);
		this.numFoundRelevantDocs = foundRelevantDocs.size();
	}
	
	
	// Getter ------------------------------------------------------------------------------------------------
	
	public String getUserID() {
		return this.userID;
	}
	
	public List<String> getRealData() {
		return this.realData;
	}
	
	public List<String> getPredictionData() {
		return this.predictionData;
	}
}
