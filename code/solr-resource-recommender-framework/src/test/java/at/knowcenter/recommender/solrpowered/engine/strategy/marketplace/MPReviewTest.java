package at.knowcenter.recommender.solrpowered.engine.strategy.marketplace;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.Before;
import org.junit.Test;

import at.knowcenter.recommender.solrpowered.engine.RecommenderEngine;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.MPReviewBasedRec;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class MPReviewTest {

	private MPReviewBasedRec mpApproach;

	@Before
	public void setUp() throws Exception {
		mpApproach = new MPReviewBasedRec();

	}

	@Test
	public void testMPreturns10() {
		int maxResult = 10;
		RecommendResponse response = mpApproach.recommend(new RecommendQuery(), maxResult);
		
		assertTrue(response.getResultItems() != null);
		assertTrue(response.getResultItems().size() == maxResult);
	}
	
	@Test
	public void testMPreturns5() {
		int maxResult = 5;
		RecommendResponse response = mpApproach.recommend(new RecommendQuery(), maxResult);
		
		assertTrue(response.getResultItems() != null);
		assertTrue(response.getResultItems().size() == maxResult);
	}
	
	@Test
	public void testMPReturnsRightOrder() {
		int maxResult = 10;
		RecommendResponse response = mpApproach.recommend(new RecommendQuery(), maxResult);
		List<String> mpProducts = response.getResultItems();
		
		assertTrue(mpProducts != null);
		String query = RecommendationQueryUtils.createOrderedQuery("id", mpProducts);
		
		SolrServer resourceServer = SolrServiceContainer.getInstance().getResourceService().getSolrServer();
		
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("q", query);
		try {
			List<Resource> mpResources = resourceServer.query(params).getBeans(Resource.class);
			
			int lastPurchaseCount = Integer.MAX_VALUE;
			for (Resource r : mpResources) {
				int ratingCountSum = r.getUsersRated1Count() + r.getUsersRated2Count() + r.getUsersRated3Count() + 
						r.getUsersRated4Count() + r.getUsersRated5Count();
				
				if (ratingCountSum > lastPurchaseCount) {
					fail("Wrong MP order for resource at index: " + mpResources.indexOf(r));
				}
				
				lastPurchaseCount = ratingCountSum;
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
			fail("Exception was thrown");
		}
	}
	
	@Test
	public void testApproachIsBoundToEngine() throws Exception {
		RecommenderEngine engine = new RecommenderEngine();
		RecommendStrategy approach = engine.getApproach(StrategyType.MostPopular_Review);
		
		assertNotNull(approach);
		assertTrue(approach instanceof MPReviewBasedRec);
	}

}
