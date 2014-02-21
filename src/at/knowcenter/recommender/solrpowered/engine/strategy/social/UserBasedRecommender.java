package at.knowcenter.recommender.solrpowered.engine.strategy.social;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public abstract class UserBasedRecommender implements RecommendStrategy {

	public static int MAX_USER_OCCURENCE_COUNT = 60;
	private List<String> alreadyBoughtProducts;
	private ContentFilter contentFilter;

	@Override
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts, SolrServer solrServer){
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
			
			if (contentFilter == null || contentFilter.getCustomer() == null) {
				searchResponse.setNumFound(0);
				searchResponse.setResultItems(recommendations);
				searchResponse.setElapsedTime(-1);
			}
			
			String queryString = "id:(" + contentFilter.getCustomer().getId() + ")";
			
			solrParams = createMLTParams(queryString, "", maxReuslts);

			response = SolrServiceContainer.getInstance().getUserService().getSolrServer().query(solrParams);
			step1ElapsedTime = response.getElapsedTime();
			
			Map<String, Float> customerScoringMap = new HashMap<String, Float>();
			
			
			if(response != null){
				searchResponse.setNumFound(response.getResults().getNumFound()-1);
				searchResponse.setElapsedTime(response.getElapsedTime());
				NamedList<Object> resLists = response.getResponse();
				Object mlt = resLists.get("moreLikeThis");
				@SuppressWarnings("unchecked")
				SimpleOrderedMap<SolrDocumentList> listOfMltResults = (SimpleOrderedMap<SolrDocumentList>) mlt;
				
				if (listOfMltResults.size() > 0) {
					SolrDocumentList mltsPerDoc = listOfMltResults.getVal(0);
					
					float score = mltsPerDoc.size() + 1.1f;
					
					for (SolrDocument solrDocument : mltsPerDoc) {
						customerScoringMap.put(
								RecommendationQueryUtils.serializeSolrDocToSearchCustomer(solrDocument),
								score);
						score -= 1.0f;
					}
				}
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
	
	
	protected abstract ModifiableSolrParams createMLTParams(String query, String filterQuery, int maxResultCount);

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
	
}