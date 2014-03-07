package at.knowcenter.recommender.solrpowered.configuration;

import at.knowcenter.recommender.solrpowered.engine.strategy.CFRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class RecommenderModule extends AbstractModule {
	 
	  @Override 
	  protected void configure() {
		    Multibinder<RecommendStrategy> recStrategyBinder = 
		    		Multibinder.newSetBinder(binder(), RecommendStrategy.class);
		    
		    recStrategyBinder.addBinding().to(CFRecommender.class);

	  }
	}