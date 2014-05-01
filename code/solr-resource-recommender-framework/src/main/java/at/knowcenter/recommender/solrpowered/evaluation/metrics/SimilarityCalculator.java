package at.knowcenter.recommender.solrpowered.evaluation.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.model.Resource;

public class SimilarityCalculator {
	
	private List<Resource> userKnownData;
	private List<Resource> predictionData;

	public SimilarityCalculator(List<Resource> userKnownData, List<Resource> predictionData, int k) {
		this.userKnownData = userKnownData;
		if (k == 0 || predictionData.size() < k) {
			this.predictionData = predictionData;
		} else {
			this.predictionData = predictionData.subList(0, k);
		}
		
 	}
	
	/**
	 * Calculates the novelty for an item.
	 * 
	 * N(i) = (1 / (p - 1 )) * Sum{j E [0,p] | i != j} dis(i,j)
	 * 
	 * @param itemI
	 * @return
	 */
	public double calculateNovelty(Resource itemI, List<Resource> itemsToCompare) {
		if (itemsToCompare == null) {
			return 0.0;
		}
		
		double disimilaritySum = 0.0;

		int similarItemCount = 0;
		for (Resource itemJ : itemsToCompare) {
			if ( ! itemI.equals(itemJ)) {
				disimilaritySum += 1.0 - calculateSimilarity(itemI, itemJ);
				similarItemCount++;
			}
		}
		
		if (similarItemCount == 0) {
			return 0.0;
		}
		return disimilaritySum / similarItemCount;
	}
	
	/**
	 * How serendipitous are items in the prediction set.
	 * Calculating how novel is each predicted item compared to already known items.
	 * @return predicted items serendipity
	 */
	public double calculateSerendipity() {
		double serendipity = 0.0;

		if (this.predictionData == null || this.predictionData.size() == 0) {
			return serendipity;
		}
		
		if (userKnownData == null || userKnownData.size() == 0) {
			return 1.0;
		}
		
		for (Resource predictedResource : predictionData) {
			serendipity += calculateNovelty(predictedResource, userKnownData);
		}
		
		return serendipity / predictionData.size();
	}
	
	/**
	 * How diverse are items in the predicted set between each other.
	 * Calculating how novel is each item compared to the predicted item set.
	 * @return predicted items diversity
	 */
	public double calculateDiversity() {
		double diversity = 0.0;

		if (this.predictionData == null || this.predictionData.size() == 0) {
			return diversity;
		}
		
		for (Resource predictedResource : predictionData) {
			diversity += calculateNovelty(predictedResource, predictionData);
		}
		
		return diversity / predictionData.size();
	}
	
	private double calculateCosineDisimilarity(Item itemK, Item itemC) {
		return 1 - calculateCosineSimilarity(itemK, itemC);
	}
	
	private double calculateCosineSimilarity(Item itemK, Item itemC) {
		double vectorItemK = Math.sqrt(
								Math.pow(itemK.getPrice(), 2) + 
								Math.pow(itemK.getValidFrom().getTime(), 2) + 
								Math.pow(itemK.getValidTo().getTime(), 2)
							);
		double vectorItemC = Math.sqrt(
								Math.pow(itemC.getPrice(), 2)  + 
								Math.pow(itemC.getValidFrom().getTime(), 2) + 
								Math.pow(itemC.getValidTo().getTime(), 2)
							);
		
		double dotProduct = 
				itemK.getPrice() * itemC.getPrice() + 
				itemK.getValidFrom().getTime() * itemC.getValidFrom().getTime() + 
				itemK.getValidTo().getTime() * itemC.getValidTo().getTime();
		
		return dotProduct / (vectorItemC * vectorItemK);
	}
	
	private double calculateSimilarity(Resource itemK, Resource itemC) {
		Map<String, Integer> wordOccurencItemK = fillDescriptionWordCount(itemK.getDescription(), "|");
		Map<String, Integer> wordOccurencItemC = fillDescriptionWordCount(itemC.getDescription(), "|");
		
		double descriptionSimilarity = getCosineSim(wordOccurencItemC, wordOccurencItemK);
		
		wordOccurencItemK = fillDescriptionWordCount(itemK.getItemName(), " ");
		wordOccurencItemC = fillDescriptionWordCount(itemC.getItemName(), " ");
		
		double nameSimilarity = getCosineSim(wordOccurencItemC, wordOccurencItemK);
		
		wordOccurencItemK = fillListWordCount(itemK.getTags());
		wordOccurencItemC = fillListWordCount(itemC.getTags());
		
		double tagSimilarity = getCosineSim(wordOccurencItemC, wordOccurencItemK);
		
		return  ( 1.5 * descriptionSimilarity + 1.7 * nameSimilarity + 0.8 * tagSimilarity) / 4;
	}

	private Map<String, Integer> fillListWordCount(List<String> itemAttributeList) {
		if (itemAttributeList == null) {
			itemAttributeList = new ArrayList<String>();
		}
		Map<String, Integer> wordOccurencItemK;
		String collectionItemK = "";
		for (String collection : itemAttributeList) {
			collectionItemK += collection + " ";
		}
		wordOccurencItemK = fillDescriptionWordCount(collectionItemK.trim(), " ");
		return wordOccurencItemK;
	}

	private Map<String, Integer> fillDescriptionWordCount(String itemAttribute, String splitDelimiter) {
		Map<String, Integer> wordOccurencItemC = new HashMap<String, Integer>();
		String[] descWordsItemC = itemAttribute.split(splitDelimiter);
		
		for (String word : descWordsItemC){
			if (wordOccurencItemC.containsKey(word)) {
				wordOccurencItemC.put(word, wordOccurencItemC.get(word) + 1);
			} else {
				wordOccurencItemC.put(word, 1);
			}
		}
		
		return wordOccurencItemC;
	}

	
	public static double getJaccardSim(Map<? extends Object, Integer> targetMap, Map<? extends Object, Integer> nMap) {
		Set<Object> unionSet = new HashSet<Object>(targetMap.keySet());
		Set<Object> intersectSet = new HashSet<Object>(targetMap.keySet());
		unionSet.addAll(nMap.keySet());
		intersectSet.retainAll(nMap.keySet());
		return (double)intersectSet.size() / (double)unionSet.size();
	}
	
	public static double getCosineSim(Map<? extends Object, Integer> targetMap, Map<? extends Object, Integer> nMap) {
        Set<Object> both = new HashSet<Object>(targetMap.keySet());
        both.retainAll(nMap.keySet());
        double scalar = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (Object k : both) {
        	scalar += (targetMap.get(k) * nMap.get(k));
        }
        for (Object k : targetMap.keySet()) {
        	norm1 += (targetMap.get(k) * targetMap.get(k));
        }
        for (Object k : nMap.keySet()) {
        	norm2 += (nMap.get(k) * nMap.get(k));
        }
        return scalar / Math.sqrt(norm1 * norm2);
	}
	
	

}
