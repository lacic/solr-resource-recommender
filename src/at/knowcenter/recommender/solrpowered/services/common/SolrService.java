package at.knowcenter.recommender.solrpowered.services.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkMessage;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkUploadThread;
import at.knowcenter.recommender.solrpowered.services.impl.user.UserQuery;
import at.knowcenter.recommender.solrpowered.services.impl.user.UserResponse;

/**
 * A generic service to access SOLR
 * @author elacic
 *
 */
public abstract class SolrService<Q, I, R extends SolrResponse<?>> {

	protected SolrServer solrServer;
	protected Integer port;
	protected String address;
	protected String collection;
	
	/**
	 * Searches the index for the query and returns a response object
	 * 
	 * @param query
	 * @return
	 */
	public abstract R search(Q query, int maxResultCount);
	
	/**
	 * Updates/adds the Document as bean to the index
	 * If there is already a document with the same id it will be overwritten
	 * @param searchItem
	 */
	public abstract void updateDocument(I searchItem);
	
	/**
	 * Updates/adds the Document as bean to the index
	 * If there is already a document with the same id it will be overwritten
	 * @param searchItem
	 */
	protected void updateDocument(Object searchItem, SolrServer solrServer) {
		try {
			solrServer.addBean(searchItem);
			solrServer.commit();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Updates/adds a list of Documents as beans to the index in an own thread
	 * If there are already documents with the same ids they will be overwritten
	 * @param searchItem
	 */
	public abstract void updateDocuments(List<I> searchItems, SearchServerBulkMessage searchServerBulkUpload);
		
	/**
	 * Updates/adds a list of Documents as beans to the index in an own thread
	 * If there are already documents with the same ids they will be overwritten
	 * @param searchItem
	 */
	protected void updateDocuments(	
							List<I> searchItems, 
							SearchServerBulkMessage searchServerBulkUpload, 
							SolrServer solrServer) 
	{
				
		SearchServerBulkUploadThread<I> searchServerBulkUploadThread = new SearchServerBulkUploadThread<I>(solrServer, searchServerBulkUpload);		
		searchServerBulkUploadThread.setUploadItems(searchItems);
		searchServerBulkUploadThread.start();
		try {
			searchServerBulkUploadThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	
	}
	
	/**
	 * Returns the port of the current search server
	 */
	public Integer getPort() {
		return port;
	}
	
	/**
	 * Returns the address of the current search server
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * returns the address of the current search server
	 */
	public String getCollection() {
		return collection;
	}
	
	/**
	 * This removes an search entry by its id
	 */
	public abstract void removeElementById(String id, String language);
	
	/**
	 * This removes an search entry by its id
	 */
	protected void removeElementById(String id, SolrServer solrServer){
		try {
			solrServer.deleteById(id);
			solrServer.commit();
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Initiates the wipe of the index
	 */
	public void wipeIndex() {
		if (solrServer != null) {
			wipeIndex(this.solrServer);
		}
	}
	
	/**
	 * This really wipes the whole index, no data is left in there anymore.
	 */
	protected void wipeIndex(SolrServer solrServer){
		try {
			solrServer.deleteByQuery("*:*");
			solrServer.commit();
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public QueryResponse findElementById(String id, SolrServer solrServer) {
		QueryResponse response = null;
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		solrParams.set("q", "id:(\"" + id + "\")");

		try {
			response = solrServer.query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		
		return response;
	}
	
	public QueryResponse findElementsById(List<String> ids, SolrServer solrServer) {
		QueryResponse response = null;
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		StringBuilder queryBuilder = new StringBuilder();
		for (String id : ids) {
			queryBuilder.append("id:(\"" + id + "\") OR ");
		}
		if (ids != null && ids.size() > 0) {
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), "");
		}
		solrParams.set("q", queryBuilder.toString());

		try {
			response = solrServer.query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		
		return response;
	}
	
	/**
	 * Loads the user profile data for the user which is getting recommendations
	 * @param userID
	 * @return
	 */
	public Customer fetchUserProfileData(String userID) {
		Customer customer  = null;
		SolrServer userServer = SolrServiceContainer.getInstance().getUserService().getSolrServer();
		
		if (userServer != null) {
			UserQuery uq = new UserQuery();
			uq.setQuery("id:(\"" + userID + "\")");
			
			
			ModifiableSolrParams solrParams = new ModifiableSolrParams();
			String queryString = uq.getQuery();
			solrParams.set("q", "*:*");
			solrParams.set("fq", queryString);
			solrParams.set("rows", "1");

			QueryResponse response = null;
			UserResponse searchResponse = new UserResponse();
			try {
				response = userServer.query(solrParams);
				searchResponse.setResultItems(response.getBeans(Customer.class));
				searchResponse.setElapsedTime(response.getElapsedTime());
				SolrDocumentList docResults = response.getResults();
				searchResponse.setNumFound(docResults.getNumFound());
			} catch (SolrServerException e) {
				e.printStackTrace();
			}
			
			
			if (searchResponse.getResultItems().size() > 0 ) {
				customer = searchResponse.getResultItems().get(0);
			}
		}
		return customer;
	}
	
	public QueryResponse findItemsFromUser(final String userID, String usersFieldName, SolrServer solrServer) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = usersFieldName + ":(\"" +  userID + "\")";
//		solrParams.set("fq", "*:*");
		solrParams.set("q", queryString);
		solrParams.set("fl", "id");
		solrParams.set("rows",Integer.MAX_VALUE);
		try {
			response = solrServer.query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return response;
	}
	
	protected List<Snippet> extractSnippets(
			Map<String, Map<String, List<String>>> highlights) {
		List<Snippet> snippets = new ArrayList<Snippet>();
		for (Entry<String, Map<String, List<String>>> snippet : highlights.entrySet()) {
			Snippet s = new Snippet();
			s.setId(snippet.getKey());
			List<SnippetField> fields = new ArrayList<SnippetField>();
			for (Entry<String, List<String>> snippetField : snippet.getValue().entrySet()) {
				SnippetField f = new SnippetField();
				f.setDescription(snippetField.getKey());
				f.setSnippets(snippetField.getValue());
				fields.add(f);
			}
			s.setSnippets(fields);
			snippets.add(s);
		}
		return snippets;
	}
	
	public SolrServer getSolrServer() {
		return this.solrServer;
	}
	
	public void closeConnection() {
		if (this.solrServer != null) {
			this.solrServer.shutdown();
		}
	}
	
}
