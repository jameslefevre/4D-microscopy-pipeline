// TODO: check/finish doc below; check info in old version 8 (or latest)

#@ String feature_model_table
#@ String imageStackName
#@ String featurePath
#@ String modelPath
#@ String modelName
#@ String savePath
#@ String numberThreadsToUse
#@ String saveProbabilityMaps
#@ String pixelSize_unit
#@ Float pixelSize_xy
#@ Float pixelSize_z
#@ int[][] channel_grouping
#@ int channels

/**
 *
 * 
 * Apply a segmentation model using specified set of precalculated features
 * Core functions are in wekaSegInterface.ApplyClassifier (classifyImage and segFromProbMap), including loading of features for each slice, applying the segmentation model,
 * compling result into converting probability map to segmentation
 * 
 * 
 *  		(which is the argmax across the probability distribution)

 feature_model_table: path to tsv file containing a table of information on features and which features are used in each model 
   This is used to determine which features to load into feature stack so that the model can be applied.
 imageStackName: Used in name of output; use name of original stack or name that can be easily linked to it
 featurePath: directory where the features are saved; expect a subdirectory for each required feature (feature_name in feature_model_table) containing the feature as 
 		an image sequence with slices named slice_xxxx.tif, where x is the (padded) slice number, starting from zero. This allows all features for a given slice of the
 		image to be loaded in isolation from other slices.
 modelPath, modelName: location of model file; modelName also used to look up the features to load in feature_model_table
 savePath: location to save segmented image and probability map
 numberThreadsToUse: processing threads to request ImageJ to use
 saveProbabilityMaps: 'true' or 'false': whether to save the estimated probability distribution across the clases for each voxel, alongside the segmentation 
 pixelSize_unit, pixelSize_xy, pixelSize_z: Used to set image properties in the output files.
 */

 

// cropBox: min,max for x,y,z (0 indexed, endpoints included)

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

// int[][] channel_grouping = [[0],[1],[2,3]];
// int channels = 4

println("starting apply_classifiers.groovy")
sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")

if (numberThreadsToUse != null){
	IJ.run("Memory & Threads...", "parallel="+ numberThreadsToUse +" run");
}

modelNames = [modelName];
// modelNames = ["d10_all16_rf"]; // can hard code model names here - currently required to do a list

segmentator = new WekaSegmentation(true);

String fl = saveProbabilityMaps.substring(0,1).toUpperCase()
boolean probMaps =  (fl == "T" || fl == "Y")

int threadNum = 4 // default
if (numberThreadsToUse != null){
	if (numberThreadsToUse.isInteger()) {threadNum = numberThreadsToUse as Integer}
}
date = new Date(); println("Classifying using " + threadNum + " threads   " + sdf.format(date))
println("Probability maps " + (probMaps ? "WILL" : "will NOT") + " be produced")

model_features = ResultsTable.open(feature_model_table);

for (int m = 0 ; m < modelNames.size(); m++){
	println();
	classifyStart = new Date(); println("Applying model " + modelNames[m] + ".tif  " + sdf.format(classifyStart));
	segmentator.loadClassifier(modelPath + modelNames[m] + ".model");

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
		
	ImageStack classifiedImageStack = ApplyClassifier.segFromProbMap(probImageStack,channel_grouping);
    println("Generated image stack with dimensions " + classifiedImageStack.getWidth() + "x" + classifiedImageStack.getHeight() + "x" + classifiedImageStack.getSize());
    probMapEnd = new Date();
	float probDurationMinutes = TimeCategory.minus( probMapEnd, probMapStart ).toMilliseconds()/60000
	println("Probability map using model " + modelName + " generated in " + probDurationMinutes + " minutes")

	if (probMaps){
		saveName = imageStackName + "_prob_"  + modelNames[m]
	    date = new Date(); println("Preparing to save " + saveName + "  " + sdf.format(date));
	    ImagePlus probImage = new ImagePlus(saveName,probImageStack);
	    
		int stackSlices = probImage.getDimensions()[3]
		if ( (stackSlices % channels) == 0){
		  println("reshaping");
		  int sliceNum = stackSlices / channels
		  probImage.setDimensions(channels, sliceNum,1)  // (int nChannels, int nSlices, int nFrames)
		  probImage = new CompositeImage(probImage)
		}
		
		IJ.run(probImage,"Multiply...", "value=255 stack");
	    IJ.run("Conversions...", " ");
        IJ.run(probImage, "8-bit", "");
        IJ.run("Conversions...", "scale");
        println("Setting probImage voxel dimensions to: " + pixelSize_xy+ ", " + pixelSize_xy + ", " + pixelSize_z + " (" + pixelSize_unit + ")");
        IJ.run(probImage, "Properties...", "unit="+pixelSize_unit+" pixel_width="+pixelSize_xy+" pixel_height="+pixelSize_xy+" voxel_depth="+pixelSize_z);
        cal = probImage.getCalibration();
        println("New voxel dimensions: " + cal.pixelWidth + ", " + cal.pixelHeight + ", " + cal.pixelDepth );
        
    	def folderProb = new File( savePath + "probability_maps/")
		if( !folderProb.exists() ) {
     	 // Create all folders up-to and including B
     	 folderProb.mkdirs()
    	}
    	
	    // new FileSaver( probImage ).saveAsTiff( savePath + "probability_maps/" + saveName + ".tif");

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
   println(m)
   saveName = imageStackName + "_seg_"  + modelNames[m]
	date = new Date(); println("Saving " + saveName + "  " + sdf.format(date));
	ImagePlus classifiedImage = new ImagePlus(saveName,classifiedImageStack);
	IJ.run(classifiedImage, "5 class v1", "");
	println("Setting classifiedImage voxel dimensions to: " + pixelSize_xy+ ", " + pixelSize_xy + ", " + pixelSize_z + " (" + pixelSize_unit + ")");
	IJ.run(classifiedImage, "Properties...", "unit="+pixelSize_unit+" pixel_width="+pixelSize_xy+" pixel_height="+pixelSize_xy+" voxel_depth="+pixelSize_z);
	cal = classifiedImage.getCalibration();
    println("New voxel dimensions: " + cal.pixelWidth + ", " + cal.pixelHeight + ", " + cal.pixelDepth );
	def folderSeg = new File( savePath + "segmented/")
	if( !folderSeg.exists() ) {folderSeg.mkdirs()}
	//new FileSaver( classifiedImage ).saveAsTiff( savePath + "segmented/" + saveName + ".tif");
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

}
