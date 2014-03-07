package at.knowcenter.recommender.solrpowered.engine.filtering;

/**
 * Defines the evaluation method for preceding items.
 * Each method denotes if from the found following items all, only half, 20 %  or none 
 * of the already got recommendations should shifted to the right with a lower rank
 * @author elacic
 *
 */
public enum PrecedingItemEvaluation {
	
	MAX_ALL_AS_RESULT, 
	MAX_HALF_AS_RESULT,
	MAX_20_PERCENT_AS_RESULT,
	NOTHING

}
