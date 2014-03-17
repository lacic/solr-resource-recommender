package at.knowcenter.recommender.solrpowered.engine.utils;

import org.apache.solr.common.params.ModifiableSolrParams;

public class SocialQueryBuilder {
	
	public static ModifiableSolrParams getInteractionsFromMeAndUSersThatILikedOrCommented(String user) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		
		String queryString = "id:(\"" + user + "\")";
		
		solrParams.set("q", queryString);
		return solrParams;
	}

}
