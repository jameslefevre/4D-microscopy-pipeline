
// **** See [main document, section] for an explanation of the algorithm implemented here. *****
// **** In particular, that document explains the per-class tracking parameters from voxelThresholdsInitial onwards **** 

/* INPUT SPECIFICATION
 *  The input data for this tracking algorithm consists of 2 text files for each time step:  
 *    objectStats.txt
 *    	- contains summary information for each object
 *    	- formatted as a tab-separated table where each row represents one object 
 *    	- must have columns titled id, class, x, y, z, and voxels, with numerical values
 *    	- x,y,z describe the centre of mass of each object in voxel units
 *    	- id, class, voxels are expected to be integers, but if not will be rounded to the nearest integer
 *    	- id must be unique for each line, but does not need to be consistent across time steps (since that is the point of the tracking)
 *    objectAdjacencyTable.txt 
 *    	- describes contact between objects of all classes
 *    	- comma-separated table with 3 columns and no header (so every line consists of 3 numbers separated by commas)
 *    	- The first 2 numbers are object ids corresponding to values in objectStats.txt; order doesn't matter and each pair occurs at most once 
 *    	- The third number quantifies the contact between the 2 objects (the exact means of quantification doesn't matter, 
 *    			but may affect the optimal selection of the parameter relativeNodeContact_referenceValue)
 *  objectStats may contain additional columns listed below, but these do not influence the tracking. Instead, these are properties that are 
 *  	transferred to the output tracks for the purpose of further analysis. Objects may be merged as part of the tracking process, and in 
 *  	this case values are combined as appropriate (mean values are weighted by object sizes etc). The meanings given are intended only, since 
 *  	this process has no control over the data supplied and its derivation; however, they will affect how information is combined when 
 *  	merging objects
 *  	- intensity, channelMean, classProbs[class] represent the mean strength of signal in the source image, the mean channel/class value 
 *  		(for use when the original classes have been aggregated for object analysis), and the mean probability of each class according
 *  		to the segmentation model. 
 *  	- d[class], dv[class], dMin[class] and dMax[class], where [class] is the integer label of a class, are intended to represent the 
 *  		mean, variance, minimum and maximum distance of the object from the specified class.
 *  	- vx,vy,vz,cxy,cxz,cyx describe the covariance matric of the voxel positions (gives information on shape, compactness, orientation) 
 *  
 * PARAMETERS
 * 	String main_path: Path to a directory containing the main input data, which is assumed to be derived from a single capture 
 * 		(although break points can be specified if imaging was discontinuous).
 * 		This directory must contain sub-directories which each contain the summary object information for one image stack, consisting of
 * 		the files objectStats.txt and objectAdjacencyTable.txt as described above. 
 * 		The stack number (assumed to be a sequential timestep) should be included in each subdirectory name between 2 fixed text strings 
 * 		(the number may be padded with 0s on the left). Alternatively, each stack can be assigned a stack number based on its alphanumerical
 * 		rank within the main_path directory, starting from zero.
 *  String save_path: A fully specified path (folder and filename) under which to save the output file, may be within the folder main_path
 * 	int[] timeSteps_specified: allows tracking to be restricted to the subset of time steps listed. If null or length 0, all time steps are 
 * 		used. Time steps will always be sorted into numerical order.	
 * 	int[] breakPoints: Allows tracking to be broken up into distinct intervals. For each listed integer, tracks will be terminated at the
 * 		corresponding time step and restarted at the following step. If null, no breaks are applied.
 * 	boolean useAlphabeticalPositionForStackNumber: If true, subdirectories within main_path will be assigned a stack number based on 
 * 		alphanumerical rank, starting at zero. Otherwise, stack number is extracted using stackNumPrefix and stackNumSuffix below.
 * String stackNumPrefix: A text string preceeding the stack number in each subdirectory name (used if useAlphabeticalPositionForStackNumber false)
 * String stackNumSuffix: A text string following the stack number in each subdirectory name (used if useAlphabeticalPositionForStackNumber false)
 * double[] fieldScaling: The x,y,z size of a voxel. Used to convert object position into "real" units, most importantly an isotropic scale for 
 * 		correct matching behaviour. Units need to be consistent with the tracking parameters listed below. 
 * boolean verbose: For debugging from the ImageJ script window; if true, extensive additional information is written to the console.
 * int[] trackedClasses: the integer labels of the classes to be tracked. Each class is tracked independantly.
 * 
 * THE REMAINING PARAMETERS ARE ALL ARRAYS OF THE SAME LENGTH AS trackedClasses, REPRESENTING PARAMETERS SPECIFIED PER-CLASS
 * 
 * voxelThresholds: Objects with fewer voxels will be immediately discarded. This should be set conservatively, as small objects may be merged into larger ones
 * 		and missing pieces may reduce the quality of tracks produced. This step is designed to filter out very small objects to reduce computational cost. 
 * 		It is recommended that a more stringent filter is applied later, requiring each track to 
 * 		reach a threshold before being used in analysis. 
 * 	logSizeWeight: When finding the distance between objects for the pupose of matching, the difference in log volume is used alongside spatial distance. This is
 * 		the weighting factor to apply to the difference in log volume (equivalently, log of volume ratio). This weighted difference is then squared and added to the 
 * 		square of the spatial distance. The square root of this sum is the distance metric used in matching objects.
 * 	matchThreshold: The maximum matching distance allowed (with distance as defined above).
 * 	relativeNodeContact_referenceValue: scaling parameter controlling the influence of contact between objects on whether they should be merged.
 * 		Higher values reduce the likelihood of merging.
 * 	relativeNodeDistance_referenceValue: scaling parameter controlling the influence of proximity between objects (relative to size) on whether 
 * 		they should be merged. Positive number. Higher values reduce the likelihood of merging.
 * 	relativeNodeContact_weight: Number between 0 and 1 indating the weight that should be placed on contact vs centre of mass proximity when deciding whether 
 * 		objects should be merged.
 * 	matchScoreWeighting: The weight to place on matching distance between time steps versus object merging metrics when deciding on optimal tracks
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
