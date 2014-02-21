package at.knowcenter.recommender.solrpowered.services.impl.social;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.engine.utils.SolrUtils;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.model.SocialAction;
import at.knowcenter.recommender.solrpowered.model.CustomerAction.ACTION;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkMessage;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkRemoveThread;
import at.knowcenter.recommender.solrpowered.services.common.Facet;
import at.knowcenter.recommender.solrpowered.services.common.FacetGroup;
import at.knowcenter.recommender.solrpowered.services.common.Snippet;
import at.knowcenter.recommender.solrpowered.services.common.SolrService;

/**
 * Social Actions Service
 * 
 * @author elacic
 * 
 */
public class SocialActionService extends SolrService<SocialActionQuery, SocialAction, SocialActionResponse>{
	
	public SocialActionService(String address, Integer port, String coreName) {
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
	public SocialActionResponse search(SocialActionQuery query, int maxResultCount) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("rows", maxResultCount);

		QueryResponse response = null;
		SocialActionResponse searchResponse = new SocialActionResponse();
		try {
			response = solrServer.query(solrParams);
			List<String> users = RecommendationQueryUtils.extractRecommendationIds(response.getBeans(SocialAction.class));
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
	public void updateDocument(SocialAction socialAction) {
		updateStoredItems(socialAction);
		
		updateDocument(socialAction, solrServer);
	}
	
	private void updateStoredItems(SocialAction sa) {
		QueryResponse response = findElementById(sa.getUserId(), solrServer);
		List<SocialAction> socialActions = response.getBeans(SocialAction.class);
		
		if (socialActions.size() > 0) {
			// get first and only response (was searched by id)
			SocialAction storedSocialAction = socialActions.get(0);
			// set stored users
			setStoredValues(sa, storedSocialAction);
		}
	}
	
	private void setStoredValues(SocialAction sa, SocialAction storedSocialAction) {
		List<String> usersThatComentedOnMyPost = storedSocialAction.getUsersThatCommentedOnMyPost();
		RecommendationQueryUtils.appendItemsToStoredList(usersThatComentedOnMyPost, sa.getUsersThatCommentedOnMyPost());
		sa.setUsersThatCommentedOnMyPost(usersThatComentedOnMyPost);
		
		List<String> usersThatLikedMe = storedSocialAction.getUsersThatLikedMe();
		RecommendationQueryUtils.appendItemsToStoredList(usersThatLikedMe, sa.getUsersThatLikedMe());
		sa.setUsersThatLikedMe(usersThatLikedMe);
	}
	

	/**
	 * updates/adds a list of Documents as beans to the index in an own thread
	 * if there are already documents with the same ids they will be overwritten
	 * @param searchItem
	 */
	public void updateDocuments(List<SocialAction> socialActions, SearchServerBulkMessage searchServerBulkUpload) {
		long start = System.nanoTime();
		Map<String, SocialAction> saToStoreMap = new HashMap<String, SocialAction>();
		
		for (SocialAction saToStore : socialActions){
			SocialAction savedSocialAction = saToStoreMap.get(saToStore.getUserId());
			
			if (savedSocialAction == null) {
				saToStoreMap.put(saToStore.getUserId(), saToStore);
			} else {
				setStoredValues(saToStore, savedSocialAction);
				saToStoreMap.put(saToStore.getUserId(), saToStore);
			}
		}
		
		for (SocialAction ca : saToStoreMap.values()) {
			updateStoredItems(ca);
		}

		try {
			solrServer.addBeans(saToStoreMap.values());
			solrServer.commit();
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}
//		System.out.println("SocialActions upload done in: " + (System.nanoTime() - start) + " ns" );
	}
	

	@Override
	public void removeElementById(String id, String language) {
		if (language != null) {
			id += "_" + language;
		}
		removeElementById(id, solrServer);
		
	}
}
