// simple wrapper around java function to allow uniform execution style (groovy scripts)

import wekaSegInterface.GetTrainingData;

#@ String[] imageStackLocations
#@ String[] imageStackNames
#@ String[] featureFolders
#@ String[] selection_macro_paths
#@ float[] intensityScalingFactor	
#@ String modelName
#@ String savePath
#@ String feature_model_table
#@ String[] classNames
#@ int sliceNumberPadLength

GetTrainingData.saveTrainingDataEclipticalSelections(
	imageStackLocations,
	imageStackNames,
	featureFolders,
	selection_macro_paths,
	intensityScalingFactor,
	modelName,
	savePath,
	feature_model_table,
	classNames,
	sliceNumberPadLength)
