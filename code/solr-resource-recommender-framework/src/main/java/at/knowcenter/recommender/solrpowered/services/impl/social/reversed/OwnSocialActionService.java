package at.knowcenter.recommender.solrpowered.services.impl.social.reversed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.engine.utils.SolrUtils;
import at.knowcenter.recommender.solrpowered.model.OwnSocialAction;
import at.knowcenter.recommender.solrpowered.model.SocialAction;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkMessage;
import at.knowcenter.recommender.solrpowered.services.common.SolrService;
import at.knowcenter.recommender.solrpowered.services.impl.social.SocialActionQuery;
import at.knowcenter.recommender.solrpowered.services.impl.social.SocialActionResponse;

public class OwnSocialActionService  extends SolrService<SocialActionQuery, SocialAction, SocialActionResponse>{
	
	public OwnSocialActionService(String address, Integer port, String coreName) {
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
		throw new IllegalArgumentException("Update own social action is not yet implemented");
//		updateStoredItems(socialAction);
//		
//		updateDocument(socialAction, solrServer);
	}
	
	private void updateStoredItems(OwnSocialAction sa) {
		QueryResponse response = findElementById(sa.getUserId(), solrServer);
		List<OwnSocialAction> socialActions = response.getBeans(OwnSocialAction.class);
		
		if (socialActions.size() > 0) {
			// get first and only response (was searched by id)
			OwnSocialAction storedSocialAction = socialActions.get(0);
			// set stored users
			setStoredValues(sa, storedSocialAction);
		}
	}
	
	private void setStoredValues(OwnSocialAction sa, OwnSocialAction storedSocialAction) {
		List<String> usersThatIComentedOn = storedSocialAction.getUsersThatICommentedOn();
		RecommendationQueryUtils.appendItemsToStoredList(usersThatIComentedOn, sa.getUsersThatICommentedOn());
		sa.setUsersThatICommentedOn(usersThatIComentedOn);
		
		List<String> usersThatILiked = storedSocialAction.getUsersThatILiked();
		RecommendationQueryUtils.appendItemsToStoredList(usersThatILiked, sa.getUsersThatILiked());
		sa.setUsersThatILiked(usersThatILiked);
	}
	

	/**
	 * updates/adds a list of Documents as beans to the index in an own thread
	 * if there are already documents with the same ids they will be overwritten
	 * @param searchItem
	 */
	public void writeDocuments(List<SocialAction> socialActions, SearchServerBulkMessage searchServerBulkUpload) {
		long start = System.nanoTime();
		
		Map<String, OwnSocialAction> userSocialActionToStoreMap = new HashMap<String, OwnSocialAction>();
		
		for (SocialAction socialActionToProcess : socialActions){
			
			for (String userThatLikedMe : socialActionToProcess.getUsersThatLikedMe()) {
				
				OwnSocialAction userThatLikesMeSocialAction = userSocialActionToStoreMap.get(userThatLikedMe);
				
				if (userThatLikesMeSocialAction == null) {
					userThatLikesMeSocialAction = new OwnSocialAction();
					
					userThatLikesMeSocialAction.setUserId(userThatLikedMe);
					userThatLikesMeSocialAction.setUsersThatICommentedOn(new ArrayList<String>());
				} 
				
				userThatLikesMeSocialAction.addUserThatILiked(socialActionToProcess.getUserId());
				userSocialActionToStoreMap.put(userThatLikedMe, userThatLikesMeSocialAction);
			}
			
			for (String userThatCommentedOnMe : socialActionToProcess.getUsersThatCommentedOnMyPost()) {
				
				OwnSocialAction userThatCommentedOnMeSocialAction = userSocialActionToStoreMap.get(userThatCommentedOnMe);
				
				if (userThatCommentedOnMeSocialAction == null) {
					userThatCommentedOnMeSocialAction = new OwnSocialAction();
					
					userThatCommentedOnMeSocialAction.setUserId(userThatCommentedOnMe);
					userThatCommentedOnMeSocialAction.setUsersThatICommentedOn(new ArrayList<String>());
				} 
				
				userThatCommentedOnMeSocialAction.addUserThatICommentedOn(socialActionToProcess.getUserId());
				userSocialActionToStoreMap.put(userThatCommentedOnMe, userThatCommentedOnMeSocialAction);
			}
		}
		
		for (OwnSocialAction ca : userSocialActionToStoreMap.values()) {
			updateStoredItems(ca);
		}

		try {
			solrServer.addBeans(userSocialActionToStoreMap.values());
			solrServer.commit();
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}
		System.out.println("SocialActions upload done in: " + (System.nanoTime() - start) + " ns" );
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
