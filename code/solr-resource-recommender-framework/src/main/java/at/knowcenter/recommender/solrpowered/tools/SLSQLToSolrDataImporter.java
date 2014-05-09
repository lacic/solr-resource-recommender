package at.knowcenter.recommender.solrpowered.tools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import at.knowcenter.recommender.solrpowered.model.Customer;
import at.knowcenter.recommender.solrpowered.model.Position;
import at.knowcenter.recommender.solrpowered.model.Resource;
import at.knowcenter.recommender.solrpowered.model.Review;
import at.knowcenter.recommender.solrpowered.model.SocialAction;
import at.knowcenter.recommender.solrpowered.model.SocialStream;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.bulk.SearchServerBulkMessage;

public class SLSQLToSolrDataImporter {

	private  final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private static String url = "jdbc:mysql://localhost:3306/";
	private static String user = "root";
	private static String password = "root";
	
	private  List<Integer> productIds; 
	
	private final static int elems = 113814;
	static MySqlConnection conhandler = null;
	
	private  HashMap<String, List<String>> customerGroups;
	private  Map<Integer, List<String>> categories;
	private  Map<Integer, List<Review>> reviewCommentMap = new HashMap<Integer, List<Review>>();
	private  Map<Integer, List<Review>> reviewMap = new HashMap<Integer, List<Review>>();
	private  Map<Integer, Map<String,String>> sellerLocations = new HashMap<Integer, Map<String,String>>();
	private  Map<String, List<String>> userPicks = new HashMap<String, List<String>>();

	
	private Map<Integer, List<Resource>> storeItems = new HashMap<Integer,List<Resource>>();

	
	public static void main(String[] args) {
		SLSQLToSolrDataImporter importer = new SLSQLToSolrDataImporter();

		boolean running = importer.startConnection();
		if (!running) {
			System.out.println("MySQL not runnign");
			return;
		}

		importer.fillPicksFromFile();

		importer.createReviewsFromSQLDB();
		importer.createCategoriesFromSQLDB();
		importer.createStoresFromSQLDB();	
		importer.createResourcesFromSQLDB();
		importer.createCustomersFromMySQLDatabase();
		importer.createSocialInteractionsFromSQLDatabase();
		importer.createSocialStreamFromSQLDatabase();
		importer.createCustomersFromMySQLDatabase();
		importer.createPositionsFromMySQLDatabase();

		importer.closeConnection();
	}
	
	public boolean startConnection() {
		conhandler = new MySqlConnection();
		boolean running = conhandler.createConnection(url, user, password);
		return running;
	}
	
	public void closeConnection() {
		conhandler.closeConnection();
	}
	
	public void fillPicksFromFile() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader("/home/elacic/avatar_regions_picks.csv"));
			
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				
				String[] splittedLine = line.split(",\"\\[|.\\[");
				
				String[] picks = splittedLine[1].split("\\]|\\]\"");
				String[] splittedPicks = picks[0].replace("'", "").split(",");
				
				List<String> trimmedPicks = new ArrayList<String>();
				for (int i = 0; i < splittedPicks.length; i++) {
					trimmedPicks.add(splittedPicks[i].trim());
				}
				
				userPicks.put(splittedLine[0], trimmedPicks);
			}
			
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
	}

	public void createPositionsFromMySQLDatabase() {
		try {
			Map<String, String> uuidAvatarMap = new HashMap<String,String>();
			String query = "SELECT a.avatar, a.uuid FROM test.about AS a";
			ResultSet rs = conhandler.runQuery(query);
			while(rs.next()) {
				uuidAvatarMap.put(
						rs.getString("uuid").substring(1, rs.getString("uuid").length()), 
						rs.getString("avatar").replace("'", ""));
			}

			System.out.println(uuidAvatarMap.keySet().size());
			
			createPositionsFromTable("positions_robot_1203", uuidAvatarMap);
			createPositionsFromTable("positions_robot_1204", uuidAvatarMap);
			createPositionsFromTable("positions_robot_1205", uuidAvatarMap);
			createPositionsFromTable("positions_robot_1206", uuidAvatarMap);
			createPositionsFromTable("positions_robot_1207", uuidAvatarMap);
			createPositionsFromTable("positions_robot_1208", uuidAvatarMap);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void createPositionsFromTable(String positionsTableName, Map<String, String> uuidAvatarMap)
			throws SQLException {
		String query = "SELECT * FROM test." + positionsTableName + " AS p ";
		ResultSet rs = conhandler.runQuery(query);

		
		List<Position> positions = new ArrayList<>();
		while(rs.next()) {
			String userUUID = rs.getString("avatar").substring(0, rs.getString("avatar").length() -1);
			
			if (userUUID != null &&  uuidAvatarMap.get(userUUID) != null) {
				Position position = new Position();

				position.setUser(uuidAvatarMap.get(userUUID));
				position.setId(rs.getString("nr"));
				position.setRegionId(rs.getLong("regionhandle"));
				position.setTime(rs.getDate("time"));
				position.setRegionName(rs.getString("regionname"));
				Integer yLocal = rs.getInt("ylocal");
				Integer xLocal = rs.getInt("xlocal");
				position.setLocationInRegion(xLocal + " " + yLocal);
				position.setzLocal(rs.getInt("zlocal"));
				
				Integer xGlobal = rs.getInt("xglobal");
				Integer yGlobal = rs.getInt("yglobal");
				
				
				position.setRegionLocation(xGlobal + " " + yGlobal);
				position.setGlobalLocation((xGlobal*256 + xLocal) + " " + (yGlobal*256 + yLocal));
				
				positions.add(position);
			}
		}
		
		int positionsSize = positions.size();
		int increment = 70000;
		for (int i = 0; i < positionsSize; i += increment) {
			int toIndex = (i + increment > positionsSize) ? positionsSize : i + increment;
			done = (double)i / positionsSize;
			SolrServiceContainer.getInstance().getPositionService().writeDocuments(positions.subList(i, toIndex), new SearchServerBulkMessage() {
				@Override
				public void returnStatus(String message) {
					System.out.println("Wrote resource batch. " + message + " Done: " + done + " %");
				}
			});
		}
	}

	public void createStoresFromSQLDB() {
		String query = "";
		try {
			query = "SELECT count(*) FROM test.stores";
			ResultSet rs = conhandler.runQuery(query);
			rs.next();
			int numberOfProducts = rs.getInt(1);
			System.out.println(numberOfProducts);
			query = "SELECT * from test.stores";
			rs = conhandler.runQuery(query);

			while(rs.next()) {
				Map<String,String> sellerMap = new HashMap<String, String>();
				sellerMap.put("owner_name", rs.getString("owner_name"));
				sellerMap.put("date", rs.getString("join_date"));

				String storeLocation = rs.getString("store_location");
				if (!storeLocation.trim().equals("")) {
					// replace whitespace escape character
					storeLocation = storeLocation.replaceAll("%20", " ");
					// remove all escape characters
					storeLocation = storeLocation.replaceAll("%..", "");

					String[] storeAddress = storeLocation.split("/");
					String region = storeAddress[0].trim();

					if (storeAddress.length >= 3 && !region.equals("")) {
						String x = storeAddress[1].trim();
						String y = storeAddress[2].trim();

						if (!x.equals("") && !y.equals("")) {
							try{
								if (Double.parseDouble(y) < 256) {
									sellerMap.put("region", region);
									sellerMap.put("location", x + " " + y);
								}

							} catch (Exception ex) {
								System.out.println("exs");
								System.out.println(region + " " + x + " " + y);
							}
						}
					}

				}

				sellerLocations.put(rs.getInt("store_id"), sellerMap);
			}


		} catch (SQLException e) {
		}
	}

	public void createCategoriesFromSQLDB() {
		try {
			categories = createCategories(conhandler.runQuery("SELECT * FROM test.categories;"));
			System.out.println("Created " + categories.size() + " store categorie mappings");
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	/** Fetch data from marketplace database and extract item information from it 
	 * 
	 */
	public void createResourcesFromSQLDB() {
		String query = "";
		try {
			query = "SELECT count(*) FROM test.products";
			ResultSet rs = conhandler.runQuery(query);
			rs.next();
			int numberOfProducts = rs.getInt(1);
			System.out.println("Num of products: " + numberOfProducts);
			
			int iteratingCount = 300000;
			
			List<Resource> resources = null;
			DateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");

			for (int productCount = 0; productCount < 2162500; productCount+= iteratingCount) {
				query = "select product_id, product_name, product_price, product_description, product_rating, store_id "
						+ "from test.products LIMIT " + productCount + "," + (productCount + iteratingCount);
				rs = conhandler.runQuery(query);
				System.out.println(query);
				
				resources = new ArrayList<Resource>();
				
				while (rs.next()){
					Resource resource = new Resource();
					
					resource.setItemId(rs.getString("product_id"));
					resource.setItemName(rs.getString("product_name"));
					resource.setDescription(rs.getString("product_description"));
					resource.setPrice(rs.getDouble("product_price"));
					resource.setManufacturer(String.valueOf(rs.getInt("store_id")));
					resource.setCurrency("EUR");
					
					List<String> resourceCategories = categories.get(rs.getInt("store_id"));
					if (resourceCategories != null) {
						resource.setTags(resourceCategories);
					}
					
					List<Review> productReviews = reviewMap.get(rs.getInt("product_id"));
					if (productReviews != null) {
						for (Review rev : productReviews) {
							if (rev.getReviewType().equals(Review.REVIEW)) {
								if (rev.getRating() == 5) {
									resource.addUserRated5(rev.getUser());
								} else if (rev.getRating() == 4) {
									resource.addUserRated4(rev.getUser());
								} else if (rev.getRating() == 3) {
									resource.addUserRated3(rev.getUser());
								} else if (rev.getRating() == 2) {
									resource.addUserRated2(rev.getUser());
								} else if (rev.getRating() == 1) {
									resource.addUserRated1(rev.getUser());
								} 
							}
						}
					}
					
					Map<String, String> sellerInfo = sellerLocations.get(rs.getInt("store_id"));
					if (sellerInfo != null) {
						resource.setSeller(sellerInfo.get("owner_name"));
						resource.setSellingRegion(sellerInfo.get("region"));
						resource.setLocationInRegion(sellerInfo.get("location"));
						
						try {
							String join_date = sellerInfo.get("date");
							if (join_date != null && ! join_date.trim().equals("")) {
								resource.setValidFrom((Date)formatter.parse(join_date));
							}
						} catch (ParseException e) {
							e.printStackTrace();
						}
						
						
						
						
						}
					
					resources.add(resource);
				}
				
				int resourceSize = resources.size();
				int increment = 30000;
				for (int i = 0; i < resourceSize; i += increment) {
					int toIndex = (i + increment > resourceSize) ? resourceSize : i + increment;
					done = (double)i / resourceSize;
					SolrServiceContainer.getInstance().getResourceService().writeDocuments(resources.subList(i, toIndex), new SearchServerBulkMessage() {
						@Override
						public void returnStatus(String message) {
							System.out.println("Wrote resource batch. " + message + " Done: " + done + " %");
						}
					});
				}
				
				System.out.println("Wrote file " + ((double)productCount / numberOfProducts) + " %");
			}
			
		} catch (SQLException e) {
		}		
	}
	private double done = 0;
	/** Fetch data from profiled database and extract user information from it 
	 * 
	 */
	public void createCustomersFromMySQLDatabase() {
		String query = "";
		try {
			query = "select avatar, interest from test.interests";
			createCustomerInterests(conhandler.runQuery(query));
			
			query = "select avatar, groupUUID from test.groups";
			customerGroups = createCustomerGroupRelation(conhandler.runQuery(query));
			
			query = "select avatar, birthday, partner, biography, real_biography from test.about";
			ResultSet rs = conhandler.runQuery(query);
			List<Customer> customers = createCustomers(rs);
			
			int resourceSize = customers.size();
			int increment = 45000;
			for (int i = 0; i < resourceSize; i += increment) {
				int toIndex = (i + increment > resourceSize) ? resourceSize : i + increment;
				done = (double)i / resourceSize;
				SolrServiceContainer.getInstance().getUserService().writeDocuments(customers.subList(i, toIndex), new SearchServerBulkMessage() {
					@Override
					public void returnStatus(String message) {
						System.out.println("Wrote resource batch. " + message + " Done: " + done + " %");
					}
				});
			}
			
		} catch (SQLException e) {
		}

	}
	
	private Map<String, String> contentCustomerInterests = new HashMap<String, String>();
	private Map<String, Set<String>> customerInterests = new HashMap<String, Set<String>>();

	private List<String> usersToPersist;

	/** create a map that contains the mapping between a customer and his personal interests  
	 * @throws SQLException 
	 * 
	 */
	private void createCustomerInterests(ResultSet rs) throws SQLException {

		while (!rs.isLast()) {
			rs.next();
			String user = rs.getString(1).replace("'", "");
			String interest = rs.getString(2).
					replace("'", "").
					replace("http://maps.secondlife.com/secondlife/", "").
					replace("%20", " ").
					replace("/", " ").
					replace(","," ");
			
			if (contentCustomerInterests.containsKey(user)) {
				contentCustomerInterests.put(user, contentCustomerInterests.get(user) + " " + interest);
			} else {
				contentCustomerInterests.put(user, interest);
			}
			
			String[] inter = interest.split("\\s*(and\\s|or\\s|,|\\s+|&amp|\\||;|\\(|\\)|/)\\s*");
			List<String> interests = new ArrayList<String>(Arrays.asList(inter));
			
			if (customerInterests.containsKey(user)) {
				customerInterests.get(user).addAll(interests);
			} else {
				HashSet<String> list = new HashSet<String>();
				list.addAll(interests);
				customerInterests.put(user, list);
			}
		}
	}

	/** create a map that contains the mapping of which user belongs to which group (can be used for social recommendations)  
	 * 
	 */
	private HashMap<String, List<String>> createCustomerGroupRelation(
			ResultSet rs) throws SQLException {
		HashMap<String, List<String>> userGroupsRelation = new HashMap<String, List<String>>();
		while (!rs.isLast()) {
			rs.next();
			String user = rs.getString(1);
			String group = rs.getString(2);
			if (userGroupsRelation.containsKey(user)) {
				if (!userGroupsRelation.get(user).contains(group)) {
					userGroupsRelation.get(user).add(group);
				}
			} else {
				ArrayList<String> list = new ArrayList<String>();
				list.add(group);
				userGroupsRelation.put(user, list);
			}
		}
		return userGroupsRelation;
	}

	/** 
	 * Fetch data from marketplace database and extract customer action information from it 
	 */
	public void createReviewsFromSQLDB() {
		String query = "";
		try{
			query = "SELECT * FROM test.comments ORDER BY date asc";
			ResultSet commentRs = conhandler.runQuery(query);
			createReviewComments(commentRs);
			
			query = "select product_id, name, date, rating, review_id, review from test.reviews ORDER BY date asc";
			ResultSet rs = conhandler.runQuery(query);
			createReviews(rs);
//			
//			for (List<Review> reviews : reviewMap.values()) {
//				SolrServiceContainer.getInstance().getReviewService().writeDocuments(reviews, new SearchServerBulkMessage() {
//					@Override
//					public void returnStatus(String message) {
//						System.out.println("Wrote reviews batch. " + message + " Done: " + done + " %");
//					}
//				});
//			}
		} catch (SQLException e) {
		}
		System.out.println("Created reviews for " + reviewMap.keySet().size() + " products");
	}

	/**
	 * Create comments that are linked to a review
	 * @param commentRs set for iterating over comments
	 * @throws SQLException
	 */
	private void createReviewComments(ResultSet commentRs) throws SQLException {
		
		
		while (commentRs.next()) {
			String commenter = commentRs.getString("name").trim().toLowerCase().replace(" ",".");
			if (commenter.equals("belladna.mocha")) {
				commenter = "belladonna.mocha";
			}
			
			if (usersToPersist == null || usersToPersist.contains(commenter)) {
				Review comment = new Review();
				int rid = commentRs.getInt("rid");
				
				comment.setDate(commentRs.getDate("date"));
				comment.setReview(commentRs.getString("comment"));
				
				
				comment.setUser(commenter);
				comment.setReviewType(Review.COMMENT);
				
				List<Review> comments = reviewCommentMap.get(rid);
				if (comments == null) {
					comments = new ArrayList<Review>();
				}
				comment.setId(String.valueOf(rid) + "_" + comments.size());
				
				comments.add(comment);
				
				
				reviewCommentMap.put(rid, comments);
			}
		}
	}

	/** Fetch social data from profiles database
	 * 
	 */
	public List<SocialAction> createSocialInteractionsFromSQLDatabase() {
		List<SocialAction> socialActions = null;
		String query = "";
		try {
			query = "select * from test.feed where type = 'LOVE' ORDER BY time asc";
			ResultSet loveSet = conhandler.runQuery(query);
			Map<String, List<String>> loves = createRelations(loveSet);
			
			query = "select * from test.feed where type = 'COMMENT' ORDER BY time asc";
			ResultSet commentSet = conhandler.runQuery(query);
			Map<String, List<String>> comments = createRelations(commentSet);
			
			query = "select * from test.feed where type = 'WALLPOST' ORDER BY time asc";
			ResultSet wallpostSet = conhandler.runQuery(query);
			Map<String, List<String>> wallposts = createRelations(wallpostSet);
			
			query = "select * from test.feed where type = 'SNAPSHOT' ORDER BY time asc";
			ResultSet snapshotSet = conhandler.runQuery(query);
			Map<String, List<String>> snapshots = createRelations(snapshotSet);
			
			socialActions = createSocialInteractions(loves, comments, wallposts, snapshots);
			
			SolrServiceContainer.getInstance().getSocialActionService().writeDocuments(socialActions, new SearchServerBulkMessage() {
				@Override
				public void returnStatus(String message) {
					System.out.println("Wrote reviews batch. " + message + " Done: " + done + " %");
				}
			});
		} catch (SQLException e) {
		}
		return socialActions;
	}

	/** Create all social information based in wall interactions
	 */
	public void createSocialStreamFromSQLDatabase() {
		String query = "";
		try {
			query = "select * from test.feed where type = 'WALLPOST' and data <> ''";
			ResultSet wallpostResult = conhandler.runQuery(query);
			List<SocialStream> wallposts = createSocialInteraction(wallpostResult, "WALLPOST");
			
			int resourceSize = wallposts.size();
			int increment = 50000;
			
			for (int i = 0; i < resourceSize; i += increment) {
				int toIndex = (i + increment > resourceSize) ? resourceSize : i + increment;
				done = (double)i / resourceSize;
				SolrServiceContainer.getInstance().getSocialStreamService().writeDocuments((wallposts.subList(i, toIndex)), new SearchServerBulkMessage() {
					@Override
					public void returnStatus(String message) {
						System.out.println("Wrote reviews batch. " + message + " Done: " + done + " %");
					}
				});
			}
			

			
			query = "select * from test.feed where type = 'COMMENT' and data <> ";
			ResultSet commentsResults = conhandler.runQuery(query);
			List<SocialStream> comments = createSocialInteraction(commentsResults, "COMMENT");
			
			resourceSize = comments.size();
			
			for (int i = 0; i < resourceSize; i += increment) {
				int toIndex = (i + increment > resourceSize) ? resourceSize : i + increment;
				done = (double)i / resourceSize;
				SolrServiceContainer.getInstance().getSocialStreamService().writeDocuments((comments.subList(i, toIndex)), new SearchServerBulkMessage() {
					@Override
					public void returnStatus(String message) {
						System.out.println("Wrote reviews batch. " + message + " Done: " + done + " %");
					}
				});
			}
			
			query = "select * from test.feed where type = 'SNAPSHOT' and data <> ''";
			ResultSet snapshotResult = conhandler.runQuery(query);
			List<SocialStream> snapshots = createSocialInteraction(snapshotResult, "SNAPSHOT");
			

			resourceSize = snapshots.size();
			
			for (int i = 0; i < resourceSize; i += increment) {
				int toIndex = (i + increment > resourceSize) ? resourceSize : i + increment;
				done = (double)i / resourceSize;
				SolrServiceContainer.getInstance().getSocialStreamService().writeDocuments((snapshots.subList(i, toIndex)), new SearchServerBulkMessage() {
					@Override
					public void returnStatus(String message) {
						System.out.println("Wrote reviews batch. " + message + " Done: " + done + " %");
					}
				});
			}
			
		} catch (SQLException e) {
		}
		
	}

	/** Create the list of all existing product ids within the marketplace database
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private List<Integer> createProductIdList(ResultSet rs) throws SQLException {
		List<Integer> ids = new ArrayList<Integer>();
		while (rs.next()) {
			ids.add(rs.getInt(1));
		}
		return ids;
	}

	/** Fetch data from marketplace database which product categories are available 
	 * 
	 */
	private Map<Integer, List<String>> createCategories(ResultSet rs) throws SQLException {
		Map<Integer, List<String>> categories = new HashMap<Integer, List<String>>();
		while(rs.next()) {
			String cat = rs.getString(1);
			Integer storeID = rs.getInt(2);
			if (categories.containsKey(storeID)) {
				categories.get(storeID).add(cat);
			} else {
				List<String> category = new ArrayList<String>();
				category.add(cat);
				categories.put(storeID, category);
			}
		}
		return categories;
	}


	/** create a list of {@link Customer} objects out of the information retrieved from the database.
	 *  Some fields are filled with the values taken from the database, unknown (or not existing)
	 *  information is randomly generated
	 * 
	 * @param rs
	 * @return
	 */
	private List<Customer> createCustomers(ResultSet rs) {
		List<Customer> customers = new ArrayList<Customer>();
		SimpleDateFormat df = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
		try {
			while(!rs.isLast()) {
				rs.next();
				Customer customer = new Customer();
				customer.setId(rs.getString("avatar").replace("'", ""));
				
				if (userPicks.containsKey(customer.getId())){
					customer.setFavoriteRegions(userPicks.get(customer.getId()));
				}
				
				String birthday = rs.getString("birthday").replace("'", "");
				if (birthday.length() > 0) {
					try {
						customer.setDateOfBirth(df.parse(birthday));
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
				
				String partner = rs.getString("partner");
				if (partner != null && ! partner.equals("NULL")) {
					customer.setFriendOf(Arrays.asList(partner.replace("'", "")));
				}
				
				String unprocessedBiography = rs.getString("biography");
				if (unprocessedBiography != null && unprocessedBiography.length() > 2) {
					String biography = unprocessedBiography.
						substring(1, unprocessedBiography.length()-1).
						replace("\\'","'").
						replace("\\n"," ").
						replace("http://maps.secondlife.com/secondlife/", "").
						replace("%20", " ").
						replace(","," ");
					
					customer.setBiography(biography);
				}
				
				String unprocessedRealBiography = rs.getString("real_biography");
				if (unprocessedRealBiography != null && unprocessedRealBiography.length() > 2) {
					String realBiography = unprocessedRealBiography.
						substring(1, unprocessedRealBiography.length()-1).
						replace("\\'","'").
						replace("\\n"," ").
						replace("http://maps.secondlife.com/secondlife/", "").
						replace("%20", " ").
						replace(","," ");
					
					customer.setRealBiography(realBiography);
				}
				
				String contentInterests = contentCustomerInterests.get(customer.getId());
				customer.setInterestsContent(contentInterests);

				Set<String> interests = customerInterests.get(customer.getId());
				if (interests != null) {
					customer.setInterests(new ArrayList<String>(interests));
				} else {
					customer.setInterests(new ArrayList<String>());
				}
				
				
				customer.setCustomergroup(customerGroups.get(customer.getId()));
				customers.add(customer);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return customers;
	}

	/** create a list of {@link CustomerActionInternal} objects out of the information retrieved from the database.
	 * 
	 * @param rs
	 * @return
	 */
	private void createReviews(ResultSet rs) {
		try{
			while(rs.next()) {
				String denormalizedUser = rs.getString("name");
				String normalizedUser = denormalizedUser.trim().toLowerCase().replace(" ",".");
				
				if (usersToPersist == null || usersToPersist.contains(normalizedUser)) {
					Integer productId = rs.getInt("product_id");
					int rating = rs.getInt("rating");
					Integer reviewId = rs.getInt("review_id");
					Date date = rs.getDate("date");
					String review = rs.getString("review");
					
					Review rev = new Review();
					
					rev.setId(String.valueOf(reviewId));
					rev.setItemId(String.valueOf(productId));
					rev.setDate(date);
					rev.setRating(rating);
					rev.setReview(review);
					rev.setUser(normalizedUser);
					rev.setReviewType(Review.REVIEW);
					
					List<Review> reviews = reviewMap.get(productId);
					if (reviews == null) {
						reviews = new ArrayList<Review>();
					}
					
					reviews.add(rev);
					
					List<Review> reviewComments = reviewCommentMap.get(reviewId);
					if (reviewComments != null) {
						for (Review comment : reviewComments) {
							comment.setItemId(String.valueOf(productId));
							reviews.add(comment);
						}
					}
					
					reviewMap.put(productId, reviews);
				}
				
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/** Create a mapping for user interactions based on the information retrieved from the database
	 * 
	 * @param rs
	 * @return
	 */
	private HashMap<String, List<String>> createRelations(ResultSet rs) {
		HashMap<String, List<String>> relations = new HashMap<String, List<String>>();
		try {
			while (!rs.isLast()) {
				rs.next();
				String source = rs.getString("source");
				String destination = rs.getString("destination");
				if (source.equals(destination)) {
					continue;
				}
				if (usersToPersist != null && 
						(!usersToPersist.contains(destination))){
					continue;
				}
				if (relations.get(destination) == null) {
					List<String> friends = new ArrayList<String>();
					friends.add(source);
					relations.put(destination, friends);
				} else {
					//List<String> friends = relations.get(destination);
					//if (!friends.contains(source)) {
					relations.get(destination).add(source);
					//}
				}
			}
			return relations;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** extract the wall interactions 
	 * 
	 * @param interactionResult list of all wallposts
	 * @param comments list of all comments made on wallposts
	 * @return
	 */
	private List<SocialStream> createSocialInteraction(ResultSet rs, String type) {
		List<SocialStream> interactions = new ArrayList<SocialStream>();
		try {
			while (!rs.isLast()) {
				rs.next();
				
				String actionId = rs.getString("id");
				String source = rs.getString("source");
				String target = rs.getString("destination");
				String content = rs.getString("data");
				Date timestamp = df.parse(rs.getString("time"));
				
				if (content != null && content.trim().length() > 0) {
					SocialStream interaction = new SocialStream();

					interaction.setActionId(actionId);
					interaction.setSocialContent(content);
					interaction.setDatasource("SL");
					interaction.setActionType(type);
					interaction.setSourceUserId(source);
					interaction.setTargetActionId(null);
					interaction.setTargetUserId(target);
					interaction.setTimestamp(timestamp);
					
					interactions.add(interaction);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ParseException e) {
		}
		return interactions;
	}

	/** Create a list of {@link SocialAction} objects from the given relationship information
	 * 
	 * @param loves
	 * @param comments
	 * @return
	 */
	private List<SocialAction> createSocialInteractions(
			Map<String, List<String>> loves,
			Map<String, List<String>> comments,
			Map<String, List<String>> wallposts,
			Map<String, List<String>> snapshots) {
		
		Map<String, SocialAction> actions = new HashMap<String, SocialAction>();
		
		for (String destination : loves.keySet()) {
			SocialAction newAction = new SocialAction();

			newAction.setUserId(destination);
			newAction.setUsersThatLikedMe(loves.get(destination));

			actions.put(destination, newAction);
		}
		
		for (String destination : comments.keySet()) {
			if (actions.containsKey(destination)) {
				actions.get(destination).setUsersThatCommentedOnMyPost(comments.get(destination));
			} else {
				SocialAction newAction = new SocialAction();
				
				newAction.setUserId(destination);
				newAction.setUsersThatCommentedOnMyPost(comments.get(destination));
				actions.put(destination, newAction);				
			}
		}
		
		for (String destination : wallposts.keySet()) {
			if (actions.containsKey(destination)) {
				actions.get(destination).setUsersThatPostedOnMyWall(wallposts.get(destination));
			} else {
				SocialAction newAction = new SocialAction();
				
				newAction.setUserId(destination);
				newAction.setUsersThatPostedOnMyWall(wallposts.get(destination));
				actions.put(destination, newAction);				
			}
		}
		
		
		for (String destination : snapshots.keySet()) {
			if (actions.containsKey(destination)) {
				actions.get(destination).setUsersThatPostedASnapshopToMe(snapshots.get(destination));
			} else {
				SocialAction newAction = new SocialAction();
				
				newAction.setUserId(destination);
				newAction.setUsersThatPostedASnapshopToMe(snapshots.get(destination));
				actions.put(destination, newAction);				
			}
		}
		
		
		return new ArrayList<SocialAction>(actions.values());
	}

	public void setUsersToPersist(List<String> positionUsers) {
		this.usersToPersist = positionUsers;
	}
	
}
