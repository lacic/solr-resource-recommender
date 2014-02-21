package at.knowcenter.recommender.solrpowered.services.bulk;
/**
 * Interface to the callback function called by the SearchServerBulkUploadThread
 * in the SearchService.addDocuments function.
 * @author hziak
 *
 */
public interface SearchServerBulkMessage {
	
	/**
	 * message contains the return status of the BulkUploadThread
	 * @param message
	 */
	void returnStatus(String message);

}
