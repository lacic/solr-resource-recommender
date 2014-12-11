package at.knowcenter.recommender.solrpowered.services.cleaner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;

import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.Position;
import at.knowcenter.recommender.solrpowered.model.PositionNetwork;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.model.Review;
import at.knowcenter.recommender.solrpowered.model.SocialStream;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkMessage;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendService;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemService;
import at.knowcenter.recommender.solrpowered.services.impl.positionnetwork.PositionNetworkService;
import at.knowcenter.recommender.solrpowered.services.impl.positions.PositionService;
import at.knowcenter.recommender.solrpowered.services.impl.resource.ResourceService;
import at.knowcenter.recommender.solrpowered.services.impl.review.ReviewService;
import at.knowcenter.recommender.solrpowered.services.impl.sharedlocation.SharedLocationService;
import at.knowcenter.recommender.solrpowered.services.impl.social.SocialActionService;
import at.knowcenter.recommender.solrpowered.services.impl.social.reversed.OwnSocialActionService;
import at.knowcenter.recommender.solrpowered.services.impl.socialstream.SocialStreamService;
import at.knowcenter.recommender.solrpowered.services.impl.user.UserService;

/**
 * Purges user data for cold start experiment with increasing number of users
 * @author Emanuel Lacic
 *
 */
public class UserPercentagePurger {
	
	public static void main(String... args){
		List<String> socialPurchasingUsers = RecommendationQueryUtils.extractRecommendationIds(fetchAllUsers());
		// shuffle the users
		Collections.shuffle(socialPurchasingUsers);

		System.out.println(socialPurchasingUsers.size());
		
		int usersToRemove  = socialPurchasingUsers.size() / 10;
		System.out.println(usersToRemove);
		
		int port = 8730;
		String address = "kti-social";
		
		List<String> customersToDelete = new ArrayList<String>();

		for (int i = 1; i < 10; i++) {
			port -= 1; // start from port 8729
			
			UserService userService = new UserService(address, port, "profiles");
			SocialActionService socialService = new SocialActionService(address, port, "social_action");
			SocialStreamService socialStreamService = new SocialStreamService(address, port, "social_stream");
			ResourceService resourceService = new ResourceService(address, port, "resources");
			ReviewService reviewService = new ReviewService(address, port, "reviews");
			PositionService positionService = new PositionService(address, port, "positions");
			PositionNetworkService positionNetworkService = new PositionNetworkService(address, port, "position_network");
			SharedLocationService sharedLocationService = new SharedLocationService(address, port, "shared_locations");
			
			
			customersToDelete.addAll(socialPurchasingUsers.subList(0, usersToRemove));
			socialPurchasingUsers.removeAll(customersToDelete);
		
			System.out.println(customersToDelete.size());
			System.out.println(socialPurchasingUsers.size());
			
			if (customersToDelete.size() > 0) {
				socialService.removeElementByIds(customersToDelete);
				System.out.println("Social data removed");
				userService.removeElementByIds(customersToDelete);
				System.out.println("Profiles data removed");
				sharedLocationService.removeElementByIds(customersToDelete);
				System.out.println("Shared location data removed");
				positionNetworkService.removeElementByIds(customersToDelete);
				System.out.println("Position network data removed");
				
				List<SocialStream> allSocialStreamActions = DataFetcher.getAllSocialStreamActions();
				
				cleanSocialStream(socialPurchasingUsers, allSocialStreamActions, socialStreamService);
				System.out.println("Social stream data removed");
				cleanPositionData(customersToDelete, positionService);
				System.out.println("Position data removed");
				cleanReviewData(customersToDelete, reviewService);
				System.out.println("Review data removed");
				
				// Remove users from resource
				ModifiableSolrParams solrParams = new ModifiableSolrParams();
				QueryResponse response = null;

				String queryString = "*:*";
				solrParams.set("q", queryString);
				solrParams.set("rows", 31000);
				solrParams.set("fq", 
						"users_rated_5_count:[1 TO *] OR users_rated_4_count:[1 TO *] OR "
						+ "users_rated_3_count:[1 TO *] OR users_rated_2_count:[1 TO *] OR "
						+ "users_rated_1_count:[1 TO *]");

				try {
					response = resourceService.getSolrServer().query(solrParams);
					List<Resource> resources = response.getBeans(Resource.class);
					System.out.println(resources.size() + " resources fetched");
					cleanResources(socialPurchasingUsers, resources, resourceService);
				} catch (SolrServerException e) {
					e.printStackTrace();
				}
				
			}
		}
		
		
	}

	private static void cleanPositionData(List<String> customersToDelete, PositionService positionService) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;

		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("rows", 222000);
		solrParams.set("fl", "id,user");


		try {
			response = positionService.getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<Position> positions = response.getBeans(Position.class);
		
		List<String> positionsToDelete = new ArrayList<String>();

		for (Position position : positions) {
			if (customersToDelete.contains(position.getUser())) {
				positionsToDelete.add(position.getId());
			}
		}
		
		positionService.removeElementByIds(positionsToDelete);
	}
	
	private static void cleanReviewData(List<String> customersToDelete, ReviewService reviewService) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		QueryResponse response = null;

		String queryString = "*:*";
		solrParams.set("q", queryString);
		solrParams.set("rows", 40000);
		solrParams.set("fl", "id,user");


		try {
			response = reviewService.getSolrServer().query(solrParams);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		List<Review> reviews = response.getBeans(Review.class);
		
		List<String> reviewsToDelete = new ArrayList<String>();

		for (Review review : reviews) {
			if (customersToDelete.contains(review.getUser())) {
				reviewsToDelete.add(review.getId());
			}
		}
		
		reviewService.removeElementByIds(reviewsToDelete);
	}

	private static void cleanSocialStream(List<String> socialPurchasingUsers, List<SocialStream> allSocialStreamActions, SocialStreamService socialStreamService) {
		List<String> socialStreamsToDelete = new ArrayList<String>();
		
		for (SocialStream stream : allSocialStreamActions) {
			if (! socialPurchasingUsers.contains(stream.getSourceUserId()) || ! socialPurchasingUsers.contains(stream.getTargetUserId()) ) {
				socialStreamsToDelete.add(stream.getActionId());
			}
		}
		
		if (socialStreamsToDelete.size() > 0){
			socialStreamService.removeElementByIds(socialStreamsToDelete);
		}
	}

	private static void cleanResources(List<String> socialPurchasingUsers, List<Resource> resources, ResourceService resourceService) {
		for (Resource resource : resources) {
			List<String> rated1Users = resource.getUsersRated1();
			List<String> rated2Users = resource.getUsersRated2();
			List<String> rated3Users = resource.getUsersRated3();
			List<String> rated4Users = resource.getUsersRated4();
			List<String> rated5Users = resource.getUsersRated5();

			if (rated1Users != null) {
				rated1Users.retainAll(socialPurchasingUsers);
			}
			
			if (rated2Users != null) {
				rated2Users.retainAll(socialPurchasingUsers);
			}
			
			if (rated3Users != null) {
				rated3Users.retainAll(socialPurchasingUsers);
			}
			
			if (rated4Users != null) {
				rated4Users.retainAll(socialPurchasingUsers);
			}
			if (rated5Users != null) {
				rated5Users.retainAll(socialPurchasingUsers);
			}
			
			resource.setUsersRated1(rated1Users);
			resource.setUsersRated2(rated2Users);
			resource.setUsersRated3(rated3Users);
			resource.setUsersRated4(rated4Users);
			resource.setUsersRated5(rated5Users);
		}
		
		resourceService.writeDocuments(resources, new SearchServerBulkMessage() {
			@Override
			public void returnStatus(String message) {
				System.out.println("Uploaded resources");
			}
		});
	}
	
	public static List<Customer>  fetchAllUsers(){
		List<Customer> socialUsers = DataFetcher.getAllUserProfiles();
		return socialUsers;
	}
	
}
