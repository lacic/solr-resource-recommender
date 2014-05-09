package at.knowcenter.recommender.solrpowered.services.impl.socialstream;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.engine.utils.SolrUtils;
import at.knowcenter.recommender.solrpowered.model.SocialStream;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkMessage;
import at.knowcenter.recommender.solrpowered.services.common.SolrService;

/**
 * Social Stream Service
 * 
 * @author elacic
 * 
 */
public class SocialStreamService extends SolrService<SocialStreamQuery, SocialStream, SocialStreamResponse>{
	
	public SocialStreamService(String address, Integer port, String coreName) {
		this.address = address;
		this.port = port;
		this.collection = coreName;
		this.solrServer = SolrUtils.newServer("http://" + address + ":" + port + "/solr/" + coreName);
	}
	
	/**
	 * searches the index for the query and returns a SearchResponse Object
	 * 
	 * @param query
	 * @return
	 */
	public SocialStreamResponse search(SocialStreamQuery query, int maxResultCount) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		solrParams.set("q", query.getQuery());
		solrParams.set("rows", maxResultCount);

		QueryResponse response = null;
		SocialStreamResponse searchResponse = new SocialStreamResponse();
		try {
			response = solrServer.query(solrParams);
			List<String> users = RecommendationQueryUtils.extractRecommendationIds(response.getBeans(SocialStream.class));
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
	public void updateDocument(SocialStream socialAction) {
		updateDocument(socialAction, solrServer);
	}
	

	/**
	 * updates/adds a list of Documents as beans to the index in an own thread
	 * if there are already documents with the same ids they will be overwritten
	 * @param searchItem
	 */
	public void writeDocuments(List<SocialStream> socialActions, SearchServerBulkMessage searchServerBulkUpload) {
		updateDocuments(socialActions, searchServerBulkUpload, solrServer);
		
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
