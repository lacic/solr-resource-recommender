package at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.puresocial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.CFQueryBuilder;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemQuery;

/**
 * Collaborative Filtering Recommender strategy
 * @author elacic
 *
 */
public class ReviewBasedRecPS implements RecommendStrategy {

	public static final String USERS_RATED_1_FIELD = "users_rated_1";
	public static final String USERS_RATED_2_FIELD = "users_rated_2";
	public static final String USERS_RATED_3_FIELD = "users_rated_3";
	public static final String USERS_RATED_4_FIELD = "users_rated_4";
	public static final String USERS_RATED_5_FIELD = "users_rated_5";
	private static final int MIN_FACET_USER_COUNT = 1;
	public static int MAX_USER_OCCURENCE_COUNT = CFQueryBuilder.MAX_USER_OCCURENCE_COUNT;
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
						searchResponse.setResultItems(new ArrayList<String>());
						return searchResponse;
					}
				}
			}
			// STEP 1 - find similar users for same products
			solrParams = createParamsToFindSimilarUsers(query, 
					new String[]{ USERS_RATED_5_FIELD, USERS_RATED_4_FIELD, USERS_RATED_3_FIELD, USERS_RATED_2_FIELD, USERS_RATED_1_FIELD });

			response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(solrParams);
			step1ElapsedTime = response.getElapsedTime();
			
			List<FacetField> facetFields = response.getFacetFields();

			// STEP 2 - find products liked by similar users
			solrParams = getSTEP2Params(query, maxReuslts, facetFields);

			SolrServer server = SolrServiceContainer.getInstance().getResourceService().getSolrServer();
			response = server.query(solrParams);
			
			// fill response object
			List<Resource> beans = response.getBeans(Resource.class);
			searchResponse.setResultItems(RecommendationQueryUtils.extractRecommendationIds(beans));
			searchResponse.setElapsedTime(step0ElapsedTime + step1ElapsedTime + response.getElapsedTime());

			SolrDocumentList docResults = response.getResults();
			searchResponse.setNumFound(docResults.getNumFound());
		} catch (Exception e) {
			System.out.println(solrParams);
			e.printStackTrace();
			searchResponse.setNumFound(0);
			searchResponse.setResultItems(recommendations);
			searchResponse.setElapsedTime(-1);
		}
		
		return searchResponse;
	}


	private ModifiableSolrParams getSTEP2Params(
			RecommendQuery query, Integer maxReuslts, List<FacetField> userFacets) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		
		String queryString = "";
		
		for (FacetField userFacet : userFacets) {
			double searchRank = 0.0;
			
			if (userFacet.getName().equals(USERS_RATED_5_FIELD)) {
				searchRank = 1.0;
			}
			if (userFacet.getName().equals(USERS_RATED_4_FIELD)) {
				searchRank = 2.0;
			}
			if (userFacet.getName().equals(USERS_RATED_3_FIELD)) {
				searchRank = 3.0;
			}
			if (userFacet.getName().equals(USERS_RATED_2_FIELD)) {
				searchRank = 4.0;
			}
			if (userFacet.getName().equals(USERS_RATED_1_FIELD)) {
				searchRank = 5.0;
			}

			queryString += RecommendationQueryUtils.
					createQueryToFindProdLikedBySimilarUsers(userFacet.getValues(), query.getUser(), contentFilter, userFacet.getName(), MAX_USER_OCCURENCE_COUNT, searchRank) + " OR ";
		}
		
		// remove last OR
		queryString = queryString.substring(0, queryString.length() - 3);
		
		String filterQueryString = 
				RecommendationQueryUtils.buildFilterForContentBasedFiltering(contentFilter);
		
		if ((query.getProductIds() != null && query.getProductIds().size() > 0) || (alreadyBoughtProducts != null && alreadyBoughtProducts.size() > 0)) {
			if (filterQueryString.trim().length() > 0) {
				filterQueryString += " OR ";
			}
			filterQueryString += RecommendationQueryUtils.buildFilterForAlreadyBoughtProducts("id",
					RecommendationQueryUtils.createUniqueProducts(alreadyBoughtProducts, query.getProductIds()));
		}
		solrParams.set("q", queryString);
		solrParams.set("fq", filterQueryString);
		solrParams.set("fl", "id");
		solrParams.set("rows", maxReuslts);
		return solrParams;
	}

	private ModifiableSolrParams createParamsToFindSimilarUsers(RecommendQuery query, String... facetFields) {
		String queryString;
		ModifiableSolrParams solrParams;
		solrParams = new ModifiableSolrParams();
		queryString = RecommendationQueryUtils.createQueryToFindSimilarUsersForSameAttribute("id", query.getProductIds());
		
		solrParams.set("q", queryString);
		solrParams.set("fl", "id");
		
		solrParams.set("facet", "true");
		solrParams.set("facet.field", facetFields);
		solrParams.set("facet.limit", -1);
		solrParams.set("facet.mincount", MIN_FACET_USER_COUNT);
		return solrParams;
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
		return StrategyType.CF_Review;
	}

}
