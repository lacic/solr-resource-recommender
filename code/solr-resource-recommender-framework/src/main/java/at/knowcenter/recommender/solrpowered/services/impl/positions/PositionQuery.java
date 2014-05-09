package at.knowcenter.recommender.solrpowered.services.impl.positions;

import at.knowcenter.recommender.solrpowered.model.Position;
import at.knowcenter.recommender.solrpowered.services.common.SolrQuery;

/**
 * Review query object
 * @author elacic
 */
public class PositionQuery extends SolrQuery<Position>{
	
	
	public PositionQuery () {
		item = new Position();
	}
}
