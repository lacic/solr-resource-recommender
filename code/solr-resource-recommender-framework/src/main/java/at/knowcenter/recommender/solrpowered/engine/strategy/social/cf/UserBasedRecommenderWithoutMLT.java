package at.knowcenter.recommender.solrpowered.engine.strategy.social.cf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class UserBasedRecommenderWithoutMLT implements RecommendStrategy {

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
			
			if (contentFilter == null) {
				searchResponse.setNumFound(0);
				searchResponse.setResultItems(recommendations);
				searchResponse.setElapsedTime(-1);
				return searchResponse;
			}
			
			StringBuilder queryBuilder = new StringBuilder("interests:(");
			
			if (contentFilter.getCustomer() != null && contentFilter.getCustomer().getInterests() != null) {
				for (String interest : contentFilter.getCustomer().getInterests()) {
					queryBuilder.append("\"" + interest + "\" OR ");
				}
				queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ")");
			} else {
				queryBuilder.append("\"\")");
			}

			queryBuilder.append(" OR customergroup:(");
			
			if (contentFilter.getCustomer() != null && contentFilter.getCustomer().getCustomergroup() != null) {
				for (String customerGroup : contentFilter.getCustomer().getCustomergroup()) {
					queryBuilder.append("\"" + customerGroup + "\" OR ");
				}
				queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ")");
			} else {
				queryBuilder.append("\"\")");
			}
			
			
			solrParams = new ModifiableSolrParams();
			
			solrParams.set("q", queryBuilder.toString());
			solrParams.set("fq", "-id:(\"" + query.getUser() + "\")");
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
			List<String> productsToFilter = RecommendationQueryUtils.appendItemsToStoredList(alreadyBoughtProducts, query.getProductIds());
			filterQueryString += RecommendationQueryUtils.buildFilterForAlreadyBoughtProducts(productsToFilter);
		}
		solrParams.set("q", queryString);
		solrParams.set("fq", filterQueryString);
		solrParams.set("fl", "id");
		solrParams.set("rows", maxReuslts);
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
		return StrategyType.UB_WithOutMLT;
	}
	
}