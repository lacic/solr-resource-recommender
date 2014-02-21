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
import java.util.Date;
import java.util.List;

import org.apache.solr.client.solrj.beans.Field;

/**
 * Items in the database encompass (i) products/articles, (ii) ads, (iii) banners/promos.
 * 
 * @author Werner Klieber <mailto:wklieber@know-center.at>
 * @author Mark Kroell <mailto:mkroell@know-center.at>
 * @date 18.07.13
 * 
 */
public class Item implements Serializable  {
	


	private static final long serialVersionUID = 1L;
	
	@Field("id")
    private String itemId;
	@Field("name")
    private String itemName; 
	@Field("language")
	private List<String> language = new ArrayList<String>();
	@Field("description")
    private String description;
	@Field("price")
    private Double price;
	@Field("currency")
    private String currency;
	@Field("validFrom")
    private Date validFrom;
	@Field("validTo")
    private Date validTo;
	@Field("clientId")
	private String clientId;
	@Field("categoryIdClient")
    private List<String> categoryIdClient;
	@Field("categoryIdApp")
    private List<String> categoryIdApp;
	@Field("ageRating")
    private String ageRating;
	@Field("tags")
    private List<String> tags;
	@Field("manufacturer")
    private String manufacturer;
	@Field("preceedingItemId")
    private String preceedingItemId;
	@Field("collection")
    private List<String> collection;

  //__________________________________________________________________________
    /*
	private String clientId;
	private boolean active = true;
    private enum ITEM_TYPE {PRODUCT, AD, PROMO};
    private ITEM_TYPE type; 
    private String predecessorRelation;
    private String successorRelation; 
    private boolean inStock;
    private List<String> itemGroupIds;    
    private List<String>itemTags;
    private List<String>barCodes;
    */
  //__________________________________________________________________________
    
    public Item(){    	
    }
    
	// ------------- GETTERS SETTERS ------------------------- 

	/**
	 * @return the id
	 */
	public String getId() {
		return itemId;
	}
    

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.itemId = id;
	}
	
	public List<String> getLanguage() {
		return language;
	}

	public void setLanguage(List<String> language) {
		this.language = language;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return itemName;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.itemName = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}



	/**
	 * @return the price
	 */
	public Double getPrice() {
		return price;
	}

	/**
	 * @param price the price to set
	 */
	public void setPrice(Double price) {
		this.price = price;
	}

	/**
	 * @return the validFrom
	 */
	public Date getValidFrom() {
		return validFrom;
	}

	/**
	 * @param validFrom the validFrom to set
	 */
	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}

	/**
	 * @return the validTo
	 */
	public Date getValidTo() {
		return validTo;
	}

	/**
	 * @param validTo the validTo to set
	 */
	public void setValidTo(Date validTo) {
		this.validTo = validTo;
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

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}
	
	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public List<String> getCategoryIdClient() {
		return categoryIdClient;
	}

	public void setCategoryIdClient(List<String> categoryIdClient) {
		this.categoryIdClient = categoryIdClient;
	}

	public List<String> getCategoryIdApp() {
		return categoryIdApp;
	}

	public void setCategoryIdApp(List<String> categoryIdApp) {
		this.categoryIdApp = categoryIdApp;
	}

	public String getAgeRating() {
		return ageRating;
	}

	public void setAgeRating(String ageRating) {
		this.ageRating = ageRating;
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

	public String getPreceedingItemId() {
		return preceedingItemId;
	}

	public void setPreceedingItemId(String preceedingItemId) {
		this.preceedingItemId = preceedingItemId;
	}

	public List<String> getCollection() {
		return collection;
	}

	public void setCollection(List<String> collection) {
		this.collection = collection;
	}

    @Override
    public String toString() {
        return "Item{" +
                "itemId='" + itemId + '\'' +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                ", tags=" + tags +
                ", manufacturer='" + manufacturer + '\'' +
                ", validFrom=" + validFrom +
                ", validTo=" + validTo +
                ", clientId='" + clientId + '\'' +
                ", categoryIdClient=" + categoryIdClient +
                ", itemName='" + itemName + '\'' +
                ", language='" + language + '\'' +
                ", description='" + description + '\'' +
                ", categoryIdApp=" + categoryIdApp +
                ", ageRating='" + ageRating + '\'' +
                ", preceedingItemId='" + preceedingItemId + '\'' +
                ", collection=" + collection +
                '}';
    }
}
