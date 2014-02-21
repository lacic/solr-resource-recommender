package at.knowcenter.recommender.solrpowered.evaluation.metrics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
//		} else {
//			usersThatDidNotGetRecommended++;
//		}
	}
	
	public void appendDuaration(long duaration) {
		duarationSum += duaration;
	}
	
	public void exportCalculatedMetricsAverage(int size) {
//		System.out.println("Users:" + size + " Users without recommendations: " + usersThatDidNotGetRecommended + " %: " + (usersThatDidNotGetRecommended / size) * 100.0);
//		size = size - usersThatDidNotGetRecommended;
		try {
			File file = new File(outputDirectoryPath + algName + "_avg.txt");
			FileWriter writer = new FileWriter(file, true);
			
			BufferedWriter bw = new BufferedWriter(writer);		
			bw.write(Double.toString((recallSum / size)).replace('.', ',') + ";");		
			bw.write(Double.toString((precisionSum / size)).replace('.', ',') + ";");		
			bw.write(Double.toString((fMeasureSum / size)).replace('.', ',') + ";");		
			bw.write(Double.toString((mrrSum / size)).replace('.', ',') + ";");		
			bw.write(Double.toString((mapSum / size)).replace('.', ',') + ";");
			bw.write(Double.toString((diversitSum / size)).replace('.', ',') + ";");
			bw.write(Double.toString((serendipitySum / size)).replace('.', ',') + ";");
			bw.write(Double.toString((ndcgSum / size)).replace('.', ',') + ";");
			bw.write(Double.toString((userCoverageSum / size)).replace('.', ',') + ";");
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
	
}
