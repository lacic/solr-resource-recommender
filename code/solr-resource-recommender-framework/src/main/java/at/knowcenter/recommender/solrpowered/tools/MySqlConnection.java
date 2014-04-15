package at.knowcenter.recommender.solrpowered.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class MySqlConnection {

	private Connection mysqlConnection = null;
	private Statement st = null;
	private ResultSet rs = null;
	
	
	/** Opens a connection to the Database server and keeps it alive until it is closed manually
	 * 
	 * @param url server url
	 * @param user username
	 * @param password password
	 * @return true if connection could be established, false otherwise
	 */
	public boolean createConnection(String url, String user, String password) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Cannot find the driver in the classpath!", e);
		}

		try {
			mysqlConnection = DriverManager.getConnection(url, user, password);
			st = mysqlConnection.createStatement();
		} catch (SQLException ex) {
			ex.printStackTrace();
			return false;

		}
		return true;
	}
	
	/** Executes the given query at the connected database
	 * 
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	public ResultSet runQuery(String query) throws SQLException {
		return st.executeQuery(query);
	}
	
	/** Shutdown the connection to the database server
	 * 
	 */
	public void closeConnection() {
		try {
			if (rs != null) {
				rs.close();
			}
			if (st != null) {
				st.close();
			}
			if (mysqlConnection != null) {
				mysqlConnection.close();
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
}
