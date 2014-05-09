package at.knowcenter.recommender.solrpowered.engine.strategy.social.cn;

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
import at.knowcenter.recommender.solrpowered.engine.utils.CFQueryBuilder;
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
 * Collaborative Filtering Recommender strategy
 * @author elacic
 *
 */
public class SocialStreamRec implements RecommendStrategy {

	public static int MAX_USER_OCCURENCE_COUNT = CFQueryBuilder.MAX_USER_OCCURENCE_COUNT;
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
			if (query.getUser() != null ) {
				if (query.getProductIds() == null || query.getProductIds().size() == 0) {
					if (alreadyBoughtProducts != null) {
						query.setProductIds(alreadyBoughtProducts);
					} else {
					}
				}
			}
			
			StringBuilder socialStreamQueryBuilder = new StringBuilder();
			
			socialStreamQueryBuilder.append("source:(\"" + query.getUser() + "\")");
			
			ModifiableSolrParams params = new ModifiableSolrParams();
			params.set("q", socialStreamQueryBuilder.toString());
			params.set("fl", "content");
			params.set("rows", 600);
			params.set("sort", "timestamp desc");

			response = SolrServiceContainer.getInstance().getSocialStreamService().getSolrServer().query(params);
			List<SocialStream> userContentStreams = response.getBeans(SocialStream.class);
			
			// build stream body
			StringBuilder streamBuilder = new StringBuilder("\"");
			
			for (SocialStream ss : userContentStreams) {
				streamBuilder.append(ss.getSocialContent() + " ");
			}
			streamBuilder.append("\"");
			
			params = initMLTParams("-" + socialStreamQueryBuilder.toString(), maxReuslts, streamBuilder.toString());
			
			MoreLikeThisRequest mltRequest = new MoreLikeThisRequest(params);
			NamedList<Object> resLists = SolrServiceContainer.getInstance().getSocialStreamService().getSolrServer().request(mltRequest);

			List<SocialStream> resultSocialStream = new ArrayList<SocialStream>();
			SolrDocumentList similarDocuments =(SolrDocumentList) resLists.get("response");
			
			if (similarDocuments == null || similarDocuments.size() == 0) {
				searchResponse.setNumFound(0);
				searchResponse.setResultItems(recommendations);
				searchResponse.setElapsedTime(-1);
				return searchResponse;
			}
			
			Map<String, Double> similarityMap = new HashMap<String,Double>();
			
			
			for (SolrDocument similarDocument : similarDocuments) {
				SocialStream currentSearchItem = RecommendationQueryUtils.serializeSolrDocToSocialStream(similarDocument);
				resultSocialStream.add(currentSearchItem);

				Double userPostScore = similarityMap.get(currentSearchItem.getSourceUserId());
				if (userPostScore == null) {
					userPostScore = (double)currentSearchItem.getScore();
				} else {
					userPostScore += currentSearchItem.getScore();
				}
				similarityMap.put(currentSearchItem.getSourceUserId(), userPostScore);
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
		params.set("rows", 100);
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
