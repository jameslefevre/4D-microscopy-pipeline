package wekaSegInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.process.ImageProcessor;

import static segImAnalysis.Util.*;

public class GetTrainingData {
	public static void main(String[] args) throws Exception {
		System.out.println("hi wrld");

		//String feature_model_table= "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/code/feature_model_table.txt";
		String feature_model_table= "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/data_and_model_info/feature_model_table.txt";
		String[] classNames = new String[] {"background","cell_body","tentpole","ruffle"};
		int sliceNumberPadLength = 4;
		
		//String wrkFdr = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/20_2019-03_new_data/new_training_data_2019-03/";
		//String wrkFdr = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/20_2019-03_new_data/new_training_data_2019-04/";
		String wrkFdr = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/23_2019-09_new_data/new_training_data_and_model/";
		
		/*
		saveTrainingDataEclipticalSelections(
				new String[] {"/data/james/image_data/LLS/20190221_slide2/cropped/"},
				new String[] {"c1-t001-et0000000_decon_cr_50p"},
				new String[] {"/data/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/20190221_slide2_cr_t1_50p/"},
				new String[] {wrkFdr + "20190221_slide2_cr_t1_v2.ijm"},
				null,
				"d15_ds1gd_rf",
				wrkFdr + "d15_20190221_slide2_cr_t1_50p_features_from_dimmed_image.arff",
				feature_model_table,
				classNames,
				sliceNumberPadLength);
		*/
		
		// String imFldr = "/data/james/image_data/LLS/20190221_slide2/cropped/";
		//String nm="c1-t001-et0000000_decon_cr";
		//String ff="/data/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/20190221_slide2_cr_t1/";
		//String imFldr = "/data/james/image_data/LLS/20190221_slide1cap2/cropped/";
		//String nm="c1-t030-et125642_decon_cr";
		//String ff="/data/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/20190221_slide1cap2_cr_t30/";
		//String imFldr = "/data/james/image_data/LLS/20190830_LLSM_Yvette/20190830_pos1/cropped/";
		//String nm="c1-t001-et0000000_decon";
		//String ff="/data/james/image_data/LLS/20190830_LLSM_Yvette/20190830_pos1/fs001_cropped_adjustedInt/";
		String imFldr = "/data/james/image_data/LLS/20190830_LLSM_Yvette/20190830_pre1/cropped/";
		String nm="c1-t200-et1044911_decon";
		String ff="/data/james/image_data/LLS/20190830_LLSM_Yvette/20190830_pre1/fs200_cropped_adjustedInt/";
		
		//String mcr=wrkFdr + "20190221_slide2_cr_t1_v2.ijm";
		//String mcr=wrkFdr + "20190221_slide2_cr_t1_v3.ijm";
		//String mcr=wrkFdr + "20190221_slide2_cr_t1_v4.ijm";
		//String mcr=wrkFdr + "20190221_slide1cap2_t30_v1.ijm";
		//String mcr=wrkFdr + "20190830_pos1_t1_v1.ijm";
		String mcr=wrkFdr + "20190830_pre1_t200_v1.ijm";
		saveTrainingDataEclipticalSelections(
				new String[] {imFldr,imFldr,imFldr,imFldr},
				new String[] {nm,nm,nm,nm},
				new String[] {ff,ff,ff,ff},
				new String[] {mcr,mcr,mcr,mcr},
				new float[] {0.5f,0.75f,1.0f,1.25f},
				"d16_ds1gd_rf",
				//wrkFdr + "d16_20190221_slide2_cr_t1_4int.arff",
				//wrkFdr + "d17_20190221_slide2_cr_t1_4int.arff",
				//wrkFdr + "20190221_slide1cap2_cr_t30_v1_4int.arff",
				//wrkFdr + "20190911_pos1_t1_v1_4int.arff",
				wrkFdr + "20190911_pre1_t200_v1_4int.arff",
				feature_model_table,
				classNames,
				sliceNumberPadLength); 
		/*
		saveTrainingDataEclipticalSelections(
				new String[] {imFldr,imFldr,imFldr,imFldr},
				new String[] {nm,nm,nm,nm},
				new String[] {ff,ff,ff,ff},
				new String[] {mcr,mcr,mcr,mcr},
				new float[] {0.5f,0.75f,1.0f,1.25f},
				"d16_ds1gd_rf",
				//wrkFdr + "d16_20190221_slide2_cr_t1_4int.arff",
				//wrkFdr + "d17_20190221_slide2_cr_t1_4int.arff",
				wrkFdr + "20190221_slide1cap2_cr_t30_v1_4int.arff",
				feature_model_table,
				classNames,
				sliceNumberPadLength); 
				*/
		/*
		saveTrainingDataEclipticalSelections(
				new String[] {imFldr},
				new String[] {nm},
				new String[] {ff},
				new String[] {mcr},
				null,
				"d15_ds1gd_rf",
				wrkFdr + "d16_20190221_slide2_cr_t1.arff",
				feature_model_table,
				classNames,
				sliceNumberPadLength); 
				*/
		/*
		saveTrainingDataEclipticalSelections(
				new String[] {"/data/james/image_data/LLS/20190221_slide2/cropped/"},
				new String[] {"c1-t001-et0000000_decon_cr"},
				new String[] {"/data/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/20190221_slide2_cr_t1/"},
				new String[] {wrkFdr + "20190221_slide2_cr_t1_v2.ijm"},
				new float[] {0.5f},
				"d15_ds1gd_rf",
				wrkFdr + "d15_20190221_slide2_cr_t1_50p.arff",
				feature_model_table,
				classNames,
				sliceNumberPadLength); 
				*/
		
		/*
		saveTrainingDataEclipticalSelections(
				new String[] {"/data/james/image_data/LLS/20190221_slide2/cropped/"},
				new String[] {"c1-t001-et0000000_decon_cr"},
				new String[] {"/data/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/20190221_slide2_cr_t1/"},
				new String[] {wrkFdr + "20190221_slide2_cr_t1_v2.ijm"},
				null,
				"d15_ds1gd_rf",
				wrkFdr + "d15_20190221_slide2_cr_t1.arff",
				feature_model_table,
				classNames,
				sliceNumberPadLength);
		*/
		/*
		saveTrainingDataEclipticalSelections(
				new String[] {"/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/18_2018-12_LPS/sample_data/"},
				new String[] {"D2_Pre_1_decon.sld - GFPLifeAct_RAWd2_noLPS_20ex5int deskewed_t1"},
				new String[] {"/data/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/LPS_2018_12_07_pre1_t1/"},
				new String[] {wrkFdr + "select_pre1_t1.ijm"},
				"d12_ds1gd_rf",
				wrkFdr + "pre1_t1_bg.arff",
				feature_model_table,
				classNames,
				sliceNumberPadLength);
		*/
		
		/*
		saveTrainingDataEclipticalSelections(
				new String[] {"/home/james/image_data/LLS/r2_150202_3_t0_crops_and_feature_stacks/crop2/",
					"/home/james/image_data/LLS/r2_150202_3/",
					"/home/james/image_data/LLS/r2_150202_3/"},
				new String[] {"crop2",
						"covered_488_30mW_ch0_stack0000_488nm_0000000msec_0024968946msecAbs_decon",
						"covered_488_30mW_ch0_stack0004_488nm_0011360msec_0024980306msecAbs_decon"},
				new String[] {"/home/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/r2_150202_3_stack0_crop2/",
							"/home/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/r2_150202_3_stack0/",
							"/home/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/r2_150202_3_stack4/"},
				new String[] {wrkFdr + "select10.ijm", wrkFdr + "select_st0_1.ijm", wrkFdr + "select_st4_2.ijm"},
				"d11_ds1_rf",
				wrkFdr + "d12_ds1.arff",
				feature_model_table,
				classNames,
				sliceNumberPadLength);
		*/
		
		println("done");		
	}
	
	static void saveTrainingDataEclipticalSelections(
			String[] imageStackLocations,
			String[] imageStackNames,
			String[] featureFolders,
			String[] selection_macro_paths,
			float[] intensityScalingFactor, // adjusts all features (including original) to mimic multiplying image intensity by this factor; if null then no scaling		
			String modelName,
			String savePath,
			String feature_model_table,
			String[] classNames,
			int sliceNumberPadLength) throws Exception{
		
		int numSources = imageStackLocations.length;
		if (imageStackNames.length != numSources || featureFolders.length != numSources || selection_macro_paths.length != numSources) {
			println("imageStackLocations, imageStackNames, featureFolders and selection_macro_paths must all have same length");
			return;
		}
		ResultsTable model_features = ResultsTable.open(feature_model_table);
		println(model_features.size());
		ArrayList<String> featureList = new ArrayList<String>();
		ArrayList<String> featureParameters = new ArrayList<String>();
		ArrayList<Boolean> featureDerived = new ArrayList<Boolean>();
		for (int r=0; r<model_features.size(); r++){
			// println(model_features.getStringValue(modelName,r));
			if (model_features.getStringValue(modelName,r).equals("1")){
				featureList.add(model_features.getStringValue("feature_name",r));
				featureDerived.add(model_features.getStringValue("group",r).equals("derived"));
				featureParameters.add(model_features.getStringValue("operation",r));				
			}
		}
		println("Feature count is " + featureList.size());
		ArrayList<ArrayList<Float>> td = new ArrayList<ArrayList<Float>>();
		for (int src=0;src<numSources;src++) {
			println("Image stack " + imageStackNames[src]);
			ArrayList<int[]> ovalTable = getTableOfEllipticalTrainingRegionsFromSelectionMacro(selection_macro_paths[src]);
			for (int[] ov : ovalTable ) {for (int ii=0;ii<ov.length;ii++) {print("\t" + ov[ii]);} println();}
			println("Number of elliptical regions loaded: " + ovalTable.size());
			ArrayList<int[]> vxls = voxelAndClassListFromOvals(ovalTable);
			println("Number of voxels loaded: " + vxls.size());
			int[] classCounts = new int[classNames.length];
			for (int[] vx : vxls) {classCounts[vx[3]]++;}
			for (int cc: classCounts) {print(cc+" ");}
			println();
			ArrayList<ArrayList<Float>> td_inc = 
					getTrainingData(vxls, featureList, featureDerived, featureParameters,
							imageStackLocations[src]+imageStackNames[src] + ".tif", featureFolders[src], sliceNumberPadLength);
			if (intensityScalingFactor!=null) {
				for (ArrayList<Float> rw : td_inc) {
					for (int ii=0;ii<rw.size()-1;ii++) {
						// last number in row encodes the class, so must not be scaled
						if (featureList.get(ii).contains("Structure") || featureList.get(ii).contains("Variance")) {
							rw.set(ii,rw.get(ii)*intensityScalingFactor[src]*intensityScalingFactor[src]);
						} else {
							rw.set(ii,rw.get(ii)*intensityScalingFactor[src]);
						}
					}
				}
			}
			td.addAll(td_inc);
		}
		writeArff(td,featureList,classNames,savePath);
	}
	
	static void saveTrainingDataManualSelection() throws Exception {
		String feature_model_table= "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/code/feature_model_table.txt";
		String[] classNames = new String[] {"background","cell_body","tentpole","ruffle"};
		int sliceNumberPadLength = 4;

		// d10_all16 - on crop2
		/*
		String imageStackLocation = "/home/james/image_data/LLS/r2_150202_3_t0_crops_and_feature_stacks/crop2/"; 
		String imageStackName = "crop2";
		String featureFolder = "/home/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/r2_150202_3_stack0_crop2/";
		String selection_macro_path = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/8_reorganise_compare_models/select10.ijm";		
		String savePath = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/"
				+ "15_new_models_with_downsampling_for_features/training_data/d10_all16.arff";		
		String modelName = "d10_all16_rf";
		 */

		// d11_all16 - on stack4
		/*
		String imageStackLocation = "/home/james/image_data/LLS/r2_150202_3/"; 
		String imageStackName = "covered_488_30mW_ch0_stack0004_488nm_0011360msec_0024980306msecAbs_decon";
		String featureFolder = "/home/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/r2_150202_3_stack4/";
		String selection_macro_path = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/13_improve_segmentation/select_st4_1.ijm";		
		String savePath = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/"
				+ "15_new_models_with_downsampling_for_features/training_data/d11_all16_added.arff";		
		String modelName = "d11_all16_rf";
		*/
		
		// d10 selection on crop 2 with d11_ds1 features
		/*
		String imageStackLocation = "/home/james/image_data/LLS/r2_150202_3_t0_crops_and_feature_stacks/crop2/"; 
		String imageStackName = "crop2";
		String featureFolder = "/home/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/r2_150202_3_stack0_crop2/";
		String selection_macro_path = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/8_reorganise_compare_models/select10.ijm";		
		String savePath = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/"
				+ "15_new_models_with_downsampling_for_features/training_data/d11_ds1.arff";		
		String modelName = "d11_ds1_rf";
		*/
		
		// d11 selection on stack4 with d11_ds1 features
		/*
		String imageStackLocation = "/home/james/image_data/LLS/r2_150202_3/"; 
		String imageStackName = "covered_488_30mW_ch0_stack0004_488nm_0011360msec_0024980306msecAbs_decon";
		String featureFolder = "/home/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/r2_150202_3_stack4/";
		String selection_macro_path = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/13_improve_segmentation/select_st4_1.ijm";		
		String savePath = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/"
				+ "15_new_models_with_downsampling_for_features/training_data/d11_ds1_added.arff";		
		String modelName = "d11_ds1_rf";
		*/
		
		// d10 selection on crop 2 with d11_ds1gd_rf features
			/*	
				String imageStackLocation = "/home/james/image_data/LLS/r2_150202_3_t0_crops_and_feature_stacks/crop2/"; 
				String imageStackName = "crop2";
				String featureFolder = "/home/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/r2_150202_3_stack0_crop2/";
				String selection_macro_path = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/8_reorganise_compare_models/select10.ijm";		
				String savePath = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/"
						+ "15_new_models_with_downsampling_for_features/training_data/d11_ds1gd.arff";		
				String modelName = "d11_ds1gd_rf";
				
		*/
		// d11 selection on stack4 with d11_ds1gd_rf features
		/*
				String imageStackLocation = "/home/james/image_data/LLS/r2_150202_3/"; 
				String imageStackName = "covered_488_30mW_ch0_stack0004_488nm_0011360msec_0024980306msecAbs_decon";
				String featureFolder = "/home/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/r2_150202_3_stack4/";
				String selection_macro_path = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/13_improve_segmentation/select_st4_1.ijm";		
				String savePath = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/"
						+ "15_new_models_with_downsampling_for_features/training_data/d11_ds1gd_added.arff";		
				String modelName = "d11_ds1gd_rf";
				*/
		
		// d12 selection on stack4 with d11_ds1gd_rf features
				
						String imageStackLocation = "/home/james/image_data/LLS/r2_150202_3/"; 
						String imageStackName = "covered_488_30mW_ch0_stack0004_488nm_0011360msec_0024980306msecAbs_decon";
						String featureFolder = "/home/james/image_data/LLS/feature_stacks_and_processed_images/feature_stacks/r2_150202_3_stack4/";
						String selection_macro_path = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/13_improve_segmentation/select_st4_1.ijm";		
						String savePath = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/"
								+ "15_new_models_with_downsampling_for_features/training_data/d11_ds1gd_added.arff";		
						String modelName = "d11_ds1gd_rf";
						
		


		// String modelName = "d11_ds1_rf";


		ResultsTable model_features = ResultsTable.open(feature_model_table);
		println(model_features.size());

		ArrayList<String> featureList = new ArrayList<String>();
		ArrayList<String> featureParameters = new ArrayList<String>();
		ArrayList<Boolean> featureDerived = new ArrayList<Boolean>();
		for (int r=0; r<model_features.size(); r++){
			// println(model_features.getStringValue(modelName,r));
			if (model_features.getStringValue(modelName,r).equals("1")){
				featureList.add(model_features.getStringValue("feature_name",r));
				featureDerived.add(model_features.getStringValue("group",r).equals("derived"));
				featureParameters.add(model_features.getStringValue("operation",r));				
			}
		}
		println("Feature count is " + featureList.size());
		// for (int ii=0; ii<featureList.size(); ii++) {println(featureList.get(ii) + "; " + featureDerived.get(ii) + "; " + featureParameters.get(ii));}

		ArrayList<int[]> ovalTable = getTableOfEllipticalTrainingRegionsFromSelectionMacro(selection_macro_path);
		for (int[] ov : ovalTable ) {for (int ii=0;ii<ov.length;ii++) {print("\t" + ov[ii]);} println();}
		println("Number of elliptical regions loaded: " + ovalTable.size());
		ArrayList<int[]> vxls = voxelAndClassListFromOvals(ovalTable);
		println("Number of voxels loaded: " + vxls.size());

		ArrayList<ArrayList<Float>> td = getTrainingData(vxls, featureList, featureDerived, featureParameters, imageStackLocation+imageStackName + ".tif", featureFolder, sliceNumberPadLength);		
		writeArff(td,featureList,classNames,savePath);
	}
	
	
	

	// voxClassList members have form [x,y,z,classNum]
	static ArrayList<ArrayList<Float>> getTrainingData(

			ArrayList<int[]> voxClassList, 
			ArrayList<String> featureList, 
			ArrayList<Boolean> featureDerived,
			ArrayList<String> featureParameters,
			// Float intensityScalingFactor,
			String imagePath, 
			String featureFolder,
			int sliceNumberPadLength) throws Exception{
		ArrayList<ArrayList<Float>> td = new ArrayList<ArrayList<Float>>();
		// initialise td and get max z value; these z values are expected to be 1-indexed
		println("method getTrainingData called with features: ");
		println(featureList);
		int maxZ = 0;
		for (int ii=0 ; ii< voxClassList.size() ; ii++){
			td.add(new ArrayList<Float>());
			int z = voxClassList.get(ii)[2];
			if (z > maxZ){maxZ=z;}
		}
		println("max z = " + maxZ);


		for (int featureNum =0; featureNum< featureList.size(); featureNum++){			
			String feature = featureList.get(featureNum);
			// load feature slices
			// println(feature);
			if (feature.equals("original")) {
				// load the original image and start each voxel record with the original voxel intensity
				ImageStack originalImage  = IJ.openImage( imagePath).getStack();
				println("loaded original image, size " + originalImage.getWidth()  + "," + originalImage.getHeight() + "," + originalImage.getSize());
				for (int ii=0 ; ii< voxClassList.size() ; ii++){		
					int[] vox = voxClassList.get(ii);
					// println("voxel " + ii + " is " + vox[0] + "," + vox[1] + "," + vox[2]);
					td.get(ii).add((float) originalImage.getVoxel(vox[0],vox[1],vox[2]-1));
				}
			} else {
				ImageProcessor[] featureSlices;
				if (featureDerived.get(featureNum)) {
					featureSlices = calculateFeatureSlices(featureFolder, featureParameters.get(featureNum), maxZ, sliceNumberPadLength);
				} else {
					featureSlices = loadFeatureSlices(featureFolder, feature, maxZ, sliceNumberPadLength);
				}

				// add feature value to each voxel record
				for (int ii=0 ; ii< voxClassList.size() ; ii++){
					int[] vox = voxClassList.get(ii);
					// println("voxel " + ii + " is " + vox[0] + "," + vox[1] + "," + vox[2]);
					double val = featureSlices[vox[2]-1].getf(vox[0],vox[1]);
					td.get(ii).add((float) val);
					// if (ii<3){println("voxel " + ii + " " + feature + "=" + val);}
				}
			}
		}

		// finally, add class number to each voxel record
		for (int ii=0 ; ii< voxClassList.size() ; ii++){
			int[] vox = voxClassList.get(ii);
			td.get(ii).add((float) vox[3]);
		}
		return(td);
	}

	static ImageProcessor[] calculateFeatureSlices(String featurePath, String featureParams, int maxZ, int sliceNumberPadLength) throws Exception{
		ImageProcessor[] featureSlices = new ImageProcessor[maxZ];
		String[] baseFeatures = featureParams.split(",");
		if (baseFeatures.length != 2) {
			throw new Exception("Cannot calculate a derived feature from " + featureParams);
		} 
		for (int z=0 ; z<maxZ ; z++){
			String pth = featurePath + baseFeatures[0] + "/slice_" + String.format("%0" + sliceNumberPadLength + "d", z) + ".tif";
			ImagePlus slice = IJ.openImage( pth );
			pth = featurePath + baseFeatures[1] + "/slice_" + String.format("%0" + sliceNumberPadLength + "d", z) + ".tif";
			ImagePlus slice2 = IJ.openImage( pth );
			ImageCalculator ic = new ImageCalculator();
			ic.run("Subtract", slice, slice2);

			featureSlices[z] = slice.getProcessor();
		}
		return(featureSlices);
	}

	static ImageProcessor[] loadFeatureSlices(String featurePath, String featureName, int maxZ, int sliceNumberPadLength){
		ImageProcessor[] featureSlices = new ImageProcessor[maxZ];
		for (int z=0 ; z<maxZ ; z++){
			String pth = featurePath + featureName + "/slice_" + String.format("%0" + sliceNumberPadLength + "d", z) + ".tif";
			// println(pth);
			featureSlices[z] = IJ.openImage( pth ).getProcessor();
		}
		return(featureSlices);
	}

	// assumes consistent data, otherwise may get index out of bound or bad arff file
	static void writeArff(
			ArrayList<ArrayList<Float>> trainingData, 
			ArrayList<String> featureList,  
			String[] classNames, 
			String arffSavePath) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter arffWriter = new PrintWriter(arffSavePath, "UTF-8");
		arffWriter.println("@relation segment");
		arffWriter.println();
		// arffWriter.println("@attribute original numeric"); // if original not included in feature list but I need it anyway

		for (String ftr : featureList){
			println("writing feature " + ftr + " to arff file attributes");
			arffWriter.println("@attribute " + ftr +  " numeric");
		}
		String classList = StringUtils.join(classNames, ",");
		arffWriter.println("@attribute class {" + classList + "}");
		arffWriter.println();
		arffWriter.println("@data");
		int writtenLines = 0;
		for (ArrayList<Float> datRow : trainingData){
			int n = datRow.size();
			for (int ii=0; ii<n-1; ii++){
				arffWriter.print(datRow.get(ii) + ",");
			}
			int classNum = Math.round(datRow.get(n-1));
			arffWriter.println(classNames[classNum]);
			writtenLines++;
		}
		arffWriter.close();
		println("completed writing " + writtenLines + " lines of data");
	}

	public static ArrayList<int[]> getTableOfEllipticalTrainingRegionsFromSelectionMacro(String macro_path) throws IOException{
		ArrayList<int[]> ovalTable = new ArrayList<int[]>();
		File macro = new File(macro_path);
		List<String> macro_lines = FileUtils.readLines(macro);

		int[] ovalParams = null;
		for (String line : macro_lines){
			// println(line);
			String[] wrds = line.split("\\(");
			if (wrds[0].equals("makeOval")){
				ovalParams = new int[4];
				String[] prms = (wrds[1].split("\\)"))[0].split(",");
				// println("extracted " + prms.length + " oval parameters");
				for (int ii=0; ii<4; ii++){
					// println(prms[ii].replaceAll("\\s",""));
					ovalParams[ii] = Integer.parseInt(prms[ii].replaceAll("\\s",""));
				}
			}
			if (wrds[0].equals("call")){
				wrds = line.split("\"");
				if (wrds[0].equals("call(") && wrds[1].equals("trainableSegmentation.Weka_Segmentation.addTrace")){
					// println(wrds[3] + " " + wrds[5]);
					int[] ovalArea = new int[6];
					// x,y,z,rx,ry,class
					ovalArea[0] = ovalParams[0] ; ovalArea[1] = ovalParams[1] ; ovalArea[3] = ovalParams[2] ; ovalArea[4] = ovalParams[3];
					ovalArea[2] = Integer.parseInt(wrds[5]) ; ovalArea[5] = Integer.parseInt(wrds[3]);    
					ovalTable.add(ovalArea);
				}
			}
		}
		return(ovalTable);
	}

	public static ArrayList<int[]> voxelAndClassListFromOvals(ArrayList<int[]> ovalTable){
		ArrayList<int[]> vxs = new ArrayList<int[]>();
		for (int[] oval : ovalTable){
			ArrayList<int[]> pxs = ellipsePixels(oval[0], oval[1], oval[3], oval[4]);
			for (int[] px : pxs){
				vxs.add(new int[] {px[0],px[1],oval[2],oval[5]});
			}
		}
		return(vxs);
	}

	public static ArrayList<int[]> ellipsePixels(int posx, int posy, int dx, int dy){
		ArrayList<int[]> pxs = new ArrayList<int[]>();
		for (int x = posx; x < posx+dx ; x++){
			float tx = (float) (x+0.5-posx-dx/2.0);
			float maxYsq = (float) ((dy*dy/4.0) * (1-tx*tx*4.0/(dx*dx)));
			for (int y = posy; y <= posy+dy ; y++){
				float ty = (float) (y+0.5-posy-dy/2.0);
				if (ty*ty <= maxYsq){
					pxs.add(new int[] {x,y});
				}
			}
		}
		return(pxs);
	}

}
