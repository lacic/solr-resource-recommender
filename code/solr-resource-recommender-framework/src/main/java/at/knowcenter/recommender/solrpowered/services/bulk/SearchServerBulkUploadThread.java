package at.knowcenter.recommender.solrpowered.services.bulk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;

/**
 * Thread to upload data asynchronous
 * @author hziak
 */
public class SearchServerBulkUploadThread<I> extends Thread {
	private  List<I> uploadItems = null;
	SolrServer solrServer;
	SearchServerBulkMessage searchServerBulkUpload;
	public SearchServerBulkUploadThread(SolrServer solrServer,SearchServerBulkMessage searchServerBulkUpload) {
		this.solrServer = solrServer;
		this.searchServerBulkUpload=searchServerBulkUpload;
		this.setName("SearchServerBulkUpload");
	}
	@Override
	public void run(){
		long t1 = System.nanoTime();
		
		if(uploadItems!=null && uploadItems.size() > 0){
		  try {
			solrServer.addBeans(uploadItems);
			UpdateResponse commit = solrServer.commit();
//			System.out.println("Commit elapsed time in ms: " + commit.getElapsedTime());
		} catch (SolrServerException e) {
			if(searchServerBulkUpload!=null)
				searchServerBulkUpload.returnStatus(e.getMessage());
			e.printStackTrace();
			return;
		} catch (IOException e) {
			if(searchServerBulkUpload!=null)
				searchServerBulkUpload.returnStatus(e.getMessage());
			e.printStackTrace();
			return;
		}
		 if(searchServerBulkUpload!=null)
					searchServerBulkUpload.returnStatus("Upload done.");
		
	  }else
		  if(searchServerBulkUpload!=null)
				searchServerBulkUpload.returnStatus("No Data to upload.");
	
		
		long t2 = System.nanoTime();
		
//		String filePath = "../webserver/src/test/resources/testFiles/SearchServerBulkUploadThread.log";
//		SimpleLogger logger = new SimpleLogger(filePath);
//		long uploadTime = t2 - t1;
//		logger.log(Long.toString(uploadTime));	
		
		
//		System.out.println("ServerTread upload done in: " + (System.nanoTime() - start) + " ns");
	}
	/**
	 * sets the Data to be uploaded
	 * @param uploadItems
	 */
	public void setUploadItems(List<I> uploadItems) {
		this.uploadItems = new ArrayList<I>(uploadItems);
	}
}
