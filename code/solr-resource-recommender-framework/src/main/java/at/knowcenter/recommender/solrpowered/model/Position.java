package at.knowcenter.recommender.solrpowered.model;


import java.io.Serializable;
import java.util.Date;

import org.apache.solr.client.solrj.beans.Field;

public class Position implements Serializable {
	
	@Field("id")
	private String id;
	
	@Field("user")
	private String user;
	
	@Field("region_location")
	private String regionLocation;

	@Field("region_name")
	private String regionName;
	
	@Field("region_id")
	private Long regionId;

	@Field("location_in_region")
	private String locationInRegion;

	@Field("zlocal")
	private Integer zLocal;
	
	@Field("global_location")
	private String globalLocation;
	
	@Field("time")
	private Date time;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getRegionLocation() {
		return regionLocation;
	}

	public void setRegionLocation(String regionLocation) {
		this.regionLocation = regionLocation;
	}

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public Long getRegionId() {
		return regionId;
	}

	public void setRegionId(Long regionId) {
		this.regionId = regionId;
	}

	public String getLocationInRegion() {
		return locationInRegion;
	}

	public void setLocationInRegion(String locationInRegion) {
		this.locationInRegion = locationInRegion;
	}

	public Integer getzLocal() {
		return zLocal;
	}

	public void setzLocal(Integer zLocal) {
		this.zLocal = zLocal;
	}

	public String getGlobalLocation() {
		return globalLocation;
	}

	public void setGlobalLocation(String globalLocation) {
		this.globalLocation = globalLocation;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}
	
}
