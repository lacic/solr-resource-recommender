package at.knowcenter.recommender.solrpowered.engine.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.ReviewBasedRec;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;

public class CFQueryBuilder {
	
	public static final Integer MAX_USER_OCCURENCE_COUNT = 60;


	public static ModifiableSolrParams getInteractionsFromMeAndUSersThatILikedOrCommented(String user) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		
		String queryString = "id:(\"" + user + "\")";
		
		solrParams.set("q", queryString);
		return solrParams;
	}
	
	
	
	public static ModifiableSolrParams getCFStep2Params(
			RecommendQuery query, Integer maxReuslts, 
			Map<String, Double> userInteractionMap, Set<String> sortedKeys, 
			ContentFilter contentFilter, List<String> alreadyBoughtProducts) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		
		String queryString = createQueryToFindProdLikedBySimilarSocialUsers(
				userInteractionMap, sortedKeys, contentFilter, MAX_USER_OCCURENCE_COUNT);
		
		String filterQueryString = 
				RecommendationQueryUtils.buildFilterForContentBasedFiltering(contentFilter);
		
		if (alreadyBoughtProducts != null && alreadyBoughtProducts.size() > 0) {
			if (filterQueryString.trim().length() > 0) {
				filterQueryString += " OR ";
			}
			filterQueryString += RecommendationQueryUtils.buildFilterForAlreadyBoughtProducts("id",alreadyBoughtProducts);
		}
		
		solrParams.set("q", queryString);
		solrParams.set("fq", filterQueryString);
		solrParams.set("fl", "id");
		solrParams.set("rows", maxReuslts);
		return solrParams;
	}
	
	private static String createQueryToFindProdLikedBySimilarSocialUsers(
			Map<String, Double> userInteractionMap, Set<String> sortedKeys, ContentFilter contentFilter, int maxUserOccurence) {
		String query = createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedKeys, contentFilter, ReviewBasedRec.USERS_RATED_5_FIELD, maxUserOccurence, 1.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedKeys, contentFilter, ReviewBasedRec.USERS_RATED_4_FIELD, maxUserOccurence, 2.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedKeys, contentFilter, ReviewBasedRec.USERS_RATED_3_FIELD, maxUserOccurence, 3.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedKeys, contentFilter, ReviewBasedRec.USERS_RATED_2_FIELD, maxUserOccurence, 4.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedKeys, contentFilter, ReviewBasedRec.USERS_RATED_1_FIELD, maxUserOccurence, 5.0);
		
		return query;
	}
	
	private static String createQueryToFindProdLikedBySimilarUsers(
			Map<String, Double> userInteractionMap, 
			Set<String> sortedKeys, ContentFilter contentFilter,
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
		
		for (String user : sortedKeys) {
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

}
