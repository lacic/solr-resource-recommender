package at.knowcenter.recommender.solrpowered.services.impl.review;

import java.io.IOException;
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
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.model.Review;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkMessage;
import at.knowcenter.recommender.solrpowered.services.common.SolrService;


/**
 * Review Service
 * 
 * @author elacic
 * 
 */
public class ReviewService extends SolrService<ReviewQuery, Review, ReviewResponse>{
	
	public ReviewService(String address, Integer port, String coreName) {
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
	public ReviewResponse search(ReviewQuery query, int maxResultCount) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		solrParams.set("q", query.getQuery());
		solrParams.set("rows", maxResultCount);

		QueryResponse response = null;
		ReviewResponse searchResponse = new ReviewResponse();
		try {
			response = solrServer.query(solrParams);
			List<String> users = RecommendationQueryUtils.extractRecommendationIds(response.getBeans(Review.class));
			searchResponse.setResultItems(users);
			searchResponse.setElapsedTime(response.getElapsedTime());
			SolrDocumentList docResults = response.getResults();
			searchResponse.setNumFound(docResults.getNumFound());
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return searchResponse;
	}


	/**
	 * updates/adds the Document as bean to the index
	 * if there is already a document with the same id it will be overwritten
	 * @param socialAction
	 */
	public void updateDocument(Review resource) {
		updateDocument(resource, solrServer);
	}
	
//	public void atomicCustomerActionsUpdate(CustomerAction customerAction) {
//		SolrInputDocument inputDoc = new SolrInputDocument();
//
//		if (customerAction == null || customerAction.getItemId() == null) {
//			return;
//		}
//		
//		inputDoc.addField("id", customerAction.getItemId());
//
//		List<String> customerIdsPurchased = customerAction.getCustomerIdsPurchased();
//		List<String> customerIdsMarkedFavorite = customerAction.getCustomerIdsMarkedFavorite();
//		List<String> customerIdsViewed = customerAction.getCustomerIdsViewed();
//
//		addAtomicUpdateParameter(inputDoc, customerIdsPurchased, "users_purchased", "user_count_purchased");
//		addAtomicUpdateParameter(inputDoc, customerIdsMarkedFavorite, "users_marked_favorite", "user_count_marked_favorite");
//		addAtomicUpdateParameter(inputDoc, customerIdsViewed, "users_viewed", "user_count_viewed");
//
//		try {
//			solrServer.add(inputDoc);
//			solrServer.commit(false, false, true); 
//		} catch (SolrServerException | IOException e) {
//			e.printStackTrace();
//		}
//	}

	private void addAtomicUpdateParameter(SolrInputDocument inputDoc, List<String> customerIdsPurchased, String fieldForAdding, String fieldForIncrementing) {
		if (customerIdsPurchased != null && customerIdsPurchased.size() > 0) {
			setAdd(inputDoc, customerIdsPurchased, fieldForAdding);
			setIncrement(inputDoc, fieldForIncrementing, customerIdsPurchased.size());
		}
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
	public void writeDocuments(List<Review> resources, SearchServerBulkMessage searchServerBulkUpload) {
		updateDocuments(resources, searchServerBulkUpload, solrServer);
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
