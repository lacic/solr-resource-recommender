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
	UB_Interests_MLT, 
	UB_CustomerGroups,
	UB_InterestsCustomerGroup, 
	UB_WithOutMLT, 
	CF_Social_Likes,
	CF_Social_Comments, 
	CF_Categories, 
	SocialStream, 
	CFPurchWithSocCommonNeighborhoodRecommender,
	CFPurchWithSocCommonNeighborhoodSummedRecommender,
	CFPurchWithSocCommonNeighborhoodReplacedRecommender, UB_WithOutMLTInterests, UB_WithOutMLTGroups, CF_Physical_Region_Distance, CF_Review, MostPopular_Review, BiographyBasedMLT, RealBiographyBasedMLT, WallPostInteraction, SnapshotInteraction, CF_Loc_CN, CF_Soc_Network_CN, CF_Soc_Network_Jaccard, CF_Soc_Network_NeighOverlap, CF_Soc_Network_AdamicAdar, CF_Soc_Network_PrefAttachment

}
