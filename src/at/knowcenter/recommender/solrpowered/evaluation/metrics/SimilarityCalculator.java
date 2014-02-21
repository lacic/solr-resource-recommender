package at.knowcenter.recommender.solrpowered.evaluation.metrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.knowcenter.recommender.solrpowered.model.Item;

public class SimilarityCalculator {
	
	private List<Item> userKnownData;
	private List<Item> predictionData;

	public SimilarityCalculator(List<Item> userKnownData, List<Item> predictionData, int k) {
		this.userKnownData = userKnownData;
		if (k == 0 || predictionData.size() < k) {
			this.predictionData = predictionData;
		} else {
			this.predictionData = predictionData.subList(0, k);
		}
		
 	}
	
	public double calculateNovelty(Item itemI) {
		int p = 2;
		if (predictionData != null && predictionData.size() > 1) {
			p = predictionData.size();
		}
		
		double disimilaritySum = 0.0;
		
		for (Item itemJ : predictionData) {
			if ( ! itemI.equals(itemJ)) {
				disimilaritySum += calculateSimilarity(itemI, itemJ);
			}
		}
		
		
		return ( 1.0 / ( p - 1 ) ) * disimilaritySum;
	}
	
	public double calculateSerendipity() {
		int p = 2;
		if (userKnownData != null && userKnownData.size() > 1) {
			p = userKnownData.size();
		}
		
		double disimilaritySum = 0.0;
		
		for (Item predictedResource : predictionData) {
			for (Item knownResource : userKnownData) {
				if ( ! predictedResource.equals(knownResource)) {
					disimilaritySum += calculateSimilarity(predictedResource, knownResource);
				}
			}
		}
		
		
		return ( 1.0 / (p * ( p - 1 )) ) * disimilaritySum;
	}
	
	
	public double calculateDiversity() {
		int p = 2;
		if (predictionData != null && predictionData.size() > 1) {
			p = predictionData.size();
		}
		
		double disimilaritySum = 0.0;
		
		for (Item predictedResource : predictionData) {
			for (Item otherPredictedResource : predictionData) {
				if ( ! predictedResource.equals(otherPredictedResource)) {
					disimilaritySum += calculateSimilarity(predictedResource, otherPredictedResource);
				}
			}
		}
		
		
		return ( 1.0 / (p * ( p - 1 )) ) * disimilaritySum;
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
	
	private double calculateSimilarity(Item itemK, Item itemC) {
		Map<String, Integer> wordOccurencItemK = fillDescriptionWordCount(itemK.getDescription(), "|");
		Map<String, Integer> wordOccurencItemC = fillDescriptionWordCount(itemC.getDescription(), "|");
		
		double descriptionSimilarity = getCosineSim(wordOccurencItemC, wordOccurencItemK);
		
		wordOccurencItemK = fillDescriptionWordCount(itemK.getName(), " ");
		wordOccurencItemC = fillDescriptionWordCount(itemC.getName(), " ");
		
		double nameSimilarity = getCosineSim(wordOccurencItemC, wordOccurencItemK);
		
		wordOccurencItemK = fillListWordCount(itemK.getCollection());
		wordOccurencItemC = fillListWordCount(itemC.getCollection());
		
		double collectionSimilarity = getCosineSim(wordOccurencItemC, wordOccurencItemK);
		
		wordOccurencItemK = fillListWordCount(itemK.getTags());
		wordOccurencItemC = fillListWordCount(itemC.getTags());
		
		double tagSimilarity = getCosineSim(wordOccurencItemC, wordOccurencItemK);
		
		return  ( 1.5 * descriptionSimilarity + 1.3 * nameSimilarity + 0.4 * collectionSimilarity + 0.8 * tagSimilarity) / 4;
	}

	private Map<String, Integer> fillListWordCount(List<String> itemAttributeList) {
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
        double scalar = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (Object k : both) scalar += (targetMap.get(k) * nMap.get(k));
        for (Object k : targetMap.keySet()) norm1 += (targetMap.get(k) * targetMap.get(k));
        for (Object k : nMap.keySet()) norm2 += (nMap.get(k) * nMap.get(k));
        return scalar / Math.sqrt(norm1 * norm2);
	}
	
	

}
