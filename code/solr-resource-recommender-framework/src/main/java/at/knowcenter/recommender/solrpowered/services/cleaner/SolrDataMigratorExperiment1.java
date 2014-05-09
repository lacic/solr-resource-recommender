package at.knowcenter.recommender.solrpowered.services.cleaner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.OwnSocialAction;
import at.knowcenter.recommender.solrpowered.model.SocialAction;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkMessage;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendService;
import at.knowcenter.recommender.solrpowered.services.impl.social.SocialActionService;
import at.knowcenter.recommender.solrpowered.services.impl.user.UserService;

public class SolrDataMigratorExperiment1 {

	private static final int FULL_USER_SIZE = 10976;
	
	private static final String USER_ACTIONS_CORE = "collection2";
	private static final String USER_CORE = "collection3";
	private static final String SOCIAL_CORE = "bn_social_action";

	private static String remoteHost = "kti-social";

	// social - marketplace user ration 9:1
	private static UserService userService90Perc = new UserService(remoteHost, 8985, USER_CORE);
	private static RecommendService recommenderService90Perc = new RecommendService(remoteHost, 8985, USER_ACTIONS_CORE);
	private static SocialActionService socialService90Perc = new SocialActionService(remoteHost, 8985, SOCIAL_CORE);
	
	// social - marketplace user ration 8:2
	private static UserService userService80Perc = new UserService(remoteHost, 8986, USER_CORE);
	private static RecommendService recommenderService80Perc = new RecommendService(remoteHost, 8986, USER_ACTIONS_CORE);
	private static SocialActionService socialService80Perc = new SocialActionService(remoteHost, 8986, SOCIAL_CORE);

	// social - marketplace user ration 7:3
	private static UserService userService70Perc = new UserService(remoteHost, 8987, USER_CORE);
	private static RecommendService recommenderService70Perc = new RecommendService(remoteHost, 8987, USER_ACTIONS_CORE);
	private static SocialActionService socialService70Perc = new SocialActionService(remoteHost, 8987, SOCIAL_CORE);

	// social - marketplace user ration 6:4
	private static UserService userService60Perc = new UserService(remoteHost, 8988, USER_CORE);
	private static RecommendService recommenderService60Perc = new RecommendService(remoteHost, 8988, USER_ACTIONS_CORE);
	private static SocialActionService socialService60Perc = new SocialActionService(remoteHost, 8988, SOCIAL_CORE);

	// social - marketplace user ration 5:5
	private static UserService userService50Perc = new UserService(remoteHost, 8989, USER_CORE);
	private static RecommendService recommenderService50Perc = new RecommendService(remoteHost, 8989, USER_ACTIONS_CORE);
	private static SocialActionService socialService50Perc = new SocialActionService(remoteHost, 8989, SOCIAL_CORE);

	// social - marketplace user ration 4:6
	private static UserService userService40Perc = new UserService(remoteHost, 8990, USER_CORE);
	private static RecommendService recommenderService40Perc = new RecommendService(remoteHost, 8990, USER_ACTIONS_CORE);
	private static SocialActionService socialService40Perc = new SocialActionService(remoteHost, 8990, SOCIAL_CORE);

	// social - marketplace user ration 3:7
	private static UserService userService30Perc = new UserService(remoteHost, 8991, USER_CORE);
	private static RecommendService recommenderService30Perc = new RecommendService(remoteHost, 8991, USER_ACTIONS_CORE);
	private static SocialActionService socialService30Perc = new SocialActionService(remoteHost, 8991, SOCIAL_CORE);

	// social - marketplace user ration 2:8
	private static UserService userService20Perc = new UserService(remoteHost, 8992, USER_CORE);
	private static RecommendService recommenderService20Perc = new RecommendService(remoteHost, 8992, USER_ACTIONS_CORE);
	private static SocialActionService socialService20Perc = new SocialActionService(remoteHost, 8992, SOCIAL_CORE);

	// social - marketplace user ration 1:9
	private static UserService userService10Perc = new UserService(remoteHost, 8993, USER_CORE);
	private static RecommendService recommenderService10Perc = new RecommendService(remoteHost, 8993, USER_ACTIONS_CORE);
	private static SocialActionService socialService10Perc = new SocialActionService(remoteHost, 8993, SOCIAL_CORE);

	// social - marketplace user ration 0:10
	private static UserService userService0Perc = new UserService(remoteHost, 8994, USER_CORE);
	private static RecommendService recommenderService0Perc = new RecommendService(remoteHost, 8994, USER_ACTIONS_CORE);

	public static void main(String... args){
		List<String> socialUsers = getAllSocialUsers();
		
		List<String> purchasingUsers = getAllPurchasingUsers(SolrServiceContainer.getInstance().getRecommendService().getSolrServer());
		
		List<String> unsocialPurchasingUsers = new ArrayList<>(purchasingUsers);
		unsocialPurchasingUsers.removeAll(socialUsers);

		Collections.shuffle(unsocialPurchasingUsers);
		
//		List<String> marketPlaceUsersToAdd0Perc = unsocialPurchasingUsers.subList(0, FULL_USER_SIZE);


		List<String> purchasingUsers10Perc = getAllPurchasingUsers(recommenderService10Perc.getSolrServer());
		List<String> purchasingUsers20Perc = getAllPurchasingUsers(recommenderService20Perc.getSolrServer());
		List<String> purchasingUsers30Perc = getAllPurchasingUsers(recommenderService30Perc.getSolrServer());
		List<String> purchasingUsers40Perc = getAllPurchasingUsers(recommenderService40Perc.getSolrServer());
		List<String> purchasingUsers50Perc = getAllPurchasingUsers(recommenderService50Perc.getSolrServer());
		List<String> purchasingUsers60Perc = getAllPurchasingUsers(recommenderService60Perc.getSolrServer());
		List<String> purchasingUsers70Perc = getAllPurchasingUsers(recommenderService70Perc.getSolrServer());
		List<String> purchasingUsers80Perc = getAllPurchasingUsers(recommenderService80Perc.getSolrServer());
		List<String> purchasingUsers90Perc = getAllPurchasingUsers(recommenderService90Perc.getSolrServer());
		
		System.out.println(purchasingUsers10Perc.size());
		System.out.println(purchasingUsers20Perc.size());
		System.out.println(purchasingUsers30Perc.size());
		System.out.println(purchasingUsers40Perc.size());
		System.out.println(purchasingUsers50Perc.size());
		System.out.println(purchasingUsers60Perc.size());
		System.out.println(purchasingUsers70Perc.size());
		System.out.println(purchasingUsers80Perc.size());
		System.out.println(purchasingUsers90Perc.size());
		System.out.println("----");

//		List<String> marketPlaceUsersToAdd10Perc = marketPlaceUsersToAdd0Perc.subList(0, FULL_USER_SIZE - purchasingUsers10Perc.size());
//		List<String> marketPlaceUsersToAdd20Perc = marketPlaceUsersToAdd0Perc.subList(0, FULL_USER_SIZE - purchasingUsers20Perc.size());
//		List<String> marketPlaceUsersToAdd30Perc = marketPlaceUsersToAdd0Perc.subList(0, FULL_USER_SIZE - purchasingUsers30Perc.size());
//		List<String> marketPlaceUsersToAdd40Perc = marketPlaceUsersToAdd0Perc.subList(0, FULL_USER_SIZE - purchasingUsers40Perc.size());
//		List<String> marketPlaceUsersToAdd50Perc = marketPlaceUsersToAdd0Perc.subList(0, FULL_USER_SIZE - purchasingUsers50Perc.size());
//		List<String> marketPlaceUsersToAdd60Perc = marketPlaceUsersToAdd0Perc.subList(0, FULL_USER_SIZE - purchasingUsers60Perc.size());
//		List<String> marketPlaceUsersToAdd70Perc = marketPlaceUsersToAdd0Perc.subList(0, FULL_USER_SIZE - purchasingUsers70Perc.size());
//		List<String> marketPlaceUsersToAdd80Perc = marketPlaceUsersToAdd0Perc.subList(0, FULL_USER_SIZE - purchasingUsers80Perc.size());
//		List<String> marketPlaceUsersToAdd90Perc = marketPlaceUsersToAdd0Perc.subList(0, FULL_USER_SIZE - purchasingUsers90Perc.size());
//		
//		purchasingUsers10Perc.addAll(marketPlaceUsersToAdd10Perc);
//		purchasingUsers20Perc.addAll(marketPlaceUsersToAdd20Perc);
//		purchasingUsers30Perc.addAll(marketPlaceUsersToAdd30Perc);
//		purchasingUsers40Perc.addAll(marketPlaceUsersToAdd40Perc);
//		purchasingUsers50Perc.addAll(marketPlaceUsersToAdd50Perc);
//		purchasingUsers60Perc.addAll(marketPlaceUsersToAdd60Perc);
//		purchasingUsers70Perc.addAll(marketPlaceUsersToAdd70Perc);
//		purchasingUsers80Perc.addAll(marketPlaceUsersToAdd80Perc);
//		purchasingUsers90Perc.addAll(marketPlaceUsersToAdd90Perc);
		
//		List<Customer> customersToAdd0Perc = getUserProfiles(marketPlaceUsersToAdd0Perc);
		List<Customer> customersToAdd10Perc = getUserProfiles(purchasingUsers10Perc);
		List<Customer> customersToAdd20Perc = getUserProfiles(purchasingUsers20Perc);
		List<Customer> customersToAdd30Perc = getUserProfiles(purchasingUsers30Perc);
		List<Customer> customersToAdd40Perc = getUserProfiles(purchasingUsers40Perc);
		List<Customer> customersToAdd50Perc = getUserProfiles(purchasingUsers50Perc);
		List<Customer> customersToAdd60Perc = getUserProfiles(purchasingUsers60Perc);
		List<Customer> customersToAdd70Perc = getUserProfiles(purchasingUsers70Perc);
		List<Customer> customersToAdd80Perc = getUserProfiles(purchasingUsers80Perc);
		List<Customer> customersToAdd90Perc = getUserProfiles(purchasingUsers90Perc);
		
		
//		userService0Perc.deleteAllSolrData();
		userService10Perc.deleteAllSolrData();
		userService20Perc.deleteAllSolrData();
		userService30Perc.deleteAllSolrData();
		userService40Perc.deleteAllSolrData();
		userService50Perc.deleteAllSolrData();
		userService60Perc.deleteAllSolrData();
		userService70Perc.deleteAllSolrData();
		userService80Perc.deleteAllSolrData();
		userService90Perc.deleteAllSolrData();
		
//		uploadCustomers(userService0Perc, customersToAdd0Perc);
		uploadCustomers(userService10Perc, customersToAdd10Perc);
		uploadCustomers(userService20Perc, customersToAdd20Perc);
		uploadCustomers(userService30Perc, customersToAdd30Perc);
		uploadCustomers(userService40Perc, customersToAdd40Perc);
		uploadCustomers(userService50Perc, customersToAdd50Perc);
		uploadCustomers(userService60Perc, customersToAdd60Perc);
		uploadCustomers(userService70Perc, customersToAdd70Perc);
		uploadCustomers(userService80Perc, customersToAdd80Perc);
		uploadCustomers(userService90Perc, customersToAdd90Perc);

//		System.out.println(purchasingUsers10Perc.size());
//		System.out.println(purchasingUsers20Perc.size());
//		System.out.println(purchasingUsers30Perc.size());
//		System.out.println(purchasingUsers40Perc.size());
//		System.out.println(purchasingUsers50Perc.size());
//		System.out.println(purchasingUsers60Perc.size());
//		System.out.println(purchasingUsers70Perc.size());
//		System.out.println(purchasingUsers80Perc.size());
//		System.out.println(purchasingUsers90Perc.size());
//
//		
//		recommenderService0Perc.deleteAllSolrData();
//		recommenderService10Perc.deleteAllSolrData();
//		recommenderService20Perc.deleteAllSolrData();
//		recommenderService30Perc.deleteAllSolrData();
//		recommenderService40Perc.deleteAllSolrData();
//		recommenderService50Perc.deleteAllSolrData();
//		recommenderService60Perc.deleteAllSolrData();
//		recommenderService70Perc.deleteAllSolrData();
//		recommenderService80Perc.deleteAllSolrData();
//		recommenderService90Perc.deleteAllSolrData();
//		
//		recommenderService0Perc.write(getCustomerActions(marketPlaceUsersToAdd0Perc));
//		recommenderService10Perc.write(getCustomerActions(purchasingUsers10Perc));
//		recommenderService20Perc.write(getCustomerActions(purchasingUsers20Perc));
//		recommenderService30Perc.write(getCustomerActions(purchasingUsers30Perc));
//		recommenderService40Perc.write(getCustomerActions(purchasingUsers40Perc));
//		recommenderService50Perc.write(getCustomerActions(purchasingUsers50Perc));
//		recommenderService60Perc.write(getCustomerActions(purchasingUsers60Perc));
//		recommenderService70Perc.write(getCustomerActions(purchasingUsers70Perc));
//		recommenderService80Perc.write(getCustomerActions(purchasingUsers80Perc));
//		recommenderService90Perc.write(getCustomerActions(purchasingUsers90Perc));
	}

	private static void uploadCustomers(UserService userService, final List<Customer> customersToUpload) {
		userService.writeDocuments(customersToUpload, new SearchServerBulkMessage() {
			@Override
			public void returnStatus(String message) {
				System.out.println(customersToUpload.size() + " customers uploaded. " + message);
			}
		});
	}

	public static List<String>  fetchUnSocialPurchasingUsers(){
		List<String> socialUsers = getAllSocialUsers();
		List<String> purchasingUsers = getAllPurchasingUsers(SolrServiceContainer.getInstance().getRecommendService().getSolrServer());
		purchasingUsers.removeAll(socialUsers);
		return socialUsers;
	}


	public static List<String> getAllSocialUsers(){
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
		List<String> initialOwnSocialUserIds = RecommendationQueryUtils.extractRecommendationIds(users);

		Set<String> uniqueUsers = new HashSet<String>(initialOwnSocialUserIds);

		for (OwnSocialAction user : users) {
			List<String> usersThatICommentedOn = user.getUsersThatICommentedOn();
			List<String> usersThatILiked = user.getUsersThatILiked();

			if (usersThatICommentedOn != null) {
				uniqueUsers.addAll(usersThatICommentedOn);

			}

			if (usersThatILiked != null) {
				uniqueUsers.addAll(usersThatILiked);

			}
		}

		return new ArrayList<String>(uniqueUsers);
	}

	public static List<String> getAllPurchasingUsers(SolrServer solrServer){

		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;

		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("facet", "true");
		solrParams.set("facet.field", "users_purchased");
		solrParams.set("facet.limit", -1);
		solrParams.set("facet.mincount", 1 );

		try {
			response = solrServer.query(solrParams);
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
	
	private static List<String> getSocialUsers(SolrServer solrServer){
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("rows", 80000);
		solrParams.set("fl", "id");
	
		try {
			response = solrServer.query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<SocialAction> users = response.getBeans(SocialAction.class);
		
		return RecommendationQueryUtils.extractRecommendationIds(users);
	}

	private static List<Customer> getUserProfiles(List<String> userIds){

		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;

		StringBuilder queryBuilder = new StringBuilder();
		for (String id : userIds){
			queryBuilder.append("id:\"" + id + "\" OR ");
		}
		
		queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), "");
		
		solrParams.set("q", queryBuilder.toString());
		solrParams.set("rows", 378000);

		try {
			response = SolrServiceContainer.getInstance().getUserService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<Customer> users = response.getBeans(Customer.class);

		return users;
	}
	
	private static List<String> getRemoteUserProfiles(SolrServer solrServer){

		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;

		solrParams.set("q","*:*");
		solrParams.set("rows", 378000);
		solrParams.set("fl", "id");
		
		try {
			response = solrServer.query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<Customer> users = response.getBeans(Customer.class);

		return RecommendationQueryUtils.extractRecommendationIds(users);
	}
	
	private static List<CustomerAction> getCustomerActions(List<String> users) {
		System.out.println("----");
		System.out.println(users.size());
		System.out.println(new HashSet<String>(users).size());
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		StringBuilder queryBuilder = new StringBuilder("users_purchased:(");
		
		for (String id : users){
			queryBuilder.append("\"" + id + "\" OR ");
		}
		queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ") OR users_viewed:(");
		
		for (String id : users){
			queryBuilder.append("\"" + id + "\" OR ");
		}
		queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ") OR users_marked_favorite:(");
		
		for (String id : users){
			queryBuilder.append("\"" + id + "\" OR ");
		}
		queryBuilder.replace(queryBuilder.length() - 3, queryBuilder.length(), ")");

		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("rows", 160000);

		try {
			response = SolrServiceContainer.getInstance().getRecommendService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<CustomerAction> customerActions = response.getBeans(CustomerAction.class);
		Set<String> uniqueUsers = new HashSet<String>();
		for (CustomerAction ca : customerActions) {
			List<String> customerIdsPurchased = ca.getCustomerIdsPurchased();
			if (customerIdsPurchased != null) {
				customerIdsPurchased.retainAll(users);
				ca.setCustomerIdsPurchased(customerIdsPurchased);
				uniqueUsers.addAll(customerIdsPurchased);
			}

			List<String> customerIdsMarkedFavorite = ca.getCustomerIdsMarkedFavorite();
			if (customerIdsMarkedFavorite != null) {
				customerIdsMarkedFavorite.retainAll(users);
				ca.setCustomerIdsMarkedFavorite(customerIdsMarkedFavorite);

			}			

			List<String> customerIdsViewed = ca.getCustomerIdsViewed();
			if (customerIdsViewed != null) {
				customerIdsViewed.retainAll(users);
				ca.setCustomerIdsViewed(customerIdsViewed);
			}
		}
		System.out.println(uniqueUsers.size());
		return customerActions;
	}
	
	
}
