
// **** TODO: link paper and methods section explaining algorithm on publication *****
// **** In particular, that document explains the per-class tracking parameters from voxelThresholdsInitial onwards **** 

/** 
 *  See get_tracks_parameterised for documentation
*/

String main_path = "/home/james/image_data/visualiser/20190917_pos2/d19_intAdj_rep1ds1gd_rf/objectAnalysis2/"
String save_path = "/home/james/image_data/visualiser/20190917_pos2/d19_intAdj_rep1ds1gd_rf/objectAnalysis2/trackNodeTable1r2.csv"
int[] timeSteps_specified = null
int[] breakPoints = null
boolean useAlphabeticalPositionForStackNumber = false
String stackNumPrefix = "-t"
String stackNumSuffix = "-e"
double[] fieldScaling = [1.04,1.04,2.68]
boolean verbose = false
int[] trackedClasses = [1,2,3,4] // new int[]{1,2,3,4}
int[] voxelThresholds = [2000,30,200,200] //new int[]{2000,30,200,200}
double[] logSizeWeight = [90,22,22,22]
double[] matchThreshold = [120,20,20,20]
double[] relativeNodeContact_referenceValue = [0.06,0.02,0.04,0.04]
double[] relativeNodeDistance_referenceValue = [0.7,0.5,0.8,0.8]
double[] relativeNodeContact_weight = [0.66,0.66,0.66,0.66]
double[] matchScoreWeighting = [0.35,0.25,0.25,0.25]


import objectTracking.Tracking;

Tracking.generateSaveTracks(
				main_path,
				save_path,
				timeSteps_specified,
				breakPoints,
				useAlphabeticalPositionForStackNumber,
				stackNumPrefix,
				stackNumSuffix,
				trackedClasses,
				voxelThresholds,
				fieldScaling, 
				logSizeWeight,
				matchThreshold,
				relativeNodeContact_referenceValue,
				relativeNodeDistance_referenceValue,
				relativeNodeContact_weight,
				matchScoreWeighting,
				verbose
				);
		
		println("done");
