package at.knowcenter.recommender.solrpowered.engine.strategy.social;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class CFCategoryRecommender implements RecommendStrategy {

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
					if (alreadyBoughtProducts != null && alreadyBoughtProducts.size() > 0) {
						query.setProductIds(alreadyBoughtProducts);
					} else {
						searchResponse.setNumFound(0);
						searchResponse.setResultItems(recommendations);
						searchResponse.setElapsedTime(-1);
						return searchResponse;
					}
				}
			}
			
			if (contentFilter == null || contentFilter.getCustomer() == null ||
					contentFilter.getCustomer().getPurchasedCategories() == null ||
					contentFilter.getCustomer().getPurchasedCategories().size() == 0) {
				searchResponse.setNumFound(0);
				searchResponse.setResultItems(recommendations);
				searchResponse.setElapsedTime(-1);
				return searchResponse;
			}
			
			StringBuilder queryBuilder = new StringBuilder("");
			for (String product : alreadyBoughtProducts) {
				queryBuilder.append("id:(\"" + product + "\" OR ");
			}
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), "");
			
			solrParams.set("q", queryBuilder.toString());
			solrParams.set("rows", Integer.MAX_VALUE);
			response = SolrServiceContainer.getInstance().getItemService().getSolrServer().query(solrParams);
			
			List<Item> items = response.getBeans(Item.class);
			Set<String> knownCategories = new HashSet<String>();
			for (Item item : items) {
				if (item.getTags() != null) {
					knownCategories.addAll(item.getTags());
				}
			}
			
			
			queryBuilder = new StringBuilder("purchased_categories:(");

			for (String category : contentFilter.getCustomer().getPurchasedCategories()) {
				if (knownCategories.contains(category)) {
					queryBuilder.append("\"" + category + "\" OR ");
				}
			}
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ")");
			
			solrParams = new ModifiableSolrParams();
			
			solrParams.set("q", queryBuilder.toString());
			solrParams.set("fq", "-id:(\"" + contentFilter.getCustomer().getId() + "\")");
			solrParams.set("rows", 60);
			solrParams.set("fl", "id,score");

			response = SolrServiceContainer.getInstance().getUserService().getSolrServer().query(solrParams);
			step1ElapsedTime = response.getElapsedTime();
			
			Map<String, Float> customerScoringMap = new HashMap<String, Float>();
			
			searchResponse.setNumFound(response.getResults().getNumFound()-1);
			searchResponse.setElapsedTime(response.getElapsedTime());
			
			List<Customer> resLists = response.getBeans(Customer.class);

			for (Customer customer : resLists) {
				customerScoringMap.put(customer.getId(), customer.getScore());
			}

			solrParams = getSTEP2Params(query, maxReuslts, customerScoringMap);
			
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
	
	private ModifiableSolrParams getSTEP2Params(RecommendQuery query, Integer maxReuslts, Map<String, Float> customerScoringMap) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		
		String queryString = RecommendationQueryUtils.createQueryToFindProdLikedBySimilarSocialUsers(customerScoringMap, contentFilter, MAX_USER_OCCURENCE_COUNT);
		
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
		return StrategyType.CF_Categories;
	}
	
}