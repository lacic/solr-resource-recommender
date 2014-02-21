package at.knowcenter.recommender.solrpowered.services.impl.item;

import java.util.Collection;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;

/**
 * Request for the more like this query. Invokes the MLT handler so common 
 * parameters as fq can be defined for the MLT results
 * @author elacic
 *
 */
public class MoreLikeThisRequest extends SolrRequest {
	private static final long serialVersionUID = 6310167144488485163L;
	private SolrParams query;

	  public MoreLikeThisRequest() {
		  super( METHOD.GET, null );
	  }

	  public MoreLikeThisRequest( SolrParams q ) {
	    super( METHOD.GET, null );
	    query = q;
	  }
	  
	  public MoreLikeThisRequest( SolrParams q, METHOD method ) {
	    super( method, null );
	    query = q;
	  }

	  /**
	   * Use the params 'QT' parameter if it exists
	   */
	  @Override
	  public String getPath() {
	    String qt = query == null ? null : query.get( CommonParams.QT );
	    if( qt == null ) {
	      qt = super.getPath();
	    }
	    if( qt != null && qt.startsWith( "/" ) ) {
	      return qt;
	    }
	    return "/mlt";
	  }
	  
	  
	  @Override
	  public Collection<ContentStream> getContentStreams() {
	    return null;
	  }

	  @Override
	  public SolrParams getParams() {
	    return query;
	  }

	  @Override
	  public QueryResponse process( SolrServer server ) throws SolrServerException {
	    try {
	      long startTime = System.currentTimeMillis();
	      QueryResponse res = new QueryResponse( server.request( this ), server );
	      res.setElapsedTime( System.currentTimeMillis()-startTime );
	      return res;
	    } catch (SolrServerException e){
	      throw e;
	    } catch (SolrException s){
	      throw s;
	    } catch (Exception e) {
	      throw new SolrServerException("Error executing query", e);
	    }
	  }
	}