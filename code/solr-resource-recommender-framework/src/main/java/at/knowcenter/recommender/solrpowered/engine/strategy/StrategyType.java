package at.knowcenter.recommender.solrpowered.engine.strategy;

/**
 * Types of strategies for recommending resources
 * 
 * @author Emanuel Lacic
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
	UB_WithOutMLT, 
	CF_Social_Likes,
	CF_Social_Comments, 
	CF_Categories, 
	SocialStream, 
	CFPurchWithSocCommonNeighborhoodRecommender,
	CFPurchWithSocCommonNeighborhoodSummedRecommender,
	CFPurchWithSocCommonNeighborhoodReplacedRecommender, UB_WithOutMLTInterests, UB_WithOutMLTGroups

}
