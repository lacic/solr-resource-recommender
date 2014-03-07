package at.knowcenter.recommender.solrpowered.services.common;

import java.util.List;

/**
 * Response object of the recommendation 
 * @author elacic
 */
public abstract class SolrResponse<I> {
	
	private List<I> resultItems;
	private long numFound;
	private long elapsedTime;
	private List<FacetGroup> facets;
	private List<Snippet> snippets;
	

	public List<Snippet> getSnippets() {
		return snippets;
	}

	public void setSnippets(List<Snippet> snippets) {
		this.snippets = snippets;
	}

	public List<FacetGroup> getFacets() {
		return facets;
	}

	public void setFacets(List<FacetGroup> facets) {
		this.facets = facets;
	}
	/**
	 * Returns the number of found elements
	 * @return
	 */
	public long getNumFound() {
		return numFound;
	}

	public void setNumFound(long found) {
		this.numFound = found;
	}
	/**
	 * Returns the time in milliseconds the search needed
	 * @return
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}

	public void setElapsedTime(long elapsedTime) {
		this.elapsedTime = elapsedTime;
	}
	/**
	 * Returns the list of results
	 * @return
	 */
	public List<I> getResultItems() {
		return resultItems;
	}

	public void setResultItems(List<I> resultItems) {
		this.resultItems = resultItems;
	}

}
