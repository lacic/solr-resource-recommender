package at.knowcenter.recommender.solrpowered.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.QueryResponse;

import com.google.inject.Guice;
import com.google.inject.Injector;

import at.knowcenter.recommender.solrpowered.configuration.ConfigUtils;
import at.knowcenter.recommender.solrpowered.configuration.RecommenderModule;
import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.filtering.PrecedingItemEvaluation;
import at.knowcenter.recommender.solrpowered.engine.strategy.RecommendStrategy;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.RegionsDaysSeenBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.RegionsDaysSeenBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.distance.RegionsPhysicalDistance3DBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.distance.RegionsPhysicalDistanceBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.distance.puresocial.RegionsPhysicalDistanceBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.picks.PicksCommonNeighborBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.picks.PicksJaccardBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.picks.PicksTotalBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.picks.puresocial.PicksCommonNeighborBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.picks.puresocial.PicksJaccardBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.picks.puresocial.PicksTotalBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.regions.CommonRegionsBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.regions.CommonRegionsJaccardBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.regions.CommonRegionsTotalBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.regions.puresocial.CommonRegionsBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.regions.puresocial.CommonRegionsJaccardBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.regions.puresocial.CommonRegionsTotalBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.shared.SharedRegionsCommonBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.shared.SharedRegionsJaccardBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.shared.SharedRegionsTotalBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.shared.puresocial.SharedRegionsCommonBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.shared.puresocial.SharedRegionsJaccardBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.content.shared.puresocial.SharedRegionsTotalBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.global.LocationAdarBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.global.LocationCommonNeighborBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.global.LocationJaccardBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.global.LocationOverlapBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.global.LocationPrefAttBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.global.coocurred.LocationCoocurredCommonNeighborBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.global.coocurred.LocationCoocurredJaccardBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.region.RegionAdarBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.region.RegionCommonNeighborBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.region.RegionJaccardBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.region.RegionOverlapBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.region.RegionPrefAttBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.region.coocurred.RegionCoocurredAdarBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.region.coocurred.RegionCoocurredCommonNeighborBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.region.coocurred.RegionCoocurredJaccardBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.region.coocurred.RegionCoocurredOverlapBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.region.coocurred.RegionCoocurredPrefAttBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.location.cf.network.region.coocurred.puresocial.*;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.MPReviewBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.MostPopularRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.PrecedingItemBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.NameDescriptionBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.DescriptionBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.NameBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.TagsBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.combinations.DescriptionNameWeightedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.combinations.DescriptionNameTagsWeightedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.combinations.DescriptionTagsWeightedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.combinations.NameDescriptionWeightedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.combinations.NameDescriptionTagsWeightedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cb.combinations.NameTagsWeightedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.CategoryBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.PurchasesBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.ReviewBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.SellerBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.puresocial.ReviewBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.seller.SellerAdamicAdarBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.seller.SellerJaccardBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.seller.SellerOverlapBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.seller.SellerPrefAttBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.seller.SellerSummedBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.seller.SellerTotalBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.seller.puresocial.SellerCNBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.seller.puresocial.SellerJaccardBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.marketplace.cf.seller.puresocial.SellerTotalBasedRecPS;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.OwnSocialRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.CommentsBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.InteractionsBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.LikesBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.SnapshotBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.UserBasedRecommenderWithoutMLT;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.WallpostBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.combined.CFPurchWithSocCommonNeighborhoodRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.combined.CFPurchWithSocCommonNeighborhoodReplacedRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.combined.CFPurchWithSocCommonNeighborhoodSummedRecommender;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.groups.GroupAdamicAdarBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.groups.GroupIntersectionBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.groups.GroupJaccardBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.groups.GroupNeighborOverlapBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.groups.GroupPrefAttachmentBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.groups.GroupTotalBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.interests.InterestNeighborOverlapBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.interests.InterestsAdamicAdarBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.interests.InterestsIntersectionBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.interests.InterestsJaccardBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.interests.InterestsPrefAttachmentBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.interests.InterestsTotalBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.network.AdamicAdarBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.network.CommonNeighborBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.network.JaccardBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.network.NeighborhodOverlapBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cf.network.PrefAttachmentBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cn.BiographyBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cn.GroupBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cn.InterestsAndGroupBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cn.InterestsBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cn.RealBiographyBasedRec;
import at.knowcenter.recommender.solrpowered.engine.strategy.social.cn.SocialStream3Rec;
import at.knowcenter.recommender.solrpowered.engine.utils.RecommendationQueryUtils;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.cleaner.DataFetcher;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendQuery;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendResponse;

public class RecommenderEngine implements RecommenderOperations{
	
	protected Map<StrategyType, RecommendStrategy> recommendStrategies;
	
	protected List<RecommendStrategy> getRecommendStrategies() {
		return new ArrayList<RecommendStrategy>(recommendStrategies.values());
	}

	private RecommendStrategy recommendStrategy;
	
	public RecommenderEngine() {
		initStrategies();
	}

	/**
	 * Initializes strategies used for getting recommendation results
	 */
	private void initStrategies() {
		recommendStrategies = new HashMap<StrategyType, RecommendStrategy>();
		recommendStrategies.put(StrategyType.CollaborativeFiltering, new PurchasesBasedRec());
		recommendStrategies.put(StrategyType.ContentBased, new NameDescriptionBasedRec());
		recommendStrategies.put(StrategyType.MostPopular, new MostPopularRec());
		recommendStrategies.put(StrategyType.PrecedingItemBased, new PrecedingItemBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightName, new NameBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightDescription, new DescriptionBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightTags, new TagsBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightNameDescription, new NameDescriptionWeightedBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightDescriptionName, new DescriptionNameWeightedBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightNameTags, new NameTagsWeightedBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightDescriptionTags, new DescriptionTagsWeightedBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightDescriptionNameTags, new DescriptionNameTagsWeightedBasedRec());
		recommendStrategies.put(StrategyType.CN_WeightNameDescriptionTags, new NameDescriptionTagsWeightedBasedRec());
		recommendStrategies.put(StrategyType.CF_Social, new InteractionsBasedRec());
		recommendStrategies.put(StrategyType.CF_Own_Social, new OwnSocialRec());
		recommendStrategies.put(StrategyType.CF_Social_Likes, new LikesBasedRec());
		recommendStrategies.put(StrategyType.CF_Social_Comments, new CommentsBasedRec());
		
		recommendStrategies.put(StrategyType.UB_Interests_MLT, new InterestsBasedRec());
		recommendStrategies.put(StrategyType.UB_CustomerGroups, new GroupBasedRec());
		recommendStrategies.put(StrategyType.UB_InterestsCustomerGroup, new InterestsAndGroupBasedRec());
		recommendStrategies.put(StrategyType.UB_WithOutMLT, new UserBasedRecommenderWithoutMLT());
		
		recommendStrategies.put(StrategyType.UB_WithOutMLTInterests, new InterestsIntersectionBasedRec());
		recommendStrategies.put(StrategyType.UB_WithOutMLTGroups, new GroupIntersectionBasedRec());
		
		
		recommendStrategies.put(StrategyType.CF_Categories, new CategoryBasedRec());
		recommendStrategies.put(StrategyType.SocialStream, new SocialStream3Rec());
		recommendStrategies.put(StrategyType.CFPurchWithSocCommonNeighborhoodRecommender, 
				new CFPurchWithSocCommonNeighborhoodRecommender());
		recommendStrategies.put(StrategyType.CFPurchWithSocCommonNeighborhoodSummedRecommender, 
				new CFPurchWithSocCommonNeighborhoodSummedRecommender());
		recommendStrategies.put(StrategyType.CFPurchWithSocCommonNeighborhoodReplacedRecommender, 
				new CFPurchWithSocCommonNeighborhoodReplacedRecommender());
		recommendStrategies.put(StrategyType.MostPopular_Review, new MPReviewBasedRec());
//		recommendStrategies.put(StrategyType.CF_Review, new ReviewBasedRec());
		recommendStrategies.put(StrategyType.CF_Review, new ReviewBasedRecPS());

		recommendStrategies.put(StrategyType.BiographyBasedMLT, new BiographyBasedRec());
		recommendStrategies.put(StrategyType.RealBiographyBasedMLT, new RealBiographyBasedRec());
		
		recommendStrategies.put(StrategyType.WallPostInteraction, new WallpostBasedRec());
		recommendStrategies.put(StrategyType.SnapshotInteraction, new SnapshotBasedRec());
		recommendStrategies.put(StrategyType.CF_Soc_Network_CN, new CommonNeighborBasedRec());
		recommendStrategies.put(StrategyType.CF_Soc_Network_Jaccard, new JaccardBasedRec());
		recommendStrategies.put(StrategyType.CF_Soc_Network_NeighOverlap, new NeighborhodOverlapBasedRec());
		recommendStrategies.put(StrategyType.CF_Soc_Network_AdamicAdar, new AdamicAdarBasedRec());
		recommendStrategies.put(StrategyType.CF_Soc_Network_PrefAttachment, new PrefAttachmentBasedRec());
		
		recommendStrategies.put(StrategyType.CF_Soc_Group_Jaccard, new GroupJaccardBasedRec());
		recommendStrategies.put(StrategyType.CF_Soc_Group_Overlap, new GroupNeighborOverlapBasedRec());
		recommendStrategies.put(StrategyType.CF_Soc_Group_AdemicAdar, new GroupAdamicAdarBasedRec());
		recommendStrategies.put(StrategyType.CF_Soc_Group_PrefAttach, new GroupPrefAttachmentBasedRec());
		
		recommendStrategies.put(StrategyType.CF_Soc_Interests_Jaccard, new InterestsJaccardBasedRec());
		recommendStrategies.put(StrategyType.CF_Soc_Interests_Overlap, new InterestNeighborOverlapBasedRec());
		recommendStrategies.put(StrategyType.CF_Soc_Interests_AdemicAdar, new InterestsAdamicAdarBasedRec());
		recommendStrategies.put(StrategyType.CF_Soc_Interests_PrefAttach, new InterestsPrefAttachmentBasedRec());
		
//		recommendStrategies.put(StrategyType.CF_Market_Seller_CN, new SellerBasedRec());
		recommendStrategies.put(StrategyType.CF_Market_Seller_CN, new SellerCNBasedRecPS());
//		recommendStrategies.put(StrategyType.CF_Market_Seller_Jaccard, new SellerJaccardBasedRec());
		recommendStrategies.put(StrategyType.CF_Market_Seller_Jaccard, new SellerJaccardBasedRecPS());

		recommendStrategies.put(StrategyType.CF_Market_Seller_PrefAtt, new SellerPrefAttBasedRec());
		recommendStrategies.put(StrategyType.CF_Market_Seller_Overlap, new SellerOverlapBasedRec());
		recommendStrategies.put(StrategyType.CF_Market_Seller_AdamicAdar, new SellerAdamicAdarBasedRec());
		recommendStrategies.put(StrategyType.CF_Market_Seller_Summed, new SellerSummedBasedRec());
		
//		recommendStrategies.put(StrategyType.CF_Loc_Picks_CN, new PicksCommonNeighborBasedRec());
//		recommendStrategies.put(StrategyType.CF_Loc_Picks_Jaccard, new PicksJaccardBasedRec());
		
		recommendStrategies.put(StrategyType.CF_Loc_Picks_CN, new PicksCommonNeighborBasedRecPS());
		recommendStrategies.put(StrategyType.CF_Loc_Picks_Jaccard, new PicksJaccardBasedRecPS());
		
		
		recommendStrategies.put(StrategyType.CF_Location_Network_All_CN, new LocationCommonNeighborBasedRec());
		recommendStrategies.put(StrategyType.CF_Location_Network_Coocured_CN, new LocationCoocurredCommonNeighborBasedRec());
		recommendStrategies.put(StrategyType.CF_Region_Network_All_CN, new RegionCommonNeighborBasedRec());
//		recommendStrategies.put(StrategyType.CF_Region_Network_Coocurred_CN, new RegionCoocurredCommonNeighborBasedRec());
		recommendStrategies.put(StrategyType.CF_Region_Network_Coocurred_CN, new RegionCoocurredCNBasedRecPS());

		recommendStrategies.put(StrategyType.CF_Location_Network_All_Jaccard, new LocationJaccardBasedRec());
		recommendStrategies.put(StrategyType.CF_Location_Network_Coocured_Jaccard, new LocationCoocurredJaccardBasedRec());
		recommendStrategies.put(StrategyType.CF_Region_Network_All_Jaccard, new RegionJaccardBasedRec());
//		recommendStrategies.put(StrategyType.CF_Region_Network_Coocurred_Jaccard, new RegionCoocurredJaccardBasedRec());
		recommendStrategies.put(StrategyType.CF_Region_Network_Coocurred_Jaccard, new RegionCoocurredJaccardBasedRecPS());

		recommendStrategies.put(StrategyType.CF_Location_Network_All_Adar, new LocationAdarBasedRec());
		recommendStrategies.put(StrategyType.CF_Location_Network_Coocured_Adar, new LocationCoocurredJaccardBasedRec());
		recommendStrategies.put(StrategyType.CF_Region_Network_All_Adar, new RegionAdarBasedRec());
//		recommendStrategies.put(StrategyType.CF_Region_Network_Coocurred_Adar, new RegionCoocurredAdarBasedRec());
		recommendStrategies.put(StrategyType.CF_Region_Network_Coocurred_Adar, new RegionCoocurredAdarBasedRecPS());

//		recommendStrategies.put(StrategyType.CF_Region_Network_Coocurred_Overlap, new RegionCoocurredOverlapBasedRec());
//		recommendStrategies.put(StrategyType.CF_Region_Network_Coocurred_PrefAtt, new RegionCoocurredPrefAttBasedRec());
		recommendStrategies.put(StrategyType.CF_Region_Network_Coocurred_Overlap, new RegionCoocurredOverlapBasedRecPS());
		recommendStrategies.put(StrategyType.CF_Region_Network_Coocurred_PrefAtt, new RegionCoocurredPrefAttBasedRecPS());
		
		recommendStrategies.put(StrategyType.CF_Region_Network_All_Overlap, new RegionOverlapBasedRec());
		recommendStrategies.put(StrategyType.CF_Region_Network_All_PrefAtt, new RegionPrefAttBasedRec());
		recommendStrategies.put(StrategyType.CF_Location_Network_All_Overlap, new LocationOverlapBasedRec());
		recommendStrategies.put(StrategyType.CF_Location_Network_All_PrefAtt, new LocationPrefAttBasedRec());
		
//		recommendStrategies.put(StrategyType.CF_Loc_Common_Regions, new CommonRegionsBasedRec());
//		recommendStrategies.put(StrategyType.CF_Loc_Common_Regions_Jaccard, new CommonRegionsJaccardBasedRec());
		
		recommendStrategies.put(StrategyType.CF_Loc_Common_Regions, new CommonRegionsBasedRecPS());
		recommendStrategies.put(StrategyType.CF_Loc_Common_Regions_Jaccard, new CommonRegionsJaccardBasedRecPS());
		
		
//		recommendStrategies.put(StrategyType.CF_Market_Seller_Total, new SellerTotalBasedRec());
		recommendStrategies.put(StrategyType.CF_Market_Seller_Total, new SellerTotalBasedRecPS());

		recommendStrategies.put(StrategyType.CF_Soc_Interests_Total, new InterestsTotalBasedRec());
		recommendStrategies.put(StrategyType.CF_Soc_Group_Total, new GroupTotalBasedRec());
//		recommendStrategies.put(StrategyType.CF_Loc_Picks_Total, new PicksTotalBasedRec());
		recommendStrategies.put(StrategyType.CF_Loc_Picks_Total, new PicksTotalBasedRecPS());

//		recommendStrategies.put(StrategyType.CF_Loc_Total_Regions, new CommonRegionsTotalBasedRec());
		recommendStrategies.put(StrategyType.CF_Loc_Total_Regions, new CommonRegionsTotalBasedRecPS());

//		recommendStrategies.put(StrategyType.CF_Loc_Shared_Regions_Common, new SharedRegionsCommonBasedRec());
//		recommendStrategies.put(StrategyType.CF_Loc_Shared_Regions_Jaccard, new SharedRegionsJaccardBasedRec());
//		recommendStrategies.put(StrategyType.CF_Loc_Shared_Regions_Total, new SharedRegionsTotalBasedRec());
		
		recommendStrategies.put(StrategyType.CF_Loc_Shared_Regions_Common, new SharedRegionsCommonBasedRecPS());
		recommendStrategies.put(StrategyType.CF_Loc_Shared_Regions_Jaccard, new SharedRegionsJaccardBasedRecPS());
		recommendStrategies.put(StrategyType.CF_Loc_Shared_Regions_Total, new SharedRegionsTotalBasedRecPS());
		
		
//		recommendStrategies.put(StrategyType.CF_Loc_Days_Seen_In_Region, new RegionsDaysSeenBasedRec());
//		recommendStrategies.put(StrategyType.CF_Loc_Physical_Distance_in_Region, new RegionsPhysicalDistanceBasedRec());
		
		recommendStrategies.put(StrategyType.CF_Loc_Days_Seen_In_Region, new RegionsDaysSeenBasedRecPS());
		recommendStrategies.put(StrategyType.CF_Loc_Physical_Distance_in_Region, new RegionsPhysicalDistanceBasedRecPS());
		
		recommendStrategies.put(StrategyType.CF_Loc_Physical_Distance_3D_in_Region, new RegionsPhysicalDistance3DBasedRec());

		setRecommendStrategy(StrategyType.CollaborativeFiltering);
	}
	
	public RecommendStrategy getApproach(StrategyType type) {
		return recommendStrategies.get(type);
	}
	
	/**
	 * Based on the iteration number of tries for getting product recommendations sets an appropriate strategy
	 * @param recommendIterationNr the number of the try to get product recommendations
	 * <br/> If the number is <i>below 0</i> then the 1st strategy will be set. 
	 * <br/> If the number is <i>higher than the number of available strategies</i> then the last strategy will be used
	 */
	private void setRecommendStrategy(StrategyType strategy) {
		recommendStrategy = recommendStrategies.get(strategy);
	}
	
	@Override
	public List<String> getRecommendations(final String userID, final int n) {
		return getRecommendations(userID, null, n);
	}
	
	@Override
	public List<String> getRecommendations(final String userID, final String productID, final int n) {
		return getRecommendations(userID, productID, n, null);
	}
	
	@Override
	public List<String> getRecommendations(final String userID, final String productID, final int n, ContentFilter contentFilter) {
		List<String> recommendations;
		initUsersOwnProductsFiltering(userID);
		
		if (contentFilter == null) {
			contentFilter = new ContentFilter();
		}
		contentFilter.setCustomer( SolrServiceContainer.getInstance().getRecommendService().fetchUserProfileData(userID) );

		for (RecommendStrategy strategy : getRecommendStrategies()) {
			strategy.setContentFiltering(contentFilter);
		}
		
		recommendations = runRecommendWorkflow(userID, productID, n, contentFilter);
		
		if (contentFilter != null && contentFilter.getPrecedingEvaluationMethod() != PrecedingItemEvaluation.NOTHING) {
			List<String> piRecommendations = getRecommendations(userID, null, n, contentFilter, StrategyType.PrecedingItemBased);
			recommendations = shiftPrecedingItems(n, contentFilter, recommendations, piRecommendations);
		}
		
		if (contentFilter != null && contentFilter.getCheckVIPUser() != null && contentFilter.getCheckVIPUser()) {
			recommendations = RecommendationQueryUtils.getVIPRecommendations(n, recommendations);
		}
		
		setRecommendStrategy(StrategyType.CollaborativeFiltering);
		return recommendations;
	}
	
	/**
	 * Use this method calling a specific recommender strategy where no prior user initialization was done
	 * @param userID id of the user to get recommendations for
	 * @param productID id of the product to be used in getting recommendations
	 * @param n number of recommendations to be returned
	 * @param contentFilter container for specifying content filtering
	 * @param strategyToUse implementation of a recommender strategy
	 * @return recommendations
	 */
	public List<String> getRecommendations(final String userID, final String productID, final int n, ContentFilter contentFilter, RecommendStrategy strategyToUse) {
		recommendStrategy = strategyToUse;
		
		if (contentFilter != null) {
			contentFilter.setCustomer( SolrServiceContainer.getInstance().getRecommendService().fetchUserProfileData(userID) );
			for (RecommendStrategy strategy : getRecommendStrategies()) {
				strategy.setContentFiltering(contentFilter);
			}
			recommendStrategy.setContentFiltering(contentFilter);
		}
		
		initUsersOwnProductsFiltering(userID);
		
		RecommendQuery query = createQuery(userID, productID);
		RecommendResponse searchResponse = recommendStrategy.recommend(query, n);
				
		setRecommendStrategy(StrategyType.CollaborativeFiltering);
		
		return searchResponse.getResultItems();
	}
	
	/**
	 * Use this method calling a specific recommender strategy where user initialization was already done
	  * @param userID id of the user to get recommendations for
	 * @param productID id of the product to be used in getting recommendations
	 * @param n number of recommendations to be returned
	 * @param contentFilter container for specifying content filtering
	 * @param strategyTypeToUse type of the recommendation strategy that will be called to calculate recommendations
	 * @return recommendations
	 */
	public List<String> getRecommendations(final String userID, final String productID, final int n, ContentFilter contentFilter, StrategyType strategyTypeToUse) {
		recommendStrategy = recommendStrategies.get(strategyTypeToUse);
		
		RecommendQuery query = createQuery(userID, productID);
		
		RecommendResponse searchResponse = recommendStrategy.recommend(query, n);
		
		setRecommendStrategy(StrategyType.CollaborativeFiltering);
		
		return searchResponse.getResultItems();
	}
	
	/**
	 * Initializes all recommendation strategies with the users already purchased products
	 * so that they wont be got as recommendations
	 * @param userID
	 */
	protected List<String> initUsersOwnProductsFiltering(final String userID) {
		List<String> alreadyBoughtProducts = null;
		// STEP 0 - get products from a user
		if (userID != null ) {
//			QueryResponse response = SolrServiceContainer.getInstance().getRecommendService().findItemsFromUser(userID, "users_purchased", SolrServiceContainer.getInstance().getRecommendService().getSolrServer());
//
//			alreadyBoughtProducts = RecommendationQueryUtils.createUserProductsList(response);
			alreadyBoughtProducts = DataFetcher.getRatedProductsFromUser(userID);
			for (RecommendStrategy strategy : getRecommendStrategies()) {
				strategy.setAlreadyPurchasedResources(alreadyBoughtProducts);
			}
			if (recommendStrategy != null) {
				recommendStrategy.setAlreadyPurchasedResources(alreadyBoughtProducts);
			}
		} else {
			alreadyBoughtProducts = new ArrayList<String>();
		}
		return alreadyBoughtProducts;
	}

	/**
	 * Workflow for getting recommendations based on the input
	 * @param userID
	 * @param productID
	 * @param n
	 * @param contentFilter
	 * @return
	 */
	private List<String> runRecommendWorkflow(final String userID, final String productID, final int n, ContentFilter contentFilter) {
		List<String> recommendations = new ArrayList<String>();
	
		
		if (contentFilter.getCustomer() != null) {
			List<String> cfRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.CollaborativeFiltering);
			
			if (productID != null) {
				recommendations.addAll(cfRecommendations);
				
				if (recommendations.size() < n) {
					List<String> cbRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.ContentBased);
					RecommendationQueryUtils.appendDifferentProducts(n, recommendations, cbRecommendations);
				}
				if (recommendations.size() < n) {
					List<String> cbRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.SocialStream);
					RecommendationQueryUtils.appendDifferentProducts(n, recommendations, cbRecommendations);
				}
				if (recommendations.size() < n) {
					List<String> mpRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.MostPopular);
					RecommendationQueryUtils.appendDifferentProducts(n, recommendations, mpRecommendations);
				}
			} else {
				Double cfWeight = 0.0236;
				Double cbWeight = 0.0055;
				Double ubWeight = 0.0103;
				Double socWeight = 0.0214;
				Double streamWeight = 0.0008;
				
				List<String> cbRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.ContentBased);
				List<String> ubRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.UB_WithOutMLT);
				List<String> socialRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.CF_Social);
				List<String> streamRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.SocialStream);

				Map<String, Double> occurencesMap = new HashMap<String, Double>();
				
				RecommendationQueryUtils.fillWeightedMap(occurencesMap, cfRecommendations, cfWeight);
				RecommendationQueryUtils.fillWeightedMap(occurencesMap, cbRecommendations, cbWeight);
				RecommendationQueryUtils.fillWeightedMap(occurencesMap, ubRecommendations, ubWeight);
				RecommendationQueryUtils.fillWeightedMap(occurencesMap, socialRecommendations, socWeight);
				RecommendationQueryUtils.fillWeightedMap(occurencesMap, streamRecommendations, streamWeight);

				List<String> sortedAndTrimedRecommendations = RecommendationQueryUtils.extractCrossRankedProducts(occurencesMap);
				RecommendationQueryUtils.appendDifferentProducts(n, recommendations, sortedAndTrimedRecommendations);
				
				if (recommendations.size() < n) {
					List<String> mpRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.MostPopular);
					RecommendationQueryUtils.appendDifferentProducts(n, recommendations, mpRecommendations);
				}
			}
		} else {
			if (productID != null) {
				List<String> cbRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.ContentBased);
				recommendations.addAll(cbRecommendations);
				
				if (recommendations.size() < n) {
					List<String> cfRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.CollaborativeFiltering);
					RecommendationQueryUtils.appendDifferentProducts(n, recommendations, cfRecommendations);
				}
				if (recommendations.size() < n) {
					List<String> mpRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.MostPopular);
					RecommendationQueryUtils.appendDifferentProducts(n, recommendations, mpRecommendations);
				}
			} else  {
				List<String> mpRecommendations = getRecommendations(userID, productID, n, contentFilter, StrategyType.MostPopular);
				recommendations.addAll(mpRecommendations);
			}
		}
		
		return recommendations;
	}


	private List<String> shiftPrecedingItems(final int n, ContentFilter contentFilter, List<String> recommendations, List<String> piRecommendations) {
		List<String> shiftedRecommendations = null;
		if (contentFilter.getPrecedingEvaluationMethod() == PrecedingItemEvaluation.MAX_ALL_AS_RESULT) {
			shiftedRecommendations = RecommendationQueryUtils.shiftProducts(recommendations, piRecommendations, n, n);
		}
		if (contentFilter.getPrecedingEvaluationMethod() == PrecedingItemEvaluation.MAX_HALF_AS_RESULT) {
			shiftedRecommendations = RecommendationQueryUtils.shiftProducts(recommendations, piRecommendations, n / 2, n);
		}
		if (contentFilter.getPrecedingEvaluationMethod() == PrecedingItemEvaluation.MAX_20_PERCENT_AS_RESULT) {
			shiftedRecommendations = RecommendationQueryUtils.shiftProducts(recommendations, piRecommendations, (int) (n * 0.2), n);
		}
		return shiftedRecommendations;
	}

	private RecommendQuery createQuery(final String userID, final String productID) {
		RecommendQuery query = new RecommendQuery();
		query.setUser(userID);
		
		// set products if exist
		if (productID != null) {
			List<String> products = new ArrayList<>();
			products.add(productID);
			query.setProductIds(products);

		}
		
		return query;
	}
}
