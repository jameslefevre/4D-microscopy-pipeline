package segImAnalysis;

import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.process.StackStatistics;

public class SegmentedImageOperations {
	
	/**
	 * Generate a binary mask from an integer valued image and a selected pixel value
	 * 
	 * @param im
	 * @param classNumber
	 */
	public static void selectChannel(ImagePlus im, int pixelValue){
		selectChannel(im, pixelValue, 255);
	}
	public static void selectChannel(ImagePlus im, int pixelValue, int saveValueForChannel){
		IJ.run(im, "Macro...", "code=v=" + saveValueForChannel + "*(v==" + pixelValue + ") stack");
		IJ.run(im, "Grays", ""); // had issue with LUT inherited from original image which mapped 0 and classNumber to 0 ... 
		// when converting from 8-bit color to 8-bit, it appears that conversion will be via LUT even given following option
		IJ.run("Conversions...", " ");
	    IJ.run(im, "8-bit", "");
	    IJ.run("Conversions...", "scale");
	}
	public static void selectChannels(ImagePlus im, int[] classNumbers){
		selectChannels(im, classNumbers, 255);
	}
	public static void selectChannels(ImagePlus im, int[] classNumbers, float saveValue){
		IJ.run(im, "Grays", ""); 
		IJ.run("Conversions...", " ");
	    IJ.run(im, "8-bit", "");
	    
		ImageStack imSt = im.getStack();
		int width = imSt.getWidth();
		int height = imSt.getHeight();
		int voxSum = 0;
		for (int s=0; s<imSt.size(); s++) {
			for (int c=0;c<width;c++) {
				for (int r=0;r<height;r++) {
					int v = (int) Math.round(imSt.getVoxel(c, r, s));
					voxSum+=v;
					float ret = 0;
					for (int cl : classNumbers) {
						if (v==cl) {ret=saveValue;}
					}
					imSt.setVoxel(c,r,s,ret);
				}
			}
		}
		System.out.println("voxel sum " + voxSum);
	}
	
	public static int[] getStackHistogramInteger(ImagePlus im){
		assert(im.getBitDepth() <=16);
		int vals = (int) Math.pow(2,im.getBitDepth());
	    StackStatistics st = new StackStatistics(im, vals,0,vals-1);
	    return(st.histogram);
	}
	public static ArrayList<int[]> getCompactHistogram(ImagePlus im){
		ArrayList<int[]> h = new ArrayList<int[]>();
	    int[] histogram;
	    if (im.getBitDepth() <=16){
	    	histogram = getStackHistogramInteger(im);
	    } else {
	    	StackStatistics st = new StackStatistics(im); // can't do integer bins, so hope for the best
	    	histogram = st.histogram;
	    }

	    for (int ii=0; ii< histogram.length; ii++){
	    	if (histogram[ii]>0){
	    		int[] entry = {ii,histogram[ii]};
	    		h.add(entry);
	    	}
	    }
	    return(h);
	}
	
	public static int[][] confusionMatrix(ImagePlus om1, ImagePlus om2){
		int[] dims = om1.getDimensions();
		int[] dims_check = om2.getDimensions();
		for (int ii=0 ; ii<5; ii++){
			if (dims[ii] != dims_check[ii]){
				IJ.log("!!! object map images do not have same dimensions and cannot be compared - aborting !!!");
				Util.println(dims) ; Util.println(dims_check);
				return(null);
			}
		}
		Util.println(dims);
		int maxVoxelValue1 = (int) (new StackStatistics(om1)).max;
		int maxVoxelValue2 = (int) (new StackStatistics(om2)).max;
		ImageStack ims1 = om1.getImageStack();
		ImageStack ims2 = om2.getImageStack();
		int[][] cm = new int[maxVoxelValue1+1][maxVoxelValue2+1];
		for (int z=0; z<dims[3]; z++){
			// println(z)
			for (int x=0; x<dims[0]; x++){
				for (int y=0; y<dims[1]; y++){
					int v1 = (int) Math.round(ims1.getVoxel(x,y,z));
					int v2 = (int) Math.round(ims2.getVoxel(x,y,z));
					cm[v1][v2] += 1;
				}
			}
		}
		return(cm);
	}
	public static ResultsTable confusionMatrixNonZero(ImagePlus om1, ImagePlus om2){
		int[][] cm = confusionMatrix(om1,om2);
		ResultsTable rt = new ResultsTable();
		for (int ii=0; ii<cm.length; ii++){
			for (int jj=0; jj<cm[ii].length; jj++){
				if (cm[ii][jj] == 0){continue;}
				rt.incrementCounter();
				rt.addValue("map1_id",ii); rt.addValue("map2_id",jj); rt.addValue("voxels",cm[ii][jj]);
			}
		}
		return(rt);
	}
	public static void saveConfusionMatrixNonZero(ImagePlus om1, ImagePlus om2, String saveName){
		ResultsTable rt = confusionMatrixNonZero(om1,om2);
		Util.println(rt);
		IJ.log("Saving to " + saveName);
		rt.save(saveName);
	}

}
