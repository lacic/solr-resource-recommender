package at.knowcenter.recommender.solrpowered.engine.strategy.social;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.SocialAction;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemQuery;

/**
 * Collaborative Filtering Recommender strategy
 * @author elacic
 *
 */
public class CFSocialLikesRecommender implements RecommendStrategy {

	public static int MAX_USER_OCCURENCE_COUNT = 60;
	private List<String> alreadyBoughtProducts;
	private ContentFilter contentFilter;

	@Override
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts){
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		RecommendResponse searchResponse = new RecommendResponse();
		
		long step0ElapsedTime = 0;
		long step1ElapsedTime;
		List<String> recommendations = new ArrayList<String>();

		try {
			// STEP 0 - get products from a user
			if (query.getUser() != null ) {
				if (query.getProductIds() == null || query.getProductIds().size() == 0) {
					if (alreadyBoughtProducts != null) {
						query.setProductIds(alreadyBoughtProducts);
					} else {
					}
				}
			}
			
			solrParams = getSTEP1Params(query.getUser());

			response = SolrServiceContainer.getInstance().getSocialActionService().getSolrServer().query(solrParams);
			step1ElapsedTime = response.getElapsedTime();
			
			List<SocialAction> socialUsers = response.getBeans(SocialAction.class);
			
			if (socialUsers.size() == 0) {
				searchResponse.setNumFound(0);
				searchResponse.setResultItems(recommendations);
				searchResponse.setElapsedTime(-1);
				return searchResponse;
			}
			
			SocialAction firstUser = socialUsers.get(0);
			if (firstUser.getUserId().equals(query.getUser())){
				socialUsers.remove(0);
			} else {
				firstUser = null;
			}
			
			final Map<String, Integer> userInteractionMap = new HashMap<String, Integer>();

			if (firstUser != null && firstUser.getUsersThatLikedMe() != null) {
				for(String userLikedCurrentUser : firstUser.getUsersThatLikedMe()) {
					Integer userInteraction = userInteractionMap.get(userLikedCurrentUser);
					if (userInteraction == null) {
						userInteraction = 0;
					}
					
					userInteractionMap.put(userLikedCurrentUser, userInteraction + 1);
				}
			}
			
			for (SocialAction socialUser : socialUsers) {
				Integer userInteraction = userInteractionMap.get(socialUser.getUserId());
				if (userInteraction == null) {
					userInteraction = 0;
				}
				
				if (socialUser.getUsersThatLikedMe() != null) {
					userInteraction += Collections.frequency(socialUser.getUsersThatLikedMe(), query.getUser());
				}
				
				userInteractionMap.put(socialUser.getUserId(), userInteraction);
			}
			
			
			Comparator<String> interactionCountComparator = new Comparator<String>() {

				
				@Override
				public int compare(String a, String b) {
					if (userInteractionMap.get(a) > userInteractionMap.get(b)) {
			            return -1;
			        } else if (userInteractionMap.get(a).equals(userInteractionMap.get(b))){
			        	return 0;
			        } else {
			            return 1;
			        }
				}
				
			};
			
	        TreeMap<String,Integer> sorted_map = new TreeMap<String,Integer>(interactionCountComparator);
	        sorted_map.putAll(userInteractionMap);
			
			
			solrParams = getSTEP2Params(query, maxReuslts, sorted_map);
			// TODO Facet for confidence value
			response = SolrServiceContainer.getInstance().getRecommendService().getSolrServer().query(solrParams);
			// fill response object
			List<CustomerAction> beans = response.getBeans(CustomerAction.class);
			searchResponse.setResultItems(RecommendationQueryUtils.extractRecommendationIds(beans));
			searchResponse.setElapsedTime(step0ElapsedTime + step1ElapsedTime + response.getElapsedTime());

			SolrDocumentList docResults = response.getResults();
			searchResponse.setNumFound(docResults.getNumFound());
		} catch (SolrServerException e) {
			e.printStackTrace();
			searchResponse.setNumFound(0);
			searchResponse.setResultItems(recommendations);
			searchResponse.setElapsedTime(-1);
		}
		
		return searchResponse;
	}

	private double calcJaccardSimilarity(SocialAction firstUser, SocialAction socialUser) {
		List<String> firstUsersThatLikedMe = getList(firstUser.getUsersThatLikedMe());
		List<String> likedUsersIntersection = new ArrayList<String>(firstUsersThatLikedMe);
		List<String> otherUsersThatLikedMe = getList(socialUser.getUsersThatLikedMe());
		likedUsersIntersection.retainAll(otherUsersThatLikedMe);
		
		double jaccardSimilarity = likedUsersIntersection.size();
		
		Set<String> uniqueUserUnion = new HashSet<String>(firstUsersThatLikedMe);
		uniqueUserUnion.addAll(otherUsersThatLikedMe);
		
		jaccardSimilarity = jaccardSimilarity / uniqueUserUnion.size();
		return jaccardSimilarity;
	}

	private List<String> getList(List<String> usersThatCommentedOnMyPost) {
		if (usersThatCommentedOnMyPost == null) {
			usersThatCommentedOnMyPost = new ArrayList<String>();
		}
		return usersThatCommentedOnMyPost;
	}

	private ModifiableSolrParams getSTEP2Params(
			RecommendQuery query, Integer maxReuslts, Map<String, Integer> userInteractionMap) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		
		String queryString = createQueryToFindProdLikedBySimilarSocialUsers(userInteractionMap, contentFilter, MAX_USER_OCCURENCE_COUNT);
		
		String filterQueryString = 
				RecommendationQueryUtils.buildFilterForContentBasedFiltering(contentFilter);
		
		if (alreadyBoughtProducts != null && alreadyBoughtProducts.size() > 0) {
			if (filterQueryString.trim().length() > 0) {
				filterQueryString += " OR ";
			}
			filterQueryString += RecommendationQueryUtils.buildFilterForAlreadyBoughtProducts(alreadyBoughtProducts);
		}
		solrParams.set("q", queryString);
		solrParams.set("fq", filterQueryString);
		solrParams.set("fl", "id");
		solrParams.set("rows", maxReuslts);
		return solrParams;
	}

	private ModifiableSolrParams getSTEP1Params(String user) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		String queryString = "id:(\"" + user + "\"^2) OR users_that_liked_me:(\"" + user + "\")";
		
		solrParams.set("q", queryString);
		return solrParams;
	}
	
	public static String createQueryToFindProdLikedBySimilarSocialUsers(
			Map<String, Integer> userInteractionMap, ContentFilter contentFilter, int maxUserOccurence) {
		StringBuilder purchaseQueryBuilder = new StringBuilder();
		StringBuilder markedAsFavoriteQueryBuilder = new StringBuilder();
		StringBuilder viewedQueryBuilder = new StringBuilder();

		purchaseQueryBuilder.append("users_purchased:(");
		markedAsFavoriteQueryBuilder.append("users_marked_favorite:(");
		viewedQueryBuilder.append("users_viewed:(");
		//  max users
		int userOccurenceCount = 0;
		
		for (String user : userInteractionMap.keySet()) {
			if (userOccurenceCount >= maxUserOccurence) { break; }
			
			purchaseQueryBuilder.append("\"" + user + "\"^" + userInteractionMap.get(user) + " OR ");
			markedAsFavoriteQueryBuilder.append("\"" + user + "\"^" + (userInteractionMap.get(user) / 2) + " OR ");
			viewedQueryBuilder.append("\"" + user + "\"^" + (userInteractionMap.get(user) / 3) + " OR ");
			
			userOccurenceCount++;
		}
		
		
		if (purchaseQueryBuilder.length() > ("users_purchased:(").length()){
			purchaseQueryBuilder.replace(purchaseQueryBuilder.length() - 3, purchaseQueryBuilder.length(), ")");
		}
		if (markedAsFavoriteQueryBuilder.length() > ("users_marked_favorite:(").length()){
			markedAsFavoriteQueryBuilder.replace(markedAsFavoriteQueryBuilder.length() - 3, markedAsFavoriteQueryBuilder.length(), ")");
		}
		if (viewedQueryBuilder.length() > ("users_viewed:(").length()){
			viewedQueryBuilder.replace(viewedQueryBuilder.length() - 3, viewedQueryBuilder.length(), ")");
		}
		
		return purchaseQueryBuilder.toString() + " OR " + markedAsFavoriteQueryBuilder.toString() + " OR " + viewedQueryBuilder.toString();
	}
	
	

	@Override
	public void setAlreadyPurchasedResources(List<String> alreadyBoughtProducts) {
		this.alreadyBoughtProducts = alreadyBoughtProducts;
	}

	@Override
	public List<String> getAlreadyBoughtProducts() {
		return alreadyBoughtProducts;
	}
	
	@Override
	public void setContentFiltering(ContentFilter contentFilter) {
		this.contentFilter = contentFilter;
	}
	
	@Override
	public StrategyType getStrategyType() {
		return StrategyType.CF_Social_Likes;
	}

}
