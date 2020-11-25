/**
 * This script is for creating a training data set to produce a segmentation model using the Weka ML platform.
 * The output (input for Weka) is in arff format, which is a csv file with a header containing field (column) information. 
 * Each row in the main part of the file (after @data) corresponds to a single voxel in the training data; the final column 
 * is the segmentation class, the others are image features.
 * 
 * The script is a wrapper around the java code GetTrainingData.saveTrainingDataEclipticalSelections
 * Intended use is to make a copy of this script, edit the paths, filename and other parameters, run to produce the data set, 
 * then save the script to document the process.
 * 
 * Data replication and intensity scaling is supported in this script, and demonstrated in the template version.
 * 
 * The inputs for this script are 1 or more sets of image features (each corresponding to an image stack), 
 * saved to disk using the layout produced by generate_save_features.groovy,
 * and 1 or more macros containing training data selections, recorded from the Trainable Weka plugin.
 * The macro file(s) must contain a sequence of line pairs in the following format (other lines between are ignored, but order matters)
 *   makeOval(326, 725, 3, 1);
 *   call("trainableSegmentation.Weka_Segmentation.addTrace", "0", "59");
 * Typically, the macro contains other lines which allow it to be run in imageJ (with target stack open) to open Trainable Weka
 * and recreate the state.
 * The example above allocates the voxels in the specified ellipse (326, 725, 3, 1) on slice 59 to class 0.
 * In the ellipse (oval) specification makeOval(x, y, w, h), the parameters define a bounding box for the ellipse.
 * (x,y) is the top-left corner position (minimum x and y values), while x and h are the dimensions in the x and y directions
 * (so the bounding box contains w*h voxels)
 * Currently only elliptical data selections are supported, other selection methods require new java code to support.
 * 
 * The script is designed to concatonate m "selection sets", where each "selection set" is defined by an 
 * image stack (and corresponding set of features), a selection macro (which of course must be based on this stack),  
 * and an intensity scaling factor. This allows, for example, training data from different stacks to be combined 
 * (optionally with intensity adjustment to get them on the same scale), or multiple training data selections from the same 
 * stack to be combined, or data to be replicated with adjusted intensity (as in the template script).
 * These selection sets are defined by the first 5 parameters: imageStackLocations, imageStackNames, featureFolders, 
 * selection_macro_paths, and intensityScalingFactor. These string arrays must all have the same length, corresponding to 
 * the m selection sets.
 * 
 * The image features used must be consistent across the selection sets, in order for the data to be combined. The required feature names 
 * are obtained from a specified table (feature_model_table) by lookup on a defined column (modelName). This is designed to maintain 
 * consistency with other parts of the pipeline.
 * Note that arff files can also be combined directly (in a text editor) provided the fields (image features and classes) are consistent.
 * 
 * @param imageStackLocations
 * The path to the folder for each image stack used (one for each selection sets, may be repeated).
 * @param imageStackNames
 * The image stack file names (one for each selection sets, may be repeated).
 * @param featureFolders
 * Paths to the root folder for the set of saved image features (one for each selection sets, may be repeated).
 * @param selection_macro_paths
 * Full paths to the imageJ macro files containing training data selections (one for each selection sets, may be repeated).
 * @param intensityScalingFactor
 * Features are scaled to get the same effect as multiplying the source image intensity by this factor
 * (one for each selection sets, may be repeated).
 * Note that the image may have actually been scaled before feature computation, so check to make sure it
 * is not being done twice (this will cause an error).
 * @param modelName
 * A label for the model to be trained, used to specify required features (see feature_model_table below)
 * @param savePath
 * The full path for the output file
 * @param feature_model_table
 * path to tsv file containing a table of information on features and which features are used in each specified model,  
 * known as the feature-model table.
 * The names of the required features are obtained from column "modelName" in this table.
 * @param classNames
 * The segmentation classes are numbered 0,1,2,...,(n-1). This is a string array which associates these numbers with labels,
 * and must have length n.
 * @param sliceNumberPadLength
 * Image features are recorded (within nested folders) in the format slice_[x], where [x] is the slice number (numbered from 0), 
 * left-padded with zeros to this number of digits. Generally fixed, and defined by generate_save_features.groovy.
 */

import wekaSegInterface.GetTrainingData;


// I have defined variables here to help with the repetition in the parameters below; this repetition is due to the replication with 
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
