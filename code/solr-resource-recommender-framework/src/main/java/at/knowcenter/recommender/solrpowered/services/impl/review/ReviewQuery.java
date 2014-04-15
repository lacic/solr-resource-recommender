package at.knowcenter.recommender.solrpowered.services.impl.review;

import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.model.Review;
import at.knowcenter.recommender.solrpowered.services.common.SolrQuery;

/**
 * Review query object
 * @author elacic
 */
public class ReviewQuery extends SolrQuery<Review>{
	
	
	public ReviewQuery () {
		item = new Review();
	}
}
