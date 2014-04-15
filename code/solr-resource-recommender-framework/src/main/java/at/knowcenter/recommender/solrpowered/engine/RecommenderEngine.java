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
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.MPReviewBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.MostPopularRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.PrecedingItemBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.NameDescriptionBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.DescriptionBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.NameBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.TagsBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.combinations.DescriptionNameWeightedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.combinations.DescriptionNameTagsWeightedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.combinations.DescriptionTagsWeightedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.combinations.NameDescriptionWeightedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.combinations.NameDescriptionTagsWeightedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.combinations.NameTagsWeightedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.CategoryBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.PurchasesBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.ReviewBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.GroupWithoutMLTRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.InterestsWithoutMLTRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.OwnSocialRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.CommentsBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.InteractionsBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.LikesBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.SnapshotBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.UserBasedRecommenderWithoutMLT;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.WallpostBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.combined.CFPurchWithSocCommonNeighborhoodRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.combined.CFPurchWithSocCommonNeighborhoodReplacedRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.combined.CFPurchWithSocCommonNeighborhoodSummedRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cn.BiographyBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cn.GroupBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cn.InterestsAndGroupBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cn.InterestsBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cn.RealBiographyBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cn.SocialStream3Rec;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.cleaner.DataFetcher;
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
		recommendStrategies.put(StrategyType.CollaborativeFiltering, new PurchasesBasedRec());
		recommendStrategies.put(StrategyType.ContentBased, new NameDescriptionBasedRec());
		recommendStrategies.put(StrategyType.MostPopular, new MostPopularRec());
		recommendStrategies.put(StrategyType.PrecedingItemBased, new PrecedingItemBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightName, new NameBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightDescription, new DescriptionBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightTags, new TagsBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightNameDescription, new NameDescriptionWeightedBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightDescriptionName, new DescriptionNameWeightedBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightNameTags, new NameTagsWeightedBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightDescriptionTags, new DescriptionTagsWeightedBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightDescriptionNameTags, new DescriptionNameTagsWeightedBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightNameDescriptionTags, new NameDescriptionTagsWeightedBasedRec());
		recommendStrategies.put(StrategyType.CF_Social, new InteractionsBasedRec());
		recommendStrategies.put(StrategyType.CF_Own_Social, new OwnSocialRec());
		recommendStrategies.put(StrategyType.CF_Social_Likes, new LikesBasedRec());
		recommendStrategies.put(StrategyType.CF_Social_Comments, new CommentsBasedRec());
		
		recommendStrategies.put(StrategyType.UB_Interests_MLT, new InterestsBasedRec());
		recommendStrategies.put(StrategyType.UB_CustomerGroups, new GroupBasedRec());
		recommendStrategies.put(StrategyType.UB_InterestsCustomerGroup, new InterestsAndGroupBasedRec());
		recommendStrategies.put(StrategyType.UB_WithOutMLT, new UserBasedRecommenderWithoutMLT());
		
		recommendStrategies.put(StrategyType.UB_WithOutMLTInterests, new InterestsWithoutMLTRec());
		recommendStrategies.put(StrategyType.UB_WithOutMLTGroups, new GroupWithoutMLTRec());
		
		
		recommendStrategies.put(StrategyType.CF_Categories, new CategoryBasedRec());
		recommendStrategies.put(StrategyType.SocialStream, new SocialStream3Rec());
		recommendStrategies.put(StrategyType.CFPurchWithSocCommonNeighborhoodRecommender, 
				new CFPurchWithSocCommonNeighborhoodRecommender());
		recommendStrategies.put(StrategyType.CFPurchWithSocCommonNeighborhoodSummedRecommender, 
				new CFPurchWithSocCommonNeighborhoodSummedRecommender());
		recommendStrategies.put(StrategyType.CFPurchWithSocCommonNeighborhoodReplacedRecommender, 
				new CFPurchWithSocCommonNeighborhoodReplacedRecommender());
		recommendStrategies.put(StrategyType.MostPopular_Review, new MPReviewBasedRec());
		recommendStrategies.put(StrategyType.CF_Review, new ReviewBasedRec());
		
		recommendStrategies.put(StrategyType.BiographyBasedMLT, new BiographyBasedRec());
		recommendStrategies.put(StrategyType.RealBiographyBasedMLT, new RealBiographyBasedRec());
		
		recommendStrategies.put(StrategyType.WallPostInteraction, new WallpostBasedRec());
		recommendStrategies.put(StrategyType.SnapshotInteraction, new SnapshotBasedRec());

		setRecommendStrategy(StrategyType.CollaborativeFiltering);
	}
	
	public RecommendStrategy getApproach(StrategyType type) {
		return recommendStrategies.get(type);
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
//			QueryResponse response = SolrServiceContainer.getInstance().getRecommendService().findItemsFromUser(userID, "users_purchased", SolrServiceContainer.getInstance().getRecommendService().getSolrServer());
//
//			alreadyBoughtProducts = RecommendationQueryUtils.createUserProductsList(response);
			alreadyBoughtProducts = DataFetcher.getRatedProductsFromUser(userID);
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
