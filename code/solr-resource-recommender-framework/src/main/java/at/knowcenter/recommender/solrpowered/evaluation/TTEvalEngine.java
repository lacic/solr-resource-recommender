package at.knowcenter.recommender.solrpowered.evaluation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.RecommenderEngine;
import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.filtering.FriendsEvaluation;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.PredictionCalculator;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.SimilarityCalculator;
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.model.OwnSocialAction;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.model.SocialAction;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.cleaner.DataFetcher;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendService;

/**
 * Evaluator class for offline evaluation that is based on a training and test data set in two different Apache Solr instances
 * @author elacic
 *
 */
public class TTEvalEngine extends RecommenderEngine{

	private List<String> removedOwnProducts;
	private PredictionCalculator eval;
	private SimilarityCalculator similarityItemEval;
	
	protected int resultSize = 10;
	protected int userCount = -1;
	protected int minimalPurchasedCount = 1;
	protected String jobDescription = "";

	
	@Override
	protected List<String> initUsersOwnProductsFiltering(String userID) {
		super.initUsersOwnProductsFiltering(userID);
		
		List<RecommendStrategy> strategies = this.getRecommendStrategies();
		RecommendStrategy someStrategy = strategies.get(0);
		
		List<String> alreadyBoughtProducts = someStrategy.getAlreadyBoughtProducts();
		
		removedOwnProducts = DataFetcher.getRatedProductsFromUser(userID, SolrServiceContainer.getInstance().getTestReviewService());
//		removedOwnProducts.removeAll(alreadyBoughtProducts);
		
		return alreadyBoughtProducts;
	}

	@Override
	public List<String> getRecommendations(String userID, String productID, int n) {
		List<String> recommendations = super.getRecommendations(userID, productID, n);
		
		if (removedOwnProducts == null) {
			System.out.println("not enough bought products to evaluate");
			return recommendations;
		}

		this.eval = new PredictionCalculator(userID, removedOwnProducts, recommendations, n);
		
		List<Resource> recommendedItems = new ArrayList<Resource>();
		
		for (String product : recommendations) {
			QueryResponse findElementById = 
					SolrServiceContainer.getInstance().getResourceService().findElementById(product, SolrServiceContainer.getInstance().getResourceService().getSolrServer());
			recommendedItems.addAll(findElementById.getBeans(Resource.class));
		}
		
		this.similarityItemEval = new SimilarityCalculator(new ArrayList<Resource>(), recommendedItems, n);
		
		return recommendations;
	}
	
	/**
	 * Appends different metric calculators to the metrics exporter
	 * @param mExp metrics exporter component
	 * @param k number of recommendations
	 * @param userID id of the user
	 * @param recommendations resources that were recommended
	 * @param alreadyBoughtProducts resources that were already purchased
	 */
	protected void appendMetrics(
			MetricsExporter mExp, int k, String userID,
			List<String> recommendations, List<String> alreadyBoughtProducts) {
		
		QueryResponse recResponse = 
				SolrServiceContainer.getInstance().getResourceService().findElementsById(
						recommendations, SolrServiceContainer.getInstance().getResourceService().getSolrServer());
		
		
		List<Resource> fetchedAlreadyBoughtItems = fetchAlreadyPurchasedProducts(alreadyBoughtProducts);
		
		List<Resource> recommendedResources = recResponse.getBeans(Resource.class);
		

		PredictionCalculator pEval = new PredictionCalculator(userID, removedOwnProducts, recommendations, k);
		SimilarityCalculator sEval = new SimilarityCalculator(fetchedAlreadyBoughtItems, recommendedResources, k);
		
		mExp.appendMetrics(pEval, sEval);
	}

	private List<Resource> fetchAlreadyPurchasedProducts(
			List<String> alreadyBoughtProducts) {
		List<Resource> fetchedAlreadyBoughtItems = new ArrayList<Resource>();
		
		if (alreadyBoughtProducts.size() <= 2500) {
			QueryResponse alreadyBoughtItems = 
					SolrServiceContainer.getInstance().getResourceService().findElementsById(alreadyBoughtProducts, SolrServiceContainer.getInstance().getResourceService().getSolrServer());
			fetchedAlreadyBoughtItems.addAll(alreadyBoughtItems.getBeans(Resource.class));
		} else {
			int fetchIteration = 2500;
			int fetchOffset = 0;
			
			while (fetchOffset < alreadyBoughtProducts.size()) {
				QueryResponse alreadyBoughtItems = null;
				if (fetchOffset + fetchIteration < alreadyBoughtProducts.size()) {
					alreadyBoughtItems = SolrServiceContainer.getInstance().getRecommendService().findElementsById(
							alreadyBoughtProducts.subList(fetchOffset, fetchOffset + fetchIteration), 
							SolrServiceContainer.getInstance().getItemService().getSolrServer());
				} else {
					alreadyBoughtItems = SolrServiceContainer.getInstance().getRecommendService().findElementsById(
							alreadyBoughtProducts.subList(fetchOffset, alreadyBoughtProducts.size()), 
							SolrServiceContainer.getInstance().getItemService().getSolrServer());
				}
				
				fetchedAlreadyBoughtItems.addAll(alreadyBoughtItems.getBeans(Resource.class));
				fetchOffset += fetchIteration;
			}
		}
		return fetchedAlreadyBoughtItems;
	}
	
	
	public List<String> getRecommendations(final String userID, final String productID, final int n, ContentFilter contentFilter, RecommendStrategy strategyToUse) {
		
		List<String> recommendations;
		if (strategyToUse != null) {
			recommendations = super.getRecommendations(userID, productID, n, contentFilter, strategyToUse);
		} else {
			recommendations = super.getRecommendations(userID, productID, n, contentFilter);
		}
		return recommendations;
	}
	
	public List<String> getAllUsers(){
		
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("facet", "true");
		solrParams.set("facet.field", "users_purchased");
		solrParams.set("facet.limit", userCount);
		solrParams.set("facet.mincount", minimalPurchasedCount );
	
		try {
			response = SolrServiceContainer.getInstance().getRecommendService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<FacetField> facetFields = response.getFacetFields();
		// get the first and only facet field -> users
		FacetField userFacet = facetFields.get(0);
		List<String> users = new ArrayList<String>();
		
		for(Count c : userFacet.getValues()) {
			users.add(c.getName());
		}
		
		return users;
	}
	
	public List<String> getAllSocialStreamUsers(){
		
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("facet", "true");
		solrParams.set("facet.field", "source");
		solrParams.set("facet.limit", userCount);
		solrParams.set("facet.mincount", minimalPurchasedCount );
	
		try {
			response = SolrServiceContainer.getInstance().getSocialStreamService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<FacetField> facetFields = response.getFacetFields();
		// get the first and only facet field -> users
		FacetField userFacet = facetFields.get(0);
		List<String> users = new ArrayList<String>();
		
		for(Count c : userFacet.getValues()) {
			users.add(c.getName());
		}
		
		return users;
	}
	
	public List<String> getAllSocialUsers(){
		
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("rows", 80000);
		solrParams.set("fl", "id");
	
		try {
			response = SolrServiceContainer.getInstance().getSocialActionService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<OwnSocialAction> users = response.getBeans(OwnSocialAction.class);
		
		return RecommendationQueryUtils.extractRecommendationIds(users);
	}
	
	public List<String> getAllExistingUsers(){
		
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("rows", 1600000);
		solrParams.set("fl", "id");
	
		try {
			response = SolrServiceContainer.getInstance().getUserService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<Customer> customers = response.getBeans(Customer.class);
		List<String> users = new ArrayList<String>();
		
		for(Customer c : customers) {
			users.add(c.getId());
		}
		
		return users;
	}

	public PredictionCalculator getEval() {
		return eval;
	}
	
	public SimilarityCalculator getSimilarityEval() {
		return similarityItemEval;
	}

	public int getResultSize() {
		return resultSize;
	}

	public void setResultSize(int resultSize) {
		this.resultSize = resultSize;
	}

	protected List<MetricsExporter> initMetricCalcs(String algName) {
		List<MetricsExporter> metricsExporters = new ArrayList<MetricsExporter>();
		
		for (int n = 1; n <= resultSize; n++) {
			MetricsExporter metricExporter = new MetricsExporter("./evaluation_data/");
			metricExporter.setAlgorithmName(algName);
			metricsExporters.add(metricExporter);
		}
		
		return metricsExporters;
	}

}
