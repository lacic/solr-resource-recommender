package at.knowcenter.recommender.solrpowered.services.impl.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.engine.utils.SolrUtils;
import at.knowcenter.recommender.solrpowered.model.Item;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkMessage;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkRemoveThread;
import at.knowcenter.recommender.solrpowered.services.common.Facet;
import at.knowcenter.recommender.solrpowered.services.common.FacetGroup;
import at.knowcenter.recommender.solrpowered.services.common.Snippet;
import at.knowcenter.recommender.solrpowered.services.common.SolrService;

/**
 * Search service provider
 * 
 * @author hziak
 * 
 */
public class ItemService extends SolrService<ItemQuery, Item, ItemResponse>{
	private final String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private String facet = "on";
	private String highlighting = "on";
	private String[] facetFields;
	private String metadata;
	private String snippetFields;
	private String defaultLanguage = "en";
	
	public ItemService(String address, Integer port, String coreName) {
		this.address = address;
		this.port = port;
		this.collection = coreName;

		this.solrServer = SolrUtils.newServer("http://" + address + ":" + port + "/solr/" + coreName);
        System.out.println(solrServer);
    }
	
	public ItemService(String address, Integer port, String coreName, List<String> facetFields, String metadata, String snippetFields) {
		this(address, port, coreName);
		this.facetFields = facetFields.toArray(new String[facetFields.size()]);
		this.metadata = metadata;
		this.snippetFields = snippetFields;
	}

	
	public String getDefaultLanguage() {
		return defaultLanguage;
	}

	public void setDefaultLanguage(String defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
	}

	/**
	 * searches the index for the query and returns a SearchResponse Object
	 * 
	 * @param query
	 * @return
	 */
	public ItemResponse search(ItemQuery query, int maxResultCount) {
		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		String queryString = createQueryOutOfSearchQuery(query);
		solrParams.set("q", queryString);
		if (query.getFilterQuery() != null) {
			String filterQueryString = createFilterQueryOutOfSearchQuery(query.getFilterQuery());
			solrParams.set("fq", filterQueryString);
		}
		solrParams.set("fl", metadata);
		solrParams.set("facet", facet);
		solrParams.set("facet.mincount", 1);
		if (facetFields != null) {
			solrParams.set("facet.field", this.facetFields);
		}
		solrParams.set("hl", highlighting);
		solrParams.set("hl.fl", snippetFields);
		if (query.getSortCriteria() != null) {
			solrParams.set("sort", query.getSortCriteria());
		}
		solrParams.set("rows", maxResultCount);
		solrParams.set("start", query.getStartNumber());

		QueryResponse response = null;
		ItemResponse searchResponse = new ItemResponse();
		try {
			response = solrServer.query(solrParams);
			List<Item> items = response.getBeans(Item.class);
//			List<Item> items = extractIDs(response.getBeans(Item.class));
			List<Snippet> snippets = extractSnippets(response.getHighlighting());
			searchResponse.setResultItems(items);
			searchResponse.setSnippets(snippets);
			searchResponse.setElapsedTime(response.getElapsedTime());
			SolrDocumentList docResults = response.getResults();
			searchResponse.setNumFound(docResults.getNumFound());
			searchResponse.setFacets(extractFacetGroups(response.getFacetFields()));
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return searchResponse;
	}

//	private List<Item> extractIDs(List<Item> items) {
//		for (Item item : items) {
//			if (item.getLanguage() != null && item.getLanguage().length() > 0) {
//				item.setId(item.getId().substring(0, item.getId().lastIndexOf("_")));
//			}
//		}
//		return items;
//	}

	/** Extract facet values from solrResponse into something sendable
	 * 
	 * @param facetFields
	 * @return
	 */
	private List<FacetGroup> extractFacetGroups(List<FacetField> facetFields) {
		List<FacetGroup> facets = new ArrayList<FacetGroup>();
		for (FacetField facetField : facetFields) {
			FacetGroup group = new FacetGroup();
			group.setName(facetField.getName());
			group.setCount(facetField.getValueCount());
			for (Count element : facetField.getValues()) {
				Facet f = new Facet();
				f.setName(element.getName());
				f.setCount(element.getCount());
				if (group.getFacets() == null) {
					List<Facet> facet = new ArrayList<Facet>();
					facet.add(f);
					group.setFacets(facet);
				} else {
					group.getFacets().add(f);
				}
			}
			facets.add(group);
		}
		return facets;
	}

	/**
	 * creates the filterquery part of the solr search query
	 * @param filterQuery
	 * @return
	 */
	private String createFilterQueryOutOfSearchQuery(List<String> filterQuery) {
		StringBuilder queryBuilder = new StringBuilder();
		for (String queryFragment : filterQuery) {
			queryBuilder.append(queryFragment);
			queryBuilder.append(" AND ");
		}
		queryBuilder.delete(queryBuilder.length() - 5, queryBuilder.length());
		
//		String queryString = (item.getAgeRating() == null) ? "" : " ageRating:(" + item.getAgeRating() + ")";
//		queryString += (item.getTags() == null) ? "" : getQueryChunkFromList(item.getTags(), "tags",queryString.isEmpty());
//		queryString += (item.getCategoryIdApp() == null) ? "" : "cateGoryIdApp:(" + item.getCategoryIdApp() + ")";
//		queryString += (item.getCategoryIdClient() == null) ? "" : "categoryIdClient:(" + item.getCategoryIdClient() + ")";
//		queryString += (item.getCollection() == null) ? "" : getQueryChunkFromList(item.getCollection(), "collection",queryString.isEmpty());

//		SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);
//		if (!queryString.isEmpty())
//			if (query.getDateAfter() != null || query.getDateBefore() != null)
//					queryString += " AND ";
//		if (query.getDateAfter() != null && query.getDateBefore() != null) {
//			queryString += "(validFrom:[" + dateFormat.format(query.getDateAfter()) + " TO " + dateFormat.format(query.getDateBefore()) + "]";
//			queryString += " AND validTo:[" + dateFormat.format(query.getDateAfter()) + " TO " + dateFormat.format(query.getDateBefore()) + "])";
//		} else if (query.getDateAfter() == null && query.getDateBefore() != null) {
//			queryString += "(validFrom:[* TO " + dateFormat.format(query.getDateBefore()) + "]";
//			queryString += " AND validTo:[* TO " + dateFormat.format(query.getDateBefore()) + "])";
//		} else if (query.getDateAfter() != null && query.getDateBefore() == null) {
//			queryString += "(validFrom:[" + dateFormat.format(query.getDateAfter()) + " TO *]";
//			queryString += " AND validTo:[" + dateFormat.format(query.getDateAfter()) + " TO *]";
//		}
//		if (query.getPriceAbove() != null || query.getPriceBeneath() != null) {
//			if (query.getDateAfter() != null || query.getDateBefore() != null)
//				queryString += " AND";
//			queryString += " price:[" + (query.getPriceAbove() == null ? "*" : +query.getPriceAbove()) + " TO "
//					+ (query.getPriceBeneath() == null ? "*" : +query.getPriceBeneath()) + "]";
//		}
		return queryBuilder.toString();
	}
	/**
	 * creates an string in the form tag:(elements[1] AND .... elements[n])
	 * @param queryList
	 * @param tag
	 * @return
	 */
	private String getQueryChunkFromList(List<String> queryList, String tag, boolean prefixEmpty) {
		Set<String> querySet = new HashSet<String>(queryList);
		String queryString = new String();
		if (querySet != null){
			if (querySet.size() > 0) {
				if(!prefixEmpty)
					queryString += " AND ";
				boolean first = true;
				queryString += "  " + tag + ":(";
				for (String value : querySet) {
					if (first) {
						queryString += "\"" + value + "\"";
						first = false;
					} else
						queryString += " AND \"" + value + "\"";
				}
				queryString += ")";
			}
		}
		return queryString;
	}

	/**
	 * creates a solr query out of the SearchQuery object
	 * 
	 * @param query
	 * @return
	 */
	private String createQueryOutOfSearchQuery(ItemQuery query) {
		Item item = query.getItem();
		String queryString = (query.getQuery() == null) ? "" : query.getQuery();
		queryString += (item.getName() == null) ? "" : " name:(" + item.getName() + ")";
		queryString += (item.getId() == null) ? "" : " id:(" + item.getId() + ")";
		queryString += (item.getDescription() == null) ? "" : "  description:(" + item.getDescription() + ")";
		queryString += (item.getManufacturer() == null) ? "" : "  manufacturer:(" + item.getManufacturer() + ")";
		queryString += (item.getCollection() == null) ? "" : "   collection:(" + item.getCollection() + ")";
		queryString += (item.getPreceedingItemId() == null) ? "" : "  preceedingItemId:(" + item.getPreceedingItemId() + ")";
		return queryString;
	}

	/**
	 * updates/adds the Document as bean to the index
	 * if there is already a document with the same id it will be overwritten
	 * @param searchItem
	 */
	public void updateDocument(Item searchItem) {
		ItemQuery q = new ItemQuery();
		q.setQuery("id:" + searchItem.getItemId());
		ItemResponse r= search(q, 1);
		if (r.getNumFound() == 0) {
			updateDocument(searchItem, solrServer);			
		} else {
			Item retrievedItem = r.getResultItems().get(0);
			if (retrievedItem.getLanguage().contains(defaultLanguage)) {
				if (searchItem.getLanguage().contains(defaultLanguage)) {
					Set<String> tmp = new HashSet<String>(searchItem.getLanguage());
					tmp.addAll(retrievedItem.getLanguage());
					searchItem.setLanguage(new ArrayList<String>(tmp));
					updateDocument(searchItem, solrServer);
				} else {
					Set<String> tmp = new HashSet<String>(searchItem.getLanguage());
					tmp.addAll(retrievedItem.getLanguage());
					retrievedItem.setLanguage(new ArrayList<String>(tmp));
					updateDocument(retrievedItem, solrServer);	
				}
			} else {
				if (searchItem.getLanguage().contains(defaultLanguage)) {
					Set<String> tmp = new HashSet<String>(searchItem.getLanguage());
					tmp.addAll(retrievedItem.getLanguage());
					searchItem.setLanguage(new ArrayList<String>(tmp));
					updateDocument(searchItem, solrServer);
				} else {
					Set<String> tmp = new HashSet<String>(searchItem.getLanguage());
					tmp.addAll(retrievedItem.getLanguage());
					retrievedItem.setLanguage(new ArrayList<String>(tmp));
					updateDocument(retrievedItem, solrServer);			
				}
			}
		}
	}

	/**
	 * updates/adds a list of Documents as beans to the index in an own thread
	 * if there are already documents with the same ids they will be overwritten
	 * @param searchItem
	 */
	public void writeDocuments(List<Item> searchItems, SearchServerBulkMessage searchServerBulkUpload) {
		for (Item item : searchItems) {
			ItemQuery q = new ItemQuery();
			q.setQuery("id:" + item.getItemId());
			ItemResponse r= search(q, 1);
			if (r.getNumFound() > 0) {
				Item retrievedItem = r.getResultItems().get(0);
				if (retrievedItem.getLanguage().contains(defaultLanguage)) {
					if (item.getLanguage().contains(defaultLanguage)) {
						item.getLanguage().addAll(retrievedItem.getLanguage());
					} else {
						retrievedItem.getLanguage().addAll(item.getLanguage());
						item = retrievedItem;
					}
				} else {
					if (item.getLanguage().contains(defaultLanguage)) {
						item.getLanguage().addAll(retrievedItem.getLanguage());
					} else {
						retrievedItem.getLanguage().addAll(item.getLanguage());
						item = retrievedItem;
					}
				}
			}
		}
		updateDocuments(searchItems, searchServerBulkUpload, solrServer);
	}
	

	/**
	 * Removes a list of documentes based on their ids in an own thread
	 */
	public void bulkRemoveElementByIds(List<String> ids,SearchServerBulkMessage searchServerBulkUploadMessage){
		SearchServerBulkRemoveThread searchServerBulkRemoveThread = new SearchServerBulkRemoveThread(solrServer, searchServerBulkUploadMessage);
		searchServerBulkRemoveThread.setRemoveItems(ids);
		searchServerBulkRemoveThread.start();
	}
	
	/**
	 * Removes a list of documentes based on their ids as items in an own thread
	 */
	public void bulkRemoveElement(List<Item> items,SearchServerBulkMessage searchServerBulkUploadMessage){
		SearchServerBulkRemoveThread searchServerBulkRemoveThread = new SearchServerBulkRemoveThread(solrServer, searchServerBulkUploadMessage);
		searchServerBulkRemoveThread.setRemoveSearchItems(items);
		searchServerBulkRemoveThread.start();
	}
	
	
	
	/**
	 * returns similar elements based on the id of the original element
	 * @param searchItem
	 * @return
	 */
	public ItemResponse getSimilarElementsByItemId(String id, String filterQuery, int maxResultCount){
		
		String queryString = createMLTQueryById(id);
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("q", queryString);
		params.set("fq", filterQuery);
		params.set("mlt", "true");
		params.set("mlt.fl", "tags,collection,name,description");
		params.set("mlt", "true");
		params.set("mlt.count", maxResultCount);
		params.set("mlt.mindf", "1");
		params.set("mlt.mintf", "1");
		
		QueryResponse queryResponse = null;
		try {
			 queryResponse= solrServer.query(params);
			
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		ItemResponse searchResponse = new ItemResponse();
		if(queryResponse!=null){
			searchResponse.setNumFound(queryResponse.getResults().getNumFound()-1);
			searchResponse.setElapsedTime(queryResponse.getElapsedTime());
			NamedList<Object> resLists = queryResponse.getResponse();
			Object mlt = resLists.get("moreLikeThis");
			SimpleOrderedMap<SolrDocumentList> listOfMltResults = (SimpleOrderedMap<SolrDocumentList>) mlt;
			List<Item> resultItems = new ArrayList<Item>();
			if (listOfMltResults.size() > 0) {
				SolrDocumentList mltsPerDoc = listOfMltResults.getVal(0);
				for (SolrDocument solrDocument : mltsPerDoc) {
					Item currentSearchItem = RecommendationQueryUtils.serializeSolrDocToSearchItem(solrDocument);
					if(!currentSearchItem.getId().equals(id))
						resultItems.add(currentSearchItem);
				}
			}
			searchResponse.setResultItems(resultItems);
		}
		return searchResponse;
	}
	
	/**
	 * returns similar elements based on the id of the original element
	 * @param searchItem
	 * @return
	 */
	public ItemResponse getSimilarElementsByItemId(String id) {
		return getSimilarElementsByItemId(id, "", 10);
	}
	
	private String createMLTQueryById(List<String> ids) {
		String queryMLTString = new String();
		ModifiableSolrParams params = new ModifiableSolrParams();
		
		StringBuilder itemQueryBuilder = new StringBuilder();
		int productCount = 0;
		for (String itemId : ids) {
			if (productCount >= RecommendationQueryUtils.MAX_SAME_PRODUCT_COUNT) {
				break;
			}
			itemQueryBuilder.append("id:(\"" + itemId + "\") OR ");
			productCount++;
		}
		if (ids.size() > 0) {
			itemQueryBuilder.replace(itemQueryBuilder.length() - 4, itemQueryBuilder.length(), "");
		}
		params.add("q", itemQueryBuilder.toString());
		QueryResponse queryResponse=null;
		try {
			queryResponse = solrServer.query(params);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		
		if(queryResponse!=null){
			List<Item> documentList =queryResponse.getBeans(Item.class);
			if(queryResponse.getResults() != null){
				if(documentList.size() > 0){
					Item concatenatedItem = new Item();
					concatenatedItem.setCollection(new ArrayList<String>());
					concatenatedItem.setTags(new ArrayList<String>());
					
					StringBuilder nameBuilder = new StringBuilder();
					StringBuilder descriptionBuilder = new StringBuilder();
					for (Item item : documentList) {
						nameBuilder.append(item.getName());
						descriptionBuilder.append(item.getDescription());
						concatenatedItem.getCollection().addAll(item.getCollection());
						concatenatedItem.getTags().addAll(item.getTags());
					}
					concatenatedItem.setName(nameBuilder.toString());
					concatenatedItem.setDescription(descriptionBuilder.toString());
					
					queryMLTString = searchItemToMLTQueryChunk(concatenatedItem);
				} else {
					throw new SolrException(ErrorCode.BAD_REQUEST, " given Id doesn't seem to be an unique or valid key");
				}
			} else {
				throw new SolrException(ErrorCode.BAD_REQUEST, " given Id doesn't seem to be an unique or valid key");
			}
		}
		
		return queryMLTString;
	}
	
	private String createMLTQueryById(String id) {
		String queryMLTString = new String();
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.add("q","id:"+id);
		QueryResponse queryResponse=null;
		try {
			queryResponse=solrServer.query(params);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		if(queryResponse!=null){
			List<Item> documentList =queryResponse.getBeans(Item.class);
			if(queryResponse.getResults()!=null){
				if(documentList.size()==1){
					Item searchItem= documentList.get(0);
					queryMLTString =searchItemToMLTQueryChunk(searchItem);
				}
				else
					throw new SolrException(ErrorCode.BAD_REQUEST, " given Id doesn't seem to be an unique or valid key");
			}else
				throw new SolrException(ErrorCode.BAD_REQUEST, " given Id doesn't seem to be an unique or valid key");
		}
		
		return queryMLTString;
	}

	private String searchItemToMLTQueryChunk(Item searchItem) {
		String query=new String();
		query +=getQueryChunkFromList(searchItem.getCollection(), "collection", query.isEmpty());
		query +=getQueryChunkFromList(searchItem.getTags(), "tags", query.isEmpty());
		query += (query.isEmpty())? "name:(\""+searchItem.getName()+"\")" : " AND name:(\""+searchItem.getName()+"\")";
		query += (query.isEmpty())? "description:(\""+searchItem.getDescription()+"\")" : " AND description:(\""+searchItem.getDescription()+"\")";
		return query;
	}

	/**
	 * returns similar elements based on the given SearchItem.
	 * only the fields tags collection description and name are used!
	 * @param searchItem
	 * @return
	 */
	public ItemResponse getSimilarElementsBySearchItem(Item searchItem){
		String queryString = searchItemToMLTQueryChunk(searchItem);
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("q", queryString);
		params.set("mlt", "true");
		params.set("mlt.fl", "tags,collection,name,description");
		params.set("mlt", "true");
		params.set("mlt.mindf", "1");
		params.set("mlt.mintf", "1");
		
		QueryResponse queryResponse = null;
		try {
			 queryResponse= solrServer.query(params);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		ItemResponse searchResponse = new ItemResponse();
		if(queryResponse!=null){
			searchResponse.setNumFound(queryResponse.getResults().getNumFound());
			searchResponse.setElapsedTime(queryResponse.getElapsedTime());
			NamedList<Object> resLists = queryResponse.getResponse();
			Object mlt = resLists.get("moreLikeThis");
			SimpleOrderedMap<SolrDocumentList> listOfMltResults = (SimpleOrderedMap<SolrDocumentList>) mlt;
			SolrDocumentList mltsPerDoc = listOfMltResults.getVal(0);
			List<Item> resultItems = new ArrayList<Item>();
			for (SolrDocument solrDocument : mltsPerDoc) {
				Item currentSearchItem = RecommendationQueryUtils.serializeSolrDocToSearchItem(solrDocument);
				resultItems.add(currentSearchItem);
			}
			searchResponse.setResultItems(resultItems);
		}
		return searchResponse;
	}

	@Override
	public void removeElementById(String id) {
		removeElementById(id, solrServer);
	}
	
	@Override
	public void removeElementByIds(List<String> ids) {
		removeElementByIds(ids, solrServer);
	}
}
