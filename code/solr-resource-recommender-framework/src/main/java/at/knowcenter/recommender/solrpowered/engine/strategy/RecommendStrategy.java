package at.knowcenter.recommender.solrpowered.engine.strategy;


import java.util.List;


import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

/**
 * Base interface which any recommendation approach, i.e., strategy, needs to implement
 * @author Emanuel Lacic
 *
 */
public interface RecommendStrategy {
	
	/**
	 * Recommends resources based on the given query and result number
	 * @param query query used for making recommendations
	 * @param maxReuslts number of recommendations
	 * @return response containing recommended resources
	 */
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts);
	
	/**
	 * Sets the resources which were already purchased (used to filter them out and not getting as recommendations)
	 * @param purchasedResourceIds
	 */
	public void setAlreadyPurchasedResources(List<String> purchasedResourceIds);
	
	/**
	 * Gets the ids of already purchased resources
	 * @return
	 */
	public List<String> getAlreadyBoughtProducts();
	
	/**
	 * Sets additional content filtering for the recommendations
	 * @param contentFilter object containing defined content filters
	 */
	public void setContentFiltering(ContentFilter contentFilter);
	
	/**
	 * Returns the type of the recommendation approach
	 * @return recommendation approach
	 */
	public StrategyType getStrategyType();


}
