package at.knowcenter.recommender.solrpowered.services.impl.sharedlocation;

import at.knowcenter.recommender.solrpowered.model.PositionNetwork;
import at.knowcenter.recommender.solrpowered.model.SharedLocation;
import at.knowcenter.recommender.solrpowered.services.common.SolrQuery;

/**
 * Review query object
 * @author elacic
 */
public class SharedLocationQuery extends SolrQuery<SharedLocation>{
	
	
	public SharedLocationQuery () {
		item = new SharedLocation();
	}
}
