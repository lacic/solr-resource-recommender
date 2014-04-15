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
 * @author Werner Klieber <mailto:wklieber@know-center.at>
 * @author Mark Kroell <mailto:mkroell@know-center.at>
 * @date 18.07.13
 */

public class Customer implements Serializable {
	
	public Float getScore() {
		return score;
	}

	public void setScore(Float score) {
		this.score = score;
	}

	public List<String> getCustomergroup() {
		return customergroup;
	}


	private static final long serialVersionUID = 1L;
	@Field("id")
	private String id;
	@Field("active")
	private boolean active = true;
	
    private Sex sex = Sex.EMPTY;
    @Field("dateofbirth")
    private Date dateOfBirth; 
    @Field("zip")
    private String zip;
    @Field("street")
    private String street;
    @Field("city")
    private String city;
    @Field("country")
    private String country;
    @Field("customergroup")
    private List<String> customergroup;
    @Field("friendOf")
    private List<String> friendOf;
    @Field("education")
    private String education;
    @Field("work")
    private String work;
    @Field("language")
    private String language;
    
    @Field("interests")
    private List<String> interests;
    @Field("interests_content")
    private String interestsContent;
    @Field("biography")
    private String biography;
    @Field("real_biography")
    private String realBiography;
    
    @Field("purchased_categories")
    private List<String> purchasedCategories;
	

    @Field("favorite_regions")
    private List<String> favoriteRegions;
    
    @Field("score")
    private Float score;
    //__________________________________________________________________________
    
	public List<String> getPurchasedCategories() {
		return purchasedCategories;
	}

	public void setPurchasedCategories(List<String> purchasedCategories) {
		this.purchasedCategories = purchasedCategories;
	}
	

	public Customer(){    	
    }

	// ------------- GETTERS SETTERS ------------------------- 
    
	/** 
	 * @return the user interests
	 */
	public List<String> getInterests() {
		return interests;
	}

	public void setInterests(List<String> interests) {
		this.interests = interests;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}


	/**
	 * @return the sex
	 */
	public String getSex() {
		return sex.getSex();
	}



	/**
	 * @param sex the sex to set
	 */
	@Field ("sex")
	public void setSex(String sex) {

		if (sex != null) {
			if( sex.equals(Sex.FEMALE.getSex())) {			
				this.sex = Sex.FEMALE;
			} else if( sex.equals(Sex.MALE.getSex()))  {
				this.sex = Sex.MALE;
			} else {
				this.sex = Sex.EMPTY;
			}
		} else {
			this.sex = Sex.EMPTY;
		}
	}
	
	public void setSex(Sex sex){
		this.sex = sex;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * 
	 * @param active
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * 
	 * @return
	 */
	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	/**
	 * 
	 * @param dateOfBirth
	 */
	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}
	
	public void setDateOfBirth(String dateOfBirth) throws ParseException{
		this.dateOfBirth = DateUtil.parseDate(dateOfBirth);
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public List<String> getCustomerGroup() {
		return customergroup;
	}

	public void setCustomergroup(List<String> customergroup) {
		this.customergroup = customergroup;
	}

	public List<String> getFriendOf() {
		return friendOf;
	}

	public void setFriendOf(List<String> friendOf) {
		this.friendOf = friendOf;
	}
    public String getEducation() {
		return education;
	}

	public void setEducation(String education) {
		this.education = education;
	}

	public String getWork() {
		return work;
	}

	public void setWork(String work) {
		this.work = work;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	
	@Override
	public String toString() {
		return "Customer [id=" + id + ", active=" + active + ", sex=" + sex
				+ ", dateOfBirth=" + dateOfBirth + ", zip=" + zip + ", street="
				+ street + ", city=" + city + ", country=" + country
				+ ", customergroup=" + customergroup + ", friendOf=" + friendOf
				+ ", education=" + education + ", work=" + work + ", language="
				+ language + "]";
	}

	public String getInterestsContent() {
		return interestsContent;
	}

	public void setInterestsContent(String interestsContent) {
		this.interestsContent = interestsContent;
	}

	public String getBiography() {
		return biography;
	}

	public void setBiography(String biography) {
		this.biography = biography;
	}

	public String getRealBiography() {
		return realBiography;
	}

	public void setRealBiography(String realBiography) {
		this.realBiography = realBiography;
	}

	public List<String> getFavoriteRegions() {
		return favoriteRegions;
	}

	public void setFavoriteRegions(List<String> favoriteRegions) {
		this.favoriteRegions = favoriteRegions;
	}

	
} // end class Customer

