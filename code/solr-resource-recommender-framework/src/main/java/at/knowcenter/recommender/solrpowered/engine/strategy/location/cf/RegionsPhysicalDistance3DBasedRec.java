package at.knowcenter.recommender.solrpowered.engine.strategy.location.cf;

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
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class RegionsPhysicalDistance3DBasedRec implements RecommendStrategy{

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
			solrParams.set("fl", "time, region_id, location_in_region, zlocal");
			
			
			QueryResponse response = SolrServiceContainer.getInstance().getPositionService().getSolrServer().query(solrParams);
			List<Position> positions = response.getBeans(Position.class);
			
			if (positions == null || positions.size() == 0) {
				searchResponse.setResultItems(new ArrayList<String>());
				return searchResponse;
			}
			
			Map<Date, Long> timeRegionMap = new HashMap<Date, Long>();
			Map<Date, Integer[]> timeLocationMap = new HashMap<Date, Integer[]>();

			for (Position userPosition : positions){
				Date time = userPosition.getTime();
				Long regionId = userPosition.getRegionId();
				String location = userPosition.getLocationInRegion();
				Integer zlocal = userPosition.getzLocal();
				
				if (!timeRegionMap.containsKey(time)) {
					timeRegionMap.put(time, regionId);
					String[] xyCoordinates = location.split(" ");
					Integer[] coordinates = new Integer[3];
					coordinates[0] = Integer.parseInt(xyCoordinates[0]);
					coordinates[1] = Integer.parseInt(xyCoordinates[1]);
					coordinates[2] = zlocal;
					timeLocationMap.put(time, coordinates);
				}
			}
			
			StringBuilder queryBuilder = new StringBuilder("");

			for (Date time : timeRegionMap.keySet()) {
				Long region_id = timeRegionMap.get(time);
				// have to set 2 hours back because date transforms the time for current region
				outFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				queryBuilder.append("(region_id:" + region_id + " AND time:\"" + outFormat.format(time) + "\") OR ");
			}
			
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), "");
			
			solrParams = new ModifiableSolrParams();
			solrParams.set("q", queryBuilder.toString());
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fl", "user, time, region_id, location_in_region, zlocal");
			solrParams.set("fq", "-user:" + user);
			
			response = SolrServiceContainer.getInstance().getPositionService().getSolrServer().query(solrParams);
			positions = response.getBeans(Position.class);
			
			
			Map<String, Map<Date, Integer[]>> userTimeLocationMapping = new HashMap<String, Map<Date, Integer[]>>();
			for (Position userPosition : positions){
				String commonUser = userPosition.getUser();
				Date time = userPosition.getTime();
				String location = userPosition.getLocationInRegion();
				Integer zlocal = userPosition.getzLocal();
				
				Map<Date, Integer[]> timeLocationMapping = userTimeLocationMapping.get(commonUser);
				
				if (timeLocationMapping == null) {
					timeLocationMapping = new HashMap<Date, Integer[]>();
				}
				
				String[] xyCoordinates = location.split(" ");
				Integer[] coordinates = new Integer[3];
				coordinates[0] = Integer.parseInt(xyCoordinates[0]);
				coordinates[1] = Integer.parseInt(xyCoordinates[1]);
				coordinates[2] = zlocal;
				
				timeLocationMapping.put(time, coordinates);
				userTimeLocationMapping.put(commonUser, timeLocationMapping);
			}
			
			final Map<String, Double> commonNeighborMap = new HashMap<String, Double>();

			for (String commonUser : userTimeLocationMapping.keySet()){
				
				Map<Date, Integer[]> commonUserTimeLocationMap = userTimeLocationMapping.get(commonUser);
				
				double meanDistance = 0.0;
				for (Date time : commonUserTimeLocationMap.keySet()) {
					Integer[] commonUserLocation = commonUserTimeLocationMap.get(time);
					Integer[] targetUserLocation = timeLocationMap.get(time);
					
					Integer x1 = commonUserLocation[0];
					Integer y1 = commonUserLocation[1];
					Integer z1 = commonUserLocation[2];
					Integer x2 = targetUserLocation[0];
					Integer y2 = targetUserLocation[1];
					Integer z2 = targetUserLocation[2];
					
					meanDistance += Math.sqrt( Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2) + Math.pow((z1 - z2), 2));
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
		return StrategyType.CF_Loc_Physical_Distance_3D_in_Region;
	}

}
