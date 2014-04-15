package at.knowcenter.recommender.solrpowered.engine.strategy.social.cf;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import at.knowcenter.recommender.solrpowered.engine.RecommenderEngine;
import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.services.cleaner.DataFetcher;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class InteractionTest {

	private InteractionsBasedRec cfSocialApproach;
	
	private static final String userToEvaluate = "suki.rexen";
	private RecommendQuery query;


	@Before
	public void setUp() throws Exception {
		cfSocialApproach = new InteractionsBasedRec();

		List<String> alreadyPurchasedProducts = DataFetcher.getRatedProductsFromUser(userToEvaluate);
		Customer userProfile = DataFetcher.getUserProfile(userToEvaluate);
		ContentFilter cf = new ContentFilter();
		cf.setCustomer(userProfile);
		
		cfSocialApproach.setAlreadyPurchasedResources(alreadyPurchasedProducts);
		cfSocialApproach.setContentFiltering(cf);
		
		query = new RecommendQuery();
		query.setUser(userToEvaluate);
	}

	@Test
	public void testCFReturns10() {
		int maxResult = 10;
		RecommendResponse response = cfSocialApproach.recommend(query, maxResult);
		
		assertTrue(response.getResultItems() != null);
		assertTrue(response.getResultItems().size() == maxResult);
	}
	
	@Test
	public void testCFReturns5() {
		int maxResult = 5;
		RecommendResponse response = cfSocialApproach.recommend(query, maxResult);
		
		assertTrue(response.getResultItems() != null);
		assertTrue(response.getResultItems().size() == maxResult);
	}
	
	
	@Test
	public void testCFFiltersPurchasedProducts() {
		int maxResult = 5;
		RecommendResponse response = cfSocialApproach.recommend(query, maxResult);
		
		List<String> recommendedItems = response.getResultItems();
		
		assertTrue(recommendedItems != null);
		
		assertTrue(Collections.disjoint(
						recommendedItems, 
						cfSocialApproach.getAlreadyBoughtProducts())
					);
	}
	
	@Test
	public void testApproachIsBoundToEngine() throws Exception {
		RecommenderEngine engine = new RecommenderEngine();
		RecommendStrategy approach = engine.getApproach(StrategyType.CF_Social);
		
		assertNotNull(approach);
		assertTrue(approach instanceof SocialBasedRec);
	}
	

}
