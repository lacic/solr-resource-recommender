package at.knowcenter.recommender.solrpowered.engine.strategy.social;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.model.SocialAction;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemQuery;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemResponse;

/**
 * Collaborative Filtering Recommender strategy
 * @author elacic
 *
 */
public class UserBasedInterestsCustomerGroupRecommender extends UserBasedRecommender {

	@Override
	protected ModifiableSolrParams createMLTParams(String query,
			String filterQuery, int maxResultCount) {
		ModifiableSolrParams params = new ModifiableSolrParams();
		
		params.set("q", query);
		params.set("fq", filterQuery);
		params.set("mlt", "true");
		params.set("mlt.fl", "interests,customergroup");
		params.set("mlt", "true");
		params.set("mlt.count", 60);
		params.set("mlt.mindf", "1");
		params.set("mlt.mintf", "1");
		params.set("mlt.qf","interests^11.0 customergroup^7.0");
		
		return params;
	}
	
	@Override
	public StrategyType getStrategyType() {
		return StrategyType.UB_InterestsCustomerGroup;
	}
}
