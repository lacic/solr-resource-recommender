package at.knowcenter.recommender.solrpowered.engine.strategy.location.cf;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
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
			
			
			SolrServiceContainer.getInstance().getPositionService().getSolrServer().query(solrParams);
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
