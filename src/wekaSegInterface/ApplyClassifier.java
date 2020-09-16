package wekaSegInterface;

import java.io.File;
import java.util.ArrayList;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ImageCalculator;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import trainableSegmentation.FeatureStack;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.WekaSegmentation;
import static segImAnalysis.Util.*;

public class ApplyClassifier {

	static int sliceNumberPadLength = 4;
	
	public static ImageStack classifyImage(
			ArrayList<String> features, 
			ArrayList<Boolean> featureDerived,
			ArrayList<String> featureParameters,
			String featurePath, 
			WekaSegmentation segmentator, 
			int threads, 
			boolean probMap) throws Exception{
		return(classifyImage(null,features,featureDerived,featureParameters,featurePath,segmentator,threads,probMap));
	}

	public static ImageStack classifyImage(
			ImagePlus image, 
			ArrayList<String> features, 
			ArrayList<Boolean> featureDerived,
			ArrayList<String> featureParameters,
			String featurePath, 
			WekaSegmentation segmentator, 
			int threads, 
			boolean probMap) throws Exception{
		//println("start classifyImage");
		
		int numSlices=-1;
		File rootFldr = new File(featurePath);
		String[] subflds = rootFldr.list();
		for (String sfn : subflds) {
			File sf = new File(rootFldr, sfn);
			if (!sf.isDirectory()) {
				continue;
			}
			int k = sf.list().length;
			if (numSlices==-1) {
				numSlices=k;
			} else {
				if (numSlices!=k) {
					println("!!! Error - inconsistent slice counts in " + featurePath);
					println("Found " + k + " files in " + sfn + "; previously found " + numSlices);
					return(null);
				}
			}
		}
		
		ImageStack probOrSegStack = null;
		for (int slice = 1 ; slice <= numSlices ; slice++){
			FeatureStack fs = loadFeatureStack(slice-1, features, featureDerived, featureParameters, featurePath, image==null?null:image.getStack().getProcessor(slice)); 
			FeatureStackArray fsa = new FeatureStackArray(1) ; fsa.set(fs, 0);
			ImagePlus result = segmentator.applyClassifier(fsa, threads, probMap);
			if (probMap){
				ImageStack classProbStack = result.getStack();
				for (int jj = 1 ; jj <= classProbStack.getSize() ; jj++){
					ImageProcessor newSlice = classProbStack.getProcessor(jj);
					if (probOrSegStack==null) {
						probOrSegStack = new ImageStack(newSlice.getWidth(),newSlice.getHeight());
					}
					probOrSegStack.addSlice(newSlice);
				}
			} else {
				ImageProcessor newSlice = result.getProcessor();
				if (probOrSegStack==null) {
					probOrSegStack = new ImageStack(newSlice.getWidth(),newSlice.getHeight());
				}
				probOrSegStack.addSlice(newSlice);
			}
		}
		return(probOrSegStack);
	}

	public static ImageStack segFromProbMap(ImageStack probImageStack, int channelCount) {		
		int width = probImageStack.getWidth();
		int height = probImageStack.getHeight();
		ImageStack segStack = new ImageStack(width,height);

		int sliceCount = probImageStack.size();
		if (sliceCount % channelCount != 0) {
			println("!!! Error - channelCount must divide the number of slices in probImageStack");
			return(null);
		}
		sliceCount = sliceCount / channelCount;
		for (int s=0; s<sliceCount; s++) {
			ImageProcessor sl = new ByteProcessor(width,height);
			for (int c=0;c<width;c++) {
				for (int r=0;r<height;r++) {
					int maxCh = 0;
					double currentMax = probImageStack.getVoxel(c, r, s*channelCount);
					for (int ch=1; ch< channelCount; ch++) {
						double v = probImageStack.getVoxel(c, r, s*channelCount+ch);
						if (v>currentMax) {
							currentMax = v;
							maxCh=ch;
						}
					}
					sl.set(c,r,maxCh);
				}
			}
			segStack.addSlice(sl);			
		}
		return(segStack);
	}

	// in this version, groupedClasses must form a partition of the classes, numbered from 0 to k
	// (where k ~ channelCount-1 in simple version) 
	public static ImageStack segFromProbMap(ImageStack probImageStack, int[][] groupedClasses) {
		int width = probImageStack.getWidth();
		int height = probImageStack.getHeight();
		ImageStack segStack = new ImageStack(width,height);

		// consistency check before starting
		int groupCnt = groupedClasses.length;
		ArrayList<Integer> classReps = new ArrayList<Integer>();
		for (int g=0;g<groupCnt;g++) {
			for (int ii=0;ii<groupedClasses[g].length;ii++) {
				int v = groupedClasses[g][ii];
				while (v>=classReps.size()) {
					classReps.add(0);
				}
				classReps.set(v,classReps.get(v)+1);
			}
		}
		int channelCount = classReps.size();
		for (int ii=0;ii<channelCount;ii++) {
			if (classReps.get(ii)!=1) {
				println("!!! segFromProbMap returning null - parameter groupedClasses does not give parition of classes 0-" + (channelCount-1));
				println("  class " + ii + " occurs " +classReps.get(ii) + " times" );
				return(null);
			}
		}
		// now to get on with it
		int sliceCount = probImageStack.size();
		if (sliceCount % channelCount != 0) {
			println("!!! Error - channelCount must divide the number of slices in probImageStack");
			return(null);
		}
		sliceCount = sliceCount / channelCount;
		// println(sliceCount);
		for (int s=0; s<sliceCount; s++) {
			ImageProcessor sl = new ByteProcessor(width,height);
			for (int c=0;c<width;c++) {
				for (int r=0;r<height;r++) {
					// first find the group of channels with highest total intensity
					int maxGr = 0;
					Double currentMax = null;
					//probImageStack.getVoxel(c, r, s*channelCount);
					for (int gr=0; gr< groupCnt; gr++) {
						double totalInt = 0;
						for (int ch : groupedClasses[gr]) {
							totalInt += probImageStack.getVoxel(c, r, s*channelCount+ch);
						}
						if (gr==0) {
							currentMax = totalInt;
						} else if (totalInt>currentMax){
							currentMax=totalInt;
							maxGr = gr;
						}
					}
					// now find the channnel within the selected group with highest intensity				
					int maxCh = 0;
					currentMax = null;
					for (int ch : groupedClasses[maxGr]) {
						double v = probImageStack.getVoxel(c, r, s*channelCount+ch);
						if (currentMax == null || v>currentMax) {
							currentMax = v;
							maxCh=ch;
						}
					}
					sl.set(c,r,maxCh);
				}
			}
			segStack.addSlice(sl);			
		}
		return(segStack);
	}
	

	private static FeatureStack loadFeatureStack(
			int sliceNumber, 
			ArrayList<String> features, 
			ArrayList<Boolean> featureDerived,
			ArrayList<String> featureParameters,
			String featurePath, 
			ImageProcessor original) throws Exception {
		
		int w=0;int h=0;
		ImageStack stack =null;
	
		for (int ii = 0; ii < features.size(); ii++){
			ImageProcessor fSlice = null;
			if (features.get(ii).equals("original") && original != null){
				fSlice=original;
			} else {

			ImagePlus sl;
			if (featureDerived.get(ii)) {
				String fp = featureParameters.get(ii);
				String[] baseFeatures = fp.split(",");
				if (baseFeatures.length != 2) {
					throw new Exception("Cannot calculate a derived feature from " + fp);
				} 
				String pth = featurePath + baseFeatures[0] + "/slice_" + String.format("%0" + sliceNumberPadLength + "d", sliceNumber) + ".tif";
				sl = IJ.openImage( pth );
				pth = featurePath + baseFeatures[1] + "/slice_" + String.format("%0" + sliceNumberPadLength + "d", sliceNumber) + ".tif";
				ImagePlus slice2 = IJ.openImage( pth );
				ImageCalculator ic = new ImageCalculator();
				ic.run("Subtract", sl, slice2);
				if (sl==null){
					println("!!! failed to perform calculation for slice " + sliceNumber + " and feature " + features.get(0) + "!!!");
					// println("   attempted load location: \n" + pth + "\n"); 
				}
			} else {
				String pth = featurePath + features.get(ii) + "/slice_" + String.format("%0" + sliceNumberPadLength + "d", sliceNumber) + ".tif";
				sl = IJ.openImage( pth );
				if (sl==null){
					println("!!! failed to load slice " + sliceNumber + " for feature " + features.get(0) + "!!!");
					println("   attempted load location: " + pth); 
				}
			}
			fSlice=sl.getProcessor();
			}
			
			if (stack==null) {
				w = fSlice.getWidth() ; h = fSlice.getHeight() ;
				stack = new ImageStack(w,h);
			} else {
				if (fSlice.getWidth() != w || fSlice.getHeight() != h){
					println("!!! Slice " + sliceNumber + " for feature " + features.get(0) + " does not match size of original image or previous feature !!!");
					println("previous size is " + w + "x" + h +"; current feature slice is " + fSlice.getWidth() + "x" + fSlice.getHeight()); 
				}
			}

			stack.addSlice(features.get(ii), fSlice);

		}
		FeatureStack featureStack = new FeatureStack( w, h, false );
		featureStack.setStack( stack );
		return(featureStack);
	}
}







