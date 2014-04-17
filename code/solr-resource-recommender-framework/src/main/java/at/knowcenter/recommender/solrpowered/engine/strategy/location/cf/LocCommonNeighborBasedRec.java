package at.knowcenter.recommender.solrpowered.engine.strategy.location.cf;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.model.Position;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class LocCommonNeighborBasedRec implements RecommendStrategy{

	private List<String> alreadyPurchasedResources;
	private ContentFilter contentFilter;

	@Override
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts) {
		String user = query.getUser();
		
		if (user == null) {
			RecommendResponse response = new RecommendResponse();
			response.setResultItems(new ArrayList<String>());
			return response;
		}
		
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		try {
			String q = "user:" + user;
			
			solrParams.set("q", q);
			solrParams.set("rows", Integer.MAX_VALUE);
			
			
			QueryResponse response = SolrServiceContainer.getInstance().getPositionService().getSolrServer().query(solrParams);
			List<Position> positions = response.getBeans(Position.class);
			
			Map<String, Date> locationDate = new HashMap<String, Date>();
			
			Map<String, Map<Date, Set<String>>> locationTimeUserMapping = new HashMap<String, Map<Date, Set<String>>>();
			
			for (Position userPosition : positions){
				Date time = userPosition.getTime();
				String location = userPosition.getGlobalLocation();
				// if the key and value pair exists then the user was already at the same time on 
				// on the give location already - so can be a neighbor to some other user
				if (locationDate.containsKey(location) && 
						locationDate.get(location).equals(time)) {
					Map<Date, Set<String>> timeUsersMapping = locationTimeUserMapping.get(location);
					Set<String> neighbors;

					if (timeUsersMapping == null) {
						timeUsersMapping = new HashMap<Date, Set<String>>();
						neighbors = new HashSet<String>();
					} else {
						neighbors = timeUsersMapping.get(time);
						if (neighbors == null) {
							neighbors = new HashSet<String>();
						}
					}
					
					neighbors.add(user);
					
					timeUsersMapping.put(time, neighbors);
					locationTimeUserMapping.put(location, timeUsersMapping);
				}

				locationDate.put(location,time);
			}
			
			String fq = "time:(";
			q = "global_location:(";
			for (String location : locationTimeUserMapping.keySet()) {
				q += "Intersects() OR ";
				Map<Date, Set<String>> timeUsersMapping = locationTimeUserMapping.get(location);
				for (Date time : timeUsersMapping.keySet()) {
					fq += "\""+ time + "\" OR ";
				}
			}
			
			solrParams = new ModifiableSolrParams();
			solrParams.set("q", q);
			solrParams.set("fq", fq + "-user:" + user);
			solrParams.set("fl", "user,global_location,time");
			solrParams.set("rows", Integer.MAX_VALUE);
			
			
			response = SolrServiceContainer.getInstance().getPositionService().getSolrServer().query(solrParams);
			positions = response.getBeans(Position.class);
			
			Map<String, Map<String, Date>> userLocationTimeMapping = new HashMap<String, Map<String, Date>>();
			for (Position p : positions) {
				String otherUser = p.getUser();
				Date time = p.getTime();
				String location = p.getGlobalLocation();
				
				// a user was already at a give location at the given time previously
				if (userLocationTimeMapping.containsKey(otherUser) && 
						userLocationTimeMapping.get(otherUser).containsKey(location) &&
						userLocationTimeMapping.get(otherUser).get(location).equals(time)) {
					locationTimeUserMapping.get(location).get(time).add(otherUser);
				}
				
				Map<String, Date> otherLocDateMapping;
				if (userLocationTimeMapping.containsKey(otherUser)) {
					otherLocDateMapping = userLocationTimeMapping.get(otherUser);
				} else {
					otherLocDateMapping = new HashMap<String, Date>();
				}
				otherLocDateMapping.put(location, time);
				userLocationTimeMapping.put(otherUser, otherLocDateMapping);
			}
			
			
		} catch (SolrServerException e) {
			System.out.println(solrParams);
			e.printStackTrace();
		}
		
		return null;
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
		return StrategyType.CF_Loc_CN;
	}

}
