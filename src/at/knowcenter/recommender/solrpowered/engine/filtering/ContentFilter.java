package at.knowcenter.recommender.solrpowered.engine.filtering;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import at.knowcenter.recommender.solrpowered.model.Customer;

/**
 * Class for defining filter data for recommendations
 * @author elacic
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlAccessorOrder(XmlAccessOrder.UNDEFINED)
public class ContentFilter {
	@XmlElement(name="minPrice")
	private Double minPrice;
	@XmlElement(name="maxPrice")
	private Double maxPrice;
	@XmlElement(name="currency")
	private String currency;
	@XmlElement(name="checkValidDate")
	private Boolean checkValidDate = false;
	@XmlElement(name="checkVIPUser")
	private Boolean checkVIPUser;
	@XmlElement(name="checkAgeRating")
    private Boolean checkAgeRating;
	@XmlElement(name="tags")
    private List<String> tagsOnItems;
	@XmlElement(name="manufacturer")
    private List<String> manufacturer;
	@XmlElement(name="categoryClientIds")
    private List<String> categoryClientIds;
	@XmlElement(name="categoryAppIds")
    private List<String> categoryAppIds;
	
	@XmlElement(name="language")
	private String languageOfRecommendedItems;
    
	@XmlElement(name="checkCollection")
    private Boolean checkCollection;
    
	@XmlElement(name="friendsEvaluationMethod")
    private FriendsEvaluation friendsEvaluationMethod = FriendsEvaluation.NOTHING;
	
	@XmlElement(name="preceedingEvaluationMethod")
    private PrecedingItemEvaluation precedingEvaluationMethod = PrecedingItemEvaluation.NOTHING;
	
	@XmlElement(name="checkCrossSelling")
    private boolean checkCrossSelling;
	
    private Customer customer;
    
    
    
	public boolean getCheckCrossSelling() {
		return checkCrossSelling = false;
	}
	public void setCheckCrossSelling(boolean checkCrossSelling) {
		this.checkCrossSelling = checkCrossSelling;
	}
	
	public Boolean getCheckValidDate() {
		return checkValidDate;
	}
	public void setCheckValidDate(Boolean checkValidDate) {
		this.checkValidDate = checkValidDate;
	}
	public Customer getCustomer() {
		return customer;
	}
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
	public Double getMinPrice() {
		return minPrice;
	}
	public void setMinPrice(Double minPrice) {
		this.minPrice = minPrice;
	}
	public Double getMaxPrice() {
		return maxPrice;
	}
	public void setMaxPrice(Double maxPrice) {
		this.maxPrice = maxPrice;
	}
	public Boolean getCheckVIPUser() {
		return checkVIPUser;
	}
	public void setCheckVIPUser(Boolean checkVIPUser) {
		this.checkVIPUser = checkVIPUser;
	}
	public Boolean getCheckAgeRating() {
		return checkAgeRating;
	}
	public void setCheckAgeRating(Boolean checkAgeRating) {
		this.checkAgeRating = checkAgeRating;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public List<String> getTags() {
		return tagsOnItems;
	}
	public void setTags(List<String> tags) {
		this.tagsOnItems = tags;
	}
	public List<String> getManufacturer() {
		return manufacturer;
	}
	public void setManufacturer(List<String> manufacturer) {
		this.manufacturer = manufacturer;
	}
	public List<String> getCategoryClientIds() {
		return categoryClientIds;
	}
	public void setCategoryClientIds(List<String> categoryClientIds) {
		this.categoryClientIds = categoryClientIds;
	}
	public List<String> getCategoryAppIds() {
		return categoryAppIds;
	}
	public void setCategoryAppIds(List<String> categoryAppIds) {
		this.categoryAppIds = categoryAppIds;
	}
	public PrecedingItemEvaluation getPrecedingEvaluationMethod() {
		return precedingEvaluationMethod;
	}
	public void setPrecedingEvaluationMethod(PrecedingItemEvaluation precedingEvaluationMethod) {
		this.precedingEvaluationMethod = precedingEvaluationMethod;
	}
	public FriendsEvaluation getFriendsEvaluationMethod() {
		return friendsEvaluationMethod;
	}
	public void setFriendsEvaluationMethod(FriendsEvaluation friendsEvaluationMethod) {
		this.friendsEvaluationMethod = friendsEvaluationMethod;
	}
	public Boolean getCheckCollection() {
		return checkCollection;
	}
	public void setCheckCollection(Boolean checkCollection) {
		this.checkCollection = checkCollection;
	}
	public String getLanguageOfRecommendedItems() {
		return languageOfRecommendedItems;
	}
	public void setLanguageOfRecommendedItems(String languageOfRecommendedItems) {
		this.languageOfRecommendedItems = languageOfRecommendedItems;
	}
	

}
