package at.knowcenter.recommender.solrpowered.model;

import java.io.Serializable;
import java.util.Date;


import org.apache.solr.client.solrj.beans.Field;

/**
 * Social stream in the recommender framework
 * 
 * @author elacic
 * 
 */

public class SocialStream implements Serializable  {
	


	private static final long serialVersionUID = 1L;
	
	// Item fields
	
	// stored fields
	@Field("id")
    private String actionId;
	@Field("source")
    private String sourceUserId; 
	@Field("target_user")
    private String targetUserId;
	@Field("target_action")
	private String targetActionId;
	@Field("content")
	private String socialContent;
	@Field("action_type")
	private String actionType;
	@Field("datasource")
    private String datasource;
	@Field("timestamp")
    private Date timestamp;
	@Field("score")
    private Float score;

	public SocialStream(){    	
    }


	public String getActionId() {
		return actionId;
	}

	public void setActionId(String actionId) {
		this.actionId = actionId;
	}

	public String getSourceUserId() {
		return sourceUserId;
	}

	public void setSourceUserId(String sourceUserId) {
		this.sourceUserId = sourceUserId;
	}

	public String getTargetUserId() {
		return targetUserId;
	}

	public void setTargetUserId(String targetUserId) {
		this.targetUserId = targetUserId;
	}

	public String getTargetActionId() {
		return targetActionId;
	}

	public void setTargetActionId(String targetActionId) {
		this.targetActionId = targetActionId;
	}

	public String getSocialContent() {
		return socialContent;
	}

	public void setSocialContent(String socialContent) {
		this.socialContent = socialContent;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public String getDatasource() {
		return datasource;
	}

	public void setDatasource(String datasource) {
		this.datasource = datasource;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	
	
	public Float getScore() {
		return score;
	}


	public void setScore(Float score) {
		this.score = score;
	}


	@Override
	public String toString() {
		return "Social Stream";
	}





}
