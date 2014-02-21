package at.knowcenter.recommender.solrpowered.services.impl.socialstream;

import at.knowcenter.recommender.solrpowered.model.SocialStream;
import at.knowcenter.recommender.solrpowered.services.common.SolrQuery;
/**
 * Search query object
 * @author hziak
 */
public class SocialStreamQuery extends SolrQuery<SocialStream>{
	
	private String sortCriteria;
	
	
	public String getSortCriteria() {
		return sortCriteria;
	}
	public void setSortCriteria(String sortCriteria) {
		this.sortCriteria = sortCriteria;
	}
	
	public SocialStreamQuery () {
		item = new SocialStream();
	}
}
