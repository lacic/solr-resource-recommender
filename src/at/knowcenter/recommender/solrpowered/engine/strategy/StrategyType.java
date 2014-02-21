package at.knowcenter.recommender.solrpowered.engine.strategy;

/**
 * Types of strategies for recommending product
 * @author elacic
 *
 */
public enum StrategyType {
	
	CollaborativeFiltering,
	ContentBased,
	MostPopular,
	PrecedingItemBased,
	CN_WeightName,
	CN_WeightDescription,
	CN_WeightTags,
	CN_WeightNameDescription,
	CN_WeightDescriptionName,
	CN_WeightNameTags,
	CN_WeightDescriptionTags,
	CN_WeightNameDescriptionTags,
	CN_WeightDescriptionNameTags, 
	CF_Social, 
	CF_Own_Social, 
	UB_Interests, 
	UB_CustomerGroups,
	UB_InterestsCustomerGroup, 
	UB_InterestsWithOutMLT, 
	CF_Social_Likes,
	CF_Social_Comments, 
	CF_Categories, SocialStream

}
