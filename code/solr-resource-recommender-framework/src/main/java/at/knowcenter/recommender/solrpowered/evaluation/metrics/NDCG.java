package at.knowcenter.recommender.solrpowered.evaluation.metrics;

import java.util.List;

/**
 * Calculates the nDCG measure on the recommended resources based on expecting resources
 * 
 * @author Emanuel Lacic
 */
public class NDCG {

	/**
	 * Compute the normalized discounted cumulative gain (NDCG) of a list of ranked items.
	 *
	 * @return the NDCG for the given data
	 */
	public static double calculateNDCG(List<String> realData, List<String> predictionData) {
		double dcg = 0;
		double idcg = calculateIDCG(realData.size());

		if (idcg == 0) {
			return 0;
		}

		for (int i = 0; i < predictionData.size(); i++) {
			String predictedItem = predictionData.get(i);

			if (!realData.contains(predictedItem))
				continue;

			// the relevance in the DCG part is either 1 (the item is contained in real data) 
			// or 0 (item is not contained in the real data)
			int itemRelevance = 1;
			if (!realData.contains(predictedItem))
				itemRelevance = 0;

			// compute NDCG part
			int rank = i + 1;

			dcg += (Math.pow(2, itemRelevance) - 1.0) * (Math.log(2) / Math.log(rank + 1));
		}

		return dcg / idcg;
	}

	/**
	 * Calculates the iDCG
	 * 
	 * @param n size of the expected resource list
	 * @return iDCG
	 */
	public static double calculateIDCG(int n) {
		double idcg = 0;
		// if can get relevance for every item should replace the relevance score at this point, else
		// every item in the ideal case has relevance of 1
		int itemRelevance = 1;
		
		for (int i = 0; i < n; i++){
			idcg += (Math.pow(2, itemRelevance) - 1.0) * ( Math.log(2) / Math.log(i + 2) );
		}

		return idcg;
	}

}