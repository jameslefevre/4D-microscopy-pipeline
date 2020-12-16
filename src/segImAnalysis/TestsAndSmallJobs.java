package segImAnalysis;

import static segImAnalysis.Util.println;
import static segImAnalysis.SegmentedImageAnalysis.splitObjectAnalysis;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.measure.ResultsTable;
import sc.fiji.skeletonize3D.Skeletonize3D_;

//import segImAnalysis.Skeletonise3D_jl;
// import org.apache.commons.math3.linear; //.EigenDecomposition;

@SuppressWarnings("unused")
public class TestsAndSmallJobs {
	
	static String imFolder = "/home/james/image_data/LLS/feature_stacks_and_processed_images/classified_images/r2_150202_3_d10_all16_rf/";
	
	
	public static void main(String[] args) throws IOException {
		
		testMedialSurfaceAlgorithm("/data/james/image_data/LLS/20190830_LLSM_Yvette/20190830_pos4/medial_surface_test_stack1/c1-t001-et0000000_decon_seg_d19_rep1ds1gd_rf.tif",
				"/data/james/image_data/LLS/20190830_LLSM_Yvette/20190830_pos4/medial_surface_test_stack1/med_surfaces_cells.tif");
		
		
		// System.out.println("hi world");
		// imageConversionTests();
		// convertProbMapsForVR();
		// objectsplitDebug();
		//segmentedImageVoxCounts("/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/16_object_analysis_hpc_and_tracking/2018-10-30_watershed_tests/act4_t0_d/object_map2.tif");
//		segmentedImageVoxIntersect(
//				"/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/16_object_analysis_hpc_and_tracking/2018-10-30_watershed_tests/act4_t0_d/class_4_object_map.tif",
//				"/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/16_object_analysis_hpc_and_tracking/2018-10-30_watershed_tests/act4_t0_f/class_4_object_map.tif"
//				);
		
		// testing int overflow / double precision stuff
//		int maxInt = Integer.MAX_VALUE;
//		int minInt = Integer.MIN_VALUE;
//		println("max / min int: "+maxInt+", "+minInt);
//		println(" plus/minus 1: " +(maxInt+1)+", "+(minInt+1));
//		double maxIntAsDouble = maxInt;
//		println("max int (+1) as double: " + maxIntAsDouble+", "+(maxIntAsDouble+1));
//		double x = 1000;
//		println("x: "+x+"; (x+1)^2-x^2-2x : " + ((x+1)*(x+1)-x*x-2*x));
//		x=10000000;
//		println("x: "+x+"; (x+1)^2-x^2-2x : " + ((x+1)*(x+1)-x*x-2*x));
//		x*=10;
//		println("x: "+x+"; (x+1)^2-x^2-2x : " + ((x+1)*(x+1)-x*x-2*x));
//		x*=10;
//		println("x: "+x+"; (x+1)^2-x^2-2x : " + ((x+1)*(x+1)-x*x-2*x));
//		x*=10;
//		println("x: "+x+"; (x+1)^2-x^2-2x : " + ((x+1)*(x+1)-x*x-2*x));
//		x = 2147483647;
//		println("x: "+x+"; (x+1)^2-x^2-2x : " + ((x+1)*(x+1)-x*x-2*x));
//		x *=3;;
//		println("x: "+x+"; (x+1)^2-x^2-2x : " + ((x+1)*(x+1)-x*x-2*x));
		
		//splitObjectanalysisTest();
		println("done"); 

	}
	
	private static void testMedialSurfaceAlgorithm(String loadPath, String savePath) {		
		System.out.println("begin medial surface test");
		long timeStart = System.currentTimeMillis();
		System.out.println("loading " + loadPath);
		ImagePlus msk  = IJ.openImage( loadPath ); 	
		System.out.println("convert to mask");
		IJ.run(msk, "Macro...", "code=v=255*(v==1) stack"); // select class 3 = ruffles
	  	ImageTypeConversion.imageTypeChangeTrueValue(msk, "8-bit");
	  	IJ.run(msk, "8-bit", ""); // im.setType(0) doesn't quite work, so resort to macro command
	  	System.out.println("run erosion algorithm");
	  	//Skeletonise3D_ skel3D = new Skeletonise3D_();
		//skel3D.setup("surface",msk);
		//skel3D.run(null);
	  	MedialSurface3D_ ms3D = new MedialSurface3D_();
	  	ms3D.setup("",msk);
	  	ms3D.run(null);
	  	
		System.out.println("saving to " + savePath);
		new FileSaver( msk).saveAsTiff( savePath);
		println("done in " + (System.currentTimeMillis()-timeStart)/1000 + " seconds");
	}
	
	private static void splitObjectanalysisTest() throws IOException {
		println("Start segmented image analysis");

		// String imageStackLocation = "/run/user/1000/gvfs/smb-share:server=shares02.rdm.uq.edu.au,share=arcml2018-q0705/image_data/r2_150205_act4/d12_ds1gd_rf/segmented/";	
		// String imageStackLocation = "/data/james/image_data/LLS/feature_stacks_and_processed_images/classified_images/r2_150202_3/d12_ds1gd_rf/segmented/";		
		//String imageStackName = "covered_488_30mW_ch0_stack0000_488nm_0000000msec_0024968946msecAbs_decon";
		
//		String imageStackLocation = "/data/james/image_data/LLS/feature_stacks_and_processed_images/classified_images/r2_150205_act4/d12_ds1gd_rf/segmented/";	
//		String imageStackName = "488_30mW_ch0_stack0000_488nm_0000000msec_0024178851msecAbs_decon";
//		
//		String deconImageStackLocation = "/data/james/image_data/LLS/r2_150205_act4/";
//		String probMapStackLocation = "/data/james/image_data/LLS/feature_stacks_and_processed_images/classified_images/r2_150205_act4/d12_ds1gd_rf/probability_maps/";	
		
		String imageStackLocation = "/data/james/image_data/LLS/20190830_LLSM_Yvette/20190830_pos4/d19_intAdj_rep1ds1gd_rf/segmented/";
		String imageStackName = "c1-t001-et0000000_decon";
		String probMapStackLocation = "/data/james/image_data/LLS/20190830_LLSM_Yvette/20190830_pos4/d19_intAdj_rep1ds1gd_rf/probability_maps/";
				
		
		 
		// String modelName = "d11_all16_rf";
		//String modelName = "d12_ds1gd_rf";
		String modelName = "d19_rep1ds1gd_rf";
		//String saveFolder = "/home/james/image_data/LLS/feature_stacks_and_processed_images/classified_images/r2_150202_3/" 
		//+ modelName + "/splitObjectAnalysis/" + imageStackName + "/";
		// String saveFolder = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/16_object_analysis_hpc_and_tracking/2018-09-28_splitting_algorithm/";
		
		//String imageStackLocation = "/home/james/image_data/LLS/feature_stacks_and_processed_images/classified_images/r2_150202_3_d11_all16_rf/";		
		//String imageStackName = "Classified_Image_covered_488_30mW_ch0_stack0000_488nm_0000000msec_0024968946msecAbs_decon_d11_all16_rf";
		//String saveFolder = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/15_new_models_with_downsampling_for_features/testing_object_analysis_code_2018_08_27/d11_all16_rf_stack0/";
		// String saveFolder = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/16_object_analysis_hpc_and_tracking/2018-10-30_watershed_tests/act4_t0_f/"; // seg2_t922 etc
		//String saveFolder = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/19_2019-02_object_analysis/alternate_object_split_2019-03-06/";
		String saveFolder = "/data/james/image_data/LLS/20190830_LLSM_Yvette/20190830_pos4/d19_intAdj_rep1ds1gd_rf/objectAnalysisTest1/";
		
		String imagePath = imageStackLocation + imageStackName + "_seg_" + modelName + ".tif";
		println("Opening " + imagePath);
		ImagePlus classMapImage  = IJ.openImage( imagePath ); 
		//imagePath = deconImageStackLocation + imageStackName + ".tif";
		//println("Opening " + imagePath);
		//ImagePlus deconImage  = IJ.openImage( imagePath ); 
		//GaussianBlur3D.blur(deconImage, 2.0, 2.0, 2.0);
		imagePath = probMapStackLocation + imageStackName + "_prob_" + modelName + ".tif"; // "Probability_Maps_" +
		println("Opening " + imagePath);
		ImagePlus probMap  = IJ.openImage( imagePath ); 
		//GaussianBlur3D.blur(probMap, 2.0, 2.0, 2.0);
		
		
		
		
		splitObjectAnalysis(
				classMapImage,null,probMap,				
				saveFolder,
				new int[] {1,2,3,4}, //classesToAnalyse
				//new int[] {10000,10000,10000,10000}, 
				// new int[] {4000,4000,4000,4000}, // watershed param "dynamic" - depends on channel used for split // with layer approach, want to make sure that parents of pole objects are retained.
				new int[] {200,20,20,20}, // watershed param "dynamic" - depends on channel used for split // with layer approach, want to make sure that parents of pole objects are retained.
				75, // minVoxExtraObjects
				new int[] {1,3,2},//, // channels to fill
				new int[] {1,2,2,1}, // layers
				new int[][] {{1},{2},{3},{2,3}}, // incorporatedChannels (image channels comprising each class)
				new int[] {1,2,3}, //channelsForDistanceMap
				new int[] {300,20,20},// smoothingErosionForDistanceMap
				//null,null,
				true
						);
		
//		splitObjectAnalysis(
//				classMapImage,null,null,				
//				saveFolder,
//				new int[] {1,2,3,4}, //classesToAnalyse
//				//new int[] {10000,10000,10000,10000}, 
//				// new int[] {4000,4000,4000,4000}, // watershed param "dynamic" - depends on channel used for split // with layer approach, want to make sure that parents of pole objects are retained.
//				new int[] {25,25,25,25}, // watershed param "dynamic" - depends on channel used for split // with layer approach, want to make sure that parents of pole objects are retained.
//				75, // minVoxExtraObjects
//				new int[] {1,3,2},//, // channels to fill
//				new int[] {1,2,2,1}, // layers
//				new int[][] {{1},{2},{3},{2,3}}, // incorporatedChannels (image channels comprising each class)
//				true,
//				probMap, //deconImage,
//				new boolean[] {true,true,true,true}, // useObjectSplitChannel
//				new int[] {1,2,3,4} // objectSplitChannel_channelMap
//						);
		
		/*
		splitObjectAnalysis(
				classMapImage,
				saveFolder,
				new int[] {1,2,3,4}, //classesToAnalyse
				new int[] {200,20,80,80}, // watershed param "dynamic" // with layer approach, want to make sure that parents of pole objects are retained.
				75, // minVoxExtraObjects
				new int[] {1,3,2},//, // channels to fill
				new int[] {1,2,2,1}, // layers
				new int[][] {{1},{2},{3},{2,3}}, // incorporatedChannels (image channels comprising each class)
				true
						);*/

		println("done");
	}
	
	
	
	private static void segmentedImageVoxCounts(String pth) {
		ImagePlus segImage  = IJ.openImage( pth ); 
		int[] x = SegmentedImageOperations.getStackHistogramInteger(segImage);
		for (int id =0; id<x.length; id++) {
			if (x[id]>0) {
				println(id + "\t" + x[id]);
			}
		}
		
	}
	
	private static void segmentedImageVoxIntersect(String pth1,String pth2) {
		ImagePlus segImage1  = IJ.openImage( pth1 ); 
		ImagePlus segImage2  = IJ.openImage( pth2 );
		int[][] cm = SegmentedImageOperations.confusionMatrix(segImage1, segImage2);
		for (int ii=0; ii<cm.length; ii++){
			for (int jj=0; jj<cm[ii].length; jj++){
				if (cm[ii][jj] == 0){continue;}
				println(ii + "\t" + jj + "\t" + cm[ii][jj]);
			}
		}		
	}
	
	
	// method to replicate last bit of splitObjectAnalysis; load object maps from disk to create object stats and adjacencies tables
		// also need to load original seg and do hole filling so I can get object classes 
		
	private static void saveObjectStatsAndAdjacencies(
				ImagePlus[] unifiedObjectMap,
				ImagePlus classMap,
				String savePath,
				int[] classesToAnalyse,
				int[] objectIdClassOffsets) throws IOException {
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			println( "Saving object stats " + sdf.format(new Date()));
			ResultsTable obStats = GetObjectStats.getObjectProperties(unifiedObjectMap,classMap, null,null,classesToAnalyse, objectIdClassOffsets,null,null); 
			obStats.save(savePath + "objectStats.txt");
			
			println( "Calculating and saving object adjacencies " + sdf.format(new Date()));
			int maxId = (int) Math.round(obStats.getValue("id",obStats.size()-1));
			int[][] adj = GetAdjacencies.getSplitObjectAdjacencyTableWithLayers(unifiedObjectMap);
			//int[][] adj = GetAdjacencies.getObjectAdjacencyTable18Neighbour(unifiedObjectMap, false);
			//int[][] adj2 = GetAdjacencies.getSplitObjectAdjacencyTableViaDam18Neighbour(unifiedObjectMap,watershedDams);
			GetAdjacencies.writeObjectAdjacencyTable(adj, savePath + "objectAdjacencyTable.txt", maxId, true);		
		}
				
		
	
	private static void convertProbMapsForVR() throws IOException {
		String id_map_path = imFolder + "/time_course/time_step_file_names.txt";
		ResultsTable rt = ResultsTable.open(id_map_path);
		int[] channelSelection = {3,2,4};
		String nm;
		for (int ii=4; ii<rt.size(); ii++) {		
			nm = rt.getStringValue("fileName", ii);
			println(ii); println(nm);
			ImagePlus pm = IJ.openImage(imFolder + 
					"probability_maps/Probability_Maps_" + nm + ".tif"); 
			pm = ImageTypeConversion.makeComposite(pm,4,channelSelection);
		    pm.setDisplayMode(IJ.COMPOSITE);
			new FileSaver( pm ).saveAsTiff(imFolder + 
					"probability_maps/Prob_Maps_" + nm + ".tif");
		}
	}
	
	
	
	private static void imageConversionTests() {
		String nm = "covered_488_30mW_ch0_stack0000_488nm_0000000msec_0024968946msecAbs_decon_d10_all16_rf";
		String testFdr = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/12_object_analysis_and_tracking/";
		ImagePlus ci = IJ.openImage(imFolder + 
				"Classified_Image_covered_488_30mW_ch0_stack0000_488nm_0000000msec_0024968946msecAbs_decon_d10_all16_rf.tif"); 
		ImagePlus pm = IJ.openImage(imFolder + 
				"probability_maps/Probability_Maps_" + nm + ".tif"); 
		// These should be of the standard output types (as of 2018-07)
		// ci is 8 bit colour (LUT), with index 0 mapped to 0/black,  pm is 4 channel 8-bit composite
		int[] channelSelection = {3,2,4};
		// ImageTypeConversion.convertProbMapTo8bitRGB(pm, channelSelection);
		pm = ImageTypeConversion.makeComposite(pm,4,channelSelection);
	    pm.setDisplayMode(IJ.COMPOSITE);
		new FileSaver( pm ).saveAsTiff( testFdr + "Prob_Maps_" + nm + ".tif");
	}
	
	
	// test selectClass (create binary mask from segmented image given specified class number)
	private static void saveBinaryClassImagesFromCI(String imagePath, String savePath){
		ImagePlus image  = IJ.openImage( imagePath ); 
		saveBinaryClassImagesFromCI(image, savePath);
	}
	private static void saveBinaryClassImagesFromCI(ImagePlus ci, String savePath){
		int n = (int) Math.round(ci.getStatistics().max);
	    for (int ii =0 ; ii<= n ; ++ii){
	    	ImagePlus ch = ci.duplicate();
	    	SegmentedImageOperations.selectChannel(ch,ii);
	     new FileSaver( ch ).saveAsTiff( savePath + "seg_class_" + ii + ".tif" );
	    }
	}
	
	private static void testSkeletonisation() {
		System.out.println("hi wrld");
		long timeStart = System.currentTimeMillis();
		String workingFolder = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/19_2019-02_object_analysis/test_skeletonisation/";
		// skeletoniseClassMaskTest(workingFolder+"object_map2.tif",workingFolder);
		// skeletoniseAllObjectsTest(workingFolder+"object_map2.tif",workingFolder);
		
		/*ResultsTable rt = ResultsTable.open(workingFolder+"branchDetails.csv");
		rt.save(workingFolder+"branchDetails_trip_resave.csv");
		ResultsTable rt2 = mergeNumericalResultsTable(rt,rt);
		rt2 = mergeNumericalResultsTable(rt2,rt);
		rt2.save(workingFolder+"branchDetails_trip.csv"); */
		
		ImagePlus objectMap  = IJ.openImage( workingFolder+"object_map2.tif" ); 
		ResultsTable skels = GetSkeletons.skeletoniseTouchingObjects(objectMap,workingFolder);
		skels.save(workingFolder + "branchDetails.csv");
		println("done in " + (System.currentTimeMillis()-timeStart)/1000 + " seconds");
	}

}
