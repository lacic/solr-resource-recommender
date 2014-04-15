package at.knowcenter.recommender.solrpowered.model;

import java.io.Serializable;
import java.util.ArrayList;
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
	@Field("seller")
	private String seller;
	@Field("selling_region")
	private String sellingRegion;
	@Field("location_in_region")
	private String locationInRegion;
	@Field("price")
    private Double price;
	@Field("currency")
    private String currency;
	@Field("validFrom")
    private Date validFrom;
	
	// Customer actions
	
	// Stored fields
	
	@Field("users_rated_5")
	private List<String> usersRated5;
	@Field("users_rated_4")
	private List<String> usersRated4;
	@Field("users_rated_3")
	private List<String> usersRated3;
	@Field("users_rated_2")
	private List<String> usersRated2;
	@Field("users_rated_1")
	private List<String> usersRated1;
    
	@Field("users_rated_5_count")
	private Integer usersRated5Count;
	@Field("users_rated_4_count")
	private Integer usersRated4Count;
	@Field("users_rated_3_count")
	private Integer usersRated3Count;
	@Field("users_rated_2_count")
	private Integer usersRated2Count;
	@Field("users_rated_1_count")
	private Integer usersRated1Count;

    @Field("score")
    private Float score;
    
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

	@Override
	public String toString() {
		return "Resource";
	}


	public String getSeller() {
		return seller;
	}


	public void setSeller(String seller) {
		this.seller = seller;
	}


	public String getSellingRegion() {
		return sellingRegion;
	}


	public void setSellingRegion(String sellingRegion) {
		this.sellingRegion = sellingRegion;
	}


	public String getLocationInRegion() {
		return locationInRegion;
	}


	public void setLocationInRegion(String locationInRegion) {
		this.locationInRegion = locationInRegion;
	}


	public List<String> getUsersRated5() {
		return usersRated5;
	}
	
	public void setUsersRated5(List<String> usersRated5) {
		this.usersRated5 = usersRated5;
		setUsersRated5Count((usersRated5 != null) ? usersRated5.size() : 0);
	}
	
	public void addUserRated5(String user) {
		if (this.usersRated5 == null) {
			this.usersRated5 = new ArrayList<String>();
		}
		this.usersRated5.add(user);
		setUsersRated5Count(usersRated5.size());
	}
	
	public List<String> getUsersRated4() {
		return usersRated4;
	}


	public void setUsersRated4(List<String> usersRated4) {
		this.usersRated4 = usersRated4;
		setUsersRated4Count((usersRated4 != null) ? usersRated4.size() : 0);
	}

	public void addUserRated4(String user) {
		if (this.usersRated4 == null) {
			this.usersRated4 = new ArrayList<String>();
		}
		this.usersRated4.add(user);
		setUsersRated4Count(usersRated4.size());
	}
	
	public List<String> getUsersRated3() {
		return usersRated3;
	}


	public void setUsersRated3(List<String> usersRated3) {
		this.usersRated3 = usersRated3;
		setUsersRated3Count((usersRated3 != null) ? usersRated3.size() : 0);
	}

	public void addUserRated3(String user) {
		if (this.usersRated3 == null) {
			this.usersRated3 = new ArrayList<String>();
		}
		this.usersRated3.add(user);
		setUsersRated3Count(usersRated3.size());
	}

	public List<String> getUsersRated2() {
		return usersRated2;
	}


	public void setUsersRated2(List<String> usersRated2) {
		this.usersRated2 = usersRated2;
		setUsersRated2Count((usersRated2 != null) ? usersRated2.size() : 0);
	}

	public void addUserRated2(String user) {
		if (this.usersRated2 == null) {
			this.usersRated2 = new ArrayList<String>();
		}
		this.usersRated2.add(user);
		setUsersRated2Count(usersRated2.size());
	}
	
	public List<String> getUsersRated1() {
		return usersRated1;
	}


	public void setUsersRated1(List<String> usersRated1) {
		this.usersRated1 = usersRated1;
		setUsersRated1Count((usersRated1 != null) ? usersRated1.size() : 0);
	}

	public void addUserRated1(String user) {
		if (this.usersRated1 == null) {
			this.usersRated1 = new ArrayList<String>();
		}
		this.usersRated1.add(user);
		setUsersRated1Count(usersRated1.size());
	}
	
	public Integer getUsersRated5Count() {
		if (usersRated5Count == null) {
			return 0;
		}
		return usersRated5Count;
	}


	public void setUsersRated5Count(Integer usersRated5Count) {
		this.usersRated5Count = usersRated5Count;
	}


	public Integer getUsersRated4Count() {
		if (usersRated4Count == null) {
			return 0;
		}
		return usersRated4Count;
	}


	public void setUsersRated4Count(Integer usersRated4Count) {
		this.usersRated4Count = usersRated4Count;
	}


	public Integer getUsersRated3Count() {
		if (usersRated3Count == null) {
			return 0;
		}
		return usersRated3Count;
	}


	public void setUsersRated3Count(Integer usersRated3Count) {
		this.usersRated3Count = usersRated3Count;
	}


	public Integer getUsersRated2Count() {
		if (usersRated2Count == null) {
			return 0;
		}
		return usersRated2Count;
	}


	public void setUsersRated2Count(Integer usersRated2Count) {
		this.usersRated2Count = usersRated2Count;
	}


	public Integer getUsersRated1Count() {
		if (usersRated1Count == null) {
			return 0;
		}
		return usersRated1Count;
	}


	public void setUsersRated1Count(Integer usersRated1Count) {
		this.usersRated1Count = usersRated1Count;
	}


	public Float getScore() {
		return score;
	}


	public void setScore(Float score) {
		this.score = score;
	}

}