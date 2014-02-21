package at.knowcenter.recommender.solrpowered.services.common;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/** Wrapper class for all snippets that are found 
 *  for a specific metadata field
 * 
 * @author Michael Wittmayer <mwittmayer@know-center.at>
 *
 */
public class SnippetField {

	/** Name of the metadata field */
	private String description;
	
	/** List of snippets for the given metadata field */
	private List<String> snippets;

	@XmlAttribute(name="field")
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@XmlElement(name="snippet")
	public List<String> getSnippets() {
		return snippets;
	}
	public void setSnippets(List<String> snippets) {
		this.snippets = snippets;
	}
	
	
}
