
// thin wrapper around generateSaveTracks, replacing main method of objectTracking.Tracking
//*** removePlateAdjacentObjectsParams; has not been implemented for rewrite of tracking code; following notes describe how it used to work (and might again)
// except for 1st component (angle in degrees), must be in same units as fieldScaling
//An array of parameters used to filter out objects prior to tracking based on a clipping plane which can move (but not rotate) at a fixed rate per time step
// angle between x axis and true horizontal, estimated position of plate in transformed axis at time 0, change in posiiton each time step, then distance threshold for each tracked class

#@ String main_path
#@ String save_path
#@ int[] timeSteps_specified
#@ int[] breakPoints
#@ boolean useAlphabeticalPositionForStackNumber
#@ String stackNumPrefix
#@ String stackNumSuffix
#@ int[] trackedClasses
#@ int[] voxelThresholds
#@ double[] fieldScaling
#@ double[] logSizeWeight
#@ double[] matchThreshold
#@ double[] relativeNodeContact_referenceValue
#@ double[] relativeNodeDistance_referenceValue
#@ double[] relativeNodeContact_weight
#@ double[] matchScoreWeighting
#@ boolean verbose


import objectTracking.Tracking;

Tracking.generateSaveTracks(
				main_path,
				save_path,
				timeSteps_specified.length==0 ? null : timeSteps_specified,
				breakPoints.length==0 ? null : breakPoints,
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
