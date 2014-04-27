package at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.region.coocurred.puresocial;

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
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.Position;
import at.knowcenter.recommender.solrpowered.model.PositionNetwork;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.cleaner.DataFetcher;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class RegionCoocurredPrefAttBasedRecPS implements RecommendStrategy{

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
			solrParams.set("q", "id:" + user);
			solrParams.set("rows", 1);
			
			QueryResponse response = SolrServiceContainer.getInstance().getPositionNetworkService().getSolrServer().query(solrParams);
			List<PositionNetwork> positions = response.getBeans(PositionNetwork.class);
			
			if (positions == null || positions.size() == 0) {
					searchResponse.setResultItems(new ArrayList<String>());
					return searchResponse;
			}
			
			List<String> locationNeighbors = positions.get(0).getRegionCoocuredNeighbors();
			
			if (locationNeighbors == null || locationNeighbors.size() == 0) {
				searchResponse.setResultItems(new ArrayList<String>());
				return searchResponse;
			}
			
			
			List<String> neighbours = DataFetcher.getSocialNeighbourUsers(user);
			
			StringBuilder sb = new StringBuilder("id:(");
			
			for (String id : neighbours) {
				sb.append("\"" + id + "\" OR ");
			}
			
			if (neighbours.size() > 0 ) {
				sb.replace(sb.length() - 3, sb.length(), ")");
			} else {
				sb.append("\"\")");
			}
			
			
			solrParams = new ModifiableSolrParams();
			solrParams.set("q", sb.toString());
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fl", "id,region_cooccurred_neighborhood_count");
			solrParams.set("fq", "-id:" + user);
			
			response = SolrServiceContainer.getInstance().getPositionNetworkService().getSolrServer().query(solrParams);
			List<PositionNetwork> otherPositions = response.getBeans(PositionNetwork.class);
			
			final Map<String, Double> commonNeighborMap = new HashMap<String, Double>();
			for (PositionNetwork otherPosition : otherPositions) {
				commonNeighborMap.put(otherPosition.getUserId(), 
						locationNeighbors.size() * (double)otherPosition.getRegionCoocuredNeighborsCount());
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
		return StrategyType.CF_Region_Network_Coocurred_PrefAtt;
	}

}
