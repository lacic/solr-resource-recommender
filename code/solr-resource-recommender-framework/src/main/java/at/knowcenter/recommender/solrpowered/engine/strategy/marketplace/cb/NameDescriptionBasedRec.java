package at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemResponse;
import at.knowcenter.recommender.solrpowered.services.impl.item.MoreLikeThisRequest;
import at.knowcenter.recommender.solrpowered.services.impl.resource.ResourceResponse;

/**
 * Content based resource recommender
 * 
 * Uses either the provided resource id(s) or, if not available, known purchased resources from the given user.
 * Under the hood uses Solr's More Like This query.
 * 
 * @author Emanuel Lacic
 *
 */
public class NameDescriptionBasedRec implements RecommendStrategy {

	private List<String> alreadyBoughtProducts;
	private ContentFilter contentFilter;

	@Override
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts){
		List<Resource> recommendations = new ArrayList<Resource>();
		RecommendResponse searchResponse = new RecommendResponse();
		searchResponse.setElapsedTime(0);
		
		if (query.getProductIds() == null || query.getProductIds().size() == 0) {
			if (alreadyBoughtProducts == null || alreadyBoughtProducts.size() == 0) {
				searchResponse.setResultItems(new ArrayList<String>());
				return searchResponse;
			}
			query.setProductIds(alreadyBoughtProducts);
		}
		
		
		String filterQuery = "";
		if (contentFilter != null) {
			filterQuery = RecommendationQueryUtils.buildFilterForContentBasedFilteringOnItems(contentFilter);
		}
		
		if ((alreadyBoughtProducts != null && alreadyBoughtProducts.size() > 0)) {
			if (filterQuery.trim().length() > 0) {
				filterQuery += " OR ";
			}
			filterQuery += RecommendationQueryUtils.buildFilterForAlreadyBoughtProducts(alreadyBoughtProducts);
		}
		
		try {
			ResourceResponse similarElementsByItemId = getSimilarElementsByItemId( query.getProductIds(), filterQuery, maxReuslts );

			searchResponse.setResultItems(similarElementsByItemId.getResultItems());
			searchResponse.setElapsedTime(searchResponse.getElapsedTime() + similarElementsByItemId.getElapsedTime() );
		} catch (SolrException ex) {
			ex.printStackTrace();
		}

		
		if (alreadyBoughtProducts != null) {
			searchResponse.getResultItems().removeAll(alreadyBoughtProducts);
		}
		
		return searchResponse;
	}
	
	/**
	 * Finds similar resources based on the ids of the provided resources
	 * @param ids provided resource ids to use for finding similar resources
	 * @param filterQuery filtering criteria query
	 * @param maxResultCount number of similar resources to return
	 * @return response containing similar resources
	 */
	public ResourceResponse getSimilarElementsByItemId(List<String> ids, String filterQuery, int maxResultCount){
		StringBuilder itemQueryBuilder = new StringBuilder();
		
		int productCount = 0;
		
		for (String itemId : ids) {
			if (productCount >= RecommendationQueryUtils.MAX_SAME_PRODUCT_COUNT) {
				break;
			}
			itemQueryBuilder.append("id:(\"" + itemId + "\") OR ");
			productCount++;
		}
		
		if (ids.size() > 0) {
			itemQueryBuilder.replace(itemQueryBuilder.length() - 4, itemQueryBuilder.length(), "");
		}
		
		ModifiableSolrParams params = initMLTParams(filterQuery, maxResultCount, itemQueryBuilder.toString());
		
		ResourceResponse searchResponse = new ResourceResponse();
		try {
			MoreLikeThisRequest mltRequest = new MoreLikeThisRequest(params);
			NamedList<Object> resLists = SolrServiceContainer.getInstance().getResourceService().getSolrServer().request(mltRequest);

			List<Resource> resultItems = new ArrayList<Resource>();
			SolrDocumentList similarDocuments =(SolrDocumentList) resLists.get("response");
			if (similarDocuments != null) {
				for (SolrDocument similarDocument : similarDocuments) {
					Resource currentSearchItem = RecommendationQueryUtils.serializeSolrDocToSearchResource(similarDocument);
					resultItems.add(currentSearchItem);
				}
			}
			
			searchResponse.setResultItems(RecommendationQueryUtils.extractRecommendationIds(resultItems));
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}
		
		return searchResponse;
	}

	protected ModifiableSolrParams initMLTParams(String filterQuery, int maxResultCount, String query) {
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("q", query);
		params.set("fq", filterQuery);
		params.set("mlt.fl", "name,description");
		params.set("rows", maxResultCount);
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
		return StrategyType.ContentBased;
	}

}