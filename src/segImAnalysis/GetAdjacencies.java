package segImAnalysis;

import java.io.BufferedWriter;
// import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.StackStatistics;


/**
 * Provides 
 *  (1) Static methods to calculate a table of object adjacencies from a 3D object map (or maps)
 *  (2) Method to write the object adjacency table to text file 
 * An object map is defined as a 3D ImagePlus object (wrapping an ImageStack) with integer voxel values
 * Positive values correspond to object ids, zero (or negative values if allowed) represent background
 * The adjacency table is returned as an int[n][n], where n is the largest voxel value (id) in the object map
 * The value at [i-1][j-1] is the degree of adjacency between objects i and j, defined below
 * 
 * getObjectAdjacencyTable18Neighbour takes a single object map
 * 2 voxels (x1,y1,z1), (x2,y2,z2) are considered adjacent if the values |x1-x2|, |y1-xy|, |z1-z2| are all
 * 0 or 1, except if all 3 values equal 0 (same voxel) or 1 (double diagonal)
 * A non-edge voxel has 18 neighbours under this definition
 * The degree of adjacency between objects i and j is the number of pairs of adjacent voxels 
 * where the first voxel is in i and the second in j
 * This method also has the option to treat object 1 as background.
 * 
 * getSplitObjectAdjacencyTableWithLayers takes an array of object maps, which are treated as consecutive
 * layers in a hierarchical object structure
 * Adjacencies are calculated within each layer as in getObjectAdjacencyTable18Neighbour, and aggregated.
 * In addition, each voxel is considered adjacent to the voxel in the same position in the adjacent layer(s)
 * These adjacencies between objects in consecutive layers are added to the adjacency table.
 * 
 * writeObjectAdjacencyTable writes the object adjacency table to a text file in one of 2 ways
 * 
 * if the flag write_objectAdjacencyTable_as_edge_list is set, each non-zero adjacency between objects i and j, i<=j, 
 * is written as a line "i,j,v", where v is the degree of adjacency
 * 
 * Otherwise, the n*n table is written out directly as a tab separated table of adjacency values (no row or column labels)
 * This is typically very verbose as the table (matrix) can be expected to be sparse 
 *   
 * @author James Lefevre
 *
 */
public class GetAdjacencies {
	
	public static int[][] getSplitObjectAdjacencyTableWithLayers(ImagePlus[] unifiedObjectMaps){
		int n = unifiedObjectMaps.length;
		int maxVoxelValue = 0;
		for (ImagePlus obImage : unifiedObjectMaps) {
			assert((new StackStatistics(obImage)).min>=0); 
			int maxVox = (int) (new StackStatistics(obImage)).max;
			if (maxVox>maxVoxelValue) {maxVoxelValue=maxVox;}
		}
		int[][] adj = new int[maxVoxelValue][maxVoxelValue];
		
		ImageStack ims_prev_layer = null;
		for (int layer=0; layer<n; layer++) {
			int[] dims = unifiedObjectMaps[layer].getDimensions();
			assert(dims[2]==1 && dims[4]==1);
			int dimX = dims[0] ; int dimY = dims[1] ; int dimZ = dims[3];
			ImageStack ims = unifiedObjectMaps[layer].getImageStack();
			for (int z=0; z<dimZ; z++){
				// IJ.log(z);
				for (int x=0; x<dimX; x++){
					for (int y=0; y<dimY; y++){
						
						// 18 neighbour adjacency within layer
						int voxVal = (int) ims.getVoxel(x,y,z);
						if (voxVal>0) {
						Integer[] nbVals;
						if (x>0 && x<dimX-1 && y>0 && y<dimY-1 && z<dimZ-1){
							nbVals = new Integer[]{(int) ims.getVoxel(x+1,y,z),(int) ims.getVoxel(x-1,y+1,z),
									(int) ims.getVoxel(x,y+1,z),(int) ims.getVoxel(x+1,y+1,z),
									(int) ims.getVoxel(x,y,z+1), (int) ims.getVoxel(x-1,y,z+1),
									(int) ims.getVoxel(x+1,y,z+1),(int) ims.getVoxel(x,y-1,z+1),
									(int) ims.getVoxel(x,y+1,z+1)};
						} else {
							ArrayList<Integer> tmpVals = new ArrayList<Integer>();
							if (x != dimX-1){tmpVals.add((int) ims.getVoxel(x+1,y,z));}
							if (y != dimY-1){
								tmpVals.add((int) ims.getVoxel(x,y+1,z));
								if (x != 0){tmpVals.add((int) ims.getVoxel(x-1,y+1,z));}
								if (x != dimX-1){tmpVals.add((int) ims.getVoxel(x+1,y+1,z));}
							}
							if (z != dimZ-1){
								tmpVals.add((int) ims.getVoxel(x,y,z+1));
								if (x != 0){tmpVals.add((int) ims.getVoxel(x-1,y,z+1));}
								if (x != dimX-1){tmpVals.add((int) ims.getVoxel(x+1,y,z+1));}
								if (y != 0){tmpVals.add((int) ims.getVoxel(x,y-1,z+1));}
								if (y != dimY-1){tmpVals.add((int) ims.getVoxel(x,y+1,z+1));}
							}
							nbVals = tmpVals.toArray(new Integer[tmpVals.size()]);
						}
						for (int v : nbVals){
							if (v>0){adj[voxVal-1][v-1]++;};
						}
						}
												
						// object overlap between layers ; already have voxVal for current layer
						if (ims_prev_layer != null) {
							int voxVal2 = (int) ims_prev_layer.getVoxel(x,y,z);
							if (voxVal>0 && voxVal2>0) {
								adj[voxVal-1][voxVal2-1]++;
							}
						}
						
					}
				}
			}
			ims_prev_layer = ims;
		}
		
		for (int ii=0; ii<maxVoxelValue; ii++){
			for (int jj=ii; jj<maxVoxelValue; jj++){
				adj[ii][jj] += adj[jj][ii];
				adj[jj][ii] = adj[ii][jj];
			}
		}
		return(adj);
	}

	
	
	// includes option to designate object 1 as background
	public static int[][] getObjectAdjacencyTable18Neighbour(ImagePlus unifiedObjectMap){
		return(getObjectAdjacencyTable18Neighbour(unifiedObjectMap, false));
	}
	
	public static int[][] getObjectAdjacencyTable18Neighbour(ImagePlus unifiedObjectMap, boolean ignoreObject1){
		assert((new StackStatistics(unifiedObjectMap)).min>=0); // decided to allow zero but do not count - see above
		int maxVoxelValue = (int) (new StackStatistics(unifiedObjectMap)).max;
		int[][] adj = new int[maxVoxelValue][maxVoxelValue];
		int[] dims = unifiedObjectMap.getDimensions();
		assert(dims[2]==1 && dims[4]==1);
		int dimX = dims[0] ; int dimY = dims[1] ; int dimZ = dims[3];
		ImageStack ims = unifiedObjectMap.getImageStack();
		for (int z=0; z<dimZ; z++){
			for (int x=0; x<dimX; x++){
				for (int y=0; y<dimY; y++){
					int voxVal = (int) ims.getVoxel(x,y,z);
					if (ignoreObject1 && voxVal==1) continue;
					if (voxVal==0) continue;
					// doing this the verbose way to save running time
					// first case below should be fast and cover most voxels
					Integer[] nbVals;
					if (x>0 && x<dimX-1 && y>0 && y<dimY-1 && z<dimZ-1){
						nbVals = new Integer[]{
								(int) ims.getVoxel(x+1,y,z),(int) ims.getVoxel(x-1,y+1,z),
								(int) ims.getVoxel(x,y+1,z),(int) ims.getVoxel(x+1,y+1,z),
								(int) ims.getVoxel(x,y,z+1), (int) ims.getVoxel(x-1,y,z+1),
								(int) ims.getVoxel(x+1,y,z+1),(int) ims.getVoxel(x,y-1,z+1),
								(int) ims.getVoxel(x,y+1,z+1)};
					} else {
						ArrayList<Integer> tmpVals = new ArrayList<Integer>();
						if (x != dimX-1){tmpVals.add((int) ims.getVoxel(x+1,y,z));}
						if (y != dimY-1){
							tmpVals.add((int) ims.getVoxel(x,y+1,z));
							if (x != 0){tmpVals.add((int) ims.getVoxel(x-1,y+1,z));}
							if (x != dimX-1){tmpVals.add((int) ims.getVoxel(x+1,y+1,z));}
						}
						if (z != dimZ-1){
							tmpVals.add((int) ims.getVoxel(x,y,z+1));
							if (x != 0){tmpVals.add((int) ims.getVoxel(x-1,y,z+1));}
							if (x != dimX-1){tmpVals.add((int) ims.getVoxel(x+1,y,z+1));}
							if (y != 0){tmpVals.add((int) ims.getVoxel(x,y-1,z+1));}
							if (y != dimY-1){tmpVals.add((int) ims.getVoxel(x,y+1,z+1));}
						}
						nbVals = tmpVals.toArray(new Integer[tmpVals.size()]);
					}
					for (int v : nbVals){
						if (v>0){adj[voxVal-1][v-1]++;};
					}
				}
			}
		}

		for (int ii=0; ii<maxVoxelValue; ii++){
			for (int jj=ii; jj<maxVoxelValue; jj++){
				adj[ii][jj] += adj[jj][ii];
				adj[jj][ii] = adj[ii][jj];
			}
		}
		if (ignoreObject1){
			for (int ii=0; ii<maxVoxelValue; ii++){
				adj[0][ii] = 0;
				adj[ii][0] = 0;
			}
		}
		return(adj);
	}

	public static void writeObjectAdjacencyTable(int[][] objectAdjacencyTable, String savePath, int maxObjectId, boolean write_objectAdjacencyTable_as_edge_list) throws IOException{
		writeObjectAdjacencyTable(objectAdjacencyTable,savePath,maxObjectId,write_objectAdjacencyTable_as_edge_list,false);
	}
		
	public static void writeObjectAdjacencyTable(int[][] objectAdjacencyTable, String savePath, int maxObjectId, boolean write_objectAdjacencyTable_as_edge_list, boolean append) throws IOException{
		FileWriter fwr = new FileWriter(savePath, append);
		BufferedWriter bw = new BufferedWriter(fwr);
		if (write_objectAdjacencyTable_as_edge_list){
		  for (int ii=0; ii<maxObjectId; ii++){
			for (int jj=ii; jj<maxObjectId; jj++){
				int val = objectAdjacencyTable[ii][jj];
				if (val>0){bw.write((ii+1)+","+(jj+1)+","+val+"\n");}
			}
		  }
		} else{
		for (int jj=0; jj<maxObjectId; jj++){bw.write("\t"+(jj+1));} ; bw.write("\n");
		for (int ii=0; ii<maxObjectId; ii++){
			bw.write(ii+1);
			for (int jj=0; jj<maxObjectId; jj++){
				bw.write("\t" + objectAdjacencyTable[ii][jj]);
			}
			bw.write("\n");
		}
		}
		bw.close();
	}

	

}
