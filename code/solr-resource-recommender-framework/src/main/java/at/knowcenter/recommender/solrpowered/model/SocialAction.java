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

import org.apache.solr.client.solrj.beans.Field;


/**
 * Class which conveys information about the user's social activites
 * @author elacic
 */
public class SocialAction implements Serializable  {
	
	private static final long serialVersionUID = 1L;
	
	@Field("users_that_commented_on_my_post_count")
	private Integer usersThatCommentedOnMyPostCount = 0;
	@Field("users_that_commented_on_my_post")
	private List<String> usersThatCommentedOnMyPost;

	@Field("users_that_liked_me_count")
	private Integer usersThatLikedMeCount = 0;
	@Field("users_that_liked_me")
	private List<String> usersThatLikedMe;
	
	@Field("users_that_posted_on_my_wall_count")
	private Integer usersThatPostedOnMyWallCount = 0;
	@Field("users_that_posted_on_my_wall")
	private List<String> usersThatPostedOnMyWall;
	
	@Field("users_that_posted_a_snapshot_to_me_count")
	private Integer usersThatPostedASnapshopToMeCount = 0;
	@Field("users_that_posted_a_snapshot_to_me")
	private List<String> usersThatPostedASnapshopToMe;
	
	@Field("id")
	private String userId;	
	
    
    public SocialAction(){	
    }
	
	
	/**
	 * @return the customerId
	 */
	public List<String> getUsersThatCommentedOnMyPost() {
		return usersThatCommentedOnMyPost;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setUsersThatCommentedOnMyPost(List<String> usersThatCommentedOnMyPost) {
		if (usersThatCommentedOnMyPost == null){
			this.usersThatCommentedOnMyPostCount = 0;
			this.usersThatCommentedOnMyPost = null;
			return;
		}
		
		if (usersThatCommentedOnMyPost.size() == 1 && usersThatCommentedOnMyPost.get(0).equals("")){
			usersThatCommentedOnMyPost = new ArrayList<String>();
		}
		
		this.usersThatCommentedOnMyPostCount = usersThatCommentedOnMyPost.size();
		this.usersThatCommentedOnMyPost = usersThatCommentedOnMyPost;
	}
	
	/**
	 * @return the customerId
	 */
	public List<String> getUsersThatLikedMe() {
		return usersThatLikedMe;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setUsersThatLikedMe(List<String> usersThatLikedMe) {
		if (usersThatLikedMe == null){
			this.usersThatLikedMeCount = 0;
			this.usersThatLikedMe = null;
			return;
		}
		
		if (usersThatLikedMe.size() == 1 && usersThatLikedMe.get(0).equals("")){
			usersThatLikedMe = new ArrayList<String>();
		}
		
		this.usersThatLikedMeCount = usersThatLikedMe.size();
		this.usersThatLikedMe = usersThatLikedMe;
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

	
	public Integer getUsersThatCommentedOnMyPostCount() {
		return usersThatCommentedOnMyPostCount;
	}

	public void setUsersThatCommentedOnMyPostCount(Integer usersThatCommentedOnMyPostCount) {
		this.usersThatCommentedOnMyPostCount = usersThatCommentedOnMyPostCount;
	}
	
	public Integer getUsersThatLikedMeCount() {
		return usersThatLikedMeCount;
	}

	public void setUsersThatLikedMeCount(Integer usersThatLikedMeCount) {
		this.usersThatLikedMeCount = usersThatLikedMeCount;
	}

	@Override
	public String toString() {
		return "SocialAction [userId=" + userId + " usersThatLikedMe=" + usersThatLikedMe + ", usersThatCommentedOnMyPost="
				+ usersThatCommentedOnMyPost + ", itemId=" + userId ;
	}
	
	public void addUserThatLikedMe(String userThatLikedMe) {
		if (userThatLikedMe != null) {
			if (usersThatLikedMe == null){
				this.usersThatLikedMe = new ArrayList<String>();
			}
			
			usersThatLikedMe.add(userThatLikedMe);
			usersThatLikedMeCount = usersThatLikedMe.size();
		}
	}

	public void addUserThatCommentedOnMe(String userThatCommentedOnMe) {
		if (userThatCommentedOnMe != null) {
			if (usersThatCommentedOnMyPost == null){
				this.usersThatCommentedOnMyPost = new ArrayList<String>();
			}
			
			usersThatCommentedOnMyPost.add(userThatCommentedOnMe);
			usersThatCommentedOnMyPostCount = usersThatCommentedOnMyPost.size();
		}
	}


	public Integer getUsersThatPostedOnMyWallCount() {
		return usersThatPostedOnMyWallCount;
	}


	public void setUsersThatPostedOnMyWallCount(Integer usersThatPostedOnMyWallCount) {
		this.usersThatPostedOnMyWallCount = usersThatPostedOnMyWallCount;
	}


	public List<String> getUsersThatPostedOnMyWall() {
		return usersThatPostedOnMyWall;
	}


	public void setUsersThatPostedOnMyWall(List<String> usersThatPostedOnMyWall) {
		if (usersThatPostedOnMyWall == null){
			this.usersThatPostedOnMyWallCount = 0;
			this.usersThatPostedOnMyWall = null;
			return;
		}
		
		if (usersThatPostedOnMyWall.size() == 1 && usersThatPostedOnMyWall.get(0).equals("")){
			usersThatPostedOnMyWall = new ArrayList<String>();
		}
		
		this.usersThatPostedOnMyWallCount = usersThatPostedOnMyWall.size();
		this.usersThatPostedOnMyWall = usersThatPostedOnMyWall;
	}
	
	public void addUserThatPostedOnMyWall(String userThatPostedOnMyWall) {
		if (userThatPostedOnMyWall != null) {
			if (usersThatPostedOnMyWall == null){
				this.usersThatPostedOnMyWall = new ArrayList<String>();
			}
			
			usersThatPostedOnMyWall.add(userThatPostedOnMyWall);
			usersThatPostedOnMyWallCount = usersThatPostedOnMyWall.size();
		}
	}


	public Integer getUsersThatPostedASnapshopToMeCount() {
		return usersThatPostedASnapshopToMeCount;
	}


	public void setUsersThatPostedASnapshopToMeCount(
			Integer usersThatPostedASnapshopToMeCount) {
		this.usersThatPostedASnapshopToMeCount = usersThatPostedASnapshopToMeCount;
	}


	public List<String> getUsersThatPostedASnapshopToMe() {
		return usersThatPostedASnapshopToMe;
	}


	public void setUsersThatPostedASnapshopToMe(
			List<String> usersThatPostedASnapshopToMe) {
		if (usersThatPostedASnapshopToMe == null){
			this.usersThatPostedASnapshopToMeCount = 0;
			this.usersThatPostedASnapshopToMe = null;
			return;
		}
		
		if (usersThatPostedASnapshopToMe.size() == 1 && usersThatPostedASnapshopToMe.get(0).equals("")){
			usersThatPostedASnapshopToMe = new ArrayList<String>();
		}
		
		this.usersThatPostedASnapshopToMeCount = usersThatPostedASnapshopToMe.size();
		this.usersThatPostedASnapshopToMe = usersThatPostedASnapshopToMe;
	}
	
	public void addUserThatPostedASnapshopToMe(String userThatPostedASnapshopToMe) {
		if (userThatPostedASnapshopToMe != null) {
			if (usersThatPostedASnapshopToMe == null){
				this.usersThatPostedASnapshopToMe = new ArrayList<String>();
			}
			
			usersThatPostedASnapshopToMe.add(userThatPostedASnapshopToMe);
			usersThatPostedASnapshopToMeCount = usersThatPostedASnapshopToMe.size();
		}
	}
    
}
