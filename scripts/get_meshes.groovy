
/**
 * Generates meshes in the [OBJ format](https://en.wikipedia.org/wiki/Wavefront_.obj_file) 
 * defining object surfaces. Coordinates are in voxel units. Meshes are used primarily for visualisation, 
 * but may also be used for analysis. The required input is one or more 8- or 16-bit color tiff stacks 
 * named object_map.tif, object_map2.tif etc where the (integer) voxel values define object masks. 
 * In addition, we require a text file objectStats.txt containing a tab-separated table with integer-value 
 * columns named id and class (any other columns are allowed, but are not used). The IDs must match the 
 * voxel values in order for objects to be processed, and the class should be correct for the object as 
 * per the original segmentation and the primary object analysis. This information allows objects to be 
 * processed according to class (e.g. the amount of mesh thinning can vary by class), and recorded by class and id
 * in a consistant way.
 * 
 * Meshes are generated using the marching cubes algorithm, then optionally smoothed and pruned.
 * Code is provided by the mcib3d library. Includes an edited version of the removeVertices method from 
 * the mcib3d library, to allow recovery when the mesh pruning algorithm occasionally 
 * enters an infinite loop. To this end, the core code is in groovy to allow access to private classes 
 * in mcib3d (not possible in Java without forking the library).
 * 
 * @param imageStackLocation
 * Path to a directory containing 8-bit color tiff stacks representing segmentation to be analysed.
 * @param savePath
 * The directory to save the output files, in subdirectories named for the image stacks processed. 
 * This should be the same as imageStackLocation, or at least have the same subdirectory names.
 * @param classesToAnalyse
 * A list of comma-separated integers between square brackets, e.g. '[1,2,3]', specifying which 
 * classes to analyse. Each object ID is associated with a class number in objectStats.txt, and the 
 * object will only be processed if its class number is in this list. 
 * @param firstStackNumber
 * The first stack number to process, where the stack number is parsed from the file name using stackNumberPrefix and 
 * stackNumberSuffix (see below).
 * @param lastStackNumber
 * The last stack number to process. Stacks will be processed in order from firstStackNumber to lastStackNumber, inclusive.
 * @param numberThreadsToUse
 * The number of threads that ImageJ is asked to use.
 * @param stackNumberPrefix
 * A string identifying the start of the stack number in the filename (stack number may be left-padded with zeroes).
 * @param stackNumberSuffix
 * A string identifying the end of the stack number in the filename. The script will look for a filename 
 * which contains a substring consisting of stackNumberPrefix, optional zeros, the stack number associated 
 * with the job, then stackNumberSuffix.
 * @param fileNamePrefix
 * Only stack names starting with this string will be processed; leave blank (prefix='') to skip this filter.
 * @param overwriteExisting
 * A string equal to 'true' or 'false'. If false, the job will check for the output file objectStats.txt in the 
 * expected location, and if present it will not process the stack. This is useful for easily redoing failed 
 * tasks while not repeating tasks that completed successfully.
 * @param targetMeshVertexReduction
 * An array of numbers (i.e. a list of comma-separated numbers between square brackets) between 0 and 1 corresponding 
 * to classes 1,2, etc. After meshes have been constructed with the marching cubes algorithm, it is generally recommended 
 * to prune the meshes, reducing the number of faces while retaining a good representation of the surface, in order to 
 * save resources (disk and RAM space plus processing speed). This number is the target proportion of mesh nodes that 
 * should be removed (the target may not be obtainable). Pruning may reduce the level of detail in the surface, so this 
 * parameter should reflect the size and complexity of structures in the class. Small structures may be best without pruning 
 * (parameter value 0) while larger and simpler structures may benefit from a parameter value approaching 1.
 * @param meshSmoothingIterations
 * An array of integers corresponding to classes 1,2, etc, giving the number of smoothing iterations to perform on the 
 * pruned meshes. Each iteration adjusts the mesh node positions slightly to give a smoother surface rendering, so higher 
 * numbers give more smoothing, and 0 indicates no smoothing. Again, the parameter choice should reflect the characteristics 
 * of structures of the class, and the desired tradeoff between smooth surfaces and faithfulness to the segmentation.
 */

#@ String imageStackLocation
#@ String savePath
#@ String classesToAnalyse
#@ String firstStackNumber
#@ String lastStackNumber
#@ String numberThreadsToUse
#@ String stackNumberPrefix
#@ String stackNumberSuffix
#@ String fileNamePrefix
#@ Boolean overwriteExisting
#@ String targetMeshVertexReduction
#@ String meshSmoothingIterations

import ij.IJ
import ij.ImagePlus
import ij.ImageStack;
import ij.measure.ResultsTable;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.Voxel3D;
import mcib3d.geom.MeshEditor;
import mcib3d.geom.Viewer3D_Utils;
import customnode.EdgeContraction;
import customnode.FullInfoMesh;
import customnode.CustomMesh;
import java.io.File;
import java.text.SimpleDateFormat
import java.util.Arrays;
import java.util.LinkedList;
import org.scijava.vecmath.Point3f;

int[] classesToAnalyse  = Eval.me(classesToAnalyse)
int firstStackNumber  = Eval.me(firstStackNumber)
int lastStackNumber  = Eval.me(lastStackNumber)
float[] targetMeshVertexReduction  = Eval.me(targetMeshVertexReduction)
int[] meshSmoothingIterations  = Eval.me(meshSmoothingIterations)

sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")

ArrayList<String> nameSet = new ArrayList<String>();
files = (new File(imageStackLocation)).listFiles()
Arrays.sort(files);
for (String fn : files){
	fn = fn.tokenize("/")[-1].tokenize(".")[0]

	// extract stack number
	splt_fn = fn.split(stackNumberPrefix)
	if (splt_fn.size()<2){continue}
	part_fn = splt_fn[1]
	splt_fn = part_fn.split(stackNumberSuffix)
	
	// if (splt_fn.size()<2){continue} // if suffix is at end of filename, size will be 1 and that is fine; no match will also give size 1
	int stNum;
	try{
		stNum = splt_fn[0].toInteger();
	} catch(Exception e) {continue;}
	//int stNum = splt_fn[0].toInteger();
	// println(stNum)
	
	if (firstStackNumber <= stNum && lastStackNumber >= stNum ){
		nameSet.add(fn);
	}
}
// println(nameSet);
for (imageStackName in nameSet){
  String saveName = savePath + imageStackName + "/objectMeshes.obj"
  if ( !overwriteExisting && (new File(saveName)).exists()){
	println(saveName + " already exists : skipping ")
	continue;
  }

	
  String imagePath = imageStackLocation + imageStackName + "/object_map.tif";
  print("opening " + imagePath + ":   ");
  // ImagePlus objectMapImage  = IJ.openImage( imagePath ); 
  ImagePlus objectMapImage=null;
  for (int _try=0;_try<3;_try++){
	try{
		objectMapImage  = IJ.openImage( imagePath ); 
	} catch(Exception e) {
    	println("Exception: ${e}")
    	objectMapImage=null;
	}
	if (objectMapImage!=null){break;}
	sleep(30000)
  }
  println(objectMapImage==null ? "Failed" : "Succeeded");
  if (objectMapImage==null) continue;

  ArrayList<ImagePlus> objectMapImageList = new ArrayList<ImagePlus>();
  objectMapImageList.add(objectMapImage);

  int omNum=1
  while(true){
  	objectMapImage=null;
  	omNum+=1;
  	imagePath = imageStackLocation + imageStackName + "/object_map"+omNum+".tif";
  	print("opening " + imagePath + ":   ");
  	for (int _try=0;_try<3;_try++){
	  try{
	  	objectMapImage  = IJ.openImage( imagePath ); 
  	  } catch(Exception e) {
      	 println("Exception: ${e}")
    	 objectMapImage=null;
	  }
	  if (objectMapImage!=null){break;}
	  sleep(30000)
    }
    println(objectMapImage==null ? "Failed" : "Succeeded");
    if (objectMapImage==null) break;
    objectMapImageList.add(objectMapImage);
  }

 ImagePlus[] objectMapImages = objectMapImageList.toArray(); // [objectMapImage,objectMapImage2]

  String objectStatsPath = imageStackLocation + imageStackName + "/objectStats.txt";
  print("opening " + objectStatsPath + ":   ");
  ResultsTable objectStats = ResultsTable.open(objectStatsPath);
  println(objectStats==null ? "Failed" : "Succeeded");
  int maxId = (int) Math.round(objectStats.getValue("id",objectStats.size()-1))
  println("Max object id " + maxId)
  Object3DVoxels[] obsVox = getVoxelObjects(objectMapImages, 1, maxId) // indexed from 1, the first object id
  List[] obMeshes_array = new List[obsVox.size()]; // also indexed from 1, the first object id
  for (int ii=0; ii<objectStats.size(); ii++){
    int id = (int) Math.round(objectStats.getValue("id",ii));
    int classNum = (int) Math.round(objectStats.getValue("class",ii))
    if (!classesToAnalyse.contains(classNum)){continue;}
    if (id==0){continue;}
    println(id + ", " + classNum + ", " + (obsVox[id-1]==null ? "null" : obsVox[id-1].voxels.size()));
    if (obsVox[id-1] == null){continue;} 

    Object3DVoxels obVox = obsVox[id-1];
    List obMesh = getObjectMesh(obVox,targetMeshVertexReduction[classNum-1],meshSmoothingIterations[classNum-1])
    obMeshes_array[id-1] = obMesh
  }
  
  println("Attempting to save meshes as " + saveName + "    " + sdf.format(new Date()))
  new File(savePath + imageStackName).mkdirs() 
  File file = new File(saveName)
  FileWriter writer = new FileWriter(file)
    try {
                int null_count =0
                for (om in obMeshes_array){
                 if (om==null) {null_count++} 
                }
		for (int id =1; id <= obMeshes_array.size(); id++){
                        if (obMeshes_array[id-1] == null){continue;} 
                        writer.write("g Object_" + id + "\n")
                        meshToString(obMeshes_array[id-1],writer) 
		}
		writer.close()
                println("Meshes saved    " + sdf.format(new Date()))
	} catch (Exception e) {   //catch (all) 
		println("!!! Error saving mesh !!! ... moving on") 	
	} 
        println();
}
println("done")


// **************************** functions ********************************************************************************************

// meshToString, getVoxelObjects could be moved to Java code, but decided it wasn't worth the import for this script

void meshToString(List mesh, FileWriter fw){
	Point3f[] vertices = new Point3f[mesh.size()];
	// List<Point3f> vertices = mesh;
	for (int ii=0; ii<mesh.size(); ii++){
		vertices[ii] = mesh.get(ii);
	}
	// println("mesh consists of " + vertices.size() + " Point3f objects")
	if (vertices.size() % 3 != 0){
		println("!!! Expect number of points in mesh to be divisible by 3 - triangle faces only");
		println("Aborting")
		return(null);
	}
	int faceCount = (int) vertices.size() / 3;
	// println(faceCount + " triangle faces")
	HashMap<Point3f, Integer> pointIds = new HashMap<Point3f, Integer>();
	ArrayList<Point3f> uniqueVertices = new ArrayList<Point3f>();
	int nextId = 1
	for (v in vertices){
		if (! pointIds.containsKey(v)){
			uniqueVertices.add(v);
			pointIds.put(v,nextId);
			nextId++;
		}
	}
	// println("Number of unique vertices: " + uniqueVertices.size())
	for (v in uniqueVertices){
                fw.write("v " + v.x + " " + v.y + " " + v.z + "\n");
	}
        // println("added vertices")
	for (int f = 0; f < faceCount; f++){
                fw.write("f " + pointIds.get(vertices[3*f]) + " " + pointIds.get(vertices[3*f+1]) + " " + pointIds.get(vertices[3*f+2]) + "\n");		
	}
        // println("added faces")
}

// slow, so not using this version of the method
String meshToString(List mesh){
	String st = "";
	Point3f[] vertices = new Point3f[mesh.size()];
	for (int ii=0; ii<mesh.size(); ii++){
		vertices[ii] = mesh.get(ii);
	}
	println("mesh consists of " + vertices.size() + " Point3f objects")
	if (vertices.size() % 3 != 0){
		println("!!! Expect number of points in mesh to be divisible by 3 - triangle faces only");
		println("Aborting")
		return(null);
	}
	int faceCount = (int) vertices.size() / 3;
	// println(faceCount + " triangle faces")
	HashMap<Point3f, Integer> pointIds = new HashMap<Point3f, Integer>();
	ArrayList<Point3f> uniqueVertices = new ArrayList<Point3f>();
	int nextId = 1
	for (v in vertices){
		if (! pointIds.containsKey(v)){
			uniqueVertices.add(v);
			pointIds.put(v,nextId);
			nextId++;
		}
	}
	println("Number of unique vertices: " + uniqueVertices.size())
	for (v in uniqueVertices){
		st += "v " + v.x + " " + v.y + " " + v.z + "\n";
	}
        println("added vertices")
	for (int f = 0; f < faceCount; f++){
		st += "f ";
		st+= pointIds.get(vertices[3*f]) + " ";
		st+= pointIds.get(vertices[3*f+1]) + " ";
		st+= pointIds.get(vertices[3*f+2]) + "\n";		
	}
        println("added faces")
	return(st);
}


// converting all objects into Object3DVoxels form in single pass through allows efficient 
// computation of separate meshes for each object

Object3DVoxels[] getVoxelObjects(ImagePlus objectMap, int objectIdMin, int objectIdMax){
    int n = objectIdMax-objectIdMin+1;
	int[] dims = objectMap.getDimensions()
	assert(dims[2]==1 && dims[4]==1)
	int dimX = dims[0] ; int dimY = dims[1] ; int dimZ = dims[3]
	ImageStack ims = objectMap.getImageStack();
	LinkedList<Voxel3D>[] vox_lists = new LinkedList<Voxel3D>[n]
	for (int ii=0; ii< n; ii++){
		vox_lists[ii] = new LinkedList<Voxel3D>()
	}
	for (int z=0; z<dimZ; z++){
		for (int x=0; x<dimX; x++){
			for (int y=0; y<dimY; y++){
				int voxVal = ims.getVoxel(x,y,z);
				if (voxVal<objectIdMin || voxVal>objectIdMax) continue
				vox_lists[voxVal-objectIdMin].add(new Voxel3D(x,y,z,voxVal))
			}
		}
	}
	Object3DVoxels[] obsVox = new Object3DVoxels[n]
	for (int ii=0; ii< n; ii++){
		if (vox_lists[ii].size()>0){
			obsVox[ii] = new Object3DVoxels(vox_lists[ii])
		}
	}
	return(obsVox)
}

// version with layer support
Object3DVoxels[] getVoxelObjects(ImagePlus[] objectMaps, int objectIdMin, int objectIdMax){
    int n = objectIdMax-objectIdMin+1;
	LinkedList<Voxel3D>[] vox_lists = new LinkedList<Voxel3D>[n]
	for (int ii=0; ii< n; ii++){
		vox_lists[ii] = new LinkedList<Voxel3D>()
	}
    for (ImagePlus objectMap : objectMaps){
    	int[] dims = objectMap.getDimensions()
		assert(dims[2]==1 && dims[4]==1)
		int dimX = dims[0] ; int dimY = dims[1] ; int dimZ = dims[3]
		ImageStack ims = objectMap.getImageStack();
		for (int z=0; z<dimZ; z++){
			for (int x=0; x<dimX; x++){
				for (int y=0; y<dimY; y++){
					int voxVal = ims.getVoxel(x,y,z);
					if (voxVal<objectIdMin || voxVal>objectIdMax) continue
					vox_lists[voxVal-objectIdMin].add(new Voxel3D(x,y,z,voxVal))
				}
			}
		}
	}
	Object3DVoxels[] obsVox = new Object3DVoxels[n]
	for (int ii=0; ii< n; ii++){
		if (vox_lists[ii].size()>0){
		obsVox[ii] = new Object3DVoxels(vox_lists[ii])
		}
	}
	return(obsVox)
}


// targetVertexReduction between 0 and 1; feel free to aim high as it stops when it has to
List getObjectMesh(Object3DVoxels obVox, float targetVertexReduction, int smoothingIterations){
	sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
	List<Point3f> mesh = Viewer3D_Utils.computeMeshSurface(obVox,true)
	// mesh = MeshEditor.smooth(mesh,0) // not helpful
	IJ.log("Mesh length " + mesh.size() + "    " + sdf.format(new Date()))
	FullInfoMesh fi_mesh = new FullInfoMesh(mesh)
	EdgeContraction ec = new EdgeContraction(fi_mesh)
	int vcount = ec.getRemainingVertexCount()
	date = new Date(); IJ.log("Vertex count via EdgeContraction object " + vcount + "    " + sdf.format(date))
	int n = Math.round(vcount*targetVertexReduction)
	date = new Date(); IJ.log("Attempting to remove " + n + ", leaving " + (vcount-n) + "    " + sdf.format(date))
	// simplify_mesh(ec,n) ; ec.removeNext(n) // this is the original code, had to go deeper due to infinite loop issue
	boolean simp = removeVertices(ec,n) // note this calls my edited version of the method (below)
	// in previous code had retry routine when !simp, but this sometimes caused a crach
	date = new Date(); IJ.log("Remaining vertex count via EdgeContraction object " + ec.getRemainingVertexCount() + "    " + sdf.format(date))
	mesh = MeshEditor.smooth2(ec.getMeshes().get(0).getMesh(),smoothingIterations)
	return(mesh)
}


// core mesh thinning step
// copied from https://github.com/fiji/3D_Viewer/blob/master/src/main/java/customnode/EdgeContraction.java /removeNext
// adapted to escape the occasional infinite loop 
boolean removeVertices(EdgeContraction ec, int n) {
		int curr = ec.getRemainingVertexCount();
		int goal = curr - n;
		int prev = curr
		int failCount=0

		while (curr > goal && !ec.queue.isEmpty()) {
			e = ec.queue.first();
			try {
			ec.queue.remove(e);
			ec.fuse(e);
			} catch (all) {
				IJ.log("Special exit from removeVertices() at " + curr + "; error while attempting to remove entry")	
				return(false)		
			}
			curr = ec.getRemainingVertexCount();
			if (prev==curr){
				failCount++
			} else {
				failCount=0
			}
			if (failCount==1000){
				IJ.log("Special exit from removeVertices() at " + curr + "; queue not empty but cannot remove entry")
				return(false)
			}
			prev = curr
			// IJ.log(" " + curr)
		}
		return(true)
	}

