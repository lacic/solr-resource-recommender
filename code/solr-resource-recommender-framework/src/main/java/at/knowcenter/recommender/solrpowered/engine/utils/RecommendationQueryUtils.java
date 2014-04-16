package at.knowcenter.recommender.solrpowered.engine.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.filtering.FriendsEvaluation;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.ReviewBasedRec;
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.model.OwnSocialAction;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.model.SocialAction;
import at.knowcenter.recommender.solrpowered.model.SocialStream;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemQuery;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemResponse;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemService;

/**
 * Creates query strings based on the query data
 * @author elacic
 *
 */
public class RecommendationQueryUtils {
	
	private static final String ITEMS_CORE = "bn_items";
	public static int MAX_SAME_PRODUCT_COUNT = 2900;
	/**
	 * Step 1 in Collaborative Filtering using SOLR.
	 * <br/>
	 * Finds similar users who like the same document
	 */
	public static String createQueryToFindSimilarUsersForSameAttribute(String attributeName, List<String> ids) {
		StringBuilder queryBuilder = new StringBuilder();
		
		if (ids != null) {

			queryBuilder.append(attributeName + ":(");
			
			int productCounter = 0;
			
			for (String product : ids) {
				if (productCounter >= MAX_SAME_PRODUCT_COUNT) {
					break;
				}
				
				queryBuilder.append("\"" + product + "\" OR ");
				productCounter++;
			}
			
			if (ids != null && ids.size() > 0){
				queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), "");
			} else {
				queryBuilder.append("\"\"");
			}
			
			queryBuilder.append(")");
		}

		return queryBuilder.toString();
	}
	
	/**
	 * Step 1 in Collaborative Filtering using SOLR.
	 * <br/>
	 * Finds similar users who like the same document
	 */
	public static String createOrderedQuery(String attributeName, List<String> ids) {
		StringBuilder queryBuilder = new StringBuilder();
		
		if (ids != null) {

			queryBuilder.append(attributeName + ":(");
			
			int productCounter = 0;
			
			int weight = ids.size();
			
			for (String product : ids) {
				if (productCounter >= MAX_SAME_PRODUCT_COUNT) {
					break;
				}
				
				queryBuilder.append("\"" + product + "\"^"+ weight +" OR ");
				productCounter++;
				weight--;
			}
			
			if (ids != null && ids.size() > 0){
				queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), "");
			}
			
			queryBuilder.append(")");
		}

		return queryBuilder.toString();
	}
	
	/**
	 * Step 2 in Collaborative Filtering using SOLR.
	 * <br/>
	 * Finds docs which are "liked" by the similar users in Step 1
	 * @param contentFilter 
	 */
	public static String createQueryToFindProdLikedBySimilarUsers(
			List<Count> userOccurences, 
			String currentUser, 
			ContentFilter contentFilter,
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
		long maxUserCount = 0;
		long userCountSum = 0;
		
		for (Count userOccurence : userOccurences) {
			if ( ! userOccurence.getName().equals(currentUser)) {
				if (userOccurenceCount >= maxUserOccurence) {
					break;
				}
				queryBuilder.append("\"" + userOccurence.getName() + "\"^" + (userOccurence.getCount() / weightDividor ) + " OR ");
				if (userOccurence.getCount() > maxUserCount) {
					maxUserCount = userOccurence.getCount();
				}
				userCountSum += userOccurence.getCount();
				userOccurenceCount++;
			}
		}
		
		if (contentFilter != null && contentFilter.getFriendsEvaluationMethod() != FriendsEvaluation.NOTHING) {
			if (contentFilter.getCustomer() != null && contentFilter.getCustomer().getFriendOf() != null) {
				for (String friend : contentFilter.getCustomer().getFriendOf()) {
					if (contentFilter.getFriendsEvaluationMethod() == FriendsEvaluation.HIGH) {
						queryBuilder.append("\"" + friend + "\"^" + maxUserCount  + " OR ");
					} else if (contentFilter.getFriendsEvaluationMethod() == FriendsEvaluation.AVERAGE) {
						queryBuilder.append("\"" + friend + "\"^" + userCountSum / userOccurenceCount + " OR ");
					} else if (contentFilter.getFriendsEvaluationMethod() == FriendsEvaluation.LOW) {
						queryBuilder.append("\"" + friend + "\"^" + 1 + " OR ");
					}
				}
			}
		}
		
		
		if (queryBuilder.length() > (usersFieldName + ":(").length()){
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), "");
		} else {
			queryBuilder.append("\"\"");
		}
		
		queryBuilder.append(")");
		return queryBuilder.toString();
	}
	
	/**
	 * Step 2 in Social Collaborative Filtering using SOLR.
	 * <br/>
	 * Finds docs which are "liked" by the similar users in Step 1
	 * @param boostings 
	 * @param contentFilter 
	 */
	public static String createQueryToFindProdLikedBySimilarSocialUsers(
			List<String> users, List<String> boostings, ContentFilter contentFilter, int maxUserOccurence) {
		StringBuilder queryBuilder = new StringBuilder();

		queryBuilder.append("users_purchased:(");
		//  max users
		int userOccurenceCount = 0;
		
		for (String user : users) {
			if (userOccurenceCount >= maxUserOccurence) { break; }
			
			String boosting = "0.1";
			if (userOccurenceCount < boostings.size()) {
				boosting = boostings.get(userOccurenceCount);
			}

			queryBuilder.append("\"" + user + "\"^" + boosting + " OR ");
			
			userOccurenceCount++;
		}
		
		
		if (queryBuilder.length() > ("users_purchased:(").length()){
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), "");
		}
		
		queryBuilder.append(")");
		return queryBuilder.toString();
	}
	
	
	public static String createQueryToFindProdLikedBySimilarSocialUsers(
			Map<String, Double> userInteractionMap, ContentFilter contentFilter, int maxUserOccurence) {
		String query = createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, contentFilter, ReviewBasedRec.USERS_RATED_5_FIELD, maxUserOccurence, 1.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, contentFilter, ReviewBasedRec.USERS_RATED_4_FIELD, maxUserOccurence, 2.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, contentFilter, ReviewBasedRec.USERS_RATED_3_FIELD, maxUserOccurence, 3.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, contentFilter, ReviewBasedRec.USERS_RATED_2_FIELD, maxUserOccurence, 4.0);
		query += " OR " + createQueryToFindProdLikedBySimilarUsers(
				userInteractionMap, contentFilter, ReviewBasedRec.USERS_RATED_1_FIELD, maxUserOccurence, 5.0);
		
		return query;
	}
	
	private static String createQueryToFindProdLikedBySimilarUsers(
			Map<String, Double> userInteractionMap, 
			ContentFilter contentFilter,
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
		
		for (String user : userInteractionMap.keySet()) {
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
	
	public static String buildFilterForContentBasedFilteringOnItems(ContentFilter contentFilter) {
		StringBuilder queryBuilder = new StringBuilder();
		
		if (contentFilter != null) {
			buildContentFilters(contentFilter, queryBuilder);
			if (queryBuilder.toString().trim().length() > 0) {
				queryBuilder.insert(0, "(");
				queryBuilder.append(")");
			}
		}
		return queryBuilder.toString();
	}
	
	public static String buildFilterForContentBasedFiltering(ContentFilter contentFilter) {
		StringBuilder queryBuilder = new StringBuilder();
		
		if (contentFilter != null) {
			buildContentFilters(contentFilter, queryBuilder);
			if (queryBuilder.length() > 0 ) {
				queryBuilder.insert(0, "{!join from=id to=id fromIndex=" + ITEMS_CORE + "}(");
				queryBuilder.append(")");
			}
		}
		return queryBuilder.toString();
	}

	private static void buildContentFilters(ContentFilter contentFilter, StringBuilder queryBuilder) {
		int startingBuilderLength = queryBuilder.length();
		
		if (contentFilter.getCheckValidDate() != null && contentFilter.getCheckValidDate()) {
			queryBuilder.append("validFrom:[* TO NOW] AND validTo:[NOW TO *] AND ");
		}

		if (contentFilter.getMinPrice() != null || contentFilter.getMaxPrice() != null) {
			String minPrice = (contentFilter.getMinPrice() == null) ? "*" : contentFilter.getMinPrice().toString();
			String maxPrice = (contentFilter.getMaxPrice() == null) ? "*" : contentFilter.getMaxPrice().toString();
			queryBuilder.append(" price:[" + minPrice + " TO " + maxPrice + "] AND");
		}

		if (contentFilter.getCurrency() != null) {
			queryBuilder.append(" currency:(\"" + contentFilter.getCurrency() + "\") AND");
		}
		
		if (contentFilter.getLanguageOfRecommendedItems() != null) {
			queryBuilder.append(" language:(\"" + contentFilter.getLanguageOfRecommendedItems() + "\") AND");
		}
		
		if (contentFilter.getTags() != null && contentFilter.getTags().size() > 0) {
			queryBuilder.append(" tags:(");
			for (String tag : contentFilter.getTags()) {
				queryBuilder.append("\"" + tag + "\" OR ");
			}
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ") AND");
		}
		
		if (contentFilter.getManufacturer() != null && contentFilter.getManufacturer().size() > 0) {
			queryBuilder.append(" manufacturerIndexed:(");
			for (String manufacturer : contentFilter.getManufacturer()) {
				queryBuilder.append("\"" + manufacturer + "\" OR ");
			}
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ") AND");
		}
		
		if (contentFilter.getCategoryClientIds() != null && contentFilter.getCategoryClientIds().size() > 0) {
			queryBuilder.append(" categoryIdClient:(");
			for (String categoryClientId : contentFilter.getCategoryClientIds()) {
				queryBuilder.append("\"" + categoryClientId + "\" OR ");
			}
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ") AND");
		}
		
		if (contentFilter.getCategoryAppIds() != null && contentFilter.getCategoryAppIds().size() > 0) {
			queryBuilder.append(" categoryIdApp:(");
			for (String categoryAppId : contentFilter.getCategoryAppIds()) {
				queryBuilder.append("\"" + categoryAppId + "\" OR ");
			}
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ") AND");
		}
		
		if (contentFilter.getCustomer() != null) {
			if (contentFilter.getCheckAgeRating() != null && contentFilter.getCheckAgeRating()) {
				int age = calculateAge(contentFilter.getCustomer().getDateOfBirth());
				queryBuilder.append(" ageRating:[* TO \"" + age + "\"] AND");
			}
		}
		
		if (queryBuilder.length() > startingBuilderLength ) {
			queryBuilder.replace(queryBuilder.length() - 4, queryBuilder.length(), "");
		}
	}

	private static int calculateAge(Date dateOfBirth) {
		Calendar dob = Calendar.getInstance();
		dob.setTime(dateOfBirth);
		Calendar today = Calendar.getInstance();
		
		int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
		
		if ( today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR) ) {
			age--;
		}
		
		return age;
	}
	
	/**
	 * Creates a filter query to remove products from the searched user in the final result list 
	 * (should not be recommended additionally)
	 * @param query query object containing the products from the initially searched user
	 * @return query string for filtering
	 */
	public static String buildFilterForAlreadyBoughtProducts(List<String> alreadyBoughtProducts) {
		StringBuilder queryBuilder = new StringBuilder();
		
		
		if (alreadyBoughtProducts != null && alreadyBoughtProducts.size() > 0 ) {
			int productCounter = 0;
			for (String product : alreadyBoughtProducts) {
				if (productCounter >= MAX_SAME_PRODUCT_COUNT) {
					break;
				}
				queryBuilder.append("-id:(\"" + product + "\") OR ");
				productCounter++;
			}
			
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), "");
		}
		
		return queryBuilder.toString();
	}
	
	/**
	 * Creates a filter query to remove products from the searched user in the final result list 
	 * (should not be recommended additionally)
	 * @param query query object containing the products from the initially searched user
	 * @return query string for filtering
	 */
	public static String buildFilterForAlreadyBoughtProducts(String filterFiled, List<String> alreadyBoughtProducts) {
		StringBuilder queryBuilder = new StringBuilder("-" + filterFiled + ":(");
		
		
		int productCounter = 0;
		if (alreadyBoughtProducts != null && alreadyBoughtProducts.size() > 0 ) {
			for (String product : alreadyBoughtProducts) {
				if (productCounter >= MAX_SAME_PRODUCT_COUNT) {
					break;
				}
				queryBuilder.append(product + " OR ");
				productCounter++;
			}
			
		}
		if (productCounter > 0) {
			queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ")");
		} else {
			queryBuilder.append("\"\")");
		}
		
		
		return queryBuilder.toString();
	}
	
	/**
	 * Creates a list of products from a user
	 * @param response object that encapsulates products and users that are mapped to them 
	 * @return list of products
	 */
	public static List<String> createUserProductsList(QueryResponse response) {
		List<CustomerAction> respondItem = response.getBeans(CustomerAction.class);
		
		return RecommendationQueryUtils.extractRecommendationIds(respondItem);
	}
	
	/**
	 * Returns VIP recommendations based on gotten recommendations
	 * @param n result count
	 * @param recommendations already got recommendations for a user
	 * @return VIP recommendations
	 */
	public static List<String> getVIPRecommendations(final int n,
			List<String> recommendations) {
		if (recommendations.size() == 0) {
			return recommendations;
		}
		StringBuilder vipProductBuilder = new StringBuilder();
		for (String recommendedProductId : recommendations) {
			vipProductBuilder.append("id:(\"" + recommendedProductId + "\") OR ");
		}
		vipProductBuilder.replace(vipProductBuilder.length() - 3, vipProductBuilder.length(), "");

		
		ItemQuery vipItemQuery = new ItemQuery();
		vipItemQuery.setFacetFields(new ArrayList<String>());
		vipItemQuery.setQuery(vipProductBuilder.toString());
		vipItemQuery.setSortCriteria("price desc");
		
		ItemService itemService = SolrServiceContainer.getInstance().getItemService();
		ItemResponse itemResponse = itemService.search(vipItemQuery, n);
		List<String> vipRecommendations = new ArrayList<String>();
		for (Item vipItem : itemResponse.getResultItems()) {
			vipRecommendations.add(vipItem.getId());
		}
		
		recommendations = vipRecommendations;
		return recommendations;
	}
	
	/**
	 * Gives a filled list with products that should be recommended
	 * @param searchResponse recommended response
	 * @return list of products
	 */
	public static List<String> extractRecommendationIds(List<? extends Serializable> searchResponse) {
		List<String> recommendations = new ArrayList<String>();
		
		for (Object searchItem : searchResponse) {
			if (searchItem instanceof CustomerAction) {
				recommendations.add( ((CustomerAction) searchItem ).getItemId());
			}
			if (searchItem instanceof Item) {
				recommendations.add( ((Item) searchItem ).getItemId());
			}
			if (searchItem instanceof SocialAction) {
				recommendations.add( ((SocialAction) searchItem ).getUserId());
			}
			if (searchItem instanceof OwnSocialAction) {
				recommendations.add( ((OwnSocialAction) searchItem ).getUserId());
			}
			if (searchItem instanceof Customer) {
				recommendations.add( ((Customer) searchItem ).getId());
			}
			if (searchItem instanceof Resource) {
				recommendations.add( ((Resource) searchItem ).getItemId());
			}
			
		}
		
		return recommendations;
	}
	
	/**
	 * Extracts the products which occurred more than once in the unsorted list and sorts them
	 * based on their occurrence
	 * @param unsortedProducts list of products with out any sorting
	 * @return products sorted by ranking occurrence and that occurred more than 1 time
	 */
	public static List<String> extractRankedProducts(List<String> unsortedProducts) {
		final Map<String, Integer> occurencesMap = createOccurrenceMap(unsortedProducts);
	    
		Comparator<String> comparator = new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Integer occurenceO1 = occurencesMap.get(o1);
				Integer occurenceO2 = occurencesMap.get(o2);
				int comparedOccurence = occurenceO2.compareTo(occurenceO1);
				return comparedOccurence;
			}
		};
		
		List<String> sortedList = new ArrayList<String>(new HashSet<String>(unsortedProducts));
		Collections.sort(sortedList, comparator);
		
		int i = 0;
		while (i < sortedList.size()) {
			if (occurencesMap.get(sortedList.get(i)) == 1 ) {
				sortedList.remove(i);
			} else {
				i++;
			}
		}
		
		return sortedList;
	}
	
	/**
	 * Extracts the products which occurred more than once in the unsorted list and sorts them
	 * based on their occurrence
	 * @param unsortedProducts list of products with out any sorting
	 * @return products sorted by ranking occurrence and that occurred more than 1 time
	 */
	public static List<String> extractCrossRankedProducts(final Map<String, Double> occurencesMap) {
		TreeMap<String, Double> sorted_map = generateSortedMap(occurencesMap);
		return new ArrayList<String>(sorted_map.keySet());
	}

	public static TreeMap<String, Double> generateSortedMap(
			final Map<String, Double> occurencesMap) {
		Comparator<String> comparator = new Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				if (occurencesMap.get(a) >= occurencesMap.get(b)) {
		            return -1;
		        } else {
		            return 1;
		        }
			}
			
		};
		
        TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(comparator);
		sorted_map.putAll(occurencesMap);
		return sorted_map;
	}
	
	
	public static  void fillWeightedMap(Map<String, Double> occurencesMap, List<String> products, Double weight) {
		for (String recommendedItem : products) {
			if (occurencesMap.containsKey(recommendedItem)) {
				occurencesMap.put(recommendedItem, occurencesMap.get(recommendedItem) + weight);
	        } else {
	        	occurencesMap.put(recommendedItem, weight);
	        }
		}
	}

	public static  Map<String, Integer> createOccurrenceMap(List<String> unsortedProducts) {
		Map<String, Integer> occurencesMap = new HashMap<String, Integer>();
		
		for (String recommendedItem : unsortedProducts) {
			if (occurencesMap.containsKey(recommendedItem)) {
				occurencesMap.put(recommendedItem, occurencesMap.get(recommendedItem) + 1);
	        } else {
	        	occurencesMap.put(recommendedItem, 1);
	        }
		}
		return occurencesMap;
	}
	
	/**
	 * Appends the newly fetched recommendation resources (that are not already contained) to the end of the list
	 * If the given maximum count is reached, the appending will stop. 
	 * @param n count of recommendation resources that can be in the output at max
	 * @param existingRecommendations existing recommendations that will get the appended data
	 * @param newlyFetchedRecommendations recommendations to be appended (if not already present)
	 */
	public static void appendDifferentProducts(int n, List<String> existingRecommendations, List<String> newlyFetchedRecommendations) {
		for (String recommendedProduct : newlyFetchedRecommendations) {
			if (existingRecommendations.size() >= n) {
				break;
			}
			
			if (! existingRecommendations.contains(recommendedProduct)) {
				existingRecommendations.add(recommendedProduct);
			}
		}
	}
	
	/**
	 * Appends the newly fetched recommendation resources (that are not already contained) to the end of the list
	 * @param existingRecommendations existing recommendations that will get the appended data
	 * @param newlyFetchedRecommendations recommendations to be appended (if not already present)
	 */
	public static List<String> createUniqueProducts(List<String> list1, List<String> list2) {
		List<String> uniqueList = new ArrayList<String>();
		
		if (list1 != null) {
			for (String recommendedProduct : list1) {
				if (! uniqueList.contains(recommendedProduct)) {
					uniqueList.add(recommendedProduct);
				}
			}
		}

		if (list2 != null) {
			for (String recommendedProduct : list2) {
				if (! uniqueList.contains(recommendedProduct)) {
					uniqueList.add(recommendedProduct);
				}
			}
		}
		
		return uniqueList;
	}
	
	/**
	 * Shifts the newly fetched recommendations at the beginning of the existing recommendation list. If there is already
	 * a recommendations in the existing list, it will also be shifted to the begining
	 * @param existingRecommendations existing recommendations that will get the appended data
	 * @param newlyFetchedRecommendations recommendations to be shifted to the beginning
	 * @param maxShiftCoutn count of recommendation resources that can be in the output at max
	 * @param maxRecCount count of maximum number of recommendations
	 */
	public static List<String>  shiftProducts(List<String> existingRecommendations, List<String> newlyFetchedRecommendations, int maxShiftCoutn, int maxRecCount) {
		int shiftCount = 0;
		for (String recommendedProduct : newlyFetchedRecommendations) {
			if (shiftCount >= maxShiftCoutn) {
				break;
			}
			
			boolean recommendationRemoved = existingRecommendations.remove(recommendedProduct);
			existingRecommendations.add(shiftCount, recommendedProduct);
			
			shiftCount++;
		}
		
		if (maxRecCount > existingRecommendations.size()) {
			maxRecCount = existingRecommendations.size();
		}
		
		return existingRecommendations.subList(0, maxRecCount);
	}
	
	public static Item serializeSolrDocToSearchItem(SolrDocument solrDocument) {
		Item searchItem = new Item();
		searchItem.setId((String) solrDocument.getFieldValue("id"));
		searchItem.setName((String) solrDocument.getFieldValue("name"));
		searchItem.setDescription((String) solrDocument.getFieldValue("description"));
		searchItem.setPrice(((Double) solrDocument.getFieldValue("price")));
		searchItem.setCurrency((String) solrDocument.getFieldValue("currency"));
		searchItem.setValidFrom((Date) solrDocument.getFieldValue("validFrom"));
		searchItem.setValidTo((Date) solrDocument.getFieldValue("validTo"));
		searchItem.setCategoryIdClient((List<String>) solrDocument.getFieldValue("categoryIdClient"));
		searchItem.setCategoryIdApp((List<String>)solrDocument.getFieldValue("categoryIdApp"));
		searchItem.setAgeRating((String) solrDocument.getFieldValue("ageRating"));
		searchItem.setTags((List<String>) solrDocument.getFieldValue("tags"));
		searchItem.setManufacturer((String) solrDocument.getFieldValue("manufacturer"));
		searchItem.setPreceedingItemId((String) solrDocument.getFieldValue("preceedingItemId"));
		searchItem.setCollection( (List<String>) solrDocument.getFieldValue("collection"));
		return searchItem;
	}
	
	public static Resource serializeSolrDocToSearchResource(SolrDocument solrDocument) {
		Resource searchItem = new Resource();
		searchItem.setItemId((String) solrDocument.getFieldValue("id"));
		searchItem.setItemName((String) solrDocument.getFieldValue("name"));
		searchItem.setDescription((String) solrDocument.getFieldValue("description"));
		searchItem.setPrice(((Double) solrDocument.getFieldValue("price")));
		searchItem.setCurrency((String) solrDocument.getFieldValue("currency"));
		searchItem.setValidFrom((Date) solrDocument.getFieldValue("validFrom"));
		searchItem.setTags((List<String>) solrDocument.getFieldValue("tags"));
		searchItem.setManufacturer((String) solrDocument.getFieldValue("manufacturer"));
		return searchItem;
	}
	
	public static SocialStream serializeSolrDocToSocialStream(SolrDocument solrDocument) {
		SocialStream socialStream = new SocialStream();
//		socialStream.setActionId(		(String) 	solrDocument.getFieldValue("id"));
		socialStream.setSourceUserId(	(String) 	solrDocument.getFieldValue("source"));
		socialStream.setScore(	(Float) 	solrDocument.getFieldValue("score"));
//		socialStream.setTargetActionId(	(String) 	solrDocument.getFieldValue("target_user"));
//		socialStream.setTargetUserId(	(String) 	solrDocument.getFieldValue("target_action"));
//		socialStream.setSocialContent(	(String) 	solrDocument.getFieldValue("content"));
//		socialStream.setTimestamp(		(Date) 		solrDocument.getFieldValue("timestamp"));
		return socialStream;
	}
	
	/**
	 * Serializes the data to produce a map where every fetched customer id is mapped to a confidence value
	 * @param solrDocument document to serialize
	 * @return a customer scoring map
	 */
	public static String serializeSolrDocToId(SolrDocument solrDocument) {
		return (String) solrDocument.getFieldValue("id");
	}
	
	public static Float serializeSolrDocForScore(SolrDocument solrDocument) {
		Object score = solrDocument.getFieldValue("score");
		return score == null ? 0f : (Float) score;
	}
	
	
	public static String serializeSolrDocToSearchItemId(SolrDocument solrDocument) {
		return (String) solrDocument.getFieldValue("id");
	}
	
	public static List<String> appendItemsToStoredList(List<String> storedItems, List<String> itemsToAdd) {
		if (storedItems == null) {
			storedItems = new ArrayList<String>();
		}
		if (itemsToAdd == null) {
			itemsToAdd = new ArrayList<String>();
		}
		List<String> stored = new ArrayList<String>(storedItems);
		stored.addAll(itemsToAdd);
		return stored;
	}
	
}
