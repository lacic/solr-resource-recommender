package at.knowcenter.recommender.solrpowered.services.impl.sharedlocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.Position;
import at.knowcenter.recommender.solrpowered.model.PositionNetwork;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.model.Review;
import at.knowcenter.recommender.solrpowered.model.SharedLocation;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkMessage;
import at.knowcenter.recommender.solrpowered.services.common.SolrService;


/**
 * PositionNetwork Service
 * 
 * @author elacic
 * 
 */
public class SharedLocationService extends SolrService<SharedLocationQuery, SharedLocation, SharedLocationResponse>{
	
	public SharedLocationService(String address, Integer port, String coreName) {
		this.address = address;
		this.port = port;
		this.collection = coreName;
		this.solrServer = new HttpSolrServer("http://" + address + ":" + port + "/solr/" + coreName);
	}
	
	/**
	 * searches the index for the query and returns a SearchResponse Object
	 * 
	 * @param query
	 * @return
	 */
	public SharedLocationResponse search(SharedLocationQuery query, int maxResultCount) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		solrParams.set("q", query.getQuery());
		solrParams.set("rows", maxResultCount);
//		solrParams.set("fl", "region_location");
//		solrParams.set("fq","region_id:2787433776054");

		QueryResponse response = null;
		SharedLocationResponse searchResponse = new SharedLocationResponse();
		return searchResponse;
	}


	/**
	 * updates/adds the Document as bean to the index
	 * if there is already a document with the same id it will be overwritten
	 * @param socialAction
	 */
	public void updateDocument(SharedLocation resource) {
		updateDocument(resource, solrServer);
	}
	

	private void setAdd(SolrInputDocument inputDoc, List<String> customerIdsPurchased, String fieldForAdding) {
		Map<String,  List<String>> addAction = new HashMap<String,  List<String>>();
		addAction.put("add", customerIdsPurchased);
		inputDoc.setField(fieldForAdding, addAction);
	}

	private void setIncrement(SolrInputDocument inputDoc, String fieldForIncrementing, int incrementSize) {
		Map<String, Integer> incrementActionCount = new HashMap<String, Integer>();
		incrementActionCount.put("inc", incrementSize);
		inputDoc.setField(fieldForIncrementing, incrementActionCount);
	}
	

	/**
	 * updates/adds a list of Documents as beans to the index in an own thread
	 * if there are already documents with the same ids they will be overwritten
	 * @param searchItem
	 */
	public void writeDocuments(List<SharedLocation> positions, SearchServerBulkMessage searchServerBulkUpload) {
		updateDocuments(positions, searchServerBulkUpload, solrServer);
	}
	
	@Override
	public void removeElementById(String id) {
		removeElementById(id, solrServer);
	}
	
	@Override
	public void removeElementByIds(List<String> ids) {
		removeElementByIds(ids, solrServer);
	}
}
