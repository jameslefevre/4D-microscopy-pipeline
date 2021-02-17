package segImAnalysis;

import java.util.HashMap;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.GaussianBlur3D;
import ij.plugin.ImageCalculator;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.distanceMap3d.EDT;
import inra.ijpb.data.image.Images3D;
import inra.ijpb.watershed.ExtendedMinimaWatershed;
import inra.ijpb.binary.BinaryImages;

/**
 * Provides static methods to split objects in a 3D segmentation mask
 * Core algorithm is inra.ijpb.watershed.ExtendedMinimaWatershed
 * watershedSplit expects a second grayscale image for watershedding
 * distanceWatershedSplit calculates the distance from the edge of the mask and uses this to watershed
 * The following post-processing is applied:
 *   dams (layers of voxels separating the generated objects) are removed, with voxels assigned to the dominant neighbouring object
 *   Objects which were removed in the watershedding process but are larger than the parameter minVoxExtraObjects are added back to the object map
 *   Any object voxels which are not within the mask (due to quirk in watershed algorithm) are removed from the object (set to zero)
 * @author james
 *
 */
public class SplitObjects {
	static int connectivity_value = 6; // defines neighbourhood in watershed algorithm. Must be 6 or 26
	
	public static ImagePlus watershedSplit(ImagePlus objectSplitSignal, ImagePlus mask,  int dynamic, int minVoxExtraObjects) {
		
		ImagePlus msk = mask.duplicate();
		
		//IJ.run(msk, "Macro...", "code=v=1*(v>0) stack");
		ImageStack imSt = msk.getStack();
		int width=imSt.getWidth();int height=imSt.getHeight();int slices=imSt.getSize();
		for (int x=0;x<width;x++) {for (int y=0;y<height;y++) {for (int z=0;z<slices;z++) {
			double v = imSt.getVoxel(x,y,z);
			imSt.setVoxel(x,y,z,v>0 ? 1 : 0 );
		}}}
		msk.setStack(imSt);
		
		ImageCalculator ic = new ImageCalculator();
		ImagePlus spt = ic.run("Multiply create stack", objectSplitSignal, msk);
		ImageStack image = spt.getStack();
		Images3D.invert( image );

		ImageStack result = ExtendedMinimaWatershed.extendedMinimaWatershed(image, mask.getImageStack(), dynamic, connectivity_value, 16, false );
		ImagePlus ip = new ImagePlus( mask.getShortTitle() + "_dist-watershed",result );
		ip.setCalibration( mask.getCalibration() );
		Images3D.invert( image );
		fillDams(ip,mask,image);
		addSmallObjects(ip,mask,minVoxExtraObjects);
		trimObjectsToClass(ip,mask);
		try {IJ.run(ip, "glasbey on dark", "");} catch(Exception e) {System.out.println("could not find LUT for split object map");} // "5 class v1"
		return(ip);
	}
	
	public static ImagePlus distanceWatershedSplit(ImagePlus mask, int dynamic, int minVoxExtraObjects) {
		ImagePlus edt16Plus = smoothedDistanceMap(mask,false);	
		return watershedSplit(edt16Plus,mask,dynamic,minVoxExtraObjects);
	}
	
	public static ImagePlus smoothedDistanceMap(ImagePlus mask, boolean inverse) {
		return smoothedDistanceMap(mask,inverse,2.0,2.0);
	}
	public static ImagePlus smoothedDistanceMap(ImagePlus mask, boolean inverse, double blurXY, double blurZ) {
		Calibration calib = mask.getCalibration();
		double vx = calib.pixelWidth ; double vy = calib.pixelHeight ; double vz = calib.pixelDepth ; 
		if ( vx/vy >1.01 || vy/vx>1.01){
			IJ.log("!!! Warning: EDT in watershed split algorithm cannot handle unequal x and y voxel dimensions (" + vx + "," + vy + ")");
		    IJ.log("Using x dimension for both");
		}	
		IJ.log("Computing EDT; using xy res " + vx + " and z res " + vz);
		ImageInt imgMask = ImageInt.wrap(mask);
		ImageFloat edt = EDT.run(imgMask, (float) 0.0, (float) vx, (float) vz, inverse, 0);
		ImageHandler edt16 = edt.convertToShort(false); // don't rescale
		IJ.log("Smoothing EDT");
		ImagePlus edt16Plus = edt16.getImagePlus();
		GaussianBlur3D.blur(edt16Plus, blurXY, blurXY, blurZ);
		return edt16Plus;
	}
	
	// to add objects which have been filtered out by original process
	// could probably make this a lot more concise by tracking down the right methods ...
	public static void addSmallObjects(ImagePlus objectMap, ImagePlus classMask, int minVoxels) {
		ImagePlus extraVoxels = classMask.duplicate();
		int[] dims = objectMap.getDimensions();
		assert(dims[2]==1 && dims[4]==1);
		int dimX = dims[0] ; int dimY = dims[1] ; int dimZ = dims[3];
		// TODO: check dims all the same
		
		// remove all voxels which are already in objects, and get the max object id while I'm at it
		ImageStack obStack = objectMap.getImageStack();
		ImageStack extraStack = extraVoxels.getImageStack();
		int maxId = 0;
		for (int z=0; z<dimZ; z++){
			for (int x=0; x<dimX; x++){
				for (int y=0; y<dimY; y++){
					int obVal = (int) obStack.getVoxel(x,y,z);
					int classVal = (int) extraStack.getVoxel(x,y,z);
					if (obVal>maxId) {maxId=obVal;}
					if (obVal>0 && classVal>0) {
						extraStack.setVoxel(x, y, z, 0);				
					}
				}
			}
		}
		
		ImagePlus extraObjects = BinaryImages.componentsLabeling(extraVoxels, 6, 16);
		ImageStack extraObjectStack = extraObjects.getImageStack();
		int[] voxCounts = SegmentedImageOperations.getStackHistogramInteger(extraObjects);
		int[] newIdMap = new int[voxCounts.length];
		int nextId = maxId+1;
		for (int ii=1;ii<voxCounts.length;ii++) {
			if (voxCounts[ii]<minVoxels) {
				newIdMap[ii]=0;
			} else {
				newIdMap[ii]=nextId;
				nextId++;
			}
		}
		for (int z=0; z<dimZ; z++){
			for (int x=0; x<dimX; x++){
				for (int y=0; y<dimY; y++){
					// int obVal = (int) obStack.getVoxel(x,y,z);
					int exObVal = (int) extraObjectStack.getVoxel(x,y,z);
					if (exObVal>0) {
						obStack.setVoxel(x, y, z, newIdMap[exObVal]); //  maxId+exObVal
					}
				}
			}
		}
	}
	
	public static void trimObjectsToClass(ImagePlus objectMap, ImagePlus classMask) {
		int[] dims = objectMap.getDimensions();
		assert(dims[2]==1 && dims[4]==1);
		int dimX = dims[0] ; int dimY = dims[1] ; int dimZ = dims[3];
		// TODO: check dims all the same 
		ImageStack obStack = objectMap.getImageStack();
		ImageStack classStack = classMask.getImageStack();
		for (int z=0; z<dimZ; z++){
			for (int x=0; x<dimX; x++){
				for (int y=0; y<dimY; y++){
					int obVal = (int) obStack.getVoxel(x,y,z);
					int classVal = (int) classStack.getVoxel(x,y,z);
					if (obVal>0 && classVal==0) {
						obStack.setVoxel(x, y, z, 0);				
					}
				}
			}
		}
	}
	
	public static void fillDams(ImagePlus objectMap, ImagePlus classMask, ImageStack EDT) {
		int[] dims = objectMap.getDimensions();
		assert(dims[2]==1 && dims[4]==1);
		int dimX = dims[0] ; int dimY = dims[1] ; int dimZ = dims[3];
		// TODO: check dims all the same 
		ImageStack obStack = objectMap.getImageStack();
		ImageStack classStack = classMask.getImageStack();
		for (int z=0; z<dimZ; z++){
			// IJ.log(z);
			for (int x=0; x<dimX; x++){
				for (int y=0; y<dimY; y++){
					int obVal = (int) obStack.getVoxel(x,y,z);
					int classVal = (int) classStack.getVoxel(x,y,z);
					if (obVal==0 && classVal>0) {
						HashMap<Integer,Float> nbSums = new HashMap<Integer,Float>();
						for (int i=-1;i<2;i++){
							for (int j=-1;j<2;j++){
								for (int k=-1;k<2;k++){
									int dist = i*i+j*j+k*k;
									if (dist==0){continue;}
									//if (dist==0 || dist ==3){continue;} // this should ensure we pick up just the 18 neighbours we want
									if (x+i<0 || x+i >= dimX || y+j<0 || y+j >= dimY || z+k<0 || z+k >= dimZ){continue;}
									int id = (int) obStack.getVoxel(x+i,y+j,z+k);
									if (id==0){continue;}
									// print(val+ " ")
									float voxDist = (float) EDT.getVoxel(x+i,y+j,z+k);
									if (nbSums.containsKey(id)){
										nbSums.put(id, nbSums.get(id) + voxDist);
									} else {
										nbSums.put(id,voxDist);
									}
									// now find the winner
									int newId = 0;
									float bestDistSum = 0;
									for (Integer obId : nbSums.keySet()) {
										if (nbSums.get(obId)>bestDistSum) {
											newId = obId;
											bestDistSum = nbSums.get(obId);
										}
									}
									obStack.setVoxel(x, y, z, newId);
								}
							
							}
						}
					}
				}
			}
		}		
		
	}

}
