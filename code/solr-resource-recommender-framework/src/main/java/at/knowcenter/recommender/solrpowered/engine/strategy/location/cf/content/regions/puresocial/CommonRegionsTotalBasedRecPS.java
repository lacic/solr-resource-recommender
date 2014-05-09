package at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.regions.puresocial;

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
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.cleaner.DataFetcher;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class CommonRegionsTotalBasedRecPS implements RecommendStrategy{

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
			String q = "user:" + user;
			
			solrParams.set("q", q);
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fl", "region_id");
			
			
			QueryResponse response = SolrServiceContainer.getInstance().getPositionService().getSolrServer().query(solrParams);
			List<Position> positions = response.getBeans(Position.class);
			
			Set<Long> regions = new HashSet<Long>();
			for (Position userPosition : positions){
				Long regionId = userPosition.getRegionId();
				
				regions.add(regionId);
			}
			
			Map<String, Set<Long>> userRegionsMapping = new HashMap<String, Set<Long>>();
			
			StringBuilder queryBuilder = new StringBuilder("user:(");
			
			List<String> neighbours = DataFetcher.getSocialNeighbourUsers(user);
			
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
			solrParams.set("fl", "user,region_id");
			solrParams.set("fq", "region_id:[* TO *] AND -user:" + user);
			
			response = SolrServiceContainer.getInstance().getPositionService().getSolrServer().query(solrParams);
			positions = response.getBeans(Position.class);
			
			for (Position userPosition : positions){
				String regionUser = userPosition.getUser();
				
				Set<Long> userRegions = userRegionsMapping.get(regionUser);
				
				if (userRegions == null) {
					userRegions = new HashSet<Long>();
				}
				
				userRegions.add(userPosition.getRegionId());
				
				userRegionsMapping.put(regionUser, userRegions);
			}
			
			final Map<String, Double> commonNeighborMap = new HashMap<String, Double>();

			for (String commonUser : userRegionsMapping.keySet()) {
				Set<Long> union = userRegionsMapping.get(commonUser);
				union.addAll(regions);
				
				commonNeighborMap.put(commonUser, (double)union.size());
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
		return StrategyType.CF_Loc_Total_Regions;
	}

}
