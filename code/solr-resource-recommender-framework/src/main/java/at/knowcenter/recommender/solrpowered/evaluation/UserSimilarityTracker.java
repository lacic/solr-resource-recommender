package at.knowcenter.recommender.solrpowered.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.solr.client.solrj.response.FacetField.Count;


public class UserSimilarityTracker {
	
	/**
	 * Initialization-on-demand holder idiom
	 * <br/>
	 * {@link http://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom}
	 * @author elacic
	 *
	 */
	private static class Holder {
        static final UserSimilarityTracker INSTANCE = new UserSimilarityTracker();
    }

	public static UserSimilarityTracker getInstance() {
        return Holder.INSTANCE;
    }

	private UserSimilarityTracker() {
	}
	
	private static final String PATH = "/home/elacic/PhD/Projects/BlancNoir/evaluation/general_eval_data";
	
	public synchronized void writeToFile(String fileName, List<String> contents) {
//		try {
//			File file = new File(PATH + "/" + fileName + "_user_sim.csv");
//			FileWriter fw = new FileWriter(file, true);
//			String csvContent = "";
//			for (String content : contents) {
//				csvContent += content + ";";
//			}
//			fw.write(csvContent + "\n");
//			fw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	public synchronized void writeToFile(String fileName, String user, Map<String, Double> commonNeighborMap) {
//		try {
//			File file = new File(PATH + "/" + fileName + "_user_sim.csv");
//			FileWriter fw = new FileWriter(file, true);
//			for (String neighbor : commonNeighborMap.keySet()) {
//				fw.write(user + ";" + neighbor + ";" + commonNeighborMap.get(neighbor) + ";" + "\n");
//			}
//			fw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	public synchronized void writeToFileInt(String fileName, String user, Map<String, Integer> commonNeighborMap) {
//		try {
//			File file = new File(PATH + "/" + fileName + "_user_sim.csv");
//			FileWriter fw = new FileWriter(file, true);
//			for (String neighbor : commonNeighborMap.keySet()) {
//				fw.write(user + ";" + neighbor + ";" + commonNeighborMap.get(neighbor) + ";" + "\n");
//			}
//			fw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	public void writeToFile(String fileName, String currentUser,
			List<Count> userOccurences, Double weightDividor) {
//		
//		try {
//			File file = new File(PATH + "/" + fileName + "_user_sim.csv");
//			FileWriter fw = new FileWriter(file, true);
//			
//			for (Count userOccurence : userOccurences) {
//				if ( ! userOccurence.getName().equals(currentUser)) {
//					fw.write(currentUser + ";" + userOccurence.getName() + ";" + (userOccurence.getCount() / weightDividor ) + ";" + "\n");
//				}
//			}
//			
//			fw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	
}
