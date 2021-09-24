/**
 * 
 * Apply a Weka segmentation model to create a segmented image and (optional) probability map, using a specified set of pre-calculated features
 * (or features which can easily be derived from precalculated features - see Derived Features below).
 * Core functions are in wekaSegInterface.ApplyClassifier (classifyImage and segFromProbMap), including loading of features for each slice, 
 * applying the segmentation model, compiling result and converting probability map to segmentation 
 * (which is the argmax across the probability distribution by default, but can be modified slightly - see param channel_grouping below)
 * 
 * The approach is designed to minimise memory use, with features loaded from disk for one slice at a time.
 * 
 * The original image is typically used as a feature, and should be saved in the same form as other features under the name "original" 
 * 
 * Derived Features
 * Features which have value "derived" in the "group" column (see feature_model_table below) are not expected on disk.
 * Instead these can be derived using a simple calculation on other features which are assumed to be present.
 * The only operation currently implemented for derived features is a difference between 2 other features, specified by 
 * listing the 2 feature names in the "operation" column of the feature_model_table, separated by a comma.
 * 
 * @param feature_model_table 
 * path to tsv file containing a table of information on features and which features are used in each model  
 * This is used to determine which features to load into feature stack so that the model can be applied.
 * The feature names (in the feature_name column) must match the subdirectory names within the feature directory
 * 
 * @param imageStackName
 * This is intended to be the name of the source image stack, but is only used in the name of the output file(s)
 * 
 * @param featurePath
 * Directory where the features are saved; expects a subdirectory for each required feature 
 * (feature_name in feature_model_table) containing the feature as an image sequence with slices named slice_xxxx.tif, 
 * where xxxx is the (padded) slice number, starting from zero. This allows all features for a given slice of the 
 * image to be loaded in isolation from other slices.
 * 
 * @param modelPath 
 * location of model file 
 * 
 * @param modelName
 * Name of model file (with .model suffix). This name must also match a column in feature_model_table, which contains
 * a "1" for each feature used, with no entry in other rows
 * 
 * @param savePath 
 * location to save segmented image and probability map
 * 
 * @param numberThreadsToUse
 * number of threads that ImageJ is asked to use. Default = 4. 
 * 
 * @param saveProbabilityMaps 
 * value 'true' or 'false': whether to save the estimated probability distribution across the clases for each voxel, 
 * as well as the semantic segmentation. Default = 'true'
 * 
 * @param pixelSize_unit, pixelSize_xy, pixelSize_z
 * Used to set image properties in the output files.
 * 
 * @param channel_grouping
 * An array of arrays that collectively contain each of the integers 0..(n-1) exactly once, where n is the number of classes in the model 
 * ("channels" parameter below).
 * This defines a partition of the classes which can optionally be used to specify groups of classes which should be considered together;
 * if the sum of the estimated probability for a voxel over group k is higher than the sum for any other group, then the allocated class will
 * be the estimated most probable class within group k, even if there is a class from outside group k with a higher individual probability.
 * For example, if classes 1 and 2 are considered quite similar, and class 3 is very distinct, and the estimated probability distribution over 
 * classes [1,2,3] is [0.31,0.34,0.35], then it may be considered more correct to label the voxel as class 2 than class 3.
 * The default (no grouping) value is [[0],[1],...,[n-1]]
 * 
 * @param channels
 * The number of segmentation classes defined by the supplied model. Must be specified. 
 */


#@ String feature_model_table
#@ String imageStackName
#@ String featurePath
#@ String modelPath
#@ String modelName
#@ String savePath
#@ String(value="4",persist=false) numberThreadsToUse
#@ String(value="true",persist=false) saveProbabilityMaps
#@ String pixelSize_unit
#@ Float pixelSize_xy
#@ Float pixelSize_z
#@ int[][](value=[[1]],persist=false) channel_grouping
#@ int (value=0,persist=false) channels

import java.text.SimpleDateFormat
import groovy.time.TimeCategory 
import groovy.time.TimeDuration
import ij.io.FileSaver;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.ImageStack;
import ij.CompositeImage;
import ij.measure.ResultsTable;
import trainableSegmentation.*;
import trainableSegmentation.FeatureStack;
import trainableSegmentation.FeatureStackArray;
import wekaSegInterface.ApplyClassifier;


println("starting apply_classifiers.groovy")
sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")

if (numberThreadsToUse != null){
	IJ.run("Memory & Threads...", "parallel="+ numberThreadsToUse +" run");
}
segmentator = new WekaSegmentation(true);

String fl = saveProbabilityMaps.substring(0,1).toUpperCase()
boolean probMaps =  (fl == "T" || fl == "Y")

if (numberThreadsToUse.isInteger()) {threadNum = numberThreadsToUse as Integer}

date = new Date(); println("Classifying using " + threadNum + " threads   " + sdf.format(date))
println("Probability maps " + (probMaps ? "WILL" : "will NOT") + " be produced")

model_features = ResultsTable.open(feature_model_table);

println();
classifyStart = new Date(); println("Applying model " + modelName + ".tif  " + sdf.format(classifyStart));
segmentator.loadClassifier(modelPath + modelName + ".model");

ArrayList<String> featureList = new ArrayList<String>()
ArrayList<Boolean> featureDerived = new ArrayList<String>()
ArrayList<String> featureParameters = new ArrayList<String>()
for (int r=0; r<model_features.size(); r++){
 	if (model_features.getStringValue(modelName,r) == "1"){
 		featureList.add(model_features.getStringValue("feature_name",r))
    	featureDerived.add(model_features.getStringValue("group",r).equals("derived"));
		featureParameters.add(model_features.getStringValue("operation",r));
    }
   }
println("Using " + featureList.size + " features");

probMapStart = new Date();
ImageStack probImageStack =  ApplyClassifier.classifyImage(featureList, featureDerived,featureParameters, featurePath, segmentator, threadNum, true);
println("prob map stack has dimensions " + probImageStack.getWidth() + "x" + probImageStack.getHeight() + "x" + probImageStack.getSize());


// check for channel parameter supplied
if (channels == 0) {
throw new IllegalArgumentException('Parameter "channels" must be specified');
}

// get default channel grouping if none supplied
if (channel_grouping.length == 1) {
channel_grouping = new int[channels][1];
for(int i = 0; i < channel_grouping.length; i++)
    channel_grouping[i][0] = i
}

ImageStack classifiedImageStack = ApplyClassifier.segFromProbMap(probImageStack,channel_grouping);
println("Generated image stack with dimensions " + classifiedImageStack.getWidth() + "x" + classifiedImageStack.getHeight() + "x" + classifiedImageStack.getSize());
probMapEnd = new Date();
float probDurationMinutes = TimeCategory.minus( probMapEnd, probMapStart ).toMilliseconds()/60000
println("Probability map using model " + modelName + " generated in " + probDurationMinutes + " minutes")

if (probMaps){
	saveName = imageStackName + "_prob_"  + modelName
	date = new Date(); println("Preparing to save " + saveName + "  " + sdf.format(date));
	ImagePlus probImage = new ImagePlus(saveName,probImageStack);
	    
	int stackSlices = probImage.getDimensions()[3]
	if ( (stackSlices % channels) == 0){
		println("reshaping");
		int sliceNum = stackSlices / channels
		probImage.setDimensions(channels, sliceNum,1)  // (int nChannels, int nSlices, int nFrames)
		probImage = new CompositeImage(probImage)
	}
		
	// IJ.run(probImage,"Multiply...", "value=255 stack");
	ImageStack imSt = probImage.getStack();
	int width=imSt.getWidth();int height=imSt.getHeight();int slices=imSt.getSize();
		for (int x=0;x<width;x++) {for (int y=0;y<height;y++) {for (int z=0;z<slices;z++) {
			double v = imSt.getVoxel(x,y,z);
			imSt.setVoxel(x,y,z,v*255.0);
		}}}
	probImage.setStack(imSt);
		
    IJ.run("Conversions...", " ");
    IJ.run(probImage, "8-bit", "");
    IJ.run("Conversions...", "scale");
    println("Setting probImage voxel dimensions to: " + pixelSize_xy+ ", " + pixelSize_xy + ", " + pixelSize_z + " (" + pixelSize_unit + ")");
    IJ.run(probImage, "Properties...", "unit="+pixelSize_unit+" pixel_width="+pixelSize_xy+" pixel_height="+pixelSize_xy+" voxel_depth="+pixelSize_z);
    cal = probImage.getCalibration();
    println("New voxel dimensions: " + cal.pixelWidth + ", " + cal.pixelHeight + ", " + cal.pixelDepth );
        
    def folderProb = new File( savePath + "probability_maps/")
	if( !folderProb.exists() ) {
     	 folderProb.mkdirs() // Create all folders up-to and including B
    }

	for (int _try=0;_try<3;_try++){
		save_worked=true
		try{
			new FileSaver( probImage ).saveAsTiff( savePath + "probability_maps/" + saveName + ".tif");
		} catch(Exception e) {
    		println("Exception: ${e}")
    		save_worked=false
		}
		if (save_worked){break;}
		sleep(30000)
	}
	if (!save_worked){
		println("failed to save probability map " + saveName + ".tif")
	} else {
        println("saved probability map " + savePath + "probability_maps/" + saveName + ".tif")
    }
}

println(imageStackName)
saveName = imageStackName + "_seg_"  + modelName
date = new Date(); println("Saving " + saveName + "  " + sdf.format(date));
ImagePlus classifiedImage = new ImagePlus(saveName,classifiedImageStack);
IJ.run(classifiedImage, "5 class v1", "");
println("Setting classifiedImage voxel dimensions to: " + pixelSize_xy+ ", " + pixelSize_xy + ", " + pixelSize_z + " (" + pixelSize_unit + ")");
IJ.run(classifiedImage, "Properties...", "unit="+pixelSize_unit+" pixel_width="+pixelSize_xy+" pixel_height="+pixelSize_xy+" voxel_depth="+pixelSize_z);
cal = classifiedImage.getCalibration();
println("New voxel dimensions: " + cal.pixelWidth + ", " + cal.pixelHeight + ", " + cal.pixelDepth );
def folderSeg = new File( savePath + "segmented/")
if( !folderSeg.exists() ) {folderSeg.mkdirs()}
save_worked=true
for (int _try=0;_try<3;_try++){		
	try{
		new FileSaver( classifiedImage ).saveAsTiff( savePath + "segmented/" + saveName + ".tif");
	} catch(Exception e) {
   		println("Exception: ${e}")
    	save_worked=false
	}
	if (save_worked){break;}
	sleep(30000)
}
if (!save_worked){
	println("failed to save segmentation " + saveName + ".tif")
} else {
    println("save segmentation " + savePath + "segmented/" + saveName + ".tif")
}

classifyEnd = new Date();
float durationMinutes = TimeCategory.minus( classifyEnd, classifyStart ).toMilliseconds()/60000
println("Image " + imageStackName + " segmented using model " + modelName + " in " + durationMinutes + " minutes (includes prob map if produced)")
System.exit(0);
