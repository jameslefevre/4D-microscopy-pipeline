
/*
imageStackLocation='/home/james/image_data/LLS/feature_stacks_and_processed_images/classified_images/r2_150202_3/d11_all16_rf/splitObjectAnalysis/'
savePath='/home/james/image_data/LLS/feature_stacks_and_processed_images/classified_images/r2_150202_3/d11_all16_rf/splitObjectAnalysis/'
firstStackNumber='2'
lastStackNumber='3'
stackNumPadLength='4'
numberThreadsToUse='2'

float[] targetMeshVertexReduction = [0.96,0.0,0.8,0.8] 
int[] meshSmoothingIterations = [0,0,0,0]
*/

// TODO: write doc string; see old version notes; clean up

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

// import segImAnalysis.*;

// int[] classesToMesh = [1,2,3] 


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
	if (splt_fn.size()<2){continue}
	int stNum = splt_fn[0].toInteger();
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
  // ImagePlus classMapImage  = IJ.openImage( imagePath ); 
  ImagePlus classMapImage=null;
  for (int _try=0;_try<3;_try++){
	try{
		classMapImage  = IJ.openImage( imagePath ); 
	} catch(Exception e) {
    	println("Exception: ${e}")
    	classMapImage=null;
	}
	if (classMapImage!=null){break;}
	sleep(30000)
  }
  println(classMapImage==null ? "Failed" : "Succeeded");

  imagePath = imageStackLocation + imageStackName + "/object_map2.tif";
  print("opening " + imagePath + ":   ");
  //ImagePlus classMapImage2  = IJ.openImage( imagePath ); 
  ImagePlus classMapImage2=null;
  for (int _try=0;_try<3;_try++){
	try{
		classMapImage2  = IJ.openImage( imagePath ); 
	} catch(Exception e) {
    	println("Exception: ${e}")
    	classMapImage2=null;
	}
	if (classMapImage2!=null){break;}
	sleep(30000)
  }
  println(classMapImage2==null ? "Failed" : "Succeeded");
  
 ImagePlus[] classMapImages = [classMapImage,classMapImage2]

  String objectStatsPath = imageStackLocation + imageStackName + "/objectStats.txt";
  print("opening " + objectStatsPath + ":   ");
  ResultsTable objectStats = ResultsTable.open(objectStatsPath);
  println(objectStats==null ? "Failed" : "Succeeded");
  int maxId = (int) Math.round(objectStats.getValue("id",objectStats.size()-1))
  println("Max object id " + maxId)
  Object3DVoxels[] obsVox = getVoxelObjects(classMapImages, 1, maxId) // indexed from 1, the first object id
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

// slow
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

// could put this in Java ...
Object3DVoxels[] getVoxelObjects(ImagePlus objectMap, int objectIdMin, int objectIdMax){
        int n = objectIdMax-objectIdMin+1;
	int[] dims = objectMap.getDimensions()
	assert(dims[2]==1 && dims[4]==1)
	int dimX = dims[0] ; int dimY = dims[1] ; int dimZ = dims[3]
	// dimZ = 2 // temp for testing, in case very slow
	ImageStack ims = objectMap.getImageStack();
	LinkedList<Voxel3D>[] vox_lists = new LinkedList<Voxel3D>[n]
	for (int ii=0; ii< n; ii++){
		vox_lists[ii] = new LinkedList<Voxel3D>()
	}
	for (int z=0; z<dimZ; z++){
		// println(z);
		for (int x=0; x<dimX; x++){
			for (int y=0; y<dimY; y++){
				int voxVal = ims.getVoxel(x,y,z);
				if (voxVal<objectIdMin || voxVal>objectIdMax) continue
				vox_lists[voxVal-objectIdMin].add(new Voxel3D(x,y,z,voxVal))
				// obsVox[voxVal].voxels.add(new Voxel3D(x,y,z,voxVal))
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
		// dimZ = 2 // temp for testing, in case very slow
		ImageStack ims = objectMap.getImageStack();

		for (int z=0; z<dimZ; z++){
			// println(z);
			for (int x=0; x<dimX; x++){
				for (int y=0; y<dimY; y++){
					int voxVal = ims.getVoxel(x,y,z);
					if (voxVal<objectIdMin || voxVal>objectIdMax) continue
					vox_lists[voxVal-objectIdMin].add(new Voxel3D(x,y,z,voxVal))
					// obsVox[voxVal].voxels.add(new Voxel3D(x,y,z,voxVal))
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
	// ArrayList<Integer> nonNullIndices = new ArrayList<Integer>()
	// ArrayList<List> obMeshes = new ArrayList<List>() 


		// nonNullIndices.add(ii)
		List<Point3f> mesh = Viewer3D_Utils.computeMeshSurface(obVox,true)
		// mesh = MeshEditor.smooth(mesh,0) // not helpful
		IJ.log("Mesh length " + mesh.size() + "    " + sdf.format(new Date()))
		FullInfoMesh fi_mesh = new FullInfoMesh(mesh)
		EdgeContraction ec = new EdgeContraction(fi_mesh)
		int vcount = ec.getRemainingVertexCount()
		date = new Date(); IJ.log("Vertex count via EdgeContraction object " + vcount + "    " + sdf.format(date))
		int n = Math.round(vcount*targetVertexReduction)
		date = new Date(); IJ.log("Attempting to remove " + n + ", leaving " + (vcount-n) + "    " + sdf.format(date))
		// simplify_mesh(ec,n)
		// ec.removeNext(n)
		boolean simp = removeVertices(ec,n)
		// problem case crashed on retry, so bypass and deal with big mesh
		if (false && !simp){
			IJ.log("trying once more ")
			mesh = ec.getMeshes().get(0).getMesh()
			n = n + ec.getRemainingVertexCount() - vcount
			date = new Date(); IJ.log("Attempting to remove further " + n + ", leaving " + (ec.getRemainingVertexCount()-n) + "    " + sdf.format(date))
			fi_mesh = new FullInfoMesh(mesh)
			ec = new EdgeContraction(fi_mesh)
			IJ.log("check nulls: mesh=" + (mesh==null) + ", fi_mesh=" + (fi_mesh==null) + ", ec=" + (ec==null))
			if ( (mesh!=null) & (fi_mesh!=null) & (ec!=null) ){
				removeVertices(ec,n)
			}			
		}
		date = new Date(); IJ.log("Remaining vertex count via EdgeContraction object " + ec.getRemainingVertexCount() + "    " + sdf.format(date))
		mesh = MeshEditor.smooth2(ec.getMeshes().get(0).getMesh(),smoothingIterations)
		return(mesh)
}


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
		//int v = 0;
		//for (int i = 0; i < ec.mesh.size(); i++)
		//	v += ec.mesh.get(i).getVertexCount();

		//return v;
	}

