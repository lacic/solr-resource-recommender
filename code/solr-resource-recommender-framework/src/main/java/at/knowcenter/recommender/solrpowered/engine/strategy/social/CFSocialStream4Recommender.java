package at.knowcenter.recommender.solrpowered.engine.strategy.social;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.model.SocialAction;
import at.knowcenter.recommender.solrpowered.model.SocialStream;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemQuery;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemResponse;
import at.knowcenter.recommender.solrpowered.services.impl.item.MoreLikeThisRequest;

/**
 * Social Stream recommender
 * Fetch wallposts and comments
 * @author elacic
 *
 */
public class CFSocialStream4Recommender implements RecommendStrategy {

	public static int MAX_USER_OCCURENCE_COUNT = 60;
	private List<String> alreadyBoughtProducts;
	private ContentFilter contentFilter;

	@Override
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts){
		QueryResponse response = null;
		RecommendResponse searchResponse = new RecommendResponse();
		
		long step0ElapsedTime = 0;
		long step1ElapsedTime;
		List<String> recommendations = new ArrayList<String>();

		try {
			// STEP 0 - get products from a user
			String source = query.getUser();
			if (source != null ) {
				if (query.getProductIds() == null || query.getProductIds().size() == 0) {
					if (alreadyBoughtProducts != null) {
						query.setProductIds(alreadyBoughtProducts);
					} else {
					}
				}
			}
			
			StringBuilder socialStreamQueryBuilder = new StringBuilder();
			
			socialStreamQueryBuilder.append("source:(\"" + source + "\")");
			
			ModifiableSolrParams params = new ModifiableSolrParams();
			// Get WALLPOSTS and COMMENTS from user, then comments on my posts and as last posts where i commented
			params.set("q", "source:\"" + source + "\" OR "
					+ "{!join from=id to=target_action}source:\"" + source
					+ "\" OR {!join from=target_action to=id}source:\"" + source + "\"");
			params.set("fq", "content:[* TO *] AND (action_type:(\"COMMENT\") OR action_type:(\"WALLPOST\"))");
			params.set("rows", Integer.MAX_VALUE);

			response = SolrServiceContainer.getInstance().getSocialStreamService().getSolrServer().query(params);
			List<SocialStream> userContentStreams = response.getBeans(SocialStream.class);

			List<SocialStream> myWallPosts = new ArrayList<SocialStream>();
			List<SocialStream> myComments = new ArrayList<SocialStream>();
			
			List<SocialStream> commentsOnMyPosts = new ArrayList<SocialStream>();
			Set<String> usersCommentedOnMyPost = new HashSet<String>();
			
			List<SocialStream> postsWhereICommented = new ArrayList<SocialStream>();
			Set<String> usersWhichPostICommented = new HashSet<String>();
			
			StringBuilder myPostsStream = new StringBuilder();
			StringBuilder myCommentsStream = new StringBuilder();
			
			for (SocialStream ss : userContentStreams) {
				if (ss.getSourceUserId().equals(source)) {
					if (ss.getTargetActionId() == null || ss.getTargetActionId().equals("")) {
						myWallPosts.add(ss);
						myPostsStream.append(ss.getSocialContent() + " ");
					} else {
						myComments.add(ss);
						myCommentsStream.append(ss.getSocialContent() + " ");
					}
				} else {
					if (ss.getTargetActionId() == null || ss.getTargetActionId().equals("")) {
						postsWhereICommented.add(ss);
						usersWhichPostICommented.add(ss.getSourceUserId());
					} else {
						commentsOnMyPosts.add(ss);
						usersCommentedOnMyPost.add(ss.getSourceUserId());
					}
				}
			}
			
			Map<String, Double> similarityMap = new HashMap<String,Double>();
			
			params = initMLTParams("-" + socialStreamQueryBuilder.toString(), maxReuslts, myPostsStream.toString() + myCommentsStream.toString());
			
			similarityMap = findSimilarContent(params,1.5, similarityMap);
			
//			params = initMLTParams("-" + socialStreamQueryBuilder.toString(), maxReuslts, myCommentsStream.toString());
//			
//			similarityMap = findSimilarContent(params,1.2, similarityMap);
			
			for (String similarUser : similarityMap.keySet()) {
				if (usersWhichPostICommented.contains(similarUser)) {
					similarityMap.put(similarUser, similarityMap.get(similarUser) * 1.3);
				}
				if (usersCommentedOnMyPost.contains(similarUser)) {
					similarityMap.put(similarUser, similarityMap.get(similarUser) * 1.1);
				}
			}

			params = getSTEP2Params(maxReuslts, similarityMap, RecommendationQueryUtils.extractCrossRankedProducts(similarityMap));
			
			response = SolrServiceContainer.getInstance().getRecommendService().getSolrServer().query(params);
			// fill response object
			List<CustomerAction> beans = response.getBeans(CustomerAction.class);
			searchResponse.setResultItems(RecommendationQueryUtils.extractRecommendationIds(beans));
			searchResponse.setElapsedTime(step0ElapsedTime + response.getElapsedTime());

			SolrDocumentList docResults = response.getResults();
			searchResponse.setNumFound(docResults.getNumFound());
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
			searchResponse.setNumFound(0);
			searchResponse.setResultItems(recommendations);
			searchResponse.setElapsedTime(-1);
		}
		
		return searchResponse;
	}


	private Map<String, Double> findSimilarContent(ModifiableSolrParams params, double scoreBoost, Map<String, Double> similarityMap) throws SolrServerException, IOException {
		MoreLikeThisRequest mltRequest = new MoreLikeThisRequest(params);
		NamedList<Object> resLists = SolrServiceContainer.getInstance().getSocialStreamService().getSolrServer().request(mltRequest);

		List<SocialStream> resultSocialStream = new ArrayList<SocialStream>();
		SolrDocumentList similarDocuments =(SolrDocumentList) resLists.get("response");
		
		if (similarDocuments != null && similarDocuments.size() > 0) {
			for (SolrDocument similarDocument : similarDocuments) {
				SocialStream currentSearchItem = RecommendationQueryUtils.serializeSolrDocToSocialStream(similarDocument);
				resultSocialStream.add(currentSearchItem);

				Double userPostScore = similarityMap.get(currentSearchItem.getSourceUserId());
				if (userPostScore == null) {
					userPostScore = currentSearchItem.getScore() * scoreBoost;
				} else {
					userPostScore += currentSearchItem.getScore() * scoreBoost;
				}
				similarityMap.put(currentSearchItem.getSourceUserId(), userPostScore);
			}
		}
		return similarityMap;
	}


	private ModifiableSolrParams getSTEP2Params(Integer maxReuslts, Map<String, Double> userInteractionMap, List<String> sortedUsers) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		
		String queryString = createQueryToFindProdLikedBySimilarSocialUsers(userInteractionMap, sortedUsers, contentFilter, MAX_USER_OCCURENCE_COUNT);
		
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
			Map<String, Double> userInteractionMap, List<String> sortedUsers, ContentFilter contentFilter, int maxUserOccurence) {
		StringBuilder purchaseQueryBuilder = new StringBuilder();
		StringBuilder markedAsFavoriteQueryBuilder = new StringBuilder();
		StringBuilder viewedQueryBuilder = new StringBuilder();

		purchaseQueryBuilder.append("users_purchased:(");
		markedAsFavoriteQueryBuilder.append("users_marked_favorite:(");
		viewedQueryBuilder.append("users_viewed:(");
		//  max users
		
		for (String user : sortedUsers) {
			purchaseQueryBuilder.append("\"" + user + "\"^" + userInteractionMap.get(user) + " OR ");
			markedAsFavoriteQueryBuilder.append("\"" + user + "\"^" + (userInteractionMap.get(user) / 2) + " OR ");
			viewedQueryBuilder.append("\"" + user + "\"^" + (userInteractionMap.get(user) / 3) + " OR ");
			
		}
		
		
		
		if (purchaseQueryBuilder.length() > ("users_purchased:(").length()){
			purchaseQueryBuilder.replace(purchaseQueryBuilder.length() - 3, purchaseQueryBuilder.length(), ")");
		} else {
			purchaseQueryBuilder.append("\"\")");
		}
		if (markedAsFavoriteQueryBuilder.length() > ("users_marked_favorite:(").length()){
			markedAsFavoriteQueryBuilder.replace(markedAsFavoriteQueryBuilder.length() - 3, markedAsFavoriteQueryBuilder.length(), ")");
		} else {
			markedAsFavoriteQueryBuilder.append("\"\")");
		}
		if (viewedQueryBuilder.length() > ("users_viewed:(").length()){
			viewedQueryBuilder.replace(viewedQueryBuilder.length() - 3, viewedQueryBuilder.length(), ")");
		} else {
			viewedQueryBuilder.append("\"\")");
		}
		
		return purchaseQueryBuilder.toString() + " OR " + markedAsFavoriteQueryBuilder.toString() + " OR " + viewedQueryBuilder.toString();
	}

	protected ModifiableSolrParams initMLTParams(String filterQuery, int maxResultCount, String query) {
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("stream.body", query);
		params.set("fq", filterQuery);
		params.set("mlt.fl", "content");
		params.set("fl", "source,score");
		params.set("rows", 30);
		params.set("mlt.mindf", "1");
		params.set("mlt.mintf", "1");
		params.set("mlt.minwl", "4");
		params.set("mlt.maxqt", "15");
		return params;
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
		return StrategyType.SocialStream;
	}

}
