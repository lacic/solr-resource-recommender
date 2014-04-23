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
 * @author elacic
 */
public class SharedLocation implements Serializable  {
	
	private static final long serialVersionUID = 1L;
	
	@Field("shared_region_id_count")
	private Integer sharedRegionCount = 0;
	@Field("shared_region_id")
	private List<String> sharedRegions;

	@Field("monitored_event_region_id_count")
	private Integer monitoredRegionCount = 0;
	@Field("monitored_event_region_id")
	private List<String> monitoredRegions;
	
	@Field("id")
	private String userId;	
	
    
    public SharedLocation(){	
    }
	
	
	/**
	 * @return the customerId
	 */
	public List<String> getSharedRegions() {
		return sharedRegions;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setSharedRegions(List<String> sharedRegions) {
		if (sharedRegions == null){
			this.sharedRegionCount = 0;
			this.sharedRegions = null;
			return;
		}
		
		if (sharedRegions.size() == 1 && sharedRegions.get(0).equals("")){
			sharedRegions = new ArrayList<String>();
		}
		
		this.sharedRegionCount = sharedRegions.size();
		this.sharedRegions = sharedRegions;
	}
	
	/**
	 * @return the customerId
	 */
	public List<String> getMonitoredRegions() {
		return monitoredRegions;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setMonitoredRegions(List<String> monitoredRegions) {
		if (monitoredRegions == null){
			this.monitoredRegionCount = 0;
			this.monitoredRegions = null;
			return;
		}
		
		if (monitoredRegions.size() == 1 && monitoredRegions.get(0).equals("")){
			monitoredRegions = new ArrayList<String>();
		}
		
		this.monitoredRegionCount = monitoredRegions.size();
		this.monitoredRegions = monitoredRegions;
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

	
	public Integer getSharedRegionsCount() {
		return sharedRegionCount;
	}

	public void setSharedRegionsCount(Integer sharedRegionsCount) {
		this.sharedRegionCount = sharedRegionsCount;
	}
	
	public Integer getMonitoredRegionsCount() {
		return monitoredRegionCount;
	}

	public void setMonitoredRegionsCount(Integer monitoredRegionsCount) {
		this.monitoredRegionCount = monitoredRegionsCount;
	}

	@Override
	public String toString() {
		return "SharedLocation [userId=" + userId + " usersThatLikedMe=" + monitoredRegions + ", usersThatCommentedOnMyPost="
				+ sharedRegions + ", itemId=" + userId ;
	}
	
	public void addMonitoredRegion(String monitoredRegion) {
		if (monitoredRegion != null) {
			if (monitoredRegions == null){
				this.monitoredRegions = new ArrayList<String>();
			}
			
			monitoredRegions.add(monitoredRegion);
			monitoredRegionCount = monitoredRegions.size();
		}
	}

	public void addSharedRegion(String sharedRegion) {
		if (sharedRegion != null) {
			if (sharedRegions == null){
				this.sharedRegions = new ArrayList<String>();
			}
			
			sharedRegions.add(sharedRegion);
			sharedRegionCount = sharedRegions.size();
		}
	}

    
}
