package at.knowcenter.recommender.solrpowered.services.bulk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;

import at.knowcenter.recommender.solrpowered.model.Item;

public class SearchServerBulkRemoveThread extends Thread{

	private SolrServer solrServer;
	private SearchServerBulkMessage message;
	private List<String> ids;
	public SearchServerBulkRemoveThread(SolrServer solrServer,
			SearchServerBulkMessage message) {
		this.solrServer = solrServer;
		this.message = message;
	}

	public void setRemoveItems(List<String> ids) {
			this.ids=ids;
		}
	public void setRemoveSearchItems(List<Item> items){
		this.ids=new ArrayList<String>();
		for (Item searchItem : items) {
			ids.add(searchItem.getId());
		}
	}
	public void run(){
		if(ids==null){
			message.returnStatus("No elements set to remove from the index");
			return;
		}
		if(ids.size()<1){
			message.returnStatus("No elements set to remove from the index");
			return;
		}
		try {
			solrServer.deleteById(ids);
			solrServer.commit();
		} catch (SolrServerException e) {
			message.returnStatus(e.getMessage());
			e.printStackTrace();
			return;
		} catch (IOException e) {
			message.returnStatus(e.getMessage());
			e.printStackTrace();
			return;
		}
		message.returnStatus("Elements removed from the search index");
	}
	

}
