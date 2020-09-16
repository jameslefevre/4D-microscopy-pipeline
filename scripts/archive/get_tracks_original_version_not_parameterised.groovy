
// thin wrapper around makeSaveTracks, replacing main method



import ObjectTracking.ObjectTracking;

double[] removePlateAdjacentObjectsParams = null
int[] trackedClasses = [1,2,3,4]
int[] voxelThresholds = [5000,75,500,500]
double[] fieldScaling = [1.04,1.04,2.68,1]
double logSizeWeight = 22
double[] misMatchPenalty = [35,20,20,20]
double[] matchThreshold = [35,20,20,20]
boolean doAggregationMatching = true
int sliceMethod = 2 // 1=splitBranches, 2=splitBranchesWatershed
Double[] sliceParam1 = [0.2,0.2,0.2,0.2] // sliceProportion or thresholdProportion
Double[] sliceParam2 = [1000.0,10.0,100.0,100.0] // sliceMax or minSeedVolume
int[] timeSteps_specified = null // if null, uses all available\
int[] breakPoints = null // null==none
double[] zipThresholds = [20000,300,2000,2000]
int[] zipShortBranch_maxLength = [4,2,2,2]
int[] zipShortBranch_maxNonContact = [0,0,0,0]
boolean useAlphabeticalPositionForStackNumber = true
int stackNumPadLength = 3
String stackNumPrefix = "-t"


ObjectTracking.makeSaveTracks(
				//*** main_path
				"/home/james/image_data/visualiser/20190809_post3/d18_intAdj_rep1ds1gd_rf/objectAnalysis/",
				//*** save_pathtrack
				"/home/james/image_data/visualiser/20190809_post3/d18_intAdj_rep1ds1gd_rf/objectAnalysis/",
				//*** save_name
				"trackNodeTable.csv",
				//*** removePlateAdjacentObjectsParams; except for 1st component (angle in degrees), must be in same units as fieldScaling
				removePlateAdjacentObjectsParams,
				trackedClasses,
				voxelThresholds,
				fieldScaling, 
				logSizeWeight, 
				misMatchPenalty, 
				matchThreshold, 
				doAggregationMatching, 
				sliceMethod,
				sliceParam1,
				sliceParam2,
				timeSteps_specified,
				breakPoints,
				//new int[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19},
				zipThresholds,
				zipShortBranch_maxLength,
				zipShortBranch_maxNonContact, 
				useAlphabeticalPositionForStackNumber,
				stackNumPadLength,
				stackNumPrefix
				);
		
		println("done");
