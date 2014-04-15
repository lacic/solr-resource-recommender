package at.knowcenter.recommender.solrpowered.engine.strategy.social.cn;

import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;

/**
 * Collaborative Filtering Recommender strategy
 * @author elacic
 *
 */
public class RealBiographyBasedRec extends UserBasedRecommender {

	@Override
	protected ModifiableSolrParams createMLTParams(String query,
			String filterQuery, int maxResultCount) {
		ModifiableSolrParams params = new ModifiableSolrParams();
		
		params.set("q", query);
		params.set("fq", filterQuery);
		params.set("fl", "id,score");
		params.set("mlt", "true");
		params.set("mlt.fl", "real_biography");
		params.set("mlt", "true");
		params.set("mlt.count", 40);
		params.set("mlt.mindf", "1");
		params.set("mlt.mintf", "1");
		params.set("mlt.minwl", "4");
		params.set("mlt.maxqt", "15");
		
		return params;
	}
	
	@Override
	public StrategyType getStrategyType() {
		return StrategyType.RealBiographyBasedMLT;
	}
}
