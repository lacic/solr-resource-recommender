package at.knowcenter.recommender.solrpowered.services.impl.actions;

import java.util.List;

import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.services.common.SolrQuery;

/**
 * Recommend query object to get results from SOLR
 * @author elacic
 */
public class RecommendQuery extends SolrQuery<CustomerAction> {
	
	private List<String> ids;
	private String user;
	
	/**
	 * Initializes the query to search for in SOLR
	 */
	public String initializeQuery(){
		return query;
	}
	
	public String getUser() {
		return user;
	}
	/**
	 * sets the query to search for a specific product	
	 * @param id
	 */
	public void setUser(String user) {
		this.user = user;
	}
	
	public List<String> getProductIds() {
		return ids;
	}
	/**
	 * sets the query to search for a specific id
	 * @param id
	 */
	public void setProductIds(List<String> ids) {
		this.ids = ids;
	}
	
}
