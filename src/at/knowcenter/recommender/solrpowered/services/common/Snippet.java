package at.knowcenter.recommender.solrpowered.services.common;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/** Wrapper class for all snippets that are found for a specific document.
 * 
 * @author Michael Wittmayer <mwittmayer@know-center.at>
 *
 */

@XmlRootElement(name="snippet")
@XmlAccessorType(XmlAccessType.FIELD)
public class Snippet {

	/** Id of the document */
	@XmlElement(name="id")
	private String id;
	/** List of snippets that have been found */
	@XmlElement(name="snippetList")
	private List<SnippetField> snippets;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	

	public List<SnippetField> getSnippets() {
		return snippets;
	}
	public void setSnippets(List<SnippetField> snippets) {
		this.snippets = snippets;
	}
	
	
}
