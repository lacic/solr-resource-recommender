package at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.CNRecommender;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

/**
 * Content based Recommender strategy
 * @author elacic
 *
 */
public class CNRecommenderWeightedName extends CNRecommender {

	@Override
	protected ModifiableSolrParams initMLTParams(String filterQuery, int maxResultCount, String query) {
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("q", query);
		params.set("fq", filterQuery);
		params.set("mlt", "true");
		params.set("mlt.fl", "name");
		params.set("mlt", "true");
		params.set("mlt.count", maxResultCount);
		params.set("mlt.mindf", "1");
		params.set("mlt.mintf", "1");
		params.set("mlt.minwl", "4");
		params.set("mlt.maxqt", "15");
		return params;
	}


}