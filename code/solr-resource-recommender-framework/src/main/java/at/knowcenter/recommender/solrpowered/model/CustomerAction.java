/*
 * Copyright (C) 2010
 * "Kompetenzzentrum fuer wissensbasierte Anwendungen Forschungs- und EntwicklungsgmbH"
 * (Know-Center), Graz, Austria, office@know-center.at.
 *
 * Licensees holding valid Know-Center Commercial licenses may use this file in
 * accordance with the Know-Center Commercial License Agreement provided with
 * the Software or, alternatively, in accordance with the terms contained in
 * a written agreement between Licensees and Know-Center.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.knowcenter.recommender.solrpowered.model;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.util.DateUtil;


/**
 * Class which conveys information about the user's actions
 * @author Werner Klieber <mailto:wklieber@know-center.at>
 * @author Mark Kroell <mailto:mkroell@know-center.at>
 * @date 18.07.13
 */
public class CustomerAction implements Serializable  {
	
	private static final long serialVersionUID = 1L;

	private Integer userCount = 0;
	private List<String> customerIds;
	
	@Field("user_count_purchased")
	private Integer userCountPurchased = 0;
	@Field("users_purchased")
	private List<String> customerIdsPurchased;
	@Field("user_count_viewed")
	private Integer userCountViewed = 0;
	@Field("users_viewed")
	private List<String> customerIdsViewed;
	@Field("user_count_marked_favorite")
	private Integer userCountMarkedFavorite = 0;
	@Field("users_marked_favorite")
	private List<String> customerIdsMarkedFavorite;
	@Field("id")
	private String itemId;	
    private Date timeStamp;    
    public enum ACTION {VIEWED, MARKED_FAVORITE, PURCHASED, UNMARKED_FAVORITE, RANKED, RETURNED, RECOMMENDED_TO_FRIEND, EMPTY}
    private ACTION action;
    public enum ACTION_SPECIFIC_ATTRIBUTE {FIVE_STAR, NOT_LIKED, EMPTY}
    private ACTION_SPECIFIC_ATTRIBUTE actionSpecific;
    private String orderId;
    private String delay;
    private String recommendationId;
    

	public CustomerAction(){	
    }
	
	public CustomerAction(CustomerAction customerAction){
		setItemId(customerAction.getItemId());
//		setCustomerIds(Arrays.asList(customerAction.getCustomerId()));
		setTimestamp(customerAction.getTimestamp());
		setAction(customerAction.getActionAsString());
		setActionSpecific(customerAction.getActionSpecificAsString());
		setOrderId(customerAction.getOrderId());
		setRecommendationId(customerAction.getOrderId());
		setDelay(customerAction.getOrderId());		
	}
    
	// ------------- GETTERS SETTERS -------------------------
	
    public String getDelay() {
		return delay;
	}

	public void setDelay(String delay) {
		this.delay = delay;
	}	
	

	public String getRecommendationId() {
		return recommendationId;
	}

	public void setRecommendationId(String recommendationId) {
		this.recommendationId = recommendationId;
	}

	/**
	 * @return the timeStamp
	 */
	public Date getTimestamp() {
		return timeStamp;
	}

	/**
	 * @param timeStamp the timeStamp to set
	 */
	public void setTimestamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public void setTimeStamp(String timeStamp) throws ParseException {
		this.timeStamp = DateUtil.parseDate(timeStamp);
//		this.timeStamp = DateUtils.parse(timeStamp);
	}

	/**
	 * @return the customerId
	 */
	public List<String> getCustomerIds() {
		return customerIds;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerIds(List<String> customerIds) {
		if (customerIds == null){
			this.userCount = 0;
			this.customerIds = null;
			return;
		}
		this.userCount = customerIds.size();
		this.customerIds = customerIds;
	}
	
	/**
	 * @return the customerId
	 */
	public List<String> getCustomerIdsPurchased() {
		return customerIdsPurchased;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerIdsPurchased(List<String> customerIds) {
		if (customerIds == null){
			this.userCountPurchased = 0;
			this.customerIdsPurchased = null;
			return;
		}
		this.userCountPurchased = customerIds.size();
		this.customerIdsPurchased = customerIds;
	}
	
	/**
	 * @return the customerId
	 */
	public List<String> getCustomerIdsViewed() {
		return customerIdsViewed;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerIdsViewed(List<String> customerIdsViewed) {
		if (customerIdsViewed == null){
			this.userCountViewed = 0;
			this.customerIdsViewed = null;
			return;
		}
		this.userCountViewed = customerIdsViewed.size();
		this.customerIdsViewed = customerIdsViewed;
	}
	
	/**
	 * @return the customerId
	 */
	public List<String> getCustomerIdsMarkedFavorite() {
		return customerIdsMarkedFavorite;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerIdsMarkedFavorite(List<String> customerIdsMarkedFavorite) {
		if (customerIdsMarkedFavorite == null){
			this.userCountMarkedFavorite = 0;
			this.customerIdsMarkedFavorite = null;
			return;
		}
		this.userCountMarkedFavorite = customerIdsMarkedFavorite.size();
		this.customerIdsMarkedFavorite = customerIdsMarkedFavorite;
	}

	/**
	 * @return the type
	 */
	public ACTION getType() {
		return action;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(ACTION type) {
		this.action = type;
	}


	/**
	 * @return the itemId
	 */
	public String getItemId() {
		return itemId;
	}

	/**
	 * @param itemId the itemId to set
	 */
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}


    public ACTION getAction() {
		return action;
	}
    
    public String getActionAsString() {
		if( action == ACTION.VIEWED ){
			return "VIEWED";			
		} else if( action == ACTION.MARKED_FAVORITE ){
			return "MARKED_FAVORITE";			
		} else if( action == ACTION.PURCHASED ){
			return "PURCHASED";
		} else if( action == ACTION.UNMARKED_FAVORITE ){
			return "UNMARKED_FAVORITE";			
		} else if( action == ACTION.RANKED ){
			return "RANKED";			
		} else if( action == ACTION.RETURNED ){
			return "RETURNED";			
		} else if( action == ACTION.RECOMMENDED_TO_FRIEND ){
			return "RECOMMENDED_TO_FRIEND";			
		} else if( action == ACTION.EMPTY ){
			return "EMPTY";			
		} else
			return null;
    }

    public void setAction(String action){
    	if (action == null) {
    		this.action = ACTION.EMPTY;
    	} else {
			if( action.equals("VIEWED") ){
				this.action = ACTION.VIEWED;
				
			} else if( action.equals("MARKED_FAVORITE") ){
				this.action = ACTION.MARKED_FAVORITE;
				
			} else if( action.equals("PURCHASED") ){
				this.action = ACTION.PURCHASED;
				
			} else if( action.equals("UNMARKED_FAVORITE") ){
				this.action = ACTION.UNMARKED_FAVORITE;
				
			} else if( action.equals("RANKED") ){
				this.action = ACTION.RANKED;
				
			} else if( action.equals("RETURNED") ){
				this.action = ACTION.RETURNED;
				
			} else if( action.equals("RECOMMENDED_TO_FRIEND") ){
				this.action = ACTION.RECOMMENDED_TO_FRIEND;
				
			} else if( action.equals("EMPTY") ){
				this.action = ACTION.EMPTY;
			} else if( action.equals("") ){
				this.action = ACTION.EMPTY;
			} else {
				this.action = ACTION.EMPTY;
			}
    	}
    } 
    
	public void setAction(ACTION action) {
		this.action = action;
	}
    
    public void setActionSpecific(String actionSpecific) {
    	if( actionSpecific.equals("FIVE_STAR") ){
			this.actionSpecific = ACTION_SPECIFIC_ATTRIBUTE.FIVE_STAR;
			
		} else if( actionSpecific.equals("NOT_LIKED") ){
			this.actionSpecific = ACTION_SPECIFIC_ATTRIBUTE.NOT_LIKED;
			
		} else if( actionSpecific.equals("EMPTY") ){
			this.actionSpecific = ACTION_SPECIFIC_ATTRIBUTE.EMPTY;
		} else if( actionSpecific.equals("")){
			this.actionSpecific = ACTION_SPECIFIC_ATTRIBUTE.EMPTY;
		} else {
			this.actionSpecific = ACTION_SPECIFIC_ATTRIBUTE.EMPTY;
		}    	
    } // end method setActionSpecific

	public ACTION_SPECIFIC_ATTRIBUTE getActionSpecific() {
		return actionSpecific;
	}
	
	public String getActionSpecificAsString(){
		if( actionSpecific == ACTION_SPECIFIC_ATTRIBUTE.NOT_LIKED ){
			return "NOT_LIKED";			
		} else if( actionSpecific == ACTION_SPECIFIC_ATTRIBUTE.FIVE_STAR ){
			return "FIVE_STAR";			
		} else if( actionSpecific == ACTION_SPECIFIC_ATTRIBUTE.EMPTY ){
			return "EMPTY";
		} else
			return null;		
	}

	public void setActionSpecific(ACTION_SPECIFIC_ATTRIBUTE actionSpecific) {
		this.actionSpecific = actionSpecific;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	
	public Integer getUserCount() {
		return userCount;
	}

	public void setUserCount(Integer userCount) {
		this.userCount = userCount;
	}
	
	public Integer getUserCountPurchased() {
		return userCountPurchased;
	}

	public void setUserCountPurchased(Integer userCount) {
		this.userCountPurchased = userCount;
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
		return "CustomerActionInternal [userCount=" + userCount
				+ ", customerIds=" + customerIds + ", userCountPurchased="
				+ userCountPurchased + ", customerIdsPurchased="
				+ customerIdsPurchased + ", userCountViewed=" + userCountViewed
				+ ", customerIdsViewed=" + customerIdsViewed
				+ ", userCountMarkedFavorite=" + userCountMarkedFavorite
				+ ", customerIdsMarkedFavorite=" + customerIdsMarkedFavorite
				+ ", itemId=" + itemId + ", timeStamp=" + timeStamp
				+ ", action=" + action + ", actionSpecific=" + actionSpecific
				+ ", orderId=" + orderId + ", delay=" + delay
				+ ", recommendationId=" + recommendationId + "]";
	}


    
}
