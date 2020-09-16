package segImAnalysis;

import ij.measure.ResultsTable;
import ij.process.StackStatistics;

import static segImAnalysis.Util.*;

import ij.ImagePlus;
import ij.ImageStack;

/**
 * Static methods to get summary object information from objects map or an array of object maps (images with integer voxel values denoting the object to which the voxel belongs)
 * Core method is getObjectPropertiesArray, returning a 2d array in which the first index is object id, and the information for each object is [x,y,z,voxels,channelMean,intensity,prob0,prob1,...]
 * (x,y,z) is the centre of mass in voxel units; the value of z is increased by 1 so that it becomes 1-indexed (instead of 0-indexed, as x and y remain); this is for consistency with ImageJ convention 
 * channelMean is the mean value over the object voxels of a separate single-channel image of the same size as the object map, which may optionally be provided (intended to be class/channel); if null, channelMean =0 
 * intensity is the mean value over the object voxels of a separate single-channel image of the same size as the object map, which may optionally be provided (intended to be original image); if null, intensity=0
 * prob[i] is object mean for channel i of a separate multi-channel image of the same size as the object map, which may optionally be provided (intended to be class probabilities); if null, prob[i]=0 
 * Other methods are to allow for different inputs, transform the results into a ResultsTable with explicit object id, and to class id 
 * 
 * @author james
 *
 */
public class GetObjectStats {

	public static double[][] getObjectPropertiesArray(ImagePlus obImage){
		ImagePlus[] obImages = new ImagePlus[] {obImage};
		return(getObjectPropertiesArray(obImages,null,null,null,null));
	}
//	public static double[][] getObjectPropertiesArray(ImagePlus obImage, ImagePlus channelImage){
//		ImagePlus[] obImages = new ImagePlus[] {obImage};
//		return(getObjectPropertiesArray(obImages,channelImage,null,null,null,null));
//	}
//	public static double[][] getObjectPropertiesArray(ImagePlus[] obImages){
//		return(getObjectPropertiesArray(obImages,null,null,null,null,null));
//	}
//	public static double[][] getObjectPropertiesArray(ImagePlus[] obImages, ImagePlus channelImage, ImagePlus originalImage, ImagePlus probImage){
//		return(getObjectPropertiesArray(obImages,channelImage,originalImage,probImage,null,null));
//	}

	// voxel value must correspond to object id which is unique across all obImages
	
	// fields calculated:	
	// x y z voxels channel intensity vx vy vz cxy cxz cyz {probi, i=1..chCount}, {di, i=1..distMapCount}, {dvi, i=1..distMapCount}, {mini, i=1..distMapCount}, {maxi, i=1..distMapCount}
	// where chCount is number of channels in probImage, distMapCount is classDistanceMaps.length
	// number fields = 12 + chCount + 4*distMapCount (classDistanceMaps.length)
	public static double[][] getObjectPropertiesArray(ImagePlus[] obImages, ImagePlus channelImage, ImagePlus originalImage, ImagePlus probImage, ImagePlus[] classDistanceMaps){
		int maxVoxelValue = 0;
		for (ImagePlus obImage : obImages) {
			int maxVox = (int) (new StackStatistics(obImage)).max;
			if (maxVox>maxVoxelValue) {maxVoxelValue=maxVox;}
		}
		int[] dims = obImages[0].getDimensions();
		if (channelImage != null) {
			int[] dims_check = channelImage.getDimensions();
			for (int ii=0;ii<dims.length;ii++) {
				if (dims[ii]!=dims_check[ii]) {
					println("!!! in getObjectPropertiesArray, classImage must be null or have the same dimensions as obImage - not calculating object channel mean");
					channelImage = null;
				}
			}
		}
		if (originalImage != null) {
			int[] dims_check = originalImage.getDimensions();
			for (int ii=0;ii<dims.length;ii++) {
				if (dims[ii]!=dims_check[ii]) {
					println("!!! in getObjectPropertiesArray, originalImage must be null or have the same dimensions as obImage - not calculating object intensity");
					originalImage = null;
				}
			}
		}
		if (probImage != null) {
			int[] dims_check = probImage.getDimensions();
			for (int ii=0;ii<dims.length;ii++) {
				if (dims[ii]!=dims_check[ii]) {
					if (ii!=2) {
						println("!!! in getObjectPropertiesArray, probImage must be null or have the same dimensions as obImage - not calculating object probs");
						probImage = null;
					} else {
						println("Check: obImage has "+dims[ii]+" channels, probImage has "+dims_check[ii]);
					}
				}
			}
		}
		println(dims);
		println(maxVoxelValue);
//		public static double[][] getObjectPropertiesArray(ImagePlus obImage){
//		return(getObjectPropertiesArray(obImage,null));
//	}

		ImageStack ims_channels = channelImage == null ? null : channelImage.getImageStack();
		ImageStack ims_original = originalImage == null ? null : originalImage.getImageStack();
		ImageStack ims_probs = probImage == null ? null : probImage.getImageStack();
		int chCount = probImage==null? 0 : probImage.getDimensions()[2];
		int distMapCount = classDistanceMaps==null? 0 : classDistanceMaps.length;
		double[][] op = new double[maxVoxelValue+1][12 + chCount + 4*distMapCount];
		for (double[] r : op) {
			for (int classIndex = 0; classIndex<distMapCount ; classIndex++) {
				r[12+chCount+2*distMapCount+classIndex] = Double.MAX_VALUE; // default value for minimum distance
				r[12+chCount+3*distMapCount+classIndex] = Double.MIN_VALUE; // default value for maximum distance
			}
		}
		for (ImagePlus obImage : obImages) {
			ImageStack ims = obImage.getImageStack();

			for (int z=0; z<dims[3]; z++){
				// println(z)
				for (int x=0; x<dims[0]; x++){
					for (int y=0; y<dims[1]; y++){
						int v = (int) ims.getVoxel(x,y,z);
						// println(x + " " + y + " " + z + " " + v)
						double[] r = op[v];
						// println(r)
						r[0] += x ; r[1] += y ; r[2] += (z+1) ; r[3] += 1; // z (slice number) is considered to be 1-indexed in coord system, while x and y are 0-index
						if (ims_channels != null) {
							r[4] += (int) ims_channels.getVoxel(x,y,z);
						}
						if (ims_original != null) {
							r[5] += (int) ims_original.getVoxel(x,y,z);
						}
						r[6] += x*x; r[7] += y*y; r[8] += (z+1)*(z+1); ; r[9] += x*y ; r[10]+=x*(z+1) ; r[11]+=y*(z+1);  
						
						if (ims_probs != null) {
							for (int ch=0;ch<chCount;ch++) {
								r[12+ch] += (int) ims_probs.getVoxel(x,y,z*chCount+ch);
							}
							
						}
						for (int classIndex = 0; classIndex<distMapCount ; classIndex++) {
							double d = classDistanceMaps[classIndex].getImageStack().getVoxel(x,y,z);
							r[12+chCount+classIndex] += d; // mean distance
							r[12+chCount+distMapCount+classIndex] += d*d; // distance variance
							r[12+chCount+2*distMapCount+classIndex] = d<r[12+chCount+2*distMapCount+classIndex] ? d : r[12+chCount+2*distMapCount+classIndex]; // distance min
							r[12+chCount+3*distMapCount+classIndex] = d>r[12+chCount+3*distMapCount+classIndex] ? d : r[12+chCount+3*distMapCount+classIndex]; // distance max
						}
					}
				}
			}
		}
		for (int ii=0; ii< op.length; ii++){
			for (int col=0;col<12 + chCount;col++) {
				if (col==3) {
					continue;
				}
				op[ii][col] /= op[ii][3];
			}
			op[ii][6] -= op[ii][0]*op[ii][0];
			op[ii][7] -= op[ii][1]*op[ii][1];
			op[ii][8] -= op[ii][2]*op[ii][2];
			op[ii][9] -= op[ii][0]*op[ii][1];
			op[ii][10] -= op[ii][0]*op[ii][2];
			op[ii][11] -= op[ii][1]*op[ii][2];
			
			for (int col=0;col<distMapCount;col++) {
				op[ii][12+chCount+col] /= op[ii][3];
				op[ii][12+distMapCount+chCount+col] /= op[ii][3];
				op[ii][12+distMapCount+chCount+col] -= op[ii][12+chCount+col]*op[ii][12+chCount+col];
			}
			
		}
		return(op);
	}
	
	// conversion to ResultsTable in simplest case
	public static ResultsTable getObjectProperties(ImagePlus obImage){
		double[][] op = getObjectPropertiesArray(obImage);
		ResultsTable rt = new ResultsTable();
		for (int ii=0; ii<op.length; ii++){
			double[] r=op[ii];
			rt.incrementCounter();
			rt.addValue("id",ii); rt.addValue("x",r[0]); rt.addValue("y",r[1]); rt.addValue("z",r[2]); rt.addValue("voxels",r[3]);
		}
		return(rt);
	}
	public static void getObjectProperties(ImagePlus obImage, String saveName){
		ResultsTable rt = getObjectProperties(obImage);
		println(rt);
		println("Saving to " + saveName);
		rt.save(saveName);
	}

	// analyse a multiclass, possibly multilayer object map and attach class numbers based on id offsets provided  
	public static ResultsTable getObjectProperties(ImagePlus obImage, int[] classesToAnalyse, int[] objectIdClassOffsets){
		ImagePlus[] obImages = new ImagePlus[] {obImage};
		return(getObjectProperties(obImages,null,null,null,classesToAnalyse,objectIdClassOffsets,null,null));
	}

	public static ResultsTable getObjectProperties(ImagePlus[] obImages, ImagePlus channelImage, ImagePlus originalImage, ImagePlus probImage, int[] classesToAnalyse, int[] objectIdClassOffsets, int[] channelsForDistanceMap, ImagePlus[] classDistanceMaps){
		int n = classesToAnalyse.length;
		if (n != objectIdClassOffsets.length) {
			println("!!! getObjectProperties required vectors classesToAnalyse and objectIdClassOffsets to have same length");
			return(null);
		}
		double[][] op = getObjectPropertiesArray(obImages,channelImage,originalImage,probImage,classDistanceMaps);
		ResultsTable rt = new ResultsTable();
		int classIndex=0;
		for (int ii=0; ii<op.length; ii++){
			while (classIndex < n-1 && ii>objectIdClassOffsets[classIndex+1]) {classIndex++;}
			double[] r=op[ii];
			int chCount = probImage==null? 0 : probImage.getDimensions()[2];
			int distMapCount = classDistanceMaps==null? 0 : classDistanceMaps.length;
			
			rt.incrementCounter();
			// println("Adding row")

			rt.addValue("id",ii); 
			if (ii<=objectIdClassOffsets[0]) {
				rt.addValue("class",0); 
				rt.addValue("within_class_id",ii);
			} else {
				rt.addValue("class",classesToAnalyse[classIndex]); 
				rt.addValue("within_class_id",ii - objectIdClassOffsets[classIndex]);
			}
			rt.addValue("x",r[0]); rt.addValue("y",r[1]); rt.addValue("z",r[2]); rt.addValue("voxels",r[3]);
			if (channelImage!=null) {
				rt.addValue("channel",r[4]);
			}
			if (originalImage!=null) {
				rt.addValue("intensity",r[5]);
			}
			rt.addValue("vx",r[6]);rt.addValue("vy",r[7]);rt.addValue("vz",r[8]);rt.addValue("cxy",r[9]);rt.addValue("cxz",r[10]);rt.addValue("cyz",r[11]);
			
			for (int ch=0;ch<chCount;ch++) {
				rt.addValue("prob"+ch,r[12+ch]);
			}
			for (int jj=0;jj<distMapCount;jj++) {
				int ch = channelsForDistanceMap[jj];
				rt.addValue("d"+ch,r[12+chCount+jj]);
				rt.addValue("dv"+ch,r[12+chCount+distMapCount+jj]);
				rt.addValue("dMin"+ch,r[12+chCount+2*distMapCount+jj]);
				rt.addValue("dMax"+ch,r[12+chCount+3*distMapCount+jj]);
			}
		}
		return(rt);
	}
}
