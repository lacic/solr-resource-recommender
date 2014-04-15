package at.knowcenter.recommender.solrpowered.engine.strategy.marketplace;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

/**
 * Most popular based on the review core Recommender strategy
 * @author elacic
 *
 */
public class MPReviewBasedRec implements RecommendStrategy {

	private List<String> alreadyBoughtProducts;
	private ContentFilter contentFilter;
	private List<String> productsToFilter;


	@Override
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts){
		productsToFilter = new ArrayList<String>();

		String queryString = "*:*";
		String sortCriteria = "user_count_purchased desc";
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		RecommendResponse searchResponse = new RecommendResponse();
		
		String filterQueryString = 
				RecommendationQueryUtils.buildFilterForContentBasedFiltering(contentFilter);
		
		
		if (query.getProductIds() != null && query.getProductIds().size() > 0) {
			productsToFilter.addAll(query.getProductIds());
		}
		if (query.getUser() != null ) {
			if (alreadyBoughtProducts != null) {
				productsToFilter.addAll(alreadyBoughtProducts);
			}
		}
		
		if (productsToFilter != null && productsToFilter.size() > 0) {
			if (filterQueryString.length() > 0) {
				filterQueryString += " OR ";
			}
			filterQueryString += RecommendationQueryUtils.buildFilterForAlreadyBoughtProducts("product_id", productsToFilter);
		}
		
		
		solrParams.set("q", queryString);
		solrParams.set("fq", filterQueryString);
		solrParams.set("facet", "true");
		solrParams.set("facet.field", "product_id");
		solrParams.set("facet.limit", maxReuslts);
		solrParams.set("facet.mincount", 1);

		try {
			response = SolrServiceContainer.getInstance().getReviewService().getSolrServer().query(solrParams);
		} catch (Exception e) {
			System.out.println(solrParams);
			e.printStackTrace();
		}

		List<FacetField> facetFields = response.getFacetFields();
		// get the first and only facet field -> users
		FacetField userFacet = facetFields.get(0);
		List<String> products = new ArrayList<String>();
		
		for(Count c : userFacet.getValues()) {
			products.add(c.getName());
		}
		
		
		searchResponse.setResultItems(products);
		searchResponse.setElapsedTime(response.getElapsedTime());
		SolrDocumentList docResults = response.getResults();
		searchResponse.setNumFound(docResults.getNumFound());
		
		return searchResponse;
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
		return StrategyType.MostPopular_Review;
	}

}
