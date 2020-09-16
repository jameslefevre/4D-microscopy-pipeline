// simple wrapper around java function to allow uniform execution style (groovy scripts)

// In this case we demonstrate replication of a single training data set at a range of intensities.

import wekaSegInterface.GetTrainingData;


// I have defined variables here to help with the repetion in the parameters below; this repetition is due to the replication with 
// scaled intensity.
String wrkFdr = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/23_2019-09_new_data/new_training_data_and_model/";
String imFldr = "/data/james/image_data/LLS/20190830_LLSM_Yvette/20190830_pre1/cropped/";
String nm="c1-t200-et1044911_decon";
String ff="/data/james/image_data/LLS/20190830_LLSM_Yvette/20190830_pre1/fs200_cropped_adjustedInt/";
String mcr=wrkFdr + "20190830_pre1_t200_v1.ijm";

// **** the following parameters must be set for the selected training data selections and model ***
String[] imageStackLocations = [imFldr,imFldr,imFldr,imFldr]
String[] imageStackNames = [nm,nm,nm,nm]
String[] featureFolders = [ff,ff,ff,ff]
String[] selection_macro_paths = [mcr,mcr,mcr,mcr]
float[] intensityScalingFactor = [0.5f,0.75f,1.0f,1.25f] // adjusts all features (including original) to mimic multiplying image intensity by this factor; if null then no scaling		
String modelName = "d16_ds1gd_rf"
String savePath = wrkFdr + "20190911_pre1_t200_v1_4int_testRep.arff"
String feature_model_table  ="/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/data_and_model_info/feature_model_table.txt"
String[] classNames = ["background","cell_body","tentpole","ruffle"]


int sliceNumberPadLength = 4
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
