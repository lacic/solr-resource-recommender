package at.knowcenter.recommender.solrpowered.services.impl.user;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.utils.SolrUtils;
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.Sex;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkMessage;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkRemoveThread;
import at.knowcenter.recommender.solrpowered.services.common.Snippet;
import at.knowcenter.recommender.solrpowered.services.common.SolrService;

/**
 * Search service provider
 * 
 * @author hziak
 * 
 */
public class UserService extends SolrService<UserQuery, Customer, UserResponse>{
	private final String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private String facet = "on";
	private String highlighting = "on";
	private String metadata;
	private String[] facetFields;
	private String snippetFields;
	
	public UserService(String address, Integer port, String coreName) {
		this.address = address;
		this.port = port;
		this.collection = coreName;
		this.solrServer = SolrUtils.newServer("http://" + address + ":" + port + "/solr/" + coreName);
	}
	
	public UserService(String address, Integer port, String coreName, List<String> facetFields, String metadata, String snippetFields) {
		this(address, port, coreName);
		this.facetFields = facetFields.toArray(new String[facetFields.size()]);
		this.metadata = metadata;
		this.snippetFields = snippetFields;
		
	}

	/**
	 * searches the index for the query and returns a SearchResponse Object
	 * 
	 * @param query
	 * @return
	 */
	public UserResponse search(UserQuery query, int maxResultCount) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		String queryString = createQueryOutOfSearchQuery(query);
		solrParams.set("q", queryString);
		if (query.getFilterQuery() != null) {
			String filterQueryString = createFilterQueryOutOfSearchQuery(query.getFilterQuery());
			solrParams.set("fq", filterQueryString);
		}
		solrParams.set("fl", this.metadata);
		solrParams.set("rows", maxResultCount);
		solrParams.set("facet", facet);
		solrParams.set("facet.mincount", 1);
		if (this.facetFields != null) {
			solrParams.set("facet.field", facetFields);
		}
		solrParams.set("hl", highlighting);
		solrParams.set("hl.fl", snippetFields);
		solrParams.set("rows", maxResultCount);
		solrParams.set("start", query.getStartNumber());

		QueryResponse response = null;
		UserResponse searchResponse = new UserResponse();
		try {
			response = solrServer.query(solrParams);
			searchResponse.setResultItems(response.getBeans(Customer.class));
			List<Snippet> snippets = extractSnippets(response.getHighlighting());
			searchResponse.setSnippets(snippets);
			searchResponse.setElapsedTime(response.getElapsedTime());
			SolrDocumentList docResults = response.getResults();
			searchResponse.setNumFound(docResults.getNumFound());
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return searchResponse;
	}
	
	/**
	 * creates the filterquery part of the solr search query
	 * @param filterQuery
	 * @return
	 */
	private String createFilterQueryOutOfSearchQuery(List<String> filterQuery) {
		StringBuilder queryBuilder = new StringBuilder();
		for (String queryFragment : filterQuery) {
			queryBuilder.append(queryFragment);
			queryBuilder.append(" AND ");
		}
		queryBuilder.delete(queryBuilder.length() - 5, queryBuilder.length());
		return queryBuilder.toString();
	}
	
	/**
	 * creates an string in the form tag:(elements[1] AND .... elements[n])
	 * @param queryList
	 * @param tag
	 * @return
	 */
	private String getQueryChunkFromList(List<String> queryList, String tag,boolean prefixEmpty) {
		String queryString = new String();
		if (queryList != null){
			if (queryList.size() > 0) {
				if(!prefixEmpty)
					queryString += " AND ";
				boolean first = true;
				queryString += "  " + tag + ":(";
				for (String value : queryList) {
					if (first) {
						queryString += value;
						first = false;
					} else
						queryString += " AND " + value;
				}
				queryString += " )";
			}
		}
		return queryString;
	}

	/**
	 * creates a solr query out of the SearchQuery object
	 * 
	 * @param query
	 * @return
	 */
	private String createQueryOutOfSearchQuery(UserQuery query) {
		//TODO implement query
		return query.getQuery();
	}

	/**
	 * updates/adds the Document as bean to the index
	 * if there is already a document with the same id it will be overwritten
	 * @param searchItem
	 */
	public void updateDocument(Customer searchItem) {
		updateDocument(searchItem, solrServer);
	}

	/**
	 * updates/adds a list of Documents as beans to the index in an own thread
	 * if there are already documents with the same ids they will be overwritten
	 * @param searchItem
	 */
	public void writeDocuments(List<Customer> searchItems, SearchServerBulkMessage searchServerBulkUpload) {
		updateDocuments(searchItems, searchServerBulkUpload, solrServer);
	}
	
	/**
	 * Removes a list of documentes based on their ids in an own thread
	 */
	public void bulkRemoveElementByIds(List<String> ids,SearchServerBulkMessage searchServerBulkUploadMessage){
		SearchServerBulkRemoveThread searchServerBulkRemoveThread = new SearchServerBulkRemoveThread(solrServer, searchServerBulkUploadMessage);
		searchServerBulkRemoveThread.setRemoveItems(ids);
		searchServerBulkRemoveThread.start();
	}
	
	//TODO needs to be implemented
	public void bulkRemoveElement(List<Customer> items,SearchServerBulkMessage searchServerBulkUploadMessage){
	}
	
	private Customer serializeSolrDocToUserItem(SolrDocument solrDocument) {
		Customer userItem = new Customer();
		userItem.setId((String) solrDocument.getFieldValue("id"));
//		userItem.setFirstName((String) solrDocument.getFieldValue("firstName"));
//		userItem.setSecondName((String) solrDocument.getFieldValue("secondName"));
//		userItem.setLastName((String) solrDocument.getFieldValue("lastName"));
//		userItem.setAge((Integer) solrDocument.getFieldValue("age"));
		userItem.setSex(Sex.valueOf((String) solrDocument.getFieldValue("sex")));
		userItem.setDateOfBirth((Date) solrDocument.getFieldValue("dateOfBirth"));
//		userItem.setLanguages((List<String>) solrDocument.getFieldValue("languages"));
//		userItem.setAddresses((List<String>) solrDocument.getFieldValue("addresses"));
		userItem.setZip((String) solrDocument.getFieldValue("zip"));
		userItem.setStreet((String) solrDocument.getFieldValue("street"));
		userItem.setCity((String) solrDocument.getFieldValue("city"));
		userItem.setCountry((String) solrDocument.getFieldValue("country"));
		userItem.setCustomergroup((List<String>) solrDocument.getFieldValue("customerGroup"));
		userItem.setFriendOf((List<String>) solrDocument.getFieldValue("friendOf"));
		userItem.setEducation((String) solrDocument.getFieldValue("education"));
		userItem.setWork((String) solrDocument.getFieldValue("work"));
		userItem.setLanguage((String) solrDocument.getFieldValue("language"));
		userItem.setInterests((List<String>) solrDocument.getFieldValue("interests"));
//		userItem.setDevice((String) solrDocument.getFieldValue("device"));
//		userItem.setTags((List<String>) solrDocument.getFieldValue("tags"));
		return userItem;
	}

	@Override
	public void removeElementById(String id) {
		removeElementById(id, solrServer);
	}
	
	@Override
	public void removeElementByIds(List<String> ids) {
		removeElementByIds(ids, solrServer);
	}

	public void deleteAllSolrData() {
	    try {
	    	solrServer.deleteByQuery("*:*", 1);
	    } catch (SolrServerException e) {
	      throw new RuntimeException("Failed to delete data in Solr. "
	          + e.getMessage(), e);
	    } catch (IOException e) {
	      throw new RuntimeException("Failed to delete data in Solr. "
	          + e.getMessage(), e);
	    }
	}
}
