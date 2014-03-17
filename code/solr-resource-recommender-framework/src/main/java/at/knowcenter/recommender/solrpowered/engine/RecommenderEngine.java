package at.knowcenter.recommender.solrpowered.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.QueryResponse;

import com.google.inject.Guice;
import com.google.inject.Injector;

import at.knowcenter.recommender.solrpowered.configuration.ConfigUtils;
import at.knowcenter.recommender.solrpowered.configuration.RecommenderModule;
import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.filtering.PrecedingItemEvaluation;
import at.knowcenter.recommender.solrpowered.engine.strategy.CFRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.CNRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.MostPopularRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.PrecedingItemBasedRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedDescription;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedDescriptionName;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedDescriptionNameTags;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedDescriptionTags;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedName;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedNameDescription;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedNameDescriptionTags;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedNameTags;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedTags;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.CFCategoryRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.CFOwnSocialRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.CFSocialCommentsRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.CFSocialInteractionsRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.CFSocialLikesRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.CFSocialStream3Recommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.UserBasedCustomerGroupsRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.UserBasedInterestsCustomerGroupRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.UserBasedInterestsRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.UserBasedRecommenderWithoutMLT;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.UserBasedWithoutMLTGroupRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.UserBasedWithoutMLTInterestsRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.combined.CFPurchWithSocCommonNeighborhoodRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.combined.CFPurchWithSocCommonNeighborhoodReplacedRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.combined.CFPurchWithSocCommonNeighborhoodSummedRecommender;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class RecommenderEngine implements RecommenderOperations{
	
	protected Map<StrategyType, RecommendStrategy> recommendStrategies;
	
	protected List<RecommendStrategy> getRecommendStrategies() {
		return new ArrayList<RecommendStrategy>(recommendStrategies.values());
	}

	private RecommendStrategy recommendStrategy;
	
	public RecommenderEngine() {
		initStrategies();
	}

	/**
	 * Initializes strategies used for getting recommendation results
	 */
	private void initStrategies() {
		recommendStrategies = new HashMap<StrategyType, RecommendStrategy>();
		recommendStrategies.put(StrategyType.CollaborativeFiltering, new CFRecommender());
		recommendStrategies.put(StrategyType.ContentBased, new CNRecommender());
		recommendStrategies.put(StrategyType.MostPopular, new MostPopularRecommender());
		recommendStrategies.put(StrategyType.PrecedingItemBased, new PrecedingItemBasedRecommender());
		recommendStrategies.put(StrategyType.CN_WeightName, new CNRecommenderWeightedName());
		recommendStrategies.put(StrategyType.CN_WeightDescription, new CNRecommenderWeightedDescription());
		recommendStrategies.put(StrategyType.CN_WeightTags, new CNRecommenderWeightedTags());
		recommendStrategies.put(StrategyType.CN_WeightNameDescription, new CNRecommenderWeightedNameDescription());
		recommendStrategies.put(StrategyType.CN_WeightDescriptionName, new CNRecommenderWeightedDescriptionName());
		recommendStrategies.put(StrategyType.CN_WeightNameTags, new CNRecommenderWeightedNameTags());
		recommendStrategies.put(StrategyType.CN_WeightDescriptionTags, new CNRecommenderWeightedDescriptionTags());
		recommendStrategies.put(StrategyType.CN_WeightDescriptionNameTags, new CNRecommenderWeightedDescriptionNameTags());
		recommendStrategies.put(StrategyType.CN_WeightNameDescriptionTags, new CNRecommenderWeightedNameDescriptionTags());
		recommendStrategies.put(StrategyType.CF_Social, new CFSocialInteractionsRecommender());
		recommendStrategies.put(StrategyType.CF_Own_Social, new CFOwnSocialRecommender());
		recommendStrategies.put(StrategyType.CF_Social_Likes, new CFSocialLikesRecommender());
		recommendStrategies.put(StrategyType.CF_Social_Comments, new CFSocialCommentsRecommender());
		
		recommendStrategies.put(StrategyType.UB_Interests, new UserBasedInterestsRecommender());
		recommendStrategies.put(StrategyType.UB_CustomerGroups, new UserBasedCustomerGroupsRecommender());
		recommendStrategies.put(StrategyType.UB_InterestsCustomerGroup, new UserBasedInterestsCustomerGroupRecommender());
		recommendStrategies.put(StrategyType.UB_WithOutMLT, new UserBasedRecommenderWithoutMLT());
		
		recommendStrategies.put(StrategyType.UB_WithOutMLTInterests, new UserBasedWithoutMLTInterestsRecommender());
		recommendStrategies.put(StrategyType.UB_WithOutMLTGroups, new UserBasedWithoutMLTGroupRecommender());
		
		
		recommendStrategies.put(StrategyType.CF_Categories, new CFCategoryRecommender());
		recommendStrategies.put(StrategyType.SocialStream, new CFSocialStream3Recommender());
		recommendStrategies.put(StrategyType.CFPurchWithSocCommonNeighborhoodRecommender, 
				new CFPurchWithSocCommonNeighborhoodRecommender());
		recommendStrategies.put(StrategyType.CFPurchWithSocCommonNeighborhoodSummedRecommender, 
				new CFPurchWithSocCommonNeighborhoodSummedRecommender());
		recommendStrategies.put(StrategyType.CFPurchWithSocCommonNeighborhoodReplacedRecommender, 
				new CFPurchWithSocCommonNeighborhoodReplacedRecommender());
		setRecommendStrategy(StrategyType.CollaborativeFiltering);
	}
	
	/**
	 * Based on the iteration number of tries for getting product recommendations sets an appropriate strategy
	 * @param recommendIterationNr the number of the try to get product recommendations
	 * <br/> If the number is <i>below 0</i> then the 1st strategy will be set. 
	 * <br/> If the number is <i>higher than the number of available strategies</i> then the last strategy will be used
	 */
	private void setRecommendStrategy(StrategyType strategy) {
		recommendStrategy = recommendStrategies.get(strategy);
	}
	
	@Override
	public List<String> getRecommendations(final String userID, final int n) {
		return getRecommendations(userID, null, n);
	}
	
	@Override
	public List<String> getRecommendations(final String userID, final String productID, final int n) {
		return getRecommendations(userID, productID, n, null);
	}
	
	@Override
	public List<String> getRecommendations(final String userID, final String productID, final int n, ContentFilter contentFilter) {
		List<String> recommendations;
		initUsersOwnProductsFiltering(userID);
		
		if (contentFilter == null) {
			contentFilter = new ContentFilter();
		}
		contentFilter.setCustomer( SolrServiceContainer.getInstance().getRecommendService().fetchUserProfileData(userID) );

		for (RecommendStrategy strategy : getRecommendStrategies()) {
			strategy.setContentFiltering(contentFilter);
		}
		
		recommendations = runRecommendWorkflow(userID, productID, n, contentFilter);
		
		if (contentFilter != null && contentFilter.getPrecedingEvaluationMethod() != PrecedingItemEvaluation.NOTHING) {
			List<String> piRecommendations = getRecommendations(userID, null, n, contentFilter, StrategyType.PrecedingItemBased);
			recommendations = shiftPrecedingItems(n, contentFilter, recommendations, piRecommendations);
		}
		
		if (contentFilter != null && contentFilter.getCheckVIPUser() != null && contentFilter.getCheckVIPUser()) {
			recommendations = RecommendationQueryUtils.getVIPRecommendations(n, recommendations);
		}
		
		setRecommendStrategy(StrategyType.CollaborativeFiltering);
		return recommendations;
	}
	
	/**
	 * Use this method calling a specific recommender strategy where no prior user initialization was done
	 * @param userID id of the user to get recommendations for
	 * @param productID id of the product to be used in getting recommendations
	 * @param n number of recommendations to be returned
	 * @param contentFilter container for specifying content filtering
	 * @param strategyToUse implementation of a recommender strategy
	 * @return recommendations
	 */
	public List<String> getRecommendations(final String userID, final String productID, final int n, ContentFilter contentFilter, RecommendStrategy strategyToUse) {
		recommendStrategy = strategyToUse;
		
		if (contentFilter != null) {
			contentFilter.setCustomer( SolrServiceContainer.getInstance().getRecommendService().fetchUserProfileData(userID) );
			for (RecommendStrategy strategy : getRecommendStrategies()) {
				strategy.setContentFiltering(contentFilter);
			}
			recommendStrategy.setContentFiltering(contentFilter);
		}
		
		initUsersOwnProductsFiltering(userID);
		
		RecommendQuery query = createQuery(userID, productID);
		RecommendResponse searchResponse = recommendStrategy.recommend(query, n);
				
		setRecommendStrategy(StrategyType.CollaborativeFiltering);
		
		return searchResponse.getResultItems();
	}
	
	/**
	 * Use this method calling a specific recommender strategy where user initialization was already done
	  * @param userID id of the user to get recommendations for
	 * @param productID id of the product to be used in getting recommendations
	 * @param n number of recommendations to be returned
	 * @param contentFilter container for specifying content filtering
	 * @param strategyTypeToUse type of the recommendation strategy that will be called to calculate recommendations
	 * @return recommendations
	 */
	public List<String> getRecommendations(final String userID, final String productID, final int n, ContentFilter contentFilter, StrategyType strategyTypeToUse) {
		recommendStrategy = recommendStrategies.get(strategyTypeToUse);
		
		RecommendQuery query = createQuery(userID, productID);
		
		RecommendResponse searchResponse = recommendStrategy.recommend(query, n);
		
		setRecommendStrategy(StrategyType.CollaborativeFiltering);
		
		return searchResponse.getResultItems();
	}
	
	/**
	 * Initializes all recommendation strategies with the users already purchased products
	 * so that they wont be got as recommendations
	 * @param userID
	 */
	protected List<String> initUsersOwnProductsFiltering(final String userID) {
		List<String> alreadyBoughtProducts = null;
		// STEP 0 - get products from a user
		if (userID != null ) {
			QueryResponse response = SolrServiceContainer.getInstance().getRecommendService().findItemsFromUser(userID, "users_purchased", SolrServiceContainer.getInstance().getRecommendService().getSolrServer());

			alreadyBoughtProducts = RecommendationQueryUtils.createUserProductsList(response);
			for (RecommendStrategy strategy : getRecommendStrategies()) {
				strategy.setAlreadyPurchasedResources(alreadyBoughtProducts);
			}
			if (recommendStrategy != null) {
				recommendStrategy.setAlreadyPurchasedResources(alreadyBoughtProducts);
			}
		} else {
			alreadyBoughtProducts = new ArrayList<String>();
		}
		return alreadyBoughtProducts;
	}

	/**
	 * Workflow for getting recommendations based on the input
	 * @param userID
	 * @param productID
	 * @param n
	 * @param contentFilter
	 * @return
	 */
	private List<String> runRecommendWorkflow(final String userID, final String productID, final int n, ContentFilter contentFilter) {
		List<String> recommendations = new ArrayList<String>();
	
		
		if (contentFilter.getCustomer() != null) {
			List<String> cfRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.CollaborativeFiltering);
			
			if (productID != null) {
				recommendations.addAll(cfRecommendations);
				
				if (recommendations.size() < n) {
					List<String> cbRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.ContentBased);
					RecommendationQueryUtils.appendDifferentProducts(n, recommendations, cbRecommendations);
				}
				if (recommendations.size() < n) {
					List<String> cbRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.SocialStream);
					RecommendationQueryUtils.appendDifferentProducts(n, recommendations, cbRecommendations);
				}
				if (recommendations.size() < n) {
					List<String> mpRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.MostPopular);
					RecommendationQueryUtils.appendDifferentProducts(n, recommendations, mpRecommendations);
				}
			} else {
				Double cfWeight = 0.0236;
				Double cbWeight = 0.0055;
				Double ubWeight = 0.0103;
				Double socWeight = 0.0214;
				Double streamWeight = 0.0008;
				
				List<String> cbRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.ContentBased);
				List<String> ubRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.UB_WithOutMLT);
				List<String> socialRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.CF_Social);
				List<String> streamRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.SocialStream);

				Map<String, Double> occurencesMap = new HashMap<String, Double>();
				
				RecommendationQueryUtils.fillWeightedMap(occurencesMap, cfRecommendations, cfWeight);
				RecommendationQueryUtils.fillWeightedMap(occurencesMap, cbRecommendations, cbWeight);
				RecommendationQueryUtils.fillWeightedMap(occurencesMap, ubRecommendations, ubWeight);
				RecommendationQueryUtils.fillWeightedMap(occurencesMap, socialRecommendations, socWeight);
				RecommendationQueryUtils.fillWeightedMap(occurencesMap, streamRecommendations, streamWeight);

				List<String> sortedAndTrimedRecommendations = RecommendationQueryUtils.extractCrossRankedProducts(occurencesMap);
				RecommendationQueryUtils.appendDifferentProducts(n, recommendations, sortedAndTrimedRecommendations);
				
				if (recommendations.size() < n) {
					List<String> mpRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.MostPopular);
					RecommendationQueryUtils.appendDifferentProducts(n, recommendations, mpRecommendations);
				}
			}
		} else {
			if (productID != null) {
				List<String> cbRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.ContentBased);
				recommendations.addAll(cbRecommendations);
				
				if (recommendations.size() < n) {
					List<String> cfRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.CollaborativeFiltering);
					RecommendationQueryUtils.appendDifferentProducts(n, recommendations, cfRecommendations);
				}
				if (recommendations.size() < n) {
					List<String> mpRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.MostPopular);
					RecommendationQueryUtils.appendDifferentProducts(n, recommendations, mpRecommendations);
				}
			} else  {
				List<String> mpRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.MostPopular);
				recommendations.addAll(mpRecommendations);
			}
		}
		
		return recommendations;
	}


	private List<String> shiftPrecedingItems(final int n, ContentFilter contentFilter, List<String> recommendations, List<String> piRecommendations) {
		List<String> shiftedRecommendations = null;
		if (contentFilter.getPrecedingEvaluationMethod() == PrecedingItemEvaluation.MAX_ALL_AS_RESULT) {
			shiftedRecommendations = RecommendationQueryUtils.shiftProducts(recommendations, piRecommendations, n, n);
		}
		if (contentFilter.getPrecedingEvaluationMethod() == PrecedingItemEvaluation.MAX_HALF_AS_RESULT) {
			shiftedRecommendations = RecommendationQueryUtils.shiftProducts(recommendations, piRecommendations, n / 2, n);
		}
		if (contentFilter.getPrecedingEvaluationMethod() == PrecedingItemEvaluation.MAX_20_PERCENT_AS_RESULT) {
			shiftedRecommendations = RecommendationQueryUtils.shiftProducts(recommendations, piRecommendations, (int) (n * 0.2), n);
		}
		return shiftedRecommendations;
	}

	private RecommendQuery createQuery(final String userID, final String productID) {
		RecommendQuery query = new RecommendQuery();
		query.setUser(userID);
		
		// set products if exist
		if (productID != null) {
			List<String> products = new ArrayList<>();
			products.add(productID);
			query.setProductIds(products);

		}
		
		return query;
	}
}
