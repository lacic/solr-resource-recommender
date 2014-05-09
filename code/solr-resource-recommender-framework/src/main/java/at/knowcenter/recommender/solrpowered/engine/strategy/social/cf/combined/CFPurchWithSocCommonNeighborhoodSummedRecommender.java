package at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.combined;

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
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.engine.utils.CFQueryBuilder;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.SocialAction;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemQuery;

/**
 * Collaborative Filtering Recommender strategy
 * Item-Item CF approach where most similar users based on purchases are further weighted with 
 * link strength based on the Preferential Attachment Score between users (is summed)
 * 
 * 
 * @author elacic
 *
 */
public class CFPurchWithSocCommonNeighborhoodSummedRecommender implements RecommendStrategy {

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
						List<String> viewedItems = getViewedItems(query, solrParams, response);
						query.setProductIds(viewedItems);
					}
				}
			}
			// STEP 1 - find similar users for same products
			solrParams = createParamsToFindSimilarUsers(query, "users_purchased");

			response = SolrServiceContainer.getInstance().getRecommendService().getSolrServer().query(solrParams);
			step1ElapsedTime = response.getElapsedTime();

			List<FacetField> facetFields = response.getFacetFields();

			// get the first and only facet field -> users
			FacetField userFacet = facetFields.get(0);

			Map<String, Long> userConfMapping = createUserConfidenceMapping(userFacet);
			searchResponse.setUserConfidenceMapping(userConfMapping);

			// STEP 2 - find products liked by similar users
			solrParams = findProductsLikedBySimilarUsers(query, maxReuslts, userFacet);
			// TODO Facet for confidence value
			final SolrServer server = SolrServiceContainer.getInstance().getRecommendService().getSolrServer();
			//System.out.println(server.hashCode());

			//try {
			//System.out.println(solrParams);
			response = server.query(solrParams);
			//} catch(SolrException e) {
			//System.out.println("...>" + solrParams);
			//}

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

	/**
	 * if for valid user no products are defined and the user has never bought anything, 
	 * use viewed items
	 * @param query
	 * @param solrServer
	 * @param solrParams
	 * @param response
	 * @return
	 */
	private List<String> getViewedItems(RecommendQuery query,
			ModifiableSolrParams solrParams,
			QueryResponse response) {
		createQueryForViewedItems(query, solrParams);

		try {
			response = SolrServiceContainer.getInstance().getRecommendService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<String> viewedItems = RecommendationQueryUtils.createUserProductsList(response);
		return viewedItems;
	}

	private void createQueryForViewedItems(RecommendQuery query,
			ModifiableSolrParams solrParams) {
		String queryString = "users_marked_favorite:(\"" +  query.getUser() + "\")^2 OR users_viewed:(\"" +  query.getUser() + "\")";
		solrParams.set("q", queryString);
		solrParams.set("fl", "id");
		solrParams.set("rows",Integer.MAX_VALUE);
	}

	private ModifiableSolrParams findProductsLikedBySimilarUsers(
			RecommendQuery query, Integer maxReuslts, FacetField userFacet) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();

		if (query.getUser() != null) {
			Integer inDegreeSocialNeighSize = findInDegreeSocNeighborhoodSize(query.getUser());
			
			for (Count purchasingUser : userFacet.getValues()) {
				Integer purchUserInDegreeSocialNeighSize = findInDegreeSocNeighborhoodSize(purchasingUser.getName());
				int preferentialAttachmentScore = inDegreeSocialNeighSize * purchUserInDegreeSocialNeighSize;
				preferentialAttachmentScore = (preferentialAttachmentScore == 0) ? 1 : preferentialAttachmentScore;
				purchasingUser.setCount(
						purchasingUser.getCount() + preferentialAttachmentScore);
			}
		}

		String queryString = RecommendationQueryUtils.
				createQueryToFindProdLikedBySimilarUsers(userFacet.getValues(), query.getUser(), contentFilter, "users_purchased", MAX_USER_OCCURENCE_COUNT, 1.0);

		queryString += " OR " + RecommendationQueryUtils.
				createQueryToFindProdLikedBySimilarUsers(userFacet.getValues(), query.getUser(), contentFilter, "users_marked_favorite", MAX_USER_OCCURENCE_COUNT, 2.0);

		queryString += " OR " + RecommendationQueryUtils.
				createQueryToFindProdLikedBySimilarUsers(userFacet.getValues(), query.getUser(), contentFilter, "users_viewed", MAX_USER_OCCURENCE_COUNT, 3.0);


		String filterQueryString = createFilterQuery(query);

		solrParams.set("q", queryString);
		solrParams.set("fq", filterQueryString);
		solrParams.set("fl", "id");
		solrParams.set("rows", maxReuslts);

		return solrParams;
	}

	private Integer findInDegreeSocNeighborhoodSize(String currentUser) {
		Integer inDegreeSocialNeighSize = 0;
		try {
			ModifiableSolrParams params = CFQueryBuilder.
						getInteractionsFromMeAndUSersThatILikedOrCommented(currentUser);
			QueryResponse response = 
					SolrServiceContainer.getInstance().getSocialActionService().getSolrServer().query(params);


			List<SocialAction> socialUsers = response.getBeans(SocialAction.class);

			if (socialUsers.size() != 0) {
				SocialAction currentUserInteractions = socialUsers.get(0);
				
				Set<String> uniqueInDegreeInteractions = new HashSet<String>();
				
				if (currentUserInteractions.getUsersThatCommentedOnMyPost() != null) {
					uniqueInDegreeInteractions.addAll(currentUserInteractions.getUsersThatCommentedOnMyPost());
				}
				
				if (currentUserInteractions.getUsersThatLikedMe() != null) {
					uniqueInDegreeInteractions.addAll(currentUserInteractions.getUsersThatLikedMe());
				}

				inDegreeSocialNeighSize = uniqueInDegreeInteractions.size();
				
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return inDegreeSocialNeighSize;
	}

	private String createFilterQuery(RecommendQuery query) {
		String filterQueryString = 
				RecommendationQueryUtils.buildFilterForContentBasedFiltering(contentFilter);

		if ((query.getProductIds() != null && query.getProductIds().size() > 0) || (alreadyBoughtProducts != null && alreadyBoughtProducts.size() > 0)) {
			if (filterQueryString.trim().length() > 0) {
				filterQueryString += " OR ";
			}
			filterQueryString += RecommendationQueryUtils.buildFilterForAlreadyBoughtProducts(
					RecommendationQueryUtils.createUniqueProducts(alreadyBoughtProducts, query.getProductIds()));
		}

		return filterQueryString;
	}

	private ModifiableSolrParams createParamsToFindSimilarUsers(RecommendQuery query, String facetField) {
		String queryString;
		ModifiableSolrParams solrParams;
		solrParams = new ModifiableSolrParams();
		queryString = RecommendationQueryUtils.createQueryToFindSimilarUsersForSameAttribute("id", query.getProductIds());

		solrParams.set("q", queryString);
		solrParams.set("fl", "id");

		solrParams.set("facet", "true");
		solrParams.set("facet.field", facetField);
		solrParams.set("facet.mincount", MIN_FACET_USER_COUNT);
		return solrParams;
	}

	/**
	 * Creates a map containing the confidence (number of user occurrence) for a user
	 * @param userFacet {@linkplain FacetField} containing number of user occurrences
	 * @return user confidence mapping
	 */
	private Map<String, Long> createUserConfidenceMapping(FacetField userFacet) {
		Map<String, Long> userConfMapping = new HashMap<String, Long>();
		for(Count c : userFacet.getValues()) {
			userConfMapping.put(c.getName(), c.getCount());
		}
		return userConfMapping;
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
		return StrategyType.CollaborativeFiltering;
	}

}
