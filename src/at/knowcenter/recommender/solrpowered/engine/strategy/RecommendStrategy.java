package at.knowcenter.recommender.solrpowered.engine.strategy;


import java.util.List;

import org.apache.solr.client.solrj.SolrServer;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.services.common.SolrResponse;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public interface RecommendStrategy {
	
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts, SolrServer solrServer);
	
	public void setAlreadyBoughtProducts(List<String> alreadyBoughtProducts);
	
	public List<String>  getAlreadyBoughtProducts();
	
	public void setContentFiltering(ContentFilter contentFilter);
	
	public StrategyType getStrategyType();


}
