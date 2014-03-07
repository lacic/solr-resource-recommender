package at.knowcenter.recommender.solrpowered.services.impl.actions;

import java.util.Map;

import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.services.common.SolrResponse;

/**
 * Response object of the Solr Collaborative Filtering recommendation 
 * @author elacic
 */
public class RecommendResponse extends SolrResponse<String> {
	
	private Map<String, Long> userConfidenceMapping;

	public Map<String, Long> getUserConfidenceMapping() {
		return userConfidenceMapping;
	}

	public void setUserConfidenceMapping(Map<String, Long> userConfidenceMapping) {
		this.userConfidenceMapping = userConfidenceMapping;
	}
	
}
