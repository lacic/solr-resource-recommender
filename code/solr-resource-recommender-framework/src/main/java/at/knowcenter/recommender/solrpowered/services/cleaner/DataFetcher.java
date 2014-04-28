package at.knowcenter.recommender.solrpowered.services.cleaner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.OwnSocialAction;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.model.Review;
import at.knowcenter.recommender.solrpowered.model.SocialAction;
import at.knowcenter.recommender.solrpowered.model.SocialStream;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class DataFetcher {

	public static List<SocialStream> getAllSocialStreamActions() {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("rows", 762000);
		solrParams.set("fl", "id,source,target_user,target_action");

	
		try {
			response = SolrServiceContainer.getInstance().getSocialStreamService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<SocialStream> socialStreams = response.getBeans(SocialStream.class);
		return socialStreams;
	}
	

	public static List<CustomerAction> getAllCustomerActions() {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("rows", 160000);
	
		try {
			response = SolrServiceContainer.getInstance().getRecommendService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<CustomerAction> users = response.getBeans(CustomerAction.class);
		return users;
	}
	
	public static List<String> getReviewingUsers() {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("facet", "true");
		solrParams.set("facet.field", "user");
		solrParams.set("facet.limit", -1);
		solrParams.set("facet.mincount", 1);
	
		try {
			response = SolrServiceContainer.getInstance().getReviewService().getSolrServer().query(solrParams);
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
	
	public static List<String> getRatedProductsFromUser(String username) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		solrParams.set("q", "user:" + username);
		solrParams.set("facet", "true");
		solrParams.set("facet.field", "product_id");
		solrParams.set("facet.limit", -1);
		solrParams.set("facet.mincount", 1);
	
		try {
			response = SolrServiceContainer.getInstance().getReviewService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<FacetField> facetFields = response.getFacetFields();
		// get the first and only facet field -> products
		FacetField userFacet = facetFields.get(0);
		List<String> products = new ArrayList<String>();
		
		for(Count c : userFacet.getValues()) {
			products.add(c.getName());
		}
		
		return products;
	}
	
	public static List<String> getAllSocialUsers(){
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("rows", 80000);
	
		try {
			response = SolrServiceContainer.getInstance().getSocialActionService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<SocialAction> users = response.getBeans(SocialAction.class);
		List<String> initialOwnSocialUserIds = RecommendationQueryUtils.extractRecommendationIds(users);
		Set<String> uniqueUsers = new HashSet<String>(initialOwnSocialUserIds);
		
		for (SocialAction user : users) {
			List<String> usersThatICommentedOn = user.getUsersThatCommentedOnMyPost();
			List<String> usersThatILiked = user.getUsersThatLikedMe();
			List<String> usersThatPostedASnapshopToMe = user.getUsersThatPostedASnapshopToMe();
			List<String> usersThatPostedOnMyWall = user.getUsersThatPostedOnMyWall();
			
			if (usersThatICommentedOn != null) {
				uniqueUsers.addAll(usersThatICommentedOn);

			}

			if (usersThatILiked != null) {
				uniqueUsers.addAll(usersThatILiked);

			}
			
			if (usersThatPostedASnapshopToMe != null) {
				uniqueUsers.addAll(usersThatPostedASnapshopToMe);

			}
			
			if (usersThatPostedOnMyWall != null) {
				uniqueUsers.addAll(usersThatPostedOnMyWall);

			}
		}
		return new ArrayList<String>(uniqueUsers);
	}
	
	public static List<SocialAction> getAllSocialActions(){
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("rows", 80000);
	
		try {
			response = SolrServiceContainer.getInstance().getSocialActionService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return response.getBeans(SocialAction.class);
	}
	
	public static List<String> getAllPurchasingUsers(){
		
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("facet", "true");
		solrParams.set("facet.field", "users_purchased");
		solrParams.set("facet.limit", -1);
		solrParams.set("facet.mincount", 1 );
	
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
	
	public static List<Customer> getAllUserProfiles(){
		
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("rows", 378000);
	
		try {
			response = SolrServiceContainer.getInstance().getUserService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<Customer> users = response.getBeans(Customer.class);
		
		return users;
	}
	
	public static Customer getUserProfile(String user){
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;
		
		solrParams.set("q", "id:" + user);
		solrParams.set("rows", 1);
	
		try {
			response = SolrServiceContainer.getInstance().getUserService().getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<Customer> users = response.getBeans(Customer.class);
		
		if (users.size() == 0) {
			return null;
		}
		
		return users.get(0);
	}
	
	public static List<String> getSocialNeighbourUsers(String targetUser) {
		Set<String> neighbours = new HashSet<String>();
		
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		String queryString = "id:(\"" + targetUser + "\"^2) OR users_that_liked_me:(\"" + targetUser + 
								"\") OR users_that_commented_on_my_post:(\"" + targetUser + 
								"\") OR users_that_posted_on_my_wall:(\"" + targetUser + 
								"\") OR users_that_posted_a_snapshot_to_me:(\"" + targetUser + "\")";
		
		solrParams.set("q", queryString);
		solrParams.set("rows", Integer.MAX_VALUE);
		
		try {
			QueryResponse response = SolrServiceContainer.getInstance().getSocialActionService().getSolrServer().query(solrParams);
			List<SocialAction> socialUsers = response.getBeans(SocialAction.class);
			
			SocialAction currentUserInteractions = socialUsers.get(0);
			if (currentUserInteractions.getUserId().equals(targetUser)){
				socialUsers.remove(0);
			} else {
				currentUserInteractions = null;
			}
			
			if (currentUserInteractions != null) {
				List<String> usersThatLikedMe = currentUserInteractions.getUsersThatLikedMe();
				List<String> usersThatCommentedOnMyPost = currentUserInteractions.getUsersThatCommentedOnMyPost();
				List<String> usersThatPostedASnapshopToMe = currentUserInteractions.getUsersThatPostedASnapshopToMe();
				List<String> usersThatPostedOnMyWall = currentUserInteractions.getUsersThatPostedOnMyWall();
				
				if (usersThatLikedMe != null) {
					neighbours.addAll(usersThatLikedMe);
				}
				if (usersThatCommentedOnMyPost != null) {
					neighbours.addAll(usersThatCommentedOnMyPost);
				}
				if (usersThatPostedASnapshopToMe != null) {
					neighbours.addAll(usersThatPostedASnapshopToMe);
				}
				if (usersThatPostedOnMyWall != null) {
					neighbours.addAll(usersThatPostedOnMyWall);
				}
			}
			
			for (SocialAction socialUser : socialUsers) {
				neighbours.add(socialUser.getUserId());
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		
		
		return new ArrayList<String>(neighbours);
	}
	
	public static List<String> getLocationNeighbourUsers(String targetUser) {
		Set<String> neighbours = new HashSet<String>();
		
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		String queryString = "id:(\"" + targetUser + "\"^2) OR users_that_liked_me:(\"" + targetUser + 
								"\") OR users_that_commented_on_my_post:(\"" + targetUser + 
								"\") OR users_that_posted_on_my_wall:(\"" + targetUser + 
								"\") OR users_that_posted_a_snapshot_to_me:(\"" + targetUser + "\")";
		
		solrParams.set("q", queryString);
		solrParams.set("rows", Integer.MAX_VALUE);
		
		try {
			QueryResponse response = SolrServiceContainer.getInstance().getSocialActionService().getSolrServer().query(solrParams);
			List<SocialAction> socialUsers = response.getBeans(SocialAction.class);
			
			SocialAction currentUserInteractions = socialUsers.get(0);
			if (currentUserInteractions.getUserId().equals(targetUser)){
				socialUsers.remove(0);
			} else {
				currentUserInteractions = null;
			}
			
			if (currentUserInteractions != null) {
				List<String> usersThatLikedMe = currentUserInteractions.getUsersThatLikedMe();
				List<String> usersThatCommentedOnMyPost = currentUserInteractions.getUsersThatCommentedOnMyPost();
				List<String> usersThatPostedASnapshopToMe = currentUserInteractions.getUsersThatPostedASnapshopToMe();
				List<String> usersThatPostedOnMyWall = currentUserInteractions.getUsersThatPostedOnMyWall();
				
				if (usersThatLikedMe != null) {
					neighbours.addAll(usersThatLikedMe);
				}
				if (usersThatCommentedOnMyPost != null) {
					neighbours.addAll(usersThatCommentedOnMyPost);
				}
				if (usersThatPostedASnapshopToMe != null) {
					neighbours.addAll(usersThatPostedASnapshopToMe);
				}
				if (usersThatPostedOnMyWall != null) {
					neighbours.addAll(usersThatPostedOnMyWall);
				}
			}
			
			for (SocialAction socialUser : socialUsers) {
				neighbours.add(socialUser.getUserId());
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		
		
		return new ArrayList<String>(neighbours);
	}

}
