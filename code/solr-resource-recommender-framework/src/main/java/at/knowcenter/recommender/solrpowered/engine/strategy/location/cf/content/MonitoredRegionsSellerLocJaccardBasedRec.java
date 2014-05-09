package at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content;

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
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class MonitoredRegionsSellerLocJaccardBasedRec implements RecommendStrategy{

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
			solrParams.set("fl", "region_name");
			
			
			QueryResponse response = SolrServiceContainer.getInstance().getPositionService().getSolrServer().query(solrParams);
			List<Position> positions = response.getBeans(Position.class);
			
			
			Map<String, Integer> regionOccurance = new HashMap<String, Integer>();
			
			for (Position userPosition : positions){
				String regionName = userPosition.getRegionName();
				Integer occurance = regionOccurance.get(regionName);
				occurance = (occurance == null) ? 1 : occurance + 1;
				regionOccurance.put(regionName, occurance);
			}
			
			StringBuilder queryBuilder = new StringBuilder("selling_region:(");
			for (String regionName : regionOccurance.keySet()) {
				queryBuilder.append("\"" + regionName + "\"^" + regionOccurance.get(regionName) + " OR ");
			}
			
			if (regionOccurance.keySet().size() > 0) {
				queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ")");
			} else {
				queryBuilder.append("\"\")");
			}
			
			solrParams = new ModifiableSolrParams();

			solrParams.set("q", queryBuilder.toString());
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fq", RecommendationQueryUtils.buildFilterForAlreadyBoughtProducts("id",alreadyPurchasedResources));
			
			response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(solrParams);
			
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
		return StrategyType.CF_Loc_Common_Regions_Jaccard;
	}

}
