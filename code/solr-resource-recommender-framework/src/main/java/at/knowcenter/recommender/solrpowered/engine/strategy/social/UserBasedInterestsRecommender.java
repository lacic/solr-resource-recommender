package at.knowcenter.recommender.solrpowered.engine.strategy.social;

import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;

/**
 * Collaborative Filtering Recommender strategy
 * @author elacic
 *
 */
public class UserBasedInterestsRecommender extends UserBasedRecommender {

	@Override
	protected ModifiableSolrParams createMLTParams(String query,
			String filterQuery, int maxResultCount) {
		ModifiableSolrParams params = new ModifiableSolrParams();
		
		params.set("q", query);
		params.set("fq", filterQuery);
		params.set("fl", "id");
		params.set("mlt", "true");
		params.set("mlt.fl", "interests");
		params.set("mlt", "true");
		params.set("mlt.count", 60);
		params.set("mlt.mindf", "1");
		params.set("mlt.mintf", "1");
		
		return params;
	}
	
	@Override
	public StrategyType getStrategyType() {
		return StrategyType.UB_Interests;
	}
}
