package at.knowcenter.recommender.solrpowered.engine;

import java.util.List;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;

/**
 * Operations available to get recommendations
 * @author Emanuel Lacic
 *
 */
public interface RecommenderOperations {
	
	/**
	 * Gets Recommendations for a given user
	 * 
	 * @param userID the user to get the recommendations for
	 * @param maxResultCount max number of returned recommendations
	 * 
	 * @return list of products that are recommended for the user
	 */
	public List<String> getRecommendations(final String userID, final int maxResultCount);
	
	/**
	 * Gets Recommendations for a given user and based on the item the user bought
	 * 
	 * @param userID the user to get the recommendations for
	 * @param productID the product that the user bought and which is used to get similar products that other users bought
	 * @param maxResultCount max number of returned recommendations
	 * 
	 * @return list of products that are recommended for the user
	 */
	public List<String> getRecommendations(final String userID, final String productID, final int maxResultCount);
	
	/**
	 * Gets Recommendations for a given user and item the user has bought or is currently viewing
	 * 
	 * @param userID the user to get the recommendations for
	 * @param productID the product that the user bought and which is used to get similar products that other users bought
	 * @param maxResultCount max number of returned recommendations
	 * @param contentFilter filter that defines what content should taken into consideration
	 * 
	 * @return list of products that are recommended for the user
	 */
	public List<String> getRecommendations(final String userID, final String productID, final int n, ContentFilter contentFilter);

}
