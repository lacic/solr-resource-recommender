package at.knowcenter.recommender.solrpowered.engine.strategy;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

/**
 * Preceding item based Recommender strategy
 * @author elacic
 *
 */
public class PrecedingItemBasedRecommender implements RecommendStrategy {

	private List<String> alreadyBoughtProducts;
	private ContentFilter contentFilter;

	@Override
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts){
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		RecommendResponse searchResponse = new RecommendResponse();
		QueryResponse response = null;
		
		searchResponse.setElapsedTime(0);
		
		if (alreadyBoughtProducts == null || alreadyBoughtProducts.size() == 0) {
			searchResponse.setResultItems(new ArrayList<String>());
			return searchResponse;
		}
		query.setProductIds(alreadyBoughtProducts);
		
		String filterQuery = "";
		if (contentFilter != null) {
			filterQuery = RecommendationQueryUtils.buildFilterForContentBasedFilteringOnItems(contentFilter);
		}
		if (filterQuery.trim().length() > 0) {
			filterQuery += " OR ";
		}
		filterQuery += RecommendationQueryUtils.buildFilterForAlreadyBoughtProducts(alreadyBoughtProducts);
		
		StringBuilder precedingItemBuilder = new StringBuilder();
		precedingItemBuilder.append("preceedingItemId:(");
		
		for (String alreadyBoughItem : alreadyBoughtProducts) {
			precedingItemBuilder.append("\"" + alreadyBoughItem + "\" OR ");
		}
		
		precedingItemBuilder.replace(precedingItemBuilder.length() - 3, precedingItemBuilder.length(), ")");
		
		solrParams.set("q", precedingItemBuilder.toString());
		solrParams.set("fq", filterQuery);
		solrParams.set("rows", maxReuslts);
		solrParams.set("sort", "validFrom desc");
		

		try {
			response = SolrServiceContainer.getInstance().getItemService().getSolrServer().query(solrParams);
			
			// fill response object
			List<String> extractedRecommendations = 
					RecommendationQueryUtils.extractRecommendationIds(response.getBeans(Item.class));

			// remove already bough
			extractedRecommendations.removeAll(alreadyBoughtProducts);
			
			searchResponse.setResultItems(extractedRecommendations);
			searchResponse.setElapsedTime(response.getElapsedTime());
			SolrDocumentList docResults = response.getResults();
			searchResponse.setNumFound(docResults.getNumFound());
		} catch (SolrServerException e) {
			e.printStackTrace();
			searchResponse.setNumFound(0);
			searchResponse.setResultItems(new ArrayList<String>());
			searchResponse.setElapsedTime(-1);
		}
		
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
		return StrategyType.PrecedingItemBased;
	}

}