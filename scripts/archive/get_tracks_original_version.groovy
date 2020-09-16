
// used for previous tracking algorithm.

// thin wrapper around makeSaveTracks, replacing main method
//*** removePlateAdjacentObjectsParams; except for 1st component (angle in degrees), must be in same units as fieldScaling
//An array of parameters used to filter out objects prior to tracking based on a clipping plane which can move (but not rotate) at a fixed rate per time step
// angle between x axis and true horizontal, estimated position of plate in transformed axis at time 0, change in posiiton each time step, then distance threshold for each tracked class

#@ String main_path
#@ String save_path
#@ String save_name
#@ double[] removePlateAdjacentObjectsParams
#@ int[] trackedClasses
#@ int[] voxelThresholds
#@ double[] fieldScaling
#@ double logSizeWeight
#@ double[] misMatchPenalty
#@ double[] matchThreshold
#@ boolean doAggregationMatching
#@ int sliceMethod
#@ Double[] sliceParam1
#@ Double[] sliceParam2
#@ int[] timeSteps_specified
#@ int[] breakPoints
#@ double[] zipThresholds
#@ int[] zipShortBranch_maxLength
#@ int[] zipShortBranch_maxNonContact
#@ boolean useAlphabeticalPositionForStackNumber
#@ int stackNumPadLength
#@ String stackNumPrefix

import ObjectTracking.ObjectTracking;

ObjectTracking.makeSaveTracks(
				main_path,
				save_path,
				save_name,		
				removePlateAdjacentObjectsParams.length==0 ? null : removePlateAdjacentObjectsParams,
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
				timeSteps_specified.length==0 ? null : timeSteps_specified,
				breakPoints.length==0 ? null : breakPoints,
				zipThresholds,
				zipShortBranch_maxLength,
				zipShortBranch_maxNonContact, 
				useAlphabeticalPositionForStackNumber,
				stackNumPadLength,
				stackNumPrefix
				);
		
		println("done");
