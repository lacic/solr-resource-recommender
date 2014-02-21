package at.knowcenter.recommender.solrpowered.services.common;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/** Holds a list of {@link Facet}s for a given facet group
 * 
 * @author Michael Wittmayer <mwittmayer@know-center.at>
 *
 */
@XmlRootElement
public class FacetGroup {

	/** List of facets */
	private List<Facet> facets;
	/** name of this facet group */
	private String name;
	/** number of different facets within this group */
	private int count;

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = "facet")
	public List<Facet> getFacets() {
		return facets;
	}

	public void setFacets(List<Facet> facets) {
		this.facets = facets;
	}
	
	@XmlAttribute(name = "count")
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
}
