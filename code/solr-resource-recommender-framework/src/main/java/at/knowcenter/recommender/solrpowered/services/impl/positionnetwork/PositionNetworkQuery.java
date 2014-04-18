package at.knowcenter.recommender.solrpowered.services.impl.positionnetwork;

import at.knowcenter.recommender.solrpowered.model.PositionNetwork;
import at.knowcenter.recommender.solrpowered.services.common.SolrQuery;

/**
 * Review query object
 * @author elacic
 */
public class PositionNetworkQuery extends SolrQuery<PositionNetwork>{
	
	
	public PositionNetworkQuery () {
		item = new PositionNetwork();
	}
}
