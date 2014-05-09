package at.knowcenter.recommender.solrpowered.evaluation.metrics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;


public class MetricsExporter {
	
	private String algName;

	// used for averages
	private double precisionSum = 0.0;
	private double recallSum = 0.0;
	private double fMeasureSum = 0.0;
	private double mrrSum = 0.0;
	private double mapSum = 0.0;
	private double diversitSum = 0.0;
	private double ndcgSum = 0.0;
	private double serendipitySum = 0.0;
	private double userCoverageSum = 0.0;
	private long duarationSum = 0;
	
	private List<Double> precisions = new ArrayList<Double>();
	private List<Double> recalls = new ArrayList<Double>();
	private List<Double> fMeasures =new ArrayList<Double>();
	private List<Double> mrrs = new ArrayList<Double>();
	private List<Double> maps = new ArrayList<Double>();
	private List<Double> diversities = new ArrayList<Double>();
	private List<Double> ndcgs = new ArrayList<Double>();
	private List<Double> serendipities = new ArrayList<Double>();
	private List<Integer> userCoverages = new ArrayList<Integer>();
	private List<Long> duarations = new ArrayList<Long>();

	private String outputDirectoryPath;

	private int usersThatDidNotGetRecommended = 0;
	
	public MetricsExporter(String outputDirectoryPath){
		if (! outputDirectoryPath.endsWith("/")) {
			outputDirectoryPath += "/";
		}
		this.outputDirectoryPath = outputDirectoryPath;
	}
	
	public void appendMetrics(MetricsExporter metricsExporter) {
		serendipitySum += metricsExporter.getSerendipitySum();
		diversitSum += metricsExporter.getDiversitSum();
		mapSum += metricsExporter.getMapSum();
		mrrSum += metricsExporter.getMrrSum();
		fMeasureSum += metricsExporter.getfMeasureSum();
		recallSum += metricsExporter.getRecallSum();
		precisionSum += metricsExporter.getPrecisionSum();
		ndcgSum += metricsExporter.getNdcgSum();
		userCoverageSum += metricsExporter.getUserCoverageSum();
		duarationSum += metricsExporter.getDuarationSum();
		
		precisions.addAll(		metricsExporter.getPrecisions());
		recalls.addAll(			metricsExporter.getRecalls());
		fMeasures.addAll(		metricsExporter.getfMeasures());
		mrrs.addAll(			metricsExporter.getMrrs());
		maps.addAll(			metricsExporter.getMaps());
		diversities.addAll(		metricsExporter.getDiversities());
		serendipities.addAll(	metricsExporter.getSerendipities());
		ndcgs.addAll(			metricsExporter.getNdcgs());
		duarations.addAll(		metricsExporter.getDuarations());
		userCoverages.addAll(	metricsExporter.getUserCoverages());
		
//		usersThatDidNotGetRecommended += metricsExporter.getUsersThatDidNotGetRecommended();
	}
	
	private long getDuarationSum() {
		return duarationSum;
	}

	public void appendMetrics(PredictionCalculator predictionData, SimilarityCalculator similarityData) {
//		if ( predictionData.getPredictionData().size() > 0) {
			double diversity = similarityData.calculateDiversity();
			double serendipity = similarityData.calculateSerendipity();
			
			serendipitySum += serendipity;
			diversitSum += diversity;
			mapSum += predictionData.getMAP();
			mrrSum += predictionData.getMRR();
			fMeasureSum += predictionData.getFMeasure();
			recallSum += predictionData.getRecall();
			precisionSum += predictionData.getPrecision();
			ndcgSum += predictionData.getNDCG();
			userCoverageSum += predictionData.getPredictionData().size() > 0 ? 1 : 0;
			
			precisions.add(		predictionData.getPrecision());
			recalls.add(		predictionData.getRecall());
			fMeasures.add(		predictionData.getFMeasure());
			mrrs.add(			predictionData.getMRR());
			maps.add(			predictionData.getMAP());
			diversities.add(	diversity);
			serendipities.add(	serendipity);
			ndcgs.add(			predictionData.getNDCG());
			userCoverages.add(  predictionData.getPredictionData().size() > 0 ? 1 : 0);
//		} else {
//			usersThatDidNotGetRecommended++;
//		}
	}
	
	public void appendDuaration(long duaration) {
		duarationSum += duaration;
		duarations.add(duaration);
	}
	
	public void exportCalculatedMetricsAverage(int size) {
//		System.out.println("Users:" + size + " Users without recommendations: " + usersThatDidNotGetRecommended + " %: " + (usersThatDidNotGetRecommended / size) * 100.0);
//		size = size - usersThatDidNotGetRecommended;
		try {
			File file = new File("/home/elacic/PhD/Projects/BlancNoir/evaluation/general_eval_data/" + algName + "_avg.txt");
			FileWriter writer = new FileWriter(file, true);
			
			BufferedWriter bw = new BufferedWriter(writer);		
			bw.write(Double.toString((recallSum / size)) + ";");		
			bw.write(Double.toString((precisionSum / size)) + ";");		
			bw.write(Double.toString((fMeasureSum / size)) + ";");		
			bw.write(Double.toString((mrrSum / size)) + ";");		
			bw.write(Double.toString((mapSum / size)) + ";");
			bw.write(Double.toString((diversitSum / size)) + ";");
			bw.write(Double.toString((serendipitySum / size)) + ";");
			bw.write(Double.toString((ndcgSum / size)) + ";");
			bw.write(Double.toString((userCoverageSum / size)) + ";");
			// from nano to ms
			long averageDuaration = (duarationSum / size) / 1000000;
//			bw.write(Integer.toString(size) + ";");
//			long averageDuaration = (duarationSum )/ 1000000;
			bw.write(Long.toString(averageDuaration) + "\n");
			bw.close();
			
			resetMetrics();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void exportCalculatedMetricsFoAll(int size, int k) {
		try {
			File file = new File("/home/elacic/PhD/Projects/BlancNoir/evaluation/general_eval_data/" + algName + "_statistic_" + k + ".txt");
			FileWriter writer = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(writer);		
			
			for (int i = 0; i < size; i++) {
				bw.write(Double.toString((recalls.get(i))) + ";");		
				bw.write(Double.toString((precisions.get(i))) + ";");		
				bw.write(Double.toString((fMeasures.get(i))) + ";");		
				bw.write(Double.toString((mrrs.get(i))) + ";");		
				bw.write(Double.toString((maps.get(i))) + ";");
				bw.write(Double.toString((diversities.get(i))) + ";");
				bw.write(Double.toString((serendipities.get(i))) + ";");
				bw.write(Double.toString((ndcgs.get(i))) + ";");
				bw.write(Double.toString((userCoverages.get(i))) + ";");
				long averageDuaration = (duarations.get(i)) / 1000000;
				bw.write(Long.toString(averageDuaration) + "\n");
			}
			bw.close();
			
			precisions = new ArrayList<Double>();
			recalls = new ArrayList<Double>();
			fMeasures =new ArrayList<Double>();
			mrrs = new ArrayList<Double>();
			maps = new ArrayList<Double>();
			diversities = new ArrayList<Double>();
			ndcgs = new ArrayList<Double>();
			serendipities = new ArrayList<Double>();
			userCoverages = new ArrayList<Integer>();
			duarations = new ArrayList<Long>();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void resetMetrics() {
		recallSum = 0.0;
		precisionSum = 0.0;
		fMeasureSum = 0.0;
		mrrSum = 0.0;
		mapSum = 0.0;
		diversitSum = 0.0;
		ndcgSum = 0.0;
		serendipitySum = 0.0;
		userCoverageSum = 0.0;
		duarationSum = 0;
		usersThatDidNotGetRecommended = 0;
	}

	public double getPrecisionSum() {
		return precisionSum;
	}

	public double getRecallSum() {
		return recallSum;
	}

	public double getfMeasureSum() {
		return fMeasureSum;
	}

	public double getMrrSum() {
		return mrrSum;
	}

	public double getMapSum() {
		return mapSum;
	}

	public double getDiversitSum() {
		return diversitSum;
	}

	public double getNdcgSum() {
		return ndcgSum;
	}

	public double getSerendipitySum() {
		return serendipitySum;
	}

	public double getUserCoverageSum() {
		return userCoverageSum;
	}

	public String getOutputDirectoryPath() {
		return outputDirectoryPath;
	}

	public String getAlgorithmName() {
		return algName;
	}

	public void setAlgorithmName(String algName) {
		this.algName = algName;
	}

	
	public int getUsersThatDidNotGetRecommended() {
		return usersThatDidNotGetRecommended;
	}

	public void setUsersThatDidNotGetRecommended(int usersThatDidNotGetRecommended) {
		this.usersThatDidNotGetRecommended = usersThatDidNotGetRecommended;
	}

	public List<Double> getPrecisions() {
		return precisions;
	}

	public List<Double> getRecalls() {
		return recalls;
	}

	public List<Double> getfMeasures() {
		return fMeasures;
	}

	public List<Double> getMrrs() {
		return mrrs;
	}

	public List<Double> getMaps() {
		return maps;
	}

	public List<Double> getDiversities() {
		return diversities;
	}

	public List<Double> getNdcgs() {
		return ndcgs;
	}

	public List<Double> getSerendipities() {
		return serendipities;
	}

	public List<Long> getDuarations() {
		return duarations;
	}

	public List<Integer> getUserCoverages() {
		return userCoverages;
	}

	
}
