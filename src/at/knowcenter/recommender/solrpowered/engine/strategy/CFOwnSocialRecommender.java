package at.knowcenter.recommender.solrpowered.engine.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.OwnSocialAction;
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
public class CFOwnSocialRecommender implements RecommendStrategy {

	public static int MAX_USER_OCCURENCE_COUNT = 60;
	private List<String> alreadyBoughtProducts;
	private ContentFilter contentFilter;

	@Override
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts, SolrServer solrServer){
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
			
			String queryString = "id:(\"" + query.getUser() + "\")";
			
			solrParams.set("q", queryString);
			
			
			response = SolrServiceContainer.getInstance().getOwnSocialActionService().getSolrServer().query(solrParams);
			step1ElapsedTime = response.getElapsedTime();
			
			List<OwnSocialAction> socialUsers = response.getBeans(OwnSocialAction.class);
			
			if (socialUsers.size() == 0) {
				searchResponse.setNumFound(0);
				searchResponse.setResultItems(recommendations);
				searchResponse.setElapsedTime(-1);
				return searchResponse;
			}
			
			OwnSocialAction userSocialAction = socialUsers.get(0);
			
			Map<String, Double> userBoostings = new HashMap<String, Double>();
			
			if (userSocialAction.getUsersThatILiked() != null) {
				for (String userILiked : userSocialAction.getUsersThatILiked()) {
					Double userBoosting = userBoostings.get(userILiked);
					
					if (userBoosting == null) {
						userBoosting = 0.0;
					} 
					
					userBoosting += 1.7;
					
					userBoostings.put(userILiked, userBoosting);
				}
			}
			
			if (userSocialAction.getUsersThatICommentedOn() != null) {
				for (String userICommentedOn : userSocialAction.getUsersThatICommentedOn()) {
					Double userBoosting = userBoostings.get(userICommentedOn);
					
					if (userBoosting == null) {
						userBoosting = 0.0;
					} 
					
					userBoosting += 1.3;
					
					userBoostings.put(userICommentedOn, userBoosting);
				}
			}
			
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append("id:(");
			for (String user : userBoostings.keySet()) {
				queryBuilder.append("\"" + user + "\"^" + userBoostings.get(user) + " OR ");
			}
			if (queryBuilder.length() > ("id:(").length()){
				queryBuilder.replace(queryBuilder.length() - 4, queryBuilder.length(), ")");
			}
			
			
			solrParams = new ModifiableSolrParams();
			solrParams.set("q", queryBuilder.toString());
			solrParams.set("rows", 40);
			
			response = SolrServiceContainer.getInstance().getOwnSocialActionService().getSolrServer().query(solrParams);
			socialUsers = response.getBeans(OwnSocialAction.class);
			
			List<String> users = new ArrayList<String>();
			List<Double> boostings = new ArrayList<Double>();
			
			
			for (OwnSocialAction socialUser : socialUsers) {
				double jaccardSimilarity = calcJaccardSimilarity(userSocialAction, socialUser);
				boostings.add(jaccardSimilarity);
				
				users.add(socialUser.getUserId());
			}
			
			
			solrParams = getSTEP2Params(maxReuslts, users, boostings);
			response = solrServer.query(solrParams);
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

	private double calcJaccardSimilarity(OwnSocialAction firstUser,
			OwnSocialAction socialUser) {
		List<String> firstUsersILiked = getList(firstUser.getUsersThatILiked());
		List<String> likedUsersIntersection = new ArrayList<String>(firstUsersILiked);
		List<String> otherUsersThatILiked = getList(socialUser.getUsersThatILiked());
		likedUsersIntersection.retainAll(otherUsersThatILiked);
		
		List<String> firstUsersThatICommentedOn = getList(firstUser.getUsersThatICommentedOn());
		List<String> commentedUsersIntersection = new ArrayList<String>(firstUsersThatICommentedOn);
		List<String> usersThatICommentedOn = getList(socialUser.getUsersThatICommentedOn());
		commentedUsersIntersection.retainAll(usersThatICommentedOn);
		
		double jaccardSimilarity = likedUsersIntersection.size() + commentedUsersIntersection.size();
		
		Set<String> uniqueUserUnion = new HashSet<String>(firstUsersILiked);
		uniqueUserUnion.addAll(otherUsersThatILiked);
		uniqueUserUnion.addAll(firstUsersThatICommentedOn);
		uniqueUserUnion.addAll(usersThatICommentedOn);
		
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
			Integer maxReuslts, List<String> users, List<Double> boostings) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		
		String queryString = createQueryToFindProdLikedBySimilarSocialUsers(users, boostings, contentFilter, MAX_USER_OCCURENCE_COUNT);
		
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
	
	
	public static String createQueryToFindProdLikedBySimilarSocialUsers(
			List<String> users, List<Double> boostings, ContentFilter contentFilter, int maxUserOccurence) {
		StringBuilder purchaseQueryBuilder = new StringBuilder();
		StringBuilder markedAsFavoriteQueryBuilder = new StringBuilder();
		StringBuilder viewedQueryBuilder = new StringBuilder();

		purchaseQueryBuilder.append("users_purchased:(");
		markedAsFavoriteQueryBuilder.append("users_marked_favorite:(");
		viewedQueryBuilder.append("users_viewed:(");
		//  max users
		int userOccurenceCount = 0;
		
		for (String user : users) {
			if (userOccurenceCount >= maxUserOccurence) { break; }
			
			purchaseQueryBuilder.append("\"" + user + "\"^" + boostings.get(userOccurenceCount) + " OR ");
			markedAsFavoriteQueryBuilder.append("\"" + user + "\"^" + boostings.get(userOccurenceCount) / 2 + " OR ");
			viewedQueryBuilder.append("\"" + user + "\"^" + (boostings.get(userOccurenceCount) / 3) + " OR ");
			
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
	public void setAlreadyBoughtProducts(List<String> alreadyBoughtProducts) {
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
		return StrategyType.CollaborativeFiltering;
	}

}
