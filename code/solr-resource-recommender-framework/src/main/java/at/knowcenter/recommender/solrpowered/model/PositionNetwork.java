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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.beans.Field;


/**
 * Class which conveys information about the user's social activites
 * @author elacic
 */
public class PositionNetwork implements Serializable  {
	
	private static final long serialVersionUID = 1L;
	
	@Field("global_neighborhood_count")
	private Integer locationNeighborsCount = 0;
	@Field("global_neighborhood")
	private List<String> locationNeighbors;

	@Field("region_neighborhood_count")
	private Integer regionNeighborsCount = 0;
	@Field("region_neighborhood")
	private List<String> regionNeighbors;
	
	@Field("global_cooccurred_neighborhood_count")
	private Integer locationCoocuredNeighborsCount = 0;
	@Field("global_cooccurred_neighborhood")
	private List<String> locationCoocuredNeighbors;

	@Field("region_cooccurred_neighborhood_count")
	private Integer regionCoocuredNeighborsCount = 0;
	@Field("region_cooccurred_neighborhood")
	private List<String> regionCoocuredNeighbors;
	
	
	@Field("id")
	private String userId;	
	
    
    public PositionNetwork(){	
    }
	
	
	/**
	 * @return the customerId
	 */
	public List<String> getLocationNeighbors() {
		return locationNeighbors;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setLocationNeighbors(List<String> locationNeighbors) {
		if (locationNeighbors == null){
			this.locationNeighborsCount = 0;
			this.locationNeighbors = null;
			return;
		}
		
		if (locationNeighbors.size() == 1 && locationNeighbors.get(0).equals("")){
			locationNeighbors = new ArrayList<String>();
		}
		
		this.locationNeighborsCount = locationNeighbors.size();
		this.locationNeighbors = locationNeighbors;
	}
	
	/**
	 * @return the customerId
	 */
	public List<String> getRegionNeighbors() {
		return regionNeighbors;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setRegionNeighbors(List<String> regionNeighbors) {
		if (regionNeighbors == null){
			this.regionNeighborsCount = 0;
			this.regionNeighbors = null;
			return;
		}
		
		if (regionNeighbors.size() == 1 && regionNeighbors.get(0).equals("")){
			regionNeighbors = new ArrayList<String>();
		}
		
		this.regionNeighborsCount = regionNeighbors.size();
		this.regionNeighbors = regionNeighbors;
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

	
	public Integer getLocationNeighborsCount() {
		return locationNeighborsCount;
	}

	public void setLocationNeighborsCount(Integer locationNeighborsCount) {
		this.locationNeighborsCount = locationNeighborsCount;
	}
	
	public Integer getRegionNeighborsCount() {
		return regionNeighborsCount;
	}

	public void setRegionNeighborsCount(Integer regionNeighborsCount) {
		this.regionNeighborsCount = regionNeighborsCount;
	}

	@Override
	public String toString() {
		return "PositionNetwork [userId=" + userId + " regionNeighbors=" + regionNeighbors + ", locationNeighbors="
				+ locationNeighbors + ", itemId=" + userId ;
	}
	
	public void addRegionNeighbor(String regionNeighbor) {
		if (regionNeighbor != null) {
			if (regionNeighbors == null){
				this.regionNeighbors = new ArrayList<String>();
			}
			
			regionNeighbors.add(regionNeighbor);
			regionNeighborsCount = regionNeighbors.size();
		}
	}

	public void addLocationNeighbor(String locationNeighbor) {
		if (locationNeighbor != null) {
			if (locationNeighbors == null){
				this.locationNeighbors = new ArrayList<String>();
			}
			
			locationNeighbors.add(locationNeighbor);
			locationNeighborsCount = locationNeighbors.size();
		}
	}
	


	public Integer getLocationCoocuredNeighborsCount() {
		return locationCoocuredNeighborsCount;
	}


	public void setLocationCoocuredNeighborsCount(
			Integer locationCoocuredNeighborsCount) {
		this.locationCoocuredNeighborsCount = locationCoocuredNeighborsCount;
	}


	public List<String> getLocationCoocuredNeighbors() {
		return locationCoocuredNeighbors;
	}





	public Integer getRegionCoocuredNeighborsCount() {
		return regionCoocuredNeighborsCount;
	}


	public void setRegionCoocuredNeighborsCount(Integer regionCoocuredNeighborsCount) {
		this.regionCoocuredNeighborsCount = regionCoocuredNeighborsCount;
	}


	public List<String> getRegionCoocuredNeighbors() {
		return regionCoocuredNeighbors;
	}


	public void setRegionCoocuredNeighbors(List<String> regionCoocuredNeighbors) {
		if (regionCoocuredNeighbors == null){
			this.regionCoocuredNeighborsCount = 0;
			this.regionCoocuredNeighbors = null;
			return;
		}
		
		if (regionCoocuredNeighbors.size() == 1 && regionCoocuredNeighbors.get(0).equals("")){
			regionCoocuredNeighbors = new ArrayList<String>();
		}
		
		this.regionCoocuredNeighborsCount = regionCoocuredNeighbors.size();
		this.regionCoocuredNeighbors = regionCoocuredNeighbors;
	}
	
	public void setLocationCoocuredNeighbors(List<String> locationCoocuredNeighbors) {
		if (locationCoocuredNeighbors == null){
			this.locationCoocuredNeighborsCount = 0;
			this.locationCoocuredNeighbors = null;
			return;
		}
		
		if (locationCoocuredNeighbors.size() == 1 && locationCoocuredNeighbors.get(0).equals("")){
			locationCoocuredNeighbors = new ArrayList<String>();
		}
		
		this.locationCoocuredNeighborsCount = locationCoocuredNeighbors.size();
		this.locationCoocuredNeighbors = locationCoocuredNeighbors;
	}
	

}
