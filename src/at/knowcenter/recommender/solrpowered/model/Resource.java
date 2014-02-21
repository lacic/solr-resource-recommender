package at.knowcenter.recommender.solrpowered.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.solr.client.solrj.beans.Field;

/**
 * Resource in the recommender framework, is merged from item and customer action core
 * 
 * @author elacic
 * 
 */

public class Resource implements Serializable  {
	


	private static final long serialVersionUID = 1L;
	
	// Item fields
	
	// stored fields
	@Field("id")
    private String itemId;
	@Field("name")
    private String itemName; 
	@Field("description")
    private String description;
	@Field("tags")
	private List<String> tags;
	@Field("manufacturer")
	private String manufacturer;
	@Field("preceedingItemId")
	private String preceedingItemId;
	@Field("collection")
	private List<String> collection;
	
	// indexed fields
	@Field("language")
	private String language;
	@Field("price")
    private Double price;
	@Field("currency")
    private String currency;
	@Field("validFrom")
    private Date validFrom;
	@Field("validTo")
    private Date validTo;
	@Field("ageRating")
	private String ageRating;
	@Field("clientId")
	private String clientId;
	@Field("categoryIdClient")
    private List<String> categoryIdClient;
	@Field("categoryIdApp")
    private List<String> categoryIdApp;
	
	// Customer actions
	
	// Stored fields
	
	@Field("users_purchased")
	private List<String> customerIdsPurchased;
	@Field("users_viewed")
	private List<String> customerIdsViewed;
	@Field("users_marked_favorite")
	private List<String> customerIdsMarkedFavorite;
    
	// Indexed fields
	
	@Field("user_count_purchased")
	private Integer userCountPurchased = 0;
	@Field("user_count_viewed")
	private Integer userCountViewed = 0;
	@Field("user_count_marked_favorite")
	private Integer userCountMarkedFavorite = 0;

	public Resource(){    	
    }
    

	public String getItemId() {
		return itemId;
	}



	public void setItemId(String itemId) {
		this.itemId = itemId;
	}



	public String getItemName() {
		return itemName;
	}



	public void setItemName(String itemName) {
		this.itemName = itemName;
	}



	public String getDescription() {
		return description;
	}



	public void setDescription(String description) {
		this.description = description;
	}



	public List<String> getTags() {
		return tags;
	}



	public void setTags(List<String> tags) {
		this.tags = tags;
	}



	public String getManufacturer() {
		return manufacturer;
	}



	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}



	public String getPreceedingItemId() {
		return preceedingItemId;
	}



	public void setPreceedingItemId(String preceedingItemId) {
		this.preceedingItemId = preceedingItemId;
	}



	public List<String> getCollection() {
		return collection;
	}



	public void setCollection(List<String> collection) {
		this.collection = collection;
	}



	public String getLanguage() {
		return language;
	}



	public void setLanguage(String language) {
		this.language = language;
	}



	public Double getPrice() {
		return price;
	}



	public void setPrice(Double price) {
		this.price = price;
	}



	public String getCurrency() {
		return currency;
	}



	public void setCurrency(String currency) {
		this.currency = currency;
	}



	public Date getValidFrom() {
		return validFrom;
	}



	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}



	public Date getValidTo() {
		return validTo;
	}



	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}



	public String getAgeRating() {
		return ageRating;
	}



	public void setAgeRating(String ageRating) {
		this.ageRating = ageRating;
	}



	public String getClientId() {
		return clientId;
	}



	public void setClientId(String clientId) {
		this.clientId = clientId;
	}



	public List<String> getCategoryIdClient() {
		return categoryIdClient;
	}



	public void setCategoryIdClient(List<String> categoryIdClient) {
		this.categoryIdClient = categoryIdClient;
	}



	public List<String> getCategoryIdApp() {
		return categoryIdApp;
	}



	public void setCategoryIdApp(List<String> categoryIdApp) {
		this.categoryIdApp = categoryIdApp;
	}



	public List<String> getCustomerIdsPurchased() {
		return customerIdsPurchased;
	}



	public void setCustomerIdsPurchased(List<String> customerIdsPurchased) {
		this.customerIdsPurchased = customerIdsPurchased;
	}



	public List<String> getCustomerIdsViewed() {
		return customerIdsViewed;
	}



	public void setCustomerIdsViewed(List<String> customerIdsViewed) {
		this.customerIdsViewed = customerIdsViewed;
	}



	public List<String> getCustomerIdsMarkedFavorite() {
		return customerIdsMarkedFavorite;
	}



	public void setCustomerIdsMarkedFavorite(List<String> customerIdsMarkedFavorite) {
		this.customerIdsMarkedFavorite = customerIdsMarkedFavorite;
	}



	public Integer getUserCountPurchased() {
		return userCountPurchased;
	}



	public void setUserCountPurchased(Integer userCountPurchased) {
		this.userCountPurchased = userCountPurchased;
	}



	public Integer getUserCountViewed() {
		return userCountViewed;
	}



	public void setUserCountViewed(Integer userCountViewed) {
		this.userCountViewed = userCountViewed;
	}



	public Integer getUserCountMarkedFavorite() {
		return userCountMarkedFavorite;
	}



	public void setUserCountMarkedFavorite(Integer userCountMarkedFavorite) {
		this.userCountMarkedFavorite = userCountMarkedFavorite;
	}



	@Override
	public String toString() {
		return "Resource";
	}





}
