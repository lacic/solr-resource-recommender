package at.knowcenter.recommender.solrpowered.configuration;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class ConfigUtils {
	
	public static void loadConfiguration() {
		configInjector = Guice.createInjector(new RecommenderModule());
	}
	
	private static Injector configInjector = null;
	
	
	public static Injector getInjector() {
		return configInjector;
	}

}
