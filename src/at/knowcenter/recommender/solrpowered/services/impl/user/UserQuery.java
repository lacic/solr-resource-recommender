package at.knowcenter.recommender.solrpowered.services.impl.user;

import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.services.common.SolrQuery;
/**
 * user query object
 * @author Michael Wittmayer <mwittmayer@know-center.at>
 */
public class UserQuery extends SolrQuery<Customer>{

	public UserQuery() {
		item = new Customer();
	}
}