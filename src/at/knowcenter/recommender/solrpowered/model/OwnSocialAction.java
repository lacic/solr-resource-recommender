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
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.solr.client.solrj.beans.Field;


/**
 * Class which conveys information about the user's social activites
 * @author elacic
 */
@XmlRootElement (name = "ownSocialAction")
@XmlAccessorType(XmlAccessType.FIELD)
public class OwnSocialAction implements Serializable  {
	
	private static final long serialVersionUID = 1L;
	
	@Field("users_that_I_commented_on_count")
	private Integer usersThatICommentedOnCount = 0;
	@Field("users_that_I_commented_on")
	private List<String> usersThatICommentedOn;

	@Field("users_that_I_liked_count")
	private Integer usersThatILikedCount = 0;
	@Field("users_that_I_liked")
	private List<String> usersThatILiked;
	
	@Field("id")
	private String userId;	
	
    
    public OwnSocialAction(){	
    }
	
	
	/**
	 * @return the customerId
	 */
	public List<String> getUsersThatICommentedOn() {
		return usersThatICommentedOn;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setUsersThatICommentedOn(List<String> usersThatICommentedOn) {
		if (usersThatICommentedOn == null){
			this.usersThatICommentedOnCount = 0;
			this.usersThatICommentedOn = null;
			return;
		}
		
		if (usersThatICommentedOn.size() == 1 && usersThatICommentedOn.get(0).equals("")){
			usersThatICommentedOn = new ArrayList<String>();
		}
		
		this.usersThatICommentedOnCount = usersThatICommentedOn.size();
		this.usersThatICommentedOn = usersThatICommentedOn;
	}
	
	/**
	 * @return the customerId
	 */
	public List<String> getUsersThatILiked() {
		return usersThatILiked;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setUsersThatILiked(List<String> usersThatILikedCount) {
		if (usersThatILikedCount == null){
			this.usersThatILikedCount = 0;
			this.usersThatILiked = null;
			return;
		}
		
		if (usersThatILikedCount.size() == 1 && usersThatILikedCount.get(0).equals("")){
			usersThatILikedCount = new ArrayList<String>();
		}
		
		this.usersThatILikedCount = usersThatILikedCount.size();
		this.usersThatILiked = usersThatILikedCount;
	}



	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the itemId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	
	public Integer getUsersThatICommentedOnCount() {
		return usersThatICommentedOnCount;
	}

	public void setUsersThatICommentedOnCount(Integer usersThatICommentedOnCount) {
		this.usersThatICommentedOnCount = usersThatICommentedOnCount;
	}
	
	public Integer getUsersThatILikedCount() {
		return usersThatILikedCount;
	}

	public void setUsersThatILikedCount(Integer usersThatILikedCount) {
		this.usersThatILikedCount = usersThatILikedCount;
	}

	@Override
	public String toString() {
		return "SocialAction [userId=" + userId + "usersThatILiked=" + usersThatILiked + ", usersThatICommentedOn="
				+ usersThatICommentedOn + ", itemId=" + userId ;
	}
	
	public void addUserThatILiked(String userThatILiked) {
		if (userThatILiked != null) {
			if (usersThatILiked == null){
				this.usersThatILiked = new ArrayList<String>();
			}
			
			usersThatILiked.add(userThatILiked);
			usersThatILikedCount = usersThatILiked.size();
		}
	}

	public void addUserThatICommentedOn(String userThatICommentedOn) {
		if (userThatICommentedOn != null) {
			if (usersThatICommentedOn == null){
				this.usersThatICommentedOn = new ArrayList<String>();
			}
			
			usersThatICommentedOn.add(userThatICommentedOn);
			usersThatICommentedOnCount = usersThatICommentedOn.size();
		}
	}
	
    
}
