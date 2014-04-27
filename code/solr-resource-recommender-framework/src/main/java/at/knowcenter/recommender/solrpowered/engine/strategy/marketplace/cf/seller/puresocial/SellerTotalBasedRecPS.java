package at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.seller.puresocial;

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
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.utils.CFQueryBuilder;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.cleaner.DataFetcher;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemQuery;

/**
 * Collaborative Filtering Recommender strategy
 * @author elacic
 *
 */
public class SellerTotalBasedRecPS implements RecommendStrategy {

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
						searchResponse.setResultItems(new ArrayList<String>());
						return searchResponse;
					}
				}
			}
			// find sellers
			List<Resource> purchasedResources = findPurchasedResources(query.getProductIds());

			List<String> sellersPurchasedFrom = new ArrayList<String>();

			for (Resource res : purchasedResources) {
				sellersPurchasedFrom.add(res.getSeller());
			}
			
			Set<String> otherUsers = 
					new HashSet<String>(DataFetcher.getSocialNeighbourUsers(query.getUser()));
			
			Map<String, List<String>> userSellerMap = fetchUserSellerMapping(otherUsers, query.getUser());
			
			final Map<String, Double> commonNeighborMap = new HashMap<String, Double>();
			for (String commonUser : userSellerMap.keySet()) {
				List<String> sellers = userSellerMap.get(commonUser);
				
				Set<String> union = new HashSet<String>(sellers);
				union.addAll(sellersPurchasedFrom);
				
				commonNeighborMap.put(commonUser, (double) union.size());				
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
			
	        TreeMap<String,Double> sortedMap = new TreeMap<String,Double>(interactionCountComparator);
	        sortedMap.putAll(commonNeighborMap);
			// STEP 2 - find products liked by similar users
			solrParams = getSTEP2Params(query, maxReuslts, commonNeighborMap, sortedMap.keySet());

			SolrServer server = SolrServiceContainer.getInstance().getResourceService().getSolrServer();
			response = server.query(solrParams);
			
			// fill response object
			List<Resource> beans = response.getBeans(Resource.class);
			searchResponse.setResultItems(RecommendationQueryUtils.extractRecommendationIds(beans));
			searchResponse.setElapsedTime(step0ElapsedTime + response.getElapsedTime());

			SolrDocumentList docResults = response.getResults();
			searchResponse.setNumFound(docResults.getNumFound());
		} catch (Exception e) {
			System.out.println("E: " + solrParams);
			e.printStackTrace();
			searchResponse.setNumFound(0);
			searchResponse.setResultItems(recommendations);
			searchResponse.setElapsedTime(-1);
		}
		
		return searchResponse;
	}



	private Set<String> fetchUsersFromSellers(Set<String> sellersPurchasedFrom, String currentUser) {
		Set<String> usersFromSellers = new HashSet<String>();
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		
		StringBuilder sellerBuilder = new StringBuilder("seller:(");

		for (String seller : sellersPurchasedFrom) {
			sellerBuilder.append(seller + " OR ");
		}
		
		if (sellersPurchasedFrom.size() > 0) {
			sellerBuilder.replace(sellerBuilder.length() - 3, sellerBuilder.length(), ")");
		} else {
			sellerBuilder.append("\"\")");
		}
		
		solrParams.set("q", sellerBuilder.toString());
		solrParams.set("rows", Integer.MAX_VALUE);
		solrParams.set("fq", "seller:[* TO *]");
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
					usersFromSellers.addAll(usersRated1);
				}
				if (usersRated2 != null) {
					usersFromSellers.addAll(usersRated2);
				}
				if (usersRated3 != null) {
					usersFromSellers.addAll(usersRated3);
				}
				if (usersRated4 != null) {
					usersFromSellers.addAll(usersRated4);
				}
				if (usersRated5 != null) {
					usersFromSellers.addAll(usersRated5);
				}
			}
		} catch (SolrServerException e) {
			System.out.println(solrParams);
			e.printStackTrace();
		}
		usersFromSellers.remove(currentUser);
		return usersFromSellers;
	}



	private List<Resource> findPurchasedResources(List<String> productIds) throws SolrServerException {
		ModifiableSolrParams solrParams = createParamsToFindSellers(productIds);
		QueryResponse response = 
				SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(solrParams);
		List<Resource> purchasedResources = response.getBeans(Resource.class);
		
		return purchasedResources;
	}


	private Map<String, List<String>> fetchUserSellerMapping(Set<String> otherUsers, String currentUser) {
		Map<String, List<String>> userSellersMapping = new HashMap<String, List<String>>();
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
		solrParams.set("q", rated5Builder.toString() + " OR " + rated4Builder.toString() + " OR " + 
				rated3Builder.toString() + " OR " + rated2Builder.toString() + " OR " + rated1Builder.toString());
		solrParams.set("rows", Integer.MAX_VALUE);
		solrParams.set("fq", "seller:[* TO *]");
		solrParams.set("fl", "seller,users_rated_5,users_rated_4,users_rated_3,users_rated_2,users_rated_1");

		try {
			QueryResponse response = SolrServiceContainer.getInstance().getResourceService().getSolrServer().query(solrParams);
			
			List<Resource> resources = response.getBeans(Resource.class);
			
			for (Resource res : resources) {
				String seller = res.getSeller();
				
				List<String> usersRated1 = res.getUsersRated1();
				List<String> usersRated2 = res.getUsersRated2();
				List<String> usersRated3 = res.getUsersRated3();
				List<String> usersRated4 = res.getUsersRated4();
				List<String> usersRated5 = res.getUsersRated5();
				
				if (usersRated1 != null) {
					fillSellersForUsers(userSellersMapping, seller, usersRated1, currentUser, otherUsers);
				}
				if (usersRated2 != null) {
					fillSellersForUsers(userSellersMapping, seller, usersRated2, currentUser, otherUsers);
				}
				if (usersRated3 != null) {
					fillSellersForUsers(userSellersMapping, seller, usersRated3, currentUser, otherUsers);
				}
				if (usersRated4 != null) {
					fillSellersForUsers(userSellersMapping, seller, usersRated4, currentUser, otherUsers);
				}
				if (usersRated5 != null) {
					fillSellersForUsers(userSellersMapping, seller, usersRated5, currentUser, otherUsers);
				}
			}
		} catch (SolrServerException e) {
			System.out.println(solrParams);
			e.printStackTrace();
		}
		return userSellersMapping;
	}


	private void fillSellersForUsers(Map<String, List<String>> userSellersMapping, String seller, 
			List<String> usersThatPurchased, String currentUser, Set<String> otherUsers) {
		
		for (String purchasingUser :usersThatPurchased) {
			if ((! purchasingUser.equals(currentUser)) && otherUsers.contains(purchasingUser)) {
				List<String> sellers = userSellersMapping.get(purchasingUser);
				
				if (sellers == null) {
					sellers = new ArrayList<String>();
				}
				
				sellers.add(seller);
				userSellersMapping.put(purchasingUser, sellers);
			}
		}
	}


	private ModifiableSolrParams getSTEP2Params(
			RecommendQuery query, Integer maxReuslts, Map<String, Double> userInteractionMap, Set<String> sortedKeys) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		
		String queryString = createQueryToFindProdLikedBySimilarSocialUsers(
				userInteractionMap, sortedKeys, contentFilter, MAX_USER_OCCURENCE_COUNT);
		
		String filterQueryString = 
				RecommendationQueryUtils.buildFilterForContentBasedFiltering(contentFilter);
		
		if (alreadyBoughtProducts != null && alreadyBoughtProducts.size() > 0) {
			if (filterQueryString.trim().length() > 0) {
				filterQueryString += " OR ";
			}
			filterQueryString += RecommendationQueryUtils.buildFilterForAlreadyBoughtProducts("id",alreadyBoughtProducts);
		}
		
		solrParams.set("q", queryString);
		solrParams.set("fq", filterQueryString);
		solrParams.set("fl", "id");
		solrParams.set("rows", maxReuslts);
		return solrParams;
	}
	
	public static String createQueryToFindProdLikedBySimilarSocialUsers(
			Map<String, Double> userInteractionMap, Set<String> sortedKeys, ContentFilter contentFilter, int maxUserOccurence) {
		String query = createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedKeys, contentFilter, USERS_RATED_5_FIELD, maxUserOccurence, 1.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedKeys, contentFilter, USERS_RATED_4_FIELD, maxUserOccurence, 2.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedKeys, contentFilter, USERS_RATED_3_FIELD, maxUserOccurence, 3.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedKeys, contentFilter, USERS_RATED_2_FIELD, maxUserOccurence, 4.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, sortedKeys, contentFilter, USERS_RATED_1_FIELD, maxUserOccurence, 5.0);
		
		return query;
	}
	
	public static String createQueryToFindProdLikedBySimilarUsers(
			Map<String, Double> userInteractionMap, 
			Set<String> sortedKeys, ContentFilter contentFilter,
			String usersFieldName,
			int maxUserOccurence,
			double weightDividor) {
		StringBuilder queryBuilder = new StringBuilder();

		queryBuilder.append(usersFieldName + ":(");
		
		if (weightDividor <= 0.0) {
			queryBuilder.append("\"\")");
			return queryBuilder.toString();
		}
		//  max users
		int userOccurenceCount = 0;
		
		for (String user : sortedKeys) {
			if (userOccurenceCount >= maxUserOccurence) { break; }
			Double boosting = userInteractionMap.get(user) / weightDividor ;
			boosting = ((int) (boosting * 100)) / 100.0;
			
			queryBuilder.append(user + "^" + boosting + " OR ");
			userOccurenceCount++;
		}
		
		if (queryBuilder.length() > (usersFieldName + ":(").length()){
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), "");
		} else {
			queryBuilder.append("\"\"");
		}
		
		queryBuilder.append(")");
		return queryBuilder.toString();
	}

	private ModifiableSolrParams createParamsToFindSellers(List<String> purchasedProducts) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		String queryString = 
				RecommendationQueryUtils.createQueryToFindSimilarUsersForSameAttribute("id", purchasedProducts);
		
		solrParams.set("q", queryString);
		solrParams.set("fq", "seller:[* TO *]");
		solrParams.set("fl", "seller");
		solrParams.set("rows", purchasedProducts.size());
		
		return solrParams;
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
		return StrategyType.CF_Market_Seller_Total;
	}

}
