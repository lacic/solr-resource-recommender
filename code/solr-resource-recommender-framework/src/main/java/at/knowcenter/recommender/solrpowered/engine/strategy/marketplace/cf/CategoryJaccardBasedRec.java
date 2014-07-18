package at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.CFQueryBuilder;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.evaluation.UserSimilarityTracker;
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

/**
 * CF approach based on purchased categories
 * @author Emanuel Lacic
 *
 */
public class CategoryJaccardBasedRec implements RecommendStrategy {

	public static final String USERS_RATED_1_FIELD = "users_rated_1";
	public static final String USERS_RATED_2_FIELD = "users_rated_2";
	public static final String USERS_RATED_3_FIELD = "users_rated_3";
	public static final String USERS_RATED_4_FIELD = "users_rated_4";
	public static final String USERS_RATED_5_FIELD = "users_rated_5";
	public static int MAX_USER_OCCURENCE_COUNT = CFQueryBuilder.MAX_USER_OCCURENCE_COUNT;
	private List<String> alreadyBoughtProducts;
	private ContentFilter contentFilter;

	@Override
	public RecommendResponse recommend(RecommendQuery query, Integer maxReuslts){
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		RecommendResponse searchResponse = new RecommendResponse();
		
		long step0ElapsedTime = 0;
		long step1ElapsedTime;
		List<String> recommendations = new ArrayList<String>();

		try {
			// STEP 0 - get products from a user
			if (query.getUser() != null ) {
				if (query.getProductIds() == null || query.getProductIds().size() == 0) {
					if (alreadyBoughtProducts != null && alreadyBoughtProducts.size() > 0) {
						query.setProductIds(alreadyBoughtProducts);
					} else {
						searchResponse.setNumFound(0);
						searchResponse.setResultItems(recommendations);
						searchResponse.setElapsedTime(-1);
						return searchResponse;
					}
				}
			}
			
			StringBuilder queryBuilder = new StringBuilder("id:(");
			for (String product : alreadyBoughtProducts) {
				queryBuilder.append("\"" + product + "\" OR ");
			}
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ")");
			
			solrParams.set("q", queryBuilder.toString());
			solrParams.set("fl", "tags");
			solrParams.set("rows", Integer.MAX_VALUE);
			response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(solrParams);
			
			List<Resource> resources = response.getBeans(Resource.class);
			Set<String> knownCategories = new HashSet<String>();
			
			for (Resource item : resources) {
				if (item.getTags() != null) {
					knownCategories.addAll(item.getTags());
				}
			}
			Set<String> otherUsers = fetchUsersFromCategories(knownCategories, query.getUser());
			Map<String, Set<String>> userSellerMap = fetchUserCategoryMapping(otherUsers, query.getUser());
			final Map<String, Double> commonNeighborMap = new HashMap<String, Double>();
			for (String commonUser : userSellerMap.keySet()) {
				Set<String> categories = userSellerMap.get(commonUser);
				
				Set<String> intersection = new HashSet<String>(categories);
				intersection.retainAll(knownCategories);
				
				Set<String> union = new HashSet<String>(categories);
				union.addAll(knownCategories);
				
				commonNeighborMap.put(commonUser, intersection.size() / (double) union.size() );				
			}
			
			Comparator<String> interactionCountComparator = new Comparator<String>() {

				@Override
				public int compare(String a, String b) {
					if (commonNeighborMap.get(a) >= commonNeighborMap.get(b)) {
			            return -1;
			        } else {
			            return 1;
			        }
				}
				
			};
			
			final String user = query.getUser();
			Thread t = new Thread() {
				@Override public void run() {
					UserSimilarityTracker.getInstance().writeToFile("soc_market_cat_jacc", user, commonNeighborMap);
				}
			};
			t.start();
			
	        TreeMap<String,Double> sortedMap = new TreeMap<String,Double>(interactionCountComparator);
	        sortedMap.putAll(commonNeighborMap);
			// STEP 2 - find products liked by similar users
	        ModifiableSolrParams cfParams = CFQueryBuilder.getCFStep2Params(
	        		query, maxReuslts, 
	        		commonNeighborMap, sortedMap.keySet(), 
	        		contentFilter, alreadyBoughtProducts);
	        
	        response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(cfParams);
			
			// fill response object
			List<Resource> beans = response.getBeans(Resource.class);
			searchResponse.setResultItems(RecommendationQueryUtils.extractRecommendationIds(beans));
			searchResponse.setElapsedTime(response.getElapsedTime());

			SolrDocumentList docResults = response.getResults();
			searchResponse.setNumFound(docResults.getNumFound());
		} catch (SolrServerException e) {
			e.printStackTrace();
			searchResponse.setNumFound(0);
			searchResponse.setResultItems(recommendations);
			searchResponse.setElapsedTime(-1);
		}
		
		return searchResponse;
	}


	private Set<String> fetchUsersFromCategories(Set<String> tags, String currentUser) {
		Set<String> usersFromCategories = new HashSet<String>();
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		
		StringBuilder sellerBuilder = new StringBuilder("tags:(");

		for (String seller : tags) {
			sellerBuilder.append("\"" + seller + "\" OR ");
		}
		
		if (tags.size() > 0) {
			sellerBuilder.replace(sellerBuilder.length() - 3, sellerBuilder.length(), ")");
		} else {
			sellerBuilder.append("\"\")");
		}
		
		solrParams.set("q", sellerBuilder.toString());
		solrParams.set("rows", Integer.MAX_VALUE);
		solrParams.set("fq", "tags:[* TO *]");
		solrParams.set("fl", "users_rated_5,users_rated_4,users_rated_3,users_rated_2,users_rated_1");
		try {
			QueryResponse response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(solrParams);
			
			List<Resource> resources = response.getBeans(Resource.class);
			
			for (Resource res : resources) {
				List<String> usersRated1 = res.getUsersRated1();
				List<String> usersRated2 = res.getUsersRated2();
				List<String> usersRated3 = res.getUsersRated3();
				List<String> usersRated4 = res.getUsersRated4();
				List<String> usersRated5 = res.getUsersRated5();
				
				if (usersRated1 != null) {
					usersFromCategories.addAll(usersRated1);
				}
				if (usersRated2 != null) {
					usersFromCategories.addAll(usersRated2);
				}
				if (usersRated3 != null) {
					usersFromCategories.addAll(usersRated3);
				}
				if (usersRated4 != null) {
					usersFromCategories.addAll(usersRated4);
				}
				if (usersRated5 != null) {
					usersFromCategories.addAll(usersRated5);
				}
			}
		} catch (SolrServerException e) {
			System.out.println(solrParams);
			e.printStackTrace();
		}
		usersFromCategories.remove(currentUser);
		return usersFromCategories;
	}
	
	private Map<String, Set<String>> fetchUserCategoryMapping(Set<String> otherUsers, String currentUser) {
		Map<String, Set<String>> userCategoryMapping = new HashMap<String, Set<String>>();
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		
		StringBuilder rated5Builder = new StringBuilder(USERS_RATED_5_FIELD + ":(");
		StringBuilder rated4Builder = new StringBuilder(USERS_RATED_4_FIELD + ":(");
		StringBuilder rated3Builder = new StringBuilder(USERS_RATED_3_FIELD + ":(");
		StringBuilder rated2Builder = new StringBuilder(USERS_RATED_2_FIELD + ":(");
		StringBuilder rated1Builder = new StringBuilder(USERS_RATED_1_FIELD + ":(");

		for (String otherUser : otherUsers) {
			rated5Builder.append(otherUser + " OR ");
			rated4Builder.append(otherUser + " OR ");
			rated3Builder.append(otherUser + " OR ");
			rated2Builder.append(otherUser + " OR ");
			rated1Builder.append(otherUser + " OR ");
		}
		
		if (otherUsers.size() > 0) {
			rated5Builder.replace(rated5Builder.length() - 3, rated5Builder.length(), ")");
			rated4Builder.replace(rated4Builder.length() - 3, rated4Builder.length(), ")");
			rated3Builder.replace(rated3Builder.length() - 3, rated3Builder.length(), ")");
			rated2Builder.replace(rated2Builder.length() - 3, rated2Builder.length(), ")");
			rated1Builder.replace(rated1Builder.length() - 3, rated1Builder.length(), ")");
		} else {
			rated5Builder.append("\"\")");
			rated4Builder.append("\"\")");
			rated3Builder.append("\"\")");
			rated2Builder.append("\"\")");
			rated1Builder.append("\"\")");
		}
		/*
		solrParams.set("q", rated5Builder.toString() + " OR " + rated4Builder.toString() + " OR " + 
				rated3Builder.toString() + " OR " + rated2Builder.toString() + " OR " + rated1Builder.toString());
		solrParams.set("rows", Integer.MAX_VALUE);
		solrParams.set("fq", "tags:[* TO *]");
		solrParams.set("fl", "tags,users_rated_5,users_rated_4,users_rated_3,users_rated_2,users_rated_1");
*/
		try {
			QueryResponse response = null;

			solrParams = new ModifiableSolrParams();
			solrParams.set("q", rated5Builder.toString());
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fq", "tags:[* TO *]");
			solrParams.set("fl", "tags,users_rated_5");

			response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(solrParams);
			
			List<Resource> resources = response.getBeans(Resource.class);
			for (Resource res : resources) {
				List<String> categories = res.getTags();
				
				List<String> usersRated5 = res.getUsersRated5();
				
				if (usersRated5 != null) {
					fillSellersForUsers(userCategoryMapping, categories, usersRated5, currentUser, otherUsers);
				}
			}

			solrParams = new ModifiableSolrParams();
			solrParams.set("q", rated4Builder.toString());
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fq", "tags:[* TO *]");
			solrParams.set("fl", "tags,users_rated_4");

			response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(solrParams);
			
			resources = response.getBeans(Resource.class);
			for (Resource res : resources) {
				List<String> categories = res.getTags();
				
				List<String> usersRated4 = res.getUsersRated4();
				
				if (usersRated4 != null) {
					fillSellersForUsers(userCategoryMapping, categories, usersRated4, currentUser, otherUsers);
				}
			}

			solrParams = new ModifiableSolrParams();
			solrParams.set("q", rated3Builder.toString());
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fq", "tags:[* TO *]");
			solrParams.set("fl", "tags,users_rated_3");

			response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(solrParams);
			
			resources = response.getBeans(Resource.class);
			for (Resource res : resources) {
				List<String> categories = res.getTags();
				
				List<String> usersRated3 = res.getUsersRated3();
				
				if (usersRated3 != null) {
					fillSellersForUsers(userCategoryMapping, categories, usersRated3, currentUser, otherUsers);
				}
			}

			solrParams = new ModifiableSolrParams();
			solrParams.set("q", rated2Builder.toString());
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fq", "tags:[* TO *]");
			solrParams.set("fl", "tags,users_rated_2");

			response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(solrParams);
			
			resources = response.getBeans(Resource.class);
			for (Resource res : resources) {
				List<String> categories = res.getTags();
				
				List<String> usersRated2 = res.getUsersRated2();
				
				if (usersRated2 != null) {
					fillSellersForUsers(userCategoryMapping, categories, usersRated2, currentUser, otherUsers);
				}
			}

			solrParams = new ModifiableSolrParams();
			solrParams.set("q", rated1Builder.toString());
			solrParams.set("rows", Integer.MAX_VALUE);
			solrParams.set("fq", "tags:[* TO *]");
			solrParams.set("fl", "tags,users_rated_1");

			response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(solrParams);
			
			resources = response.getBeans(Resource.class);
			for (Resource res : resources) {
				List<String> categories = res.getTags();
				
				List<String> usersRated1 = res.getUsersRated1();
				
				if (usersRated1 != null) {
					fillSellersForUsers(userCategoryMapping, categories, usersRated1, currentUser, otherUsers);
				}
			}


/*
			for (Resource res : resources) {
				List<String> categories = res.getTags();
				
				List<String> usersRated1 = res.getUsersRated1();
				List<String> usersRated2 = res.getUsersRated2();
				List<String> usersRated3 = res.getUsersRated3();
				List<String> usersRated4 = res.getUsersRated4();
				List<String> usersRated5 = res.getUsersRated5();
				
				if (usersRated1 != null) {
					fillSellersForUsers(userCategoryMapping, categories, usersRated1, currentUser, otherUsers);
				}
				if (usersRated2 != null) {
					fillSellersForUsers(userCategoryMapping, categories, usersRated2, currentUser, otherUsers);
				}
				if (usersRated3 != null) {
					fillSellersForUsers(userCategoryMapping, categories, usersRated3, currentUser, otherUsers);
				}
				if (usersRated4 != null) {
					fillSellersForUsers(userCategoryMapping, categories, usersRated4, currentUser, otherUsers);
				}
				if (usersRated5 != null) {
					fillSellersForUsers(userCategoryMapping, categories, usersRated5, currentUser, otherUsers);
				}
			}
*/
		} catch (Exception e) {
			System.out.println(solrParams);
			e.printStackTrace();
		}
		return userCategoryMapping;
	}
	
	private void fillSellersForUsers(Map<String, Set<String>> userCategoryMapping, List<String> categories, 
			List<String> usersThatPurchased, String currentUser, Set<String> otherUsers) {
		
		for (String purchasingUser :usersThatPurchased) {
			if ((! purchasingUser.equals(currentUser)) && otherUsers.contains(purchasingUser)) {
				Set<String> sellers = userCategoryMapping.get(purchasingUser);
				
				if (sellers == null) {
					sellers = new HashSet<String>();
				}
				
				sellers.addAll(categories);
				userCategoryMapping.put(purchasingUser, sellers);
			}
		}
	}

	@Override
	public void setAlreadyPurchasedResources(List<String> alreadyBoughtProducts) {
		this.alreadyBoughtProducts = alreadyBoughtProducts;
	}

	@Override
	public List<String> getAlreadyBoughtProducts() {
		return alreadyBoughtProducts;
	}
	
	@Override
	public void setContentFiltering(ContentFilter contentFilter) {
		this.contentFilter = contentFilter;
	}

	@Override
	public StrategyType getStrategyType() {
		return StrategyType.CF_Categories;
	}
	
}