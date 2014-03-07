package at.knowcenter.recommender.solrpowered.evaluation.metrics;

import java.util.List;

/**
 * Calculator for the MRR measure
 * @author Emanuel Lacic
 *
 */
public class MRR {

	/**
	 * Compute the MRR of a list of ranked items.
	 *
	 * @return the MRR for the given data
	 */
	public static double calculateMRR(List<String> realData, List<String> predictionData) {
		double sum = 0.0;
		if (predictionData.size() != 0 && realData.size() != 0) {
			for (String val : realData) {
				int index = 0;
				if ((index = predictionData.indexOf(val)) != -1) {
					sum += (1.0 / (index + 1));
				} else {
					sum += 0.0;
				}
			}
		}
		if (realData.size() == 0) {
			return 0.0;
		}
		return sum / realData.size();
	}

}