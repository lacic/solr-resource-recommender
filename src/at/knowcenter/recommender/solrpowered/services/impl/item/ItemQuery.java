package at.knowcenter.recommender.solrpowered.services.impl.item;

import java.util.Date;

import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.services.common.SolrQuery;
/**
 * Search query object
 * @author hziak
 */
public class ItemQuery extends SolrQuery<Item>{
	
	private Date dateAfter;
	private Date dateBefore;
	private Float priceAbove;
	private Float priceBeneath;
	private String sortCriteria;
	
	
	public String getSortCriteria() {
		return sortCriteria;
	}
	public void setSortCriteria(String sortCriteria) {
		this.sortCriteria = sortCriteria;
	}
	
	public ItemQuery () {
		item = new Item();
	}

	public Date getDateAfter() {
		return dateAfter;
	}
	
	/**
	 * sets the query to search all elements after that date
	 * @param dateAfter
	 */
	public void setDateAfter(Date dateAfter) {
		this.dateAfter = dateAfter;
	}
	public Date getDateBefore() {
		return dateBefore;
	}
	/**
	 * sets the query to search all elements befor that date
	 * @param dateBefore
	 */
	public void setDateBefore(Date dateBefore) {
		this.dateBefore = dateBefore;
	}
	public Float getPriceAbove() {
		return priceAbove;
	}
	/**
	 * sets the query to search all elements with higher prices than this
	 * @param priceAbove
	 */
	public void setPriceAbove(Float priceAbove) {
		this.priceAbove = priceAbove;
	}
	public Float getPriceBeneath() {
		return priceBeneath;
	}
	/**
	 * to search all elements with lower prices than this
	 * @param priceBeneath
	 */
	public void setPriceBeneath(Float priceBeneath) {
		this.priceBeneath = priceBeneath;
	}
}
