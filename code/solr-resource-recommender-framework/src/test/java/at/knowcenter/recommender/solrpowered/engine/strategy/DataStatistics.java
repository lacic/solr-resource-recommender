package at.knowcenter.recommender.solrpowered.engine.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.Position;
import at.knowcenter.recommender.solrpowered.model.PositionNetwork;
import at.knowcenter.recommender.solrpowered.model.SharedLocation;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class DataStatistics {

	public static void main(String... args) throws SolrServerException {
		ModifiableSolrParams params = new ModifiableSolrParams();
		
		params.set("q", "*:*");
		params.set("rows", Integer.MAX_VALUE);
		params.set("fl", "favorite_regions");
		
		List<Customer> customers = SolrServiceContainer.getInstance().getUserService().getSolrServer().query(params).getBeans(Customer.class);
	
		System.out.println("Customers size: " + customers.size());
		Set<String> picks = new HashSet<String>();
		List<String> picksList = new ArrayList<String>();
		for (Customer c : customers) {
			picks.addAll(c.getFavoriteRegions());
			picksList.addAll(c.getFavoriteRegions());
		}
		
		System.out.println("#Favorite regions: " + picks.size());
		System.out.println("#Favorite regions used: " + picksList.size());
		System.out.println("#FL per user: " + (picksList.size() / 7029.0));
		
		params.set("fl", "shared_region_id");
		List<SharedLocation> sharedLocations = SolrServiceContainer.getInstance().getSharedLocationService().getSolrServer().query(params).getBeans(SharedLocation.class);
		
		Set<String> shareedRegions = new HashSet<String>();
		List<String> sharedRegionsList = new ArrayList<String>();
		for (SharedLocation sl : sharedLocations) {
			List<String> sharedRegions = sl.getSharedRegions();
			if (sharedRegions != null) {
				
				shareedRegions.addAll(sharedRegions);
				sharedRegionsList.addAll(sharedRegions);
			}
		}
		
		System.out.println("#Shared regions: " + shareedRegions.size());
		System.out.println("#Shared regions used: " + sharedRegionsList.size());
		System.out.println("#SL per user: " + (sharedRegionsList.size() / 7029.0));
		System.out.println("#SL per shared user: " + (sharedRegionsList.size() / (double) sharedLocations.size()));

		params.set("fl", "*");
		List<PositionNetwork> positionNetworks = SolrServiceContainer.getInstance().getPositionNetworkService().getSolrServer().query(params).getBeans(PositionNetwork.class);
		
		int minNeigh = Integer.MAX_VALUE;
		int maxNeigh = Integer.MIN_VALUE;
		int meanNeigh = 0;
		int usersWithNeigh = 0;
		for (PositionNetwork pn : positionNetworks) {
			Integer count = pn.getRegionCoocuredNeighborsCount();
			if (count != null && count > 0) {
				meanNeigh += count;
				usersWithNeigh++;
				if (count > maxNeigh)
					maxNeigh = count;
				if (count < minNeigh)
					minNeigh = count;
			}
		}
		meanNeigh = meanNeigh / usersWithNeigh;
		
		System.out.println("Min loc network neigh: " + minNeigh);
		System.out.println("Max loc network neigh: " + maxNeigh);
		System.out.println("Mean loc network neigh: " + meanNeigh);
		System.out.println("# users with loc network neigh: " + usersWithNeigh);
		
		params.set("facet", "true");
		params.set("facet.field", "region_id");
		params.set("facet.limit", -1);
		params.set("facet.mincount", 1);
		
		QueryResponse response = SolrServiceContainer.getInstance().getPositionService().getSolrServer().query(params);
		
		List<FacetField> facetFields = response.getFacetFields();
		FacetField userFacet = facetFields.get(0);

		System.out.println("Facet on region_id size: " + userFacet.getValues().size());
		
		List<Position> positions = response.getBeans(Position.class);
		
		int duplicates = 0;
		Set<String> userDuplicates = new HashSet<String>();
		Set<String> regionDuplicates = new HashSet<String>();
		Set<String> uniqueRegionsMonitored = new HashSet<String>();
		Map<String, Map<Long, Set<String>>> userTimeRegionsMapping = new HashMap<String, Map<Long, Set<String>>>();
		for (Position p : positions) {
			Long time = p.getTime().getTime();
			String region = p.getRegionName();
			String user = p.getUser();
			
			uniqueRegionsMonitored.add(region);
			
			Map<Long, Set<String>> timeRegionsMapping = userTimeRegionsMapping.get(user);
			Set<String> regionsAtTime;
			
			if (timeRegionsMapping == null) {
				timeRegionsMapping = new HashMap<Long, Set<String>>();
				regionsAtTime = new HashSet<String>();
			} else {
				regionsAtTime = timeRegionsMapping.get(time);
				if (regionsAtTime == null) {
					regionsAtTime = new HashSet<String>();
				}
			}
			if (regionsAtTime.contains(region)) {
				duplicates++;
				regionDuplicates.add(region);
				userDuplicates.add(user);
			}
			
			regionsAtTime.add(region);
			timeRegionsMapping.put(time, regionsAtTime);
			userTimeRegionsMapping.put(user, timeRegionsMapping);
		}
		
		System.out.println("Unique monitored regions: " + uniqueRegionsMonitored.size());
		System.out.println("# User-Time-Location duplicates: " + duplicates);
		System.out.println("# Users that had duplicates: " + userDuplicates.size());
		System.out.println("# Regions that had duplicates: " + regionDuplicates.size());
		
		int monitoredEvents = 0;
		for (String user : userTimeRegionsMapping.keySet()) {
			Map<Long, Set<String>> timeRegionMapping = userTimeRegionsMapping.get(user);
			for (Long time : timeRegionMapping.keySet()){
				monitoredEvents += timeRegionMapping.get(time).size();
			}
		}
		System.out.println(monitoredEvents);
		System.out.println(monitoredEvents / 7029.0);
		System.out.println(monitoredEvents / (double) userTimeRegionsMapping.keySet().size());
	}
}
