package at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.shared.puresocial;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.CFQueryBuilder;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.Position;
import at.knowcenter.recommender.solrpowered.model.PositionNetwork;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.model.SharedLocation;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.cleaner.DataFetcher;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class SharedRegionsCommonBasedRecPS implements RecommendStrategy{

	private List<String> alreadyPurchasedResources;
	private ContentFilter contentFilter;

	@Override
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts) {
		RecommendResponse searchResponse = new RecommendResponse();

		String user = query.getUser();
		
		if (user == null) {
			searchResponse.setResultItems(new ArrayList<String>());
			return searchResponse;
		}
		
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		try {
			String q = "id:" + user;
			
			solrParams.set("q", q);
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fl", "shared_region_id");
			
			
			QueryResponse response = SolrServiceContainer.getInstance().getSharedLocationService().getSolrServer().query(solrParams);
			List<SharedLocation> sharedLocations = response.getBeans(SharedLocation.class);
			
			if (sharedLocations == null || sharedLocations.size() == 0 || 
					sharedLocations.get(0).getSharedRegions() == null) {
				searchResponse.setResultItems(new ArrayList<String>());
				return searchResponse;
			}
			
			List<String> sharedRegions = sharedLocations.get(0).getSharedRegions();
			
			List<String> neighbours = DataFetcher.getSocialNeighbourUsers(user);
			StringBuilder queryBuilder = new StringBuilder("id:(");
			
			for (String id : neighbours){
				queryBuilder.append(id + " OR ");
			}
			
			if (neighbours.size() > 0) {
				queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ")");
			} else {
				queryBuilder.append("\"\")");
			}
			
			solrParams = new ModifiableSolrParams();

			solrParams.set("q", queryBuilder.toString());
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fl", "id, shared_region_id");
			solrParams.set("fq", "shared_region_id:[* TO *] AND -id:" + user);
			
			response = SolrServiceContainer.getInstance().getSharedLocationService().getSolrServer().query(solrParams);
			sharedLocations = response.getBeans(SharedLocation.class);
			
			final Map<String, Double> commonNeighborMap = new HashMap<String, Double>();

			for (SharedLocation sharedLocation : sharedLocations){
				String commonUser = sharedLocation.getUserId();
				Set<String> intersection = new HashSet<String>(sharedLocation.getSharedRegions());
				
				intersection.retainAll(sharedRegions);
				
				commonNeighborMap.put(commonUser, (double)intersection.size());
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
		} catch (SolrServerException e) {
			System.out.println(solrParams);
			e.printStackTrace();
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
		return StrategyType.CF_Loc_Shared_Regions_Common;
	}

}
