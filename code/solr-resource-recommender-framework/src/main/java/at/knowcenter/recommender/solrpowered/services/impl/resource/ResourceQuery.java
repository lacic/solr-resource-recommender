package at.knowcenter.recommender.solrpowered.services.impl.resource;

import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.services.common.SolrQuery;

/**
 * Resource query object
 * @author elacic
 */
public class ResourceQuery extends SolrQuery<Resource>{
	
	private String sortCriteria;
	
	
	public String getSortCriteria() {
		return sortCriteria;
	}
	public void setSortCriteria(String sortCriteria) {
		this.sortCriteria = sortCriteria;
	}
	
	public ResourceQuery () {
		item = new Resource();
	}
}
