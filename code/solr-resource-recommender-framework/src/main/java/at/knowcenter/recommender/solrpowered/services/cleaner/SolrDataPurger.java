package at.knowcenter.recommender.solrpowered.services.cleaner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.knowcenter.recommender.solrpowered.model.CustomerAction;
import at.knowcenter.recommender.solrpowered.model.SocialStream;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class SolrDataPurger {
	
	public static void main(String... args){
		List<String> socialPurchasingUsers = fetchSocialPurchasingUsers();

//		List<Customer> customers = getAllUserProfiles();
		
		
		List<String> customersToDelete = new ArrayList<String>();
		System.out.println(socialPurchasingUsers.size());
		Collections.shuffle(socialPurchasingUsers);
		
		customersToDelete.addAll(socialPurchasingUsers.subList(0, (int)(socialPurchasingUsers.size() * 0.9)));
		socialPurchasingUsers.removeAll(customersToDelete);
		
//		for (Customer c : customers) {
//			if (! socialPurchasingUsers.contains(c.getId())) {
//				customersToDelete.add(c.getId());
//			}
//		}
		
		if (customersToDelete.size() > 0) {
			SolrServiceContainer.getInstance().getOwnSocialActionService().removeElementByIds(customersToDelete);
			SolrServiceContainer.getInstance().getSocialActionService().removeElementByIds(customersToDelete);
			SolrServiceContainer.getInstance().getUserService().removeElementByIds(customersToDelete);
		}

		List<CustomerAction> customerActions= DataFetcher.getAllCustomerActions();
		cleanCustomerActions(socialPurchasingUsers, customerActions);
		
		List<SocialStream> allSocialStreamActions = DataFetcher.getAllSocialStreamActions();
		cleanSocialStream(socialPurchasingUsers, allSocialStreamActions);
	}

	private static void cleanSocialStream(List<String> socialPurchasingUsers,
			List<SocialStream> allSocialStreamActions) {
		List<String> socialStreamsToDelete = new ArrayList<String>();
		
		for (SocialStream stream : allSocialStreamActions) {
			if (! socialPurchasingUsers.contains(stream.getSourceUserId()) ||
					! socialPurchasingUsers.contains(stream.getTargetUserId()) ) {
				socialStreamsToDelete.add(stream.getActionId());
			}
		}
		
		if (socialStreamsToDelete.size() > 0){
			SolrServiceContainer.getInstance().getSocialStreamService().removeElementByIds(socialStreamsToDelete);
		}
	}

	private static void cleanCustomerActions(
			List<String> socialPurchasingUsers,
			List<CustomerAction> customerActions) {
		for (CustomerAction ca : customerActions) {
			List<String> customerIdsPurchased = ca.getCustomerIdsPurchased();
			List<String> customerIdsMarkedFavorite = ca.getCustomerIdsMarkedFavorite();
			List<String> customerIdsViewed = ca.getCustomerIdsViewed();

			if (customerIdsPurchased != null) {
				customerIdsPurchased.retainAll(socialPurchasingUsers);
			}
			
			if (customerIdsMarkedFavorite != null) {
				customerIdsMarkedFavorite.retainAll(socialPurchasingUsers);
			}
			
			if (customerIdsViewed != null) {
				customerIdsViewed.retainAll(socialPurchasingUsers);
			}
			
			ca.setCustomerIdsMarkedFavorite(customerIdsMarkedFavorite);
			ca.setCustomerIdsPurchased(customerIdsPurchased);
			ca.setCustomerIdsViewed(customerIdsViewed);
		}
		
		SolrServiceContainer.getInstance().getRecommendService().write(customerActions);
	}
	
	public static List<String>  fetchSocialPurchasingUsers(){
		List<String> socialUsers = DataFetcher.getAllSocialUsers();
		List<String> purchasingUsers = DataFetcher.getAllPurchasingUsers();
		socialUsers.retainAll(purchasingUsers);
		return socialUsers;
	}
	
}
