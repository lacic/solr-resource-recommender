package at.knowcenter.recommender.solrpowered.services.impl.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.RecommenderOperations;
import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.filtering.PrecedingItemEvaluation;
import at.knowcenter.recommender.solrpowered.engine.strategy.CFRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.CNRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.MostPopularRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.PrecedingItemBasedRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedDescription;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedDescriptionName;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedDescriptionNameTags;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedDescriptionTags;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedName;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedNameDescription;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedNameDescriptionTags;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedNameTags;
import at.knowcenter.recommender.solrpowered.engine.strategy.cnapproaches.CNRecommenderWeightedTags;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.CFCategoryRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.CFOwnSocialRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.CFSocialCommentsRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.CFSocialInteractionsRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.CFSocialLikesRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.CFSocialStream3Recommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.UserBasedCustomerGroupsRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.UserBasedInterestsCustomerGroupRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.UserBasedInterestsRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.UserBasedRecommenderWithoutMLT;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.engine.utils.SolrUtils;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.CustomerAction.ACTION;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkMessage;
import at.knowcenter.recommender.solrpowered.services.common.SolrService;

/**
 * Implements {@linkplain SolrService} to use SOLR as a recommender
 * @author elacic
 *
 */
public class RecommendService extends SolrService<RecommendQuery, CustomerAction, RecommendResponse> {

	
	public RecommendService(SolrServer solrServer){
		this.solrServer = solrServer;
	}

	public RecommendService(String address, Integer port, String collection) {
		this.address = address;
		this.port = port;
		this.collection = collection;
        this.solrServer = SolrUtils.newServer("http://" + address + ":" + port + "/solr/" + collection);
	}
	
	/** Search for a specific {@link CustomerAction} object by its id
	 * 
	 * @param itemId the item id of the desired customer action
	 * @return
	 */
	public CustomerAction searchById(String itemId) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		solrParams.set("q", "id:" + itemId);
		QueryResponse response = null;
		try {
			response = solrServer.query(solrParams);
			List<CustomerAction> inte = response.getBeans(CustomerAction.class);
			return inte.get(0);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public RecommendResponse search(RecommendQuery query, int maxResultCount) {
		return null;
	}

	/**
	 * Adds or Updates an item to SOLR
	 */
	@Override
	synchronized public void updateDocument(CustomerAction recommendItem) {
		updateStoredItems(recommendItem);
		
		recommendItem.setUserCount(recommendItem.getCustomerIds().size());
		updateDocument(recommendItem, solrServer);
	}

	@Override
	public void updateDocuments(List<CustomerAction> searchItems,
			SearchServerBulkMessage searchServerBulkUpload) {
		long start = System.nanoTime();
		Map<String, CustomerAction> caToStoreMap = new HashMap<String, CustomerAction>();
		
		for (CustomerAction caToStore : searchItems){
			CustomerAction savedCA = caToStoreMap.get(caToStore.getItemId());
			
			if (savedCA == null) {
				appendCustomerIds(caToStore);
				caToStoreMap.put(caToStore.getItemId(), caToStore);
			} else {
				setStoredValues(caToStore, savedCA);
				appendCustomerIds(caToStore);
				caToStoreMap.put(caToStore.getItemId(), caToStore);
			}
		}
		
		for (CustomerAction ca : caToStoreMap.values()) {
			ca.setAction(ACTION.EMPTY);
			updateStoredItems(ca);
		}

		/*********************************************************************************************************
		 * TODO
		 * UploadData.java vom test/tools ausführen. Eine kleine Datei wird hochgeladen: "../webserver/src/test/resources/testFiles/customerActions_Amazon.csv" 
		 * 1. Die generierten CustomerActionInternals zum Abspeichern ins Solr sind fehlerhaft: z.B. Item "6" wurde 15 mal verkauft, aber überall sieht man userCountPurchased=0
		 * 2. Bevor diese CustomerActionInternals ins Solr rein gehen, habe ich diese leeren Felder mit paar Integers initialisiert (jetzt auskommentiert). Diese Werte werden ins Solr nicht gespeichert.
		 * 
		 * *******************************************************************************************************
		 */
		try {
			for( CustomerAction ca : caToStoreMap.values()){
//				ca.setUserCountPurchased(3);
//				ca.setUserCountViewed(6);
//				ca.setUserCountMarkedFavorite(9);
//				ca.setUserCount(12);
				System.out.println(ca);
			}
			solrServer.addBeans(caToStoreMap.values());
			solrServer.commit();
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}
//		System.out.println("CA upload done in: " + (System.nanoTime() - start) + " ns" );
//		updateDocuments(searchItems, searchServerBulkUpload, solrServer);
	}

	private void updateStoredItems(CustomerAction ca) {
		QueryResponse response = findElementById(ca.getItemId(), solrServer);
		List<CustomerAction> items = response.getBeans(CustomerAction.class);
		
		if (items.size() > 0) {
			// get first and only response (was searched by id)
			CustomerAction storedItem = items.get(0);
			// set stored users
			setStoredValues(ca, storedItem);
		}
		
		appendCustomerIds(ca);
	}


	private void setStoredValues(CustomerAction ca, CustomerAction storedItem) {
		List<String> customerIdsViewed = storedItem.getCustomerIdsViewed();
		customerIdsViewed = RecommendationQueryUtils.appendItemsToStoredList(customerIdsViewed, ca.getCustomerIdsViewed());
		ca.setCustomerIdsViewed(customerIdsViewed);
		
		List<String> customerIdsPurchased = storedItem.getCustomerIdsPurchased();
		customerIdsPurchased = RecommendationQueryUtils.appendItemsToStoredList(customerIdsPurchased, ca.getCustomerIdsPurchased());
		ca.setCustomerIdsPurchased(customerIdsPurchased);
		
		List<String> customerIdsMarkedFavorite = storedItem.getCustomerIdsMarkedFavorite();
		customerIdsMarkedFavorite = RecommendationQueryUtils.appendItemsToStoredList(customerIdsMarkedFavorite, ca.getCustomerIdsMarkedFavorite());
		ca.setCustomerIdsMarkedFavorite(customerIdsMarkedFavorite);
	}


	private void appendCustomerIds(CustomerAction ca) {
		if (ca.getAction() == ACTION.VIEWED) {
			List<String> storedViewedItems = RecommendationQueryUtils.appendItemsToStoredList(ca.getCustomerIdsViewed(), ca.getCustomerIds());
			ca.setCustomerIdsViewed(storedViewedItems);
		} else if (ca.getAction() == ACTION.PURCHASED) {
			List<String> storedViewedItems = RecommendationQueryUtils.appendItemsToStoredList(ca.getCustomerIdsPurchased(), ca.getCustomerIds());
			ca.setCustomerIdsPurchased(storedViewedItems);
		} else if (ca.getAction() == ACTION.MARKED_FAVORITE) {
			List<String> storedViewedItems = RecommendationQueryUtils.appendItemsToStoredList(ca.getCustomerIdsMarkedFavorite(), ca.getCustomerIds());
			ca.setCustomerIdsMarkedFavorite(storedViewedItems);
		}
	}


	@Override
	public void removeElementById(String id, String language) {
		removeElementById(id, solrServer);
	}
}
