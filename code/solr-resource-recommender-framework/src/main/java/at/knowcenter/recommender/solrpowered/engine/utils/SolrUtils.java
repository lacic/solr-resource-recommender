package at.knowcenter.recommender.solrpowered.engine.utils;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

/**
 * Helper tool class for handlinge solr instances
 * Created by wklieber on 13.02.14.
 */
public class SolrUtils {

    /**
     * helper class for creating new HttpSolrServer instances.
     * Allow to set some parameters like timeout on one place
     * @param url
     * @return
     */
    public static SolrServer newServer(String url) {
        HttpSolrServer server = new HttpSolrServer(url);
        server.setMaxRetries(1);
        //server.setConnectionTimeout(20000);
        //server.setSoTimeout(20000);  // socket read timeout
        server.setDefaultMaxConnectionsPerHost(10);
        //server.setDefaultMaxConnectionsPerHost(128);

        return server;
    }
}
