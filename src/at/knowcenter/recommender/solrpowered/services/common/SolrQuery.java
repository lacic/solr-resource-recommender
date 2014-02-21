package at.knowcenter.recommender.solrpowered.services.common;

import java.io.Serializable;
import java.util.List;

public abstract class SolrQuery<T extends Serializable> {

	protected String query;
	private int startNumber;
	protected List<String> filterquery;
	protected List<String> metadata;
	protected List<String> facetFields;
	
	public List<String> getFacetFields() {
		return facetFields;
	}

	public void setFacetFields(List<String> facetFields) {
		this.facetFields = facetFields;
	}

	protected T item;
	
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public T getItem() {
		return item;
	}

	public void setItem(T item) {
		this.item = item;
	}

	public List<String> getMetadata() {
		return metadata;
	}
	
	public void setMetadata(List<String> metadata) {
		this.metadata = metadata;
	}
	
	public int getStartNumber() {
		return startNumber;
	}
	public void setStartNumber(int startNumber) {
		this.startNumber = startNumber;
	}

	public List<String> getFilterQuery() {
		return filterquery;
	}

	public void setFilterQuery(List<String> filterquery) {
		this.filterquery = filterquery;
	}	
}
