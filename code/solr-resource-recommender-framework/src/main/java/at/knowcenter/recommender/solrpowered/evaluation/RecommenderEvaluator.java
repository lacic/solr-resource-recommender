package at.knowcenter.recommender.solrpowered.evaluation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

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
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendService;

/**
 * Evaluator class for offline evaluation
 * @author elacic
 *
 */
public class RecommenderEvaluator extends RecommenderEngine{

	private List<String> removedOwnProducts;
	private double precision;
	private PredictionCalculator eval;
	private SimilarityCalculator similarityItemEval;
	
	protected int resultSize = 10;
	protected int userCount = -1;
	protected int minimalPurchasedCount = 1;
	protected String jobDescription = "";
	
	public static int miscCount = 0;

	
	@Override
	protected List<String> initUsersOwnProductsFiltering(String userID) {
		super.initUsersOwnProductsFiltering(userID);
		List<RecommendStrategy> strategies = this.getRecommendStrategies();
		RecommendStrategy someStrategy = strategies.get(0);
		
		List<String> alreadyBoughtProducts = someStrategy.getAlreadyBoughtProducts();
		
		removedOwnProducts = new ArrayList<String>();
		if (alreadyBoughtProducts != null && alreadyBoughtProducts.size() > 0 ) {
			int startIndexToRemove = alreadyBoughtProducts.size() / 2;
			int productsToRemove = 10;
			
			if (alreadyBoughtProducts.size() < 20) {
				productsToRemove = (int) Math.round((alreadyBoughtProducts.size() / 2.0));
				startIndexToRemove = 0;
			}
			
			for (int i = 0; i < productsToRemove; i++) {
				String removedProduct = alreadyBoughtProducts.remove(startIndexToRemove);
				removedOwnProducts.add(removedProduct);
			}
			
			for (RecommendStrategy recSt : strategies) {
				recSt.setAlreadyPurchasedResources(alreadyBoughtProducts);
			}
		}
		return alreadyBoughtProducts;
	}
	
	@Override
	public List<String> getRecommendations(String userID, String productID, int n) {
		precision = 0.0;
		List<String> recommendations = super.getRecommendations(userID, productID, n);
		
		if (removedOwnProducts == null) {
			System.out.println("not enough bought products to evaluate");
			return recommendations;
		}

		double foundOwnProducts = 0.0;
		for (String rec : recommendations) {
			if (removedOwnProducts.contains(rec))
				foundOwnProducts++;
		}
		precision = foundOwnProducts / removedOwnProducts.size();
		
		this.eval = new PredictionCalculator(userID, removedOwnProducts, recommendations, n);
		
		List<Resource> recommendedItems = new ArrayList<Resource>();
		
		for (String product : recommendations) {
			QueryResponse findElementById = 
					SolrServiceContainer.getInstance().getResourceService().findElementById(product, SolrServiceContainer.getInstance().getResourceService().getSolrServer());
			recommendedItems.addAll(findElementById.getBeans(Resource.class));
		}
		
		
		this.similarityItemEval = new SimilarityCalculator(new ArrayList<Resource>(), recommendedItems, n);
		
//		System.out.println( precision );
		return recommendations;
	}
	
	/**
	 * Evaluates on k from 1 to 10 for all users and no product
	 */
	public List<MetricsExporter> evaluate() {
		List<String> users = getAllUsers();
		ContentFilter cf = new ContentFilter();
//		cf.setFriendsEvaluationMethod(FriendsEvaluation.HIGH);
		List<MetricsExporter> metricsCalcs = new ArrayList<MetricsExporter>();
		
		for (int n = 1; n <= resultSize; n++) {
			MetricsExporter mCalc = new MetricsExporter("/tmp/");
			metricsCalcs.add(mCalc);
		}
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate_CF_C_MP(users, cf, metricsCalcs);

		evaluate_CF(users, cf, metricsCalcs);
		
		evaluate_C(users, cf, metricsCalcs);
		
		evaluate_MP(users, cf, metricsCalcs);
		
		evaluate_CF_C(users, cf, metricsCalcs);
		
		evaluate_CF_MP(users, cf, metricsCalcs);
		
		evaluate_C_MP(users, cf, metricsCalcs);
		
		return metricsCalcs;
	}

	protected void evaluate_CF(List<String> users, ContentFilter cf, List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		int userSize = users.size();
		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			
			List<String> recommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CollaborativeFiltering));

			long duaration = System.nanoTime() - getRecommStartTime;
			
			for (int n = 1; n <= resultSize; n++) {
				MetricsExporter mCalc = metricsCalcs.get(n - 1);
				if (recommendations.size() > n) {
					appendMetrics(mCalc, n, userID, recommendations.subList(0, n),
							recommendStrategies.get(StrategyType.CollaborativeFiltering).getAlreadyBoughtProducts());
				} else {
					appendMetrics(mCalc, n, userID, recommendations,
							recommendStrategies.get(StrategyType.CollaborativeFiltering).getAlreadyBoughtProducts());
				}
				mCalc.appendDuaration(duaration);
			}
			currentEvaluatingUser++;
			System.out.println("CF_+" + jobDescription +": Evaluation progress: " + ((currentEvaluatingUser) / (double)userSize) * 100 + " % done");
		}
//		for (int n = 1; n <= resultSize; n++) {
//			MetricsExporter mCalc = metricsCalcs.get(n - 1);
//			mCalc.exportCalculatedMetricsAverage("6_CF", users.size());
//		}
	}

	protected void evaluate_C(List<String> users, ContentFilter cf,
			List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		int userSize = users.size();
		for (String userID : users) {
			List<String> recommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CN_WeightNameDescription));

			for (int n = 1; n <= resultSize; n++) {
				MetricsExporter mCalc = metricsCalcs.get(n - 1);
				if (recommendations.size() > n) {
					appendMetrics(mCalc, n, userID, recommendations.subList(0, n),
							recommendStrategies.get(StrategyType.CN_WeightNameDescription).getAlreadyBoughtProducts());
				} else {
					appendMetrics(mCalc, n, userID, recommendations,
							recommendStrategies.get(StrategyType.CN_WeightNameDescription).getAlreadyBoughtProducts());
				}
			}
			currentEvaluatingUser++;
			System.out.println("C: Evaluation progress: " + ((currentEvaluatingUser) / (double)userSize) * 100 + " % done");
		}
//		for (int n = 1; n <= resultSize; n++) {
//			MetricsExporter mCalc = metricsCalcs.get(n - 1);
//			mCalc.exportCalculatedMetricsAverage("7_C", users.size());
//		}
	}

	protected void evaluate_MP(List<String> users, ContentFilter cf, List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		int userSize = users.size();
		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			
			List<String> recommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.MostPopular));

			long duaration = System.nanoTime() - getRecommStartTime;
			
			for (int n = 1; n <= resultSize; n++) {
				MetricsExporter mCalc = metricsCalcs.get(n - 1);
				if (recommendations.size() > n) {
					appendMetrics(mCalc, n, userID, recommendations.subList(0, n),
							recommendStrategies.get(StrategyType.MostPopular).getAlreadyBoughtProducts());
				} else {
					appendMetrics(mCalc, n, userID, recommendations,
							recommendStrategies.get(StrategyType.MostPopular).getAlreadyBoughtProducts());
				}
				mCalc.appendDuaration(duaration);
			}
			currentEvaluatingUser++;
			System.out.println("MP_+" + jobDescription +": Evaluation progress: " + ((currentEvaluatingUser) / (double)userSize) * 100 + " % done");
		}
//		for (int n = 1; n <= resultSize; n++) {
//			MetricsExporter mCalc = metricsCalcs.get(n - 1);
//			mCalc.exportCalculatedMetricsAverage("8_MP", users.size());
//		}
	}

	protected void evaluate_CF_C(List<String> users, ContentFilter cf, List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		int userSize = users.size();
		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			
			List<String> cfRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CollaborativeFiltering));
			List<String> cbRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CN_WeightNameDescription));

			List<String> unsortedAllProducts = new ArrayList<String>(cfRecommendations);
			unsortedAllProducts.addAll(cbRecommendations);
			
			List<String> sortedAndTrimedRecommendations = RecommendationQueryUtils.extractRankedProducts(unsortedAllProducts);

			RecommendationQueryUtils.appendDifferentProducts(resultSize, sortedAndTrimedRecommendations, cfRecommendations);
			RecommendationQueryUtils.appendDifferentProducts(resultSize, sortedAndTrimedRecommendations, cbRecommendations);
			
			long duaration = System.nanoTime() - getRecommStartTime;
			
			for (int n = 1; n <= resultSize; n++) {
				MetricsExporter mCalc = metricsCalcs.get(n - 1);
				if (sortedAndTrimedRecommendations.size() > n) {
					appendMetrics(mCalc, n, userID, sortedAndTrimedRecommendations.subList(0, n),
							recommendStrategies.get(StrategyType.CN_WeightNameDescription).getAlreadyBoughtProducts());
				} else {
					appendMetrics(mCalc, n, userID, sortedAndTrimedRecommendations,
							recommendStrategies.get(StrategyType.CN_WeightNameDescription).getAlreadyBoughtProducts());
				}
				mCalc.appendDuaration(duaration);
			}

			currentEvaluatingUser++;
//			if (duaration / 1000000 > 900) {
//				System.out.println(userID + " " + duaration / 1000000);
//			}
			System.out.println("CF_C_+" + jobDescription +": Evaluation progress: " + ((currentEvaluatingUser) / (double)userSize) * 100 + " % done; T= " + duaration / 1000000);
		}
//		for (int n = 1; n <= resultSize; n++) {
//			MetricsExporter mCalc = metricsCalcs.get(n - 1);
//			mCalc.exportCalculatedMetricsAverage("3_CF-C", users.size());
//		}
	}

	protected void evaluate_CF_MP(List<String> users, ContentFilter cf,	List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		int userSize = users.size();
		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			
			List<String> cfRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CollaborativeFiltering));
			List<String> mpRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.MostPopular));


			List<String> unsortedAllProducts = new ArrayList<String>(cfRecommendations);
			unsortedAllProducts.addAll(mpRecommendations);
			List<String> sortedAndTrimedRecommendations = RecommendationQueryUtils.extractRankedProducts(unsortedAllProducts);
			if (sortedAndTrimedRecommendations.size() < resultSize) {
				RecommendationQueryUtils.appendDifferentProducts(resultSize, sortedAndTrimedRecommendations, cfRecommendations);
			}
			if (sortedAndTrimedRecommendations.size() < resultSize) {
				RecommendationQueryUtils.appendDifferentProducts(resultSize, sortedAndTrimedRecommendations, mpRecommendations);
			}
			
			long duaration = System.nanoTime() - getRecommStartTime;
			
			for (int n = 1; n <= resultSize; n++) {
				MetricsExporter mCalc = metricsCalcs.get(n - 1);
				if (sortedAndTrimedRecommendations.size() > n) {
					appendMetrics(mCalc, n, userID, sortedAndTrimedRecommendations.subList(0, n),
							recommendStrategies.get(StrategyType.CollaborativeFiltering).getAlreadyBoughtProducts());
				} else {
					appendMetrics(mCalc, n, userID, sortedAndTrimedRecommendations,
							recommendStrategies.get(StrategyType.CollaborativeFiltering).getAlreadyBoughtProducts());
				}
				mCalc.appendDuaration(duaration);
			}
			currentEvaluatingUser++;
			System.out.println("CF_MP_+" + jobDescription +": Evaluation progress: " + ((currentEvaluatingUser) / (double)userSize) * 100 + " % done");
		}
//		for (int n = 1; n <= resultSize; n++) {
//			MetricsExporter mCalc = metricsCalcs.get(n - 1);
//			mCalc.exportCalculatedMetricsAverage("4_CF-MP", users.size());
//		}
	}

	protected void evaluate_C_MP(List<String> users, ContentFilter cf, List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		int userSize = users.size();
		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			
			List<String> cbRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CN_WeightNameDescription));
			List<String> mpRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.MostPopular));

			List<String> unsortedAllProducts = new ArrayList<String>(cbRecommendations);
			unsortedAllProducts.addAll(cbRecommendations);
			
			List<String> sortedAndTrimedRecommendations = RecommendationQueryUtils.extractRankedProducts(unsortedAllProducts);

			RecommendationQueryUtils.appendDifferentProducts(resultSize, sortedAndTrimedRecommendations, cbRecommendations);
			RecommendationQueryUtils.appendDifferentProducts(resultSize, sortedAndTrimedRecommendations, mpRecommendations);
			
			long duaration = System.nanoTime() - getRecommStartTime;
			
			for (int n = 1; n <= resultSize; n++) {
				MetricsExporter mCalc = metricsCalcs.get(n - 1);
				if (sortedAndTrimedRecommendations.size() > n) {
					appendMetrics(mCalc, n, userID, sortedAndTrimedRecommendations.subList(0, n),
							recommendStrategies.get(StrategyType.CN_WeightNameDescription).getAlreadyBoughtProducts());
				} else {
					appendMetrics(mCalc, n, userID, sortedAndTrimedRecommendations, 
							recommendStrategies.get(StrategyType.CN_WeightNameDescription).getAlreadyBoughtProducts());
				}
				mCalc.appendDuaration(duaration);
			}
			currentEvaluatingUser++;
			System.out.println("C_MP_+" + jobDescription +": Evaluation progress: " + ((currentEvaluatingUser) / (double)userSize) * 100 + " % done");
		}
//		for (int n = 1; n <= resultSize; n++) {
//			MetricsExporter mCalc = metricsCalcs.get(n - 1);
//			mCalc.exportCalculatedMetricsAverage("5_C-MP", users.size());
//		}
	}

	protected void evaluate_CF_C_MP(List<String> users, ContentFilter cf, List<MetricsExporter> metricsCalcs) {
		int currentEvaluatingUser = 0;
		int userSize = users.size();
		
		for (String userID : users) {
			long getRecommStartTime = System.nanoTime();
			
			List<String> cfRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CollaborativeFiltering));
			List<String> cbRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.CN_WeightNameDescription));

			List<String> unsortedAllProducts = new ArrayList<String>(cfRecommendations);
			unsortedAllProducts.addAll(cbRecommendations);
			
			List<String> sortedAndTrimedRecommendations = RecommendationQueryUtils.extractRankedProducts(unsortedAllProducts);

			RecommendationQueryUtils.appendDifferentProducts(resultSize, sortedAndTrimedRecommendations, cfRecommendations);
			RecommendationQueryUtils.appendDifferentProducts(resultSize, sortedAndTrimedRecommendations, cbRecommendations);
			
			if (sortedAndTrimedRecommendations.size() < resultSize) {
				List<String> mpRecommendations = getRecommendations(userID, null, resultSize, cf, recommendStrategies.get(StrategyType.MostPopular));
				RecommendationQueryUtils.appendDifferentProducts(resultSize, sortedAndTrimedRecommendations, mpRecommendations);
			}
			
			long duaration = System.nanoTime() - getRecommStartTime;
			
			for (int n = 1; n <= resultSize; n++) {
				MetricsExporter mCalc = metricsCalcs.get(n - 1);
				if (sortedAndTrimedRecommendations.size() > n) {
					appendMetrics(mCalc, n, userID, sortedAndTrimedRecommendations.subList(0, n), 
							recommendStrategies.get(StrategyType.CN_WeightNameDescription).getAlreadyBoughtProducts());
				} else {
					appendMetrics(mCalc, n, userID, sortedAndTrimedRecommendations,
							recommendStrategies.get(StrategyType.CN_WeightNameDescription).getAlreadyBoughtProducts());
				}
				mCalc.appendDuaration(duaration);
			}
			currentEvaluatingUser++;
			double progress = ((double) currentEvaluatingUser) / userSize;
			System.out.println("CF_C_MP_+" + jobDescription +": Evaluation progress: " + progress * 100 + " % done");
		}
//		for (int n = 1; n <= resultSize; n++) {
//			MetricsExporter mCalc = metricsCalcs.get(n - 1);
//			mCalc.exportCalculatedMetricsAverage("2_CF-C-MP", users.size());
//		}
	}

	protected void appendMetrics(MetricsExporter mCalc, int k, String userID,
			List<String> recommendations, List<String> alreadyBoughtProducts) {
		QueryResponse findElementById = 
				SolrServiceContainer.getInstance().getResourceService().findElementsById(recommendations, SolrServiceContainer.getInstance().getResourceService().getSolrServer());
		
		QueryResponse removedResponse = 
				SolrServiceContainer.getInstance().getResourceService().findElementsById(removedOwnProducts, SolrServiceContainer.getInstance().getResourceService().getSolrServer());
		
		
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
		List<Resource> recommendedResources = findElementById.getBeans(Resource.class);
		List<Resource> removedResources = removedResponse.getBeans(Resource.class);
		
        boolean topLevelOnly = true;
		List<String> recommendedCategories = extractCategories(recommendedResources, topLevelOnly);
		List<String> purchasedCategories = extractCategories(removedResources, topLevelOnly);

		PredictionCalculator pEval = new PredictionCalculator(userID, removedOwnProducts, recommendations, k);
//		PredictionCalculator pEval = new PredictionCalculator(userID, purchasedCategories, recommendedCategories, k);
		SimilarityCalculator sEval = new SimilarityCalculator(fetchedAlreadyBoughtItems, recommendedResources, k);
		
		mCalc.appendMetrics(pEval, sEval);
	}

	private List<String> extractCategories(List<Resource> removedResources, boolean topLevelOnly) {
		final Map<String, Integer> purchCategoryOccurance = new HashMap<String, Integer>();

		for (Resource res : removedResources) {
			String cat1 = res.getCategory1();
			String cat2 = res.getCategory2();
			String cat3 = res.getCategory3();
			String cat4 = res.getCategory4();
			
			if (cat4 != null && !topLevelOnly) {
				addCategoryOccurance(purchCategoryOccurance, cat4);
			} else if (cat3 != null && !topLevelOnly) {
				addCategoryOccurance(purchCategoryOccurance, cat3);
			} else if (cat2 != null && !topLevelOnly) {
				addCategoryOccurance(purchCategoryOccurance, cat2);
			} else 
			if (cat1 != null) {
				addCategoryOccurance(purchCategoryOccurance, cat1);
			}
		}
		
		Comparator<String> purchOccuranceComparator = new Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				if (purchCategoryOccurance.get(a) > purchCategoryOccurance.get(b)) {
		            return -1;
		        } else {
		            return 1;
		        }
			}
		};
		
		TreeMap<String, Integer> sorted_map = new TreeMap<String,Integer>(purchOccuranceComparator);
        sorted_map.putAll(purchCategoryOccurance);
        
        return new ArrayList<String>(sorted_map.keySet());
	}

	private void addCategoryOccurance(Map<String, Integer> recCategoryOccurance, String category) {
		Integer occurance = recCategoryOccurance.get(category);
		if (occurance == null) {
			recCategoryOccurance.put(category, 1);
		} else {
			recCategoryOccurance.put(category, occurance + 1);
		}
	}
	
	
	public List<String> getRecommendations(final String userID, final String productID, final int n, ContentFilter contentFilter, RecommendStrategy strategyToUse) {
		precision = 0.0;
		
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

	public double getPrecision() {
		return precision;
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
