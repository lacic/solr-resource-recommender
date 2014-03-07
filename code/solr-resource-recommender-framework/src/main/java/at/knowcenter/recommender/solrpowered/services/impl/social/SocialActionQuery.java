package at.knowcenter.recommender.solrpowered.services.impl.social;

import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.services.common.SolrQuery;
/**
 * Search query object
 * @author hziak
 */
public class SocialActionQuery extends SolrQuery<Item>{
	
	private String sortCriteria;
	
	
	public String getSortCriteria() {
		return sortCriteria;
	}
	public void setSortCriteria(String sortCriteria) {
		this.sortCriteria = sortCriteria;
	}
	
	public SocialActionQuery () {
		item = new Item();
	}
}
