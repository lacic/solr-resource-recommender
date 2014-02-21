package at.knowcenter.recommender.solrpowered.services.common;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

/** Serializable facet object contains name, number of elements and
 *  values of the elements for a specific facet
 * 
 * @author Michael Wittmayer <mwittmayer@know-center.at>
 *
 */
public class Facet {

	private long count;
	private String name;
	private List<String> values;
	
	@XmlElement(name = "count")
	public long getCount() {
		return count;
	}
	public void setCount(long count) {
		this.count = count;
	}
	
	@XmlElement(name = "name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@XmlElement(name = "values")
	public List<String> getValues() {
		return values;
	}
	public void setValues(List<String> values) {
		this.values = values;
	}
}
