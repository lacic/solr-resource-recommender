package at.knowcenter.recommender.solrpowered.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.solr.client.solrj.beans.Field;

/**
 * A review in the recommender framework
 * 
 * @author elacic
 * 
 */

public class Review implements Serializable  {
	


	private static final long serialVersionUID = 1L;

	public static final String REVIEW = "REVIEW";
	public static final String COMMENT = "COMMENT";
	
	// Item fields
	@Field("id")
    private String id;
	@Field("product_id")
    private String itemId;
	@Field("user")
    private String user; 
	@Field("review")
    private String review;
	@Field("rating")
	private Double rating;
	@Field("date")
	private Date date;
	@Field("review_type")
	private String reviewType;

	public Review(){    	
    }
	
	
	public String getId() {
		return id;
	}



	public void setId(String id) {
		this.id = id;
	}


	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getReview() {
		return review;
	}

	public void setReview(String review) {
		this.review = review;
	}

	public Double getRating() {
		return rating;
	}

	public void setRating(Double rating) {
		this.rating = rating;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getReviewType() {
		return reviewType;
	}

	public void setReviewType(String reviewType) {
		this.reviewType = reviewType;
	}
	
}