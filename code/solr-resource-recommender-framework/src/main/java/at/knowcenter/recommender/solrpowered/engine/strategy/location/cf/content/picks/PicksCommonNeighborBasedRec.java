package at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.picks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.CFQueryBuilder;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.evaluation.UserSimilarityTracker;
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.Position;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class PicksCommonNeighborBasedRec implements RecommendStrategy{

	private List<String> alreadyPurchasedResources;
	private ContentFilter contentFilter;

	@Override
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts) {
		RecommendResponse searchResponse = new RecommendResponse();
		final String user = query.getUser();

		if (user == null || contentFilter.getCustomer() == null) {
			searchResponse.setResultItems(new ArrayList<String>());
			return searchResponse;
		}
		
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		try {
			List<String> favoriteRegions = contentFilter.getCustomer().getFavoriteRegions();
			
			if (favoriteRegions == null) {
				searchResponse.setNumFound(0);
				searchResponse.setResultItems(new ArrayList<String>());
				searchResponse.setElapsedTime(-1);
				return searchResponse;
			}

			StringBuilder queryBuilder = new StringBuilder("favorite_regions:(");
			
			for (String region : favoriteRegions) {
				queryBuilder.append("\"" + region + "\" OR ");
			}
			
			if (favoriteRegions.size() > 0 ) {
				queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ")");
			} else {
				queryBuilder.append("\"\")");
			}
			
			solrParams.set("q", queryBuilder.toString());
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fq", "-id:" + user);
			
			
			QueryResponse response = SolrServiceContainer.getInstance().getUserService().getSolrServer().query(solrParams);
			List<Customer> customers = response.getBeans(Customer.class);
			
			
			final Map<String, Double> commonNeighborMap = new HashMap<String, Double>();
			for (Customer commonCustomer : customers) {
				List<String> commonCustomerRegions = commonCustomer.getFavoriteRegions();
				commonCustomerRegions.retainAll(favoriteRegions);
				
				commonNeighborMap.put(commonCustomer.getId(), (double)commonCustomerRegions.size());
			}
			
			Comparator<String> interactionCountComparator = new Comparator<String>() {

				@Override
				public int compare(String a, String b) {
					if (commonNeighborMap.get(a) >= commonNeighborMap.get(b)) {
			            return -1;
			        } else {
			            return 1;
			        }
				}
				
			};
			
			Thread t = new Thread() {
				@Override public void run() {
					UserSimilarityTracker.getInstance().writeToFile("loc_content_picks_cn", user, commonNeighborMap);
				}
			};
			t.start();
			
	        TreeMap<String,Double> sortedMap = new TreeMap<String,Double>(interactionCountComparator);
	        sortedMap.putAll(commonNeighborMap);
			
	        ModifiableSolrParams cfParams = CFQueryBuilder.getCFStep2Params(
	        		query, maxReuslts, 
	        		commonNeighborMap, sortedMap.keySet(), 
	        		contentFilter, alreadyPurchasedResources);
	        
	        response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(cfParams);
			
			// fill response object
			List<Resource> beans = response.getBeans(Resource.class);
			searchResponse.setResultItems(RecommendationQueryUtils.extractRecommendationIds(beans));
			searchResponse.setElapsedTime(response.getElapsedTime());

			SolrDocumentList docResults = response.getResults();
			searchResponse.setNumFound(docResults.getNumFound());
		} catch (Exception e) {
			System.out.println("E: " + solrParams);
			e.printStackTrace();
			searchResponse.setNumFound(0);
			searchResponse.setResultItems(new ArrayList<String>());
			searchResponse.setElapsedTime(-1);
		}
		
		return searchResponse;
	}

	@Override
	public void setAlreadyPurchasedResources(List<String> purchasedResourceIds) {
		this.alreadyPurchasedResources = purchasedResourceIds;
	}

	@Override
	public List<String> getAlreadyBoughtProducts() {
		return alreadyPurchasedResources;
	}

	@Override
	public void setContentFiltering(ContentFilter contentFilter) {
		this.contentFilter = contentFilter;
	}

	@Override
	public StrategyType getStrategyType() {
		return StrategyType.CF_Loc_Picks_CN;
	}

}
