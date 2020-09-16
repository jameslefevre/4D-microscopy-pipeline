package segImAnalysis;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import static segImAnalysis.Util.println;

/**
 * 
 * @author James Lefevre
 *
 */
public class SegmentedImageAnalysis {

	public static void splitObjectAnalysis(
			ImagePlus segmentedImage,
			String saveFolderPath,
			int[] classesToAnalyse,
			int[] dynamic,
			int minVoxExtraObjects,
			int[] channelsToFill,
			int[] classLayers,
			int[][] incorporatedChannels, 
			boolean saveIntermediateImages) throws IOException {
		splitObjectAnalysis(segmentedImage, null,null,saveFolderPath, classesToAnalyse, dynamic, minVoxExtraObjects, channelsToFill, classLayers, incorporatedChannels, saveIntermediateImages, null, null, null);		
	}

	public static void splitObjectAnalysis(
			ImagePlus segmentedImage,
			ImagePlus originalImage,
			ImagePlus probImage,
			String saveFolderPath,
			int[] classesToAnalyse,
			int[] dynamic,
			int minVoxExtraObjects,
			int[] channelsToFill,
			int[] classLayers,
			int[][] incorporatedChannels, 
			boolean saveIntermediateImages) throws IOException {
		splitObjectAnalysis(segmentedImage, originalImage,probImage,saveFolderPath, classesToAnalyse, dynamic, minVoxExtraObjects, channelsToFill, classLayers, incorporatedChannels, saveIntermediateImages, null, null, null);		
	}
	
	
	public static void splitObjectAnalysis(
			ImagePlus segmentedImage,
			ImagePlus originalImage,
			ImagePlus probImage,
			String saveFolderPath,
			int[] classesToAnalyse,
			int[] dynamic,
			int minVoxExtraObjects,
			int[] channelsToFill,
			int[] classLayers,
			int[][] incorporatedChannels, 
			boolean saveIntermediateImages,
			ImagePlus objectSplitChannel,
			boolean[] useObjectSplitChannel, 
			int[] objectSplitChannel_channelMap
			) throws IOException {
		splitObjectAnalysis(segmentedImage, originalImage,probImage,saveFolderPath, classesToAnalyse, dynamic, minVoxExtraObjects, channelsToFill, classLayers, incorporatedChannels, new int[0],new int[0],saveIntermediateImages, objectSplitChannel, useObjectSplitChannel, objectSplitChannel_channelMap);		
	}
	
	public static void splitObjectAnalysis(
			ImagePlus segmentedImage,
			ImagePlus originalImage,
			ImagePlus probImage,
			String saveFolderPath,
			int[] classesToAnalyse,
			int[] dynamic,
			int minVoxExtraObjects,
			int[] channelsToFill,
			int[] classLayers,
			int[][] incorporatedChannels, 
			int[] channelsForDistanceMap,
			int[] smoothingErosionForDistanceMap,
			boolean saveIntermediateImages
			) throws IOException {
		splitObjectAnalysis(segmentedImage, originalImage,probImage,saveFolderPath, classesToAnalyse, dynamic, minVoxExtraObjects, channelsToFill, classLayers, incorporatedChannels, channelsForDistanceMap,smoothingErosionForDistanceMap,saveIntermediateImages, null, null, null);
	
	}

	/**
	 * 
	 * Analyse objects in a segmented 3D image.
	 * Analysis is performed by class using class specific parameters. Classes may correspond to the channels in the segmented images, but a hierachical class structure is also supported 
	 * in which a class may contain more than one channel of the segmented image, and a channel may occur in more than one class, resulting in overlap between objects in different classes.
	 * This hierachical structure has the following constaints:
	 *  1. Each class is assigned to a layer, which are numbered consecutively from 1
	 *  2. Each class contains one or more specified channels.
	 *  3. Each channel is contained in at most one class in each layer
	 *  4. A channel can only be contained in a class at a given layer if it is contained in a class at the previous layer.
	 * Objects will be assigned to the layer of the class to which they belong, which will be reflected in the adjacency analysis 
	 * 
	 * Channels, classes, layers and the objects generated are all identified by positive integer ids.
	 * 
	 * The following output files will be saved to the specified directory:
	 * 		object_map.tif, object_map2.tif ... for each layer, a 16-bit (or 32 if necessary) grayscale tiff stack with the same dimensions as the the input segmentation, recording the voxels belonging to each object in the layer
	 * 		objectStats.txt:	tab separated table with header, containing information on each object: id,class,within_class_id,x,y,z,voxels,channel
	 * 	within_class_id numbers objects within each class consecutively, while id is unique over all objects; (x,y,z) is the centre of mass position in voxel units; 
	 * 	channel is the mean value of the segmented image over the object voxels
	 * 		classSummary.txt: tab separated table with header giving the id range for the objects in each class plus the "dynamic: parameter used in the watershed split algorithm. 
	 * 		objectAdjacencyTable.txt: comma separated table without header that lists all non-zero adjacency counts between pairs of objects across all classes; 
	 * 	columns are object id 1, object id 2, adjacency count (defined below), with object id 1 <= object id 2
	 * 
	 * 2 voxels (x1,y1,z1), (x2,y2,z2) in the same layer are considered adjacent if the values |x1-x2|, |y1-xy|, |z1-z2| are all
	 * 0 or 1, except if all 3 values equal 0 (same voxel) or 1 (double diagonal); each non-edge voxel has 18 adjacent voxels in the same layer
	 * 2 voxels in consecutive layers are considered adjacent if they have exactly the same position
	 * The degree of adjacency between objects i and j is the number of pairs of adjacent voxels where the first voxel is in i and the second in j
	 * 
	 * 
	 * @param segmentedImage				3D, 8-bit, greyscale image with voxel values representing channel ids in segmentation (0=background)
	 * @param originalImage					optional greyscale image of same dimension as segmentedImage; if not null, mean value within each object is returned as "intensity"
	 * @param probImage						optional multichannel image of same dimension as segmentedImage; if not null, mean of each channel within each object is returned as prob0,prob1, etc
	 * @param saveFolderPath				All outputs saved to this location
	 * @param classesToAnalyse				The ids of the classes to analyse, each corresponding to one or more channels (voxel values) in segmentedImage
	 * @param dynamic						Parameter passed to the watershed split algorithm for each class, the minimum max to watershed difference in edge distance (or other provided channel) for a split to occur
	 * @param minVoxExtraObjects			For each class, each object (contiguous set of voxels in class) with at least this number of voxels is prevented from being removed by the watershed algorithm
	 * @param channelsToFill				Channels (in order) in which holes are filled. Any region that is entirely surrounded in 3D by the channel will be changed to the channel; 
	 * this may flip voxels from other channels as well as background 
	 * @param classLayers					The layer of each class, defining a hierachy; each channel may be included in at most one class per layer, and may only be used if it was used in previous layer
	 * @param incorporatedChannels			For each class, the incorported channels. If null, each class consists of the channel of the same id 
	 * @param channelsForDistanceMap
	 * @param smoothingErosionForDistanceMap
	 * @param saveIntermediateImages		Save various image files representing intermediate steps in the output folder.
	 * @param objectSplitChannel			Channel(s) to use in watershed split in place of edge distance. Single channel grayscale if objectSplitChannel_channelMap is null, otherwise 1 channel per class.
	 * @param useObjectSplitChannel			Whether each class should use objectSplitChannel (if False, revert to edge distance) 
	 * @param objectSplitChannel_channelMap	Gives the channel number in objectSplitChannel corresponding to each class; if null, single channel used for each class (if useObjectSplitChannel flag on for the class)
	 * @throws IOException
	 */
	public static void splitObjectAnalysis(
			ImagePlus segmentedImage,
			ImagePlus originalImage,
			ImagePlus probImage,
			String saveFolderPath,
			int[] classesToAnalyse,
			int[] dynamic,
			int minVoxExtraObjects,
			int[] channelsToFill,
			int[] classLayers,
			int[][] incorporatedChannels, 
			int[] channelsForDistanceMap,
			int[] smoothingErosionForDistanceMap,
			boolean saveIntermediateImages,
			ImagePlus objectSplitChannel,
			boolean[] useObjectSplitChannel, 
			int[] objectSplitChannel_channelMap
			) throws IOException {

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		println( "Started splitObjectAnalyis at " + sdf.format(new Date()));
		if (segmentedImage==null || segmentedImage.getType() != ImagePlus.GRAY8) {
			println( "Aborting at " + sdf.format(new Date()));		// ResultsTable unifiedObjectListMapping = new ResultsTable();
			println("  segmented image provided must be 8-bit");
			return;
		}
		int n = classesToAnalyse.length;
		if (dynamic.length!=n  || (useObjectSplitChannel!=null && useObjectSplitChannel.length!=n) || (classLayers!=null && classLayers.length!=n) || 
				(incorporatedChannels!=null && incorporatedChannels.length!=n) || ((classLayers==null)^(incorporatedChannels==null)) ) {
			println( "Aborting splitObjectAnalysis at " + sdf.format(new Date()));
			println(" classesToAnalyse, dynamic, useObjectSplitChannel, classLayers and incorporatedClasses must have same length");
			println("   (except that useObjectSplitChannel may be null and classLayers and incorporatedClasses may either both be null or both defined)");
			return;
		}
		if (channelsForDistanceMap!=null) {
			if (smoothingErosionForDistanceMap==null || smoothingErosionForDistanceMap.length != channelsForDistanceMap.length) {
				println( "Aborting at " + sdf.format(new Date()));
				println("  if channelsForDistanceMap is not null, then smoothingErosionForDistanceMap must be non-null also, and have the same length");
				println("  channelsForDistanceMap has length "+ channelsForDistanceMap.length + " ; smoothingErosionForDistanceMap " + (smoothingErosionForDistanceMap==null ? "is null" : ("has length "+smoothingErosionForDistanceMap.length) ));
				return;
			}
		}
		println("checking save folder: " + saveFolderPath);
		File directory = new File(saveFolderPath);
		if(!directory.exists()){println("Directory not found; making it"); directory.mkdirs();}

		Fill3D.fillHolesSpecifiedClasses(segmentedImage,channelsToFill); 
		println( "Filled 3D holes " + sdf.format(new Date()));
		


		if (saveIntermediateImages) {new FileSaver( segmentedImage ).saveAsTiff( saveFolderPath + "segmentation_with_holes_filled.tif" );}

		// need to get the total number of layers k, check that the layers are numbered 1-k, check that each channel is used at most once in each layer, 
		// and that the channels used at each layer is subset of previous
		int num_layers = 1;		
		if (classLayers!=null) {
			HashMap<Integer,ArrayList<Integer>> channels_by_layer = new HashMap<Integer,ArrayList<Integer>>();
			for (int classIndex =0; classIndex<classLayers.length; classIndex++) {
				int layer = classLayers[classIndex];
				if (layer>num_layers) {num_layers=layer;}
				if (layer<1) {
					println("classLayers parameter requires layer numbers to be positive integers - aborting splitObjectAnalysis");
					return;
				}
				if (!channels_by_layer.containsKey(layer)) {
					channels_by_layer.put(layer, new ArrayList<Integer>());
				}
				for (int ch : incorporatedChannels[classIndex]) {
					if (channels_by_layer.get(layer).contains(ch)) {
						println("Repeated channel within a layer - aborting splitObjectAnalysis");
						return;
					}
					channels_by_layer.get(layer).add(ch);
				}
			}
			if (channels_by_layer.keySet().size()!=num_layers) {
				println("classLayers parameter requires layer numbers to be consecutive - aborting splitObjectAnalysis");
				return;
			}
		}

		int[] objectIdClassOffsets = new int[classesToAnalyse.length];
		int nextClassOffset = 0;
		ImagePlus[] unifiedObjectMap = new ImagePlus[num_layers];
		for (int layer=1;layer<=num_layers;layer++) {
			unifiedObjectMap[layer-1] = segmentedImage.duplicate();
			IJ.run(unifiedObjectMap[layer-1], "16-bit", "");
			IJ.run(unifiedObjectMap[layer-1],"Multiply...", "value=0 stack");
		}
		
		ImagePlus[] classDistanceMaps = new ImagePlus[channelsForDistanceMap==null ? 0 : channelsForDistanceMap.length];
		
		
		// main loop through classes

		for (int classNumIndex=0; classNumIndex<classesToAnalyse.length; classNumIndex++) {
			int classNum = classesToAnalyse[classNumIndex];
			int layer = classLayers==null ? 1 : classLayers[classNumIndex];
			println( "Started class " + classNum + ", offset " + nextClassOffset + "   " + sdf.format(new Date()));
			ImagePlus bi = segmentedImage.duplicate();
			if (incorporatedChannels==null) {
				SegmentedImageOperations.selectChannel(bi, classNum);
			} else {
				SegmentedImageOperations.selectChannels(bi, incorporatedChannels[classNumIndex]);
			}

			if (saveIntermediateImages) {new FileSaver( bi ).saveAsTiff( saveFolderPath + "class_" + classNum + "_binary_image.tif" );}

			ImagePlus objectMap;
			if (objectSplitChannel!=null && useObjectSplitChannel!=null && useObjectSplitChannel[classNumIndex]) {	
				if (objectSplitChannel_channelMap==null) {
					objectMap = SplitObjects.watershedSplit(objectSplitChannel, bi, dynamic[classNumIndex],minVoxExtraObjects);
				} else {
					ImagePlus spltCh = null;
					for (int ch : incorporatedChannels[classNumIndex]) {
						ImagePlus c = new Duplicator().run(objectSplitChannel, objectSplitChannel_channelMap[ch], objectSplitChannel_channelMap[ch], 1, objectSplitChannel.getDimensions()[3], 1, 1); 
						// IJ.log(ch+","+objectSplitChannel_channelMap[ch]+","+objectSplitChannel.getDimensions()[3]+"\n");
						String str = ""; for (int i : c.getDimensions()) {str += i + ",";} IJ.log(str+"\n");
						if (spltCh==null) {spltCh=c;} else {
							ImageCalculator ic = new ImageCalculator();
							ic.run("Add stack", spltCh, c);
						}
					}

					objectMap = SplitObjects.watershedSplit(spltCh, bi, dynamic[classNumIndex],minVoxExtraObjects);
				}

			} else {
				objectMap = SplitObjects.distanceWatershedSplit(bi, dynamic[classNumIndex],minVoxExtraObjects);
			}


			nextClassOffset = SegmentedImageAnalysis.updateUnifiedObjectMap(objectMap, unifiedObjectMap[layer-1], null, objectIdClassOffsets, nextClassOffset, classNumIndex);
			if (saveIntermediateImages) {
				ImageTypeConversion.imageTypeChangeTrueValue(objectMap,"8-bit");
				new FileSaver( objectMap ).saveAsTiff( saveFolderPath + "class_" + classNum + "_object_map.tif" );
			}
			if (channelsForDistanceMap != null) {
				for (int ii=0;ii<channelsForDistanceMap.length;ii++) {
					if (channelsForDistanceMap[ii]!=classNum) {continue;}
					ImagePlus biSmoothed = SplitObjects.smoothedDistanceMap(bi, false);
					IJ.run(biSmoothed, "Macro...", "code=v=100*(v>"+smoothingErosionForDistanceMap[ii]+") stack");
					classDistanceMaps[ii] = SplitObjects.smoothedDistanceMap(biSmoothed, true);
				}
			}
			
		}
		if (saveIntermediateImages && channelsForDistanceMap != null) {
			for (int ii=0;ii<channelsForDistanceMap.length;ii++) {
				new FileSaver( classDistanceMaps[ii] ).saveAsTiff( saveFolderPath + "class_" + channelsForDistanceMap[ii] + "_distance_map.tif" );
			}	
		}
		
		println( "Saving object stats " + sdf.format(new Date()));
		ResultsTable obStats = GetObjectStats.getObjectProperties(unifiedObjectMap,segmentedImage, originalImage, probImage, classesToAnalyse, objectIdClassOffsets, channelsForDistanceMap, classDistanceMaps); 
		if (obStats==null) {println(" get null object stats object!");}
		obStats.save(saveFolderPath + "objectStats.txt");
		println("  saved to "+saveFolderPath + "objectStats.txt");

		println( "Calculating and saving object adjacencies " + sdf.format(new Date()));
		int maxId = (int) Math.round(obStats.getValue("id",obStats.size()-1));
		int[][] adj = GetAdjacencies.getSplitObjectAdjacencyTableWithLayers(unifiedObjectMap);
		GetAdjacencies.writeObjectAdjacencyTable(adj, saveFolderPath + "objectAdjacencyTable.txt", maxId, true);
		
		for (int layer = 1; layer <= num_layers; layer++) {
			try{IJ.run(unifiedObjectMap[layer-1], "glasbey on dark", "");} catch(Exception e) {}
			new FileSaver( unifiedObjectMap[layer-1] ).saveAsTiff( saveFolderPath + "object_map" + (layer==1 ? "" : layer) + ".tif" );
		}

		println( "Save class summary info " + sdf.format(new Date()));
		ResultsTable smry = new ResultsTable();
		for (int classNumIndex=0; classNumIndex<classesToAnalyse.length; classNumIndex++) {
			smry.incrementCounter();
			smry.addValue("class",classesToAnalyse[classNumIndex]);
			smry.addValue("id_min",objectIdClassOffsets[classNumIndex]+1);
			smry.addValue("id_max",classNumIndex == classesToAnalyse.length - 1 ? nextClassOffset : objectIdClassOffsets[classNumIndex+1]);			
			smry.addValue("watershed_parameter_dynamic",dynamic[classNumIndex]);		
		}
		smry.save(saveFolderPath + "classSummary.txt");				
	}


	// updates unifiedObjectMap, unifiedObjectListMapping, objectIdClassOffsets with results for classNum
	// returns nextClassOffset (updated classOffSet)
	// assumes that the first time method is called
	//   - unifiedObjectMap is a 16-bit zero value ImagePlus of correct dimension; 
	//   - unifiedObjectListMapping is initialised but empty;  
	//   - objectIdClassOffsets is zero valued vector of length maxClassNumber+1
	private static int updateUnifiedObjectMap(ImagePlus objectMap, ImagePlus unifiedObjectMap, ResultsTable unifiedObjectListMapping, int[] objectIdClassOffsets, int classOffSet, int classNum){
		objectIdClassOffsets[classNum] = classOffSet;
		int nextClassOffset = classOffSet;
		ArrayList<int[]> objectPixelCounts = SegmentedImageOperations.getCompactHistogram(objectMap);

		for (int[] oc : objectPixelCounts){
			nextClassOffset = oc[0] + classOffSet > nextClassOffset ? oc[0] + classOffSet : nextClassOffset;
			if (oc[0]==0 || unifiedObjectListMapping==null) continue;
			unifiedObjectListMapping.incrementCounter();
			unifiedObjectListMapping.addValue("class", classNum);
			unifiedObjectListMapping.addValue("id", oc[0]);
			unifiedObjectListMapping.addValue("global_id", oc[0] + classOffSet);
			unifiedObjectListMapping.addValue("voxels", oc[1]);

		}
		String bitDepth = "16-bit";
		if (nextClassOffset >= Math.pow(2,16) && unifiedObjectMap.getBitDepth() <= 16){
			if (classOffSet>Math.pow(2,16)){Util.println("!!!! error - too many object in classes prior to " + classNum + " - unified object map will be incorrect");}
			ImageTypeConversion.imageTypeChangeTrueValue(unifiedObjectMap,"32-bit");
			bitDepth = "32-bit";
		}

		ImagePlus objectMapShifted = objectMap.duplicate();
		ImageTypeConversion.imageTypeChangeTrueValue(objectMapShifted,bitDepth);
		IJ.run(objectMapShifted,"Macro...", "code=v=(v+" + classOffSet + ")*(v>0) stack");
		ImageCalculator ic = new ImageCalculator();
		ic.run("Add stack", unifiedObjectMap, objectMapShifted);  
		return(nextClassOffset);
	}

}
