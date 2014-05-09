package at.knowcenter.recommender.solrpowered.engine.strategy.marketplace;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
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
 * Most popular (purchased) Recommender strategy
 * @author elacic
 *
 */
public class MostPopularRec implements RecommendStrategy {

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
		
		try {
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
				filterQueryString += RecommendationQueryUtils.buildFilterForAlreadyBoughtProducts(productsToFilter);
			}
			solrParams.set("q", queryString);
			solrParams.set("fq", filterQueryString);
			solrParams.set("sort", sortCriteria);
			solrParams.set("rows", maxReuslts);
			

			response = SolrServiceContainer.getInstance().getRecommendService().getSolrServer().query(solrParams);
			// fill response object
			List<String> extractedRecommendations = 
					RecommendationQueryUtils.extractRecommendationIds(
							response.getBeans(CustomerAction.class)
							);
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
		return StrategyType.MostPopular;
	}

}
