package at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.distance.puresocial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
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

public class RegionsPhysicalDistanceBasedRecPS implements RecommendStrategy{

	private List<String> alreadyPurchasedResources;
	private ContentFilter contentFilter;
	private SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

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
			solrParams.set("fl", "time, region_id, location_in_region");
			
			
			QueryResponse response = SolrServiceContainer.getInstance().getPositionService().getSolrServer().query(solrParams);
			List<Position> positions = response.getBeans(Position.class);
			
			if (positions == null || positions.size() == 0) {
				searchResponse.setResultItems(new ArrayList<String>());
				return searchResponse;
			}
			
			Map<Date, Long> timeRegionMap = new HashMap<Date, Long>();
			Map<Date, String[]> timeLocationMap = new HashMap<Date, String[]>();

			for (Position userPosition : positions){
				Date time = userPosition.getTime();
				Long regionId = userPosition.getRegionId();
				String location = userPosition.getLocationInRegion();
				
				if (!timeRegionMap.containsKey(time)) {
					timeRegionMap.put(time, regionId);
					timeLocationMap.put(time, location.split(" "));
				}
			}
			
			
			List<String> neighbours = DataFetcher.getSocialNeighbourUsers(user);
			
			StringBuilder queryBuilder = new StringBuilder("user:(");
			
			for (String id : neighbours) {
				queryBuilder.append("\"" + id + "\" OR ");
			}
			
			if (neighbours.size() > 0 ) {
				queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ")");
			} else {
				queryBuilder.append("\"\")");
			}
			
			solrParams = new ModifiableSolrParams();
			solrParams.set("q", queryBuilder.toString());
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fl", "user, time, region_id, location_in_region");
			solrParams.set("fq", "-user:" + user);
			
			response = SolrServiceContainer.getInstance().getPositionService().getSolrServer().query(solrParams);
			positions = response.getBeans(Position.class);
			
			
			Map<String, Map<Date, String[]>> userTimeLocationMapping = new HashMap<String, Map<Date, String[]>>();
			for (Position userPosition : positions){
				String commonUser = userPosition.getUser();
				Date time = userPosition.getTime();
				String location = userPosition.getLocationInRegion();
				
				Map<Date, String[]> timeLocationMapping = userTimeLocationMapping.get(commonUser);
				
				if (timeLocationMapping == null) {
					timeLocationMapping = new HashMap<Date, String[]>();
				}
				
				timeLocationMapping.put(time, location.split(" "));
				userTimeLocationMapping.put(commonUser, timeLocationMapping);
			}
			
			final Map<String, Double> commonNeighborMap = new HashMap<String, Double>();

			for (String commonUser : userTimeLocationMapping.keySet()){
				
				Map<Date, String[]> commonUserTimeLocationMap = userTimeLocationMapping.get(commonUser);
				
				double meanDistance = 0.0;
				for (Date time : commonUserTimeLocationMap.keySet()) {
					String[] commonUserLocation = commonUserTimeLocationMap.get(time);
					String[] targetUserLocation = timeLocationMap.get(time);
					
					Integer x1 = Integer.parseInt(commonUserLocation[0]);
					Integer y1 = Integer.parseInt(commonUserLocation[1]);
					Integer x2 = Integer.parseInt(targetUserLocation[0]);
					Integer y2 = Integer.parseInt(targetUserLocation[1]);
					
					meanDistance += Math.sqrt( Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
				}
				meanDistance = meanDistance / commonUserTimeLocationMap.keySet().size();
				
				commonNeighborMap.put(commonUser, meanDistance);
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
		return StrategyType.CF_Loc_Physical_Distance_in_Region;
	}

}
