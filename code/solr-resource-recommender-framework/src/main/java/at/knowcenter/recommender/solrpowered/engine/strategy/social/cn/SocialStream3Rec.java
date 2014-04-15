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
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.ReviewBasedRec;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.model.SocialAction;
import at.knowcenter.recommender.solrpowered.model.SocialStream;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemQuery;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemResponse;
import at.knowcenter.recommender.solrpowered.services.impl.item.MoreLikeThisRequest;

/**
 * Social stream recommender, makes a bag of word of the latest user's comments/wall-posts and finds similar content.
 * Based on the found similar content, users are extracted and weighted using the similarity score. From the most similar users
 * purchased products are extracted.
 * @author elacic
 *
 */
public class SocialStream3Rec implements RecommendStrategy {

	public static int MAX_USER_OCCURENCE_COUNT = 60;
	private List<String> alreadyBoughtProducts;
	private ContentFilter contentFilter;
	
	private List<String> productsToFilter;

	@Override
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts){
		productsToFilter = new ArrayList<String>();
		QueryResponse response = null;
		RecommendResponse searchResponse = new RecommendResponse();
		
		long step0ElapsedTime = 0;
		List<String> recommendations = new ArrayList<String>();

		try {
			// STEP 0 - get products from a user
			if (query.getProductIds() != null && query.getProductIds().size() > 0) {
				productsToFilter.addAll(query.getProductIds());
			}
			if (query.getUser() != null ) {
				if (alreadyBoughtProducts != null) {
					productsToFilter.addAll(alreadyBoughtProducts);
				}
			}
			
			StringBuilder socialStreamQueryBuilder = new StringBuilder();
			
			socialStreamQueryBuilder.append("source:(\"" + query.getUser() + "\")");
			
			ModifiableSolrParams params = new ModifiableSolrParams();
			params.set("q", socialStreamQueryBuilder.toString());
			params.set("fq", "content:[* TO *] AND (action_type:(\"COMMENT\") OR action_type:(\"WALLPOST\"))");
//			params.set("fq", "content:[* TO *] AND action_type:(\"WALLPOST\")");
			params.set("rows", 10);
			params.set("sort", "timestamp desc");
			response = SolrServiceContainer.getInstance().getSocialStreamService().getSolrServer().query(params);
			List<SocialStream> userContentStreams = response.getBeans(SocialStream.class);
			
			// build stream body
			StringBuilder streamBuilder = new StringBuilder("\"");
			
			for (SocialStream ss : userContentStreams) {
				streamBuilder.append(ss.getSocialContent() + " ");
			}
			
			streamBuilder.append("\"");
			
			
			
			String mltFilterQuery = "-" + socialStreamQueryBuilder.toString(); 
			params = initMLTParams(mltFilterQuery, maxReuslts, streamBuilder.toString());
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
			
			response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(params);
			// fill response object
			List<Resource> beans = response.getBeans(Resource.class);
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
		
		if (productsToFilter != null && productsToFilter.size() > 0) {
			if (filterQueryString.trim().length() > 0) {
				filterQueryString += " OR ";
			}
			filterQueryString += RecommendationQueryUtils.buildFilterForAlreadyBoughtProducts(productsToFilter);
		}
		solrParams.set("q", queryString);
		solrParams.set("fq", filterQueryString);
		solrParams.set("fl", "id");
		solrParams.set("rows", maxReuslts);
		return solrParams;
	}
	
	
	public static String createQueryToFindProdLikedBySimilarSocialUsers(
			Map<String, Double> userInteractionMap, List<String> sortedUsers, ContentFilter contentFilter, int maxUserOccurence) {
		String query = createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedUsers, contentFilter, ReviewBasedRec.USERS_RATED_5_FIELD, maxUserOccurence, 1.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedUsers, contentFilter, ReviewBasedRec.USERS_RATED_4_FIELD, maxUserOccurence, 2.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedUsers, contentFilter, ReviewBasedRec.USERS_RATED_3_FIELD, maxUserOccurence, 3.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedUsers, contentFilter, ReviewBasedRec.USERS_RATED_2_FIELD, maxUserOccurence, 4.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedUsers, contentFilter, ReviewBasedRec.USERS_RATED_1_FIELD, maxUserOccurence, 5.0);
		
		return query;
	}
	
	private static String createQueryToFindProdLikedBySimilarUsers(
			Map<String, Double> userInteractionMap, 
			List<String> sortedUsers,
			ContentFilter contentFilter,
			String usersFieldName,
			int maxUserOccurence,
			double weightDividor) {
		StringBuilder queryBuilder = new StringBuilder();

		queryBuilder.append(usersFieldName + ":(");
		
		if (weightDividor <= 0.0) {
			queryBuilder.append("\"\")");
			return queryBuilder.toString();
		}
		//  max users
		int userOccurenceCount = 0;
		
		for (String user : sortedUsers) {
			if (userOccurenceCount >= maxUserOccurence) { break; }
			Double boosting = userInteractionMap.get(user) / weightDividor ;
			boosting = ((int) (boosting * 100)) / 100.0;
			
			queryBuilder.append(user + "^" + boosting + " OR ");
			userOccurenceCount++;
		}
		
		if (queryBuilder.length() > (usersFieldName + ":(").length()){
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), "");
		} else {
			queryBuilder.append("\"\"");
		}
		
		queryBuilder.append(")");
		return queryBuilder.toString();
	}
	
	protected ModifiableSolrParams initMLTParams(String filterQuery, int maxResultCount, String query) {
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("stream.body", query);
		params.set("fq", filterQuery);
		params.set("mlt.fl", "content");
		params.set("fl", "source,score");
		params.set("rows", 500);
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
