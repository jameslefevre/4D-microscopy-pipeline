package segImAnalysis;

import ij.measure.ResultsTable;

public class Util {
	
	public static ResultsTable mergeNumericalResultsTable(ResultsTable t1, ResultsTable t2) {
		if (t1==null || t1.size()==0) {return(t2);}
		if (t2==null || t2.size()==0) {return(t1);}
				
		ResultsTable tm =  (ResultsTable) t1.clone();
		int n = tm.size();
		int cols = t2.getLastColumn();
		for (int rr=0;rr<t2.size();rr++) {
			for (int cc=0;cc<=cols;cc++) {
				tm.setValue(cc, rr+n, t2.getValueAsDouble(cc,rr));
			}
		}	
		return(tm);
	}
	
	public static double[][] objectPropertiesMatrix(ResultsTable rt, String[] fields){
		double[][] opm = new double[rt.size()][fields.length];
		for (int ii = 0; ii < rt.size(); ii++){
			for (int jj = 0; jj < fields.length; jj++){
				opm[ii][jj] = rt.getValue(fields[jj], ii);
			}
		}
		return(opm);
	}
	
	// move to object tracking where it is used

	public static double[][] distanceMatrix(double[][] m1, double[][] m2){
		double[] weight = new double[m1[0].length];
		for (int ii=0; ii<weight.length; ii++){
			weight[ii] = 1;
		}
		return(weightedDistanceMatrix(m1,m2,weight));
	}
		
	public static double[][] weightedDistanceMatrix(double[][] mat1, double[][] mat2, double[] weight){
		int n1 = mat1.length;
		int n2 = mat2.length;
		int m = mat1[0].length;
		// println("sizes " + n1 + ", " + n2 + " by " + m);
		assert(weight.length == m);
		for (int ii=0; ii<n1; ii++){assert(mat1[ii].length == m);}
		for (int ii=0; ii<n2; ii++){assert(mat2[ii].length == m);}

		double[][] dist = new double[n1][n2];
		for (int ii=0; ii<n1; ii++){
			for (int jj=0; jj<n2; jj++){
				double d=0;
				for (int kk=0; kk<m; kk++){
					 d += weight[kk] * weight[kk] * (mat1[ii][kk] - mat2[jj][kk]) * (mat1[ii][kk] - mat2[jj][kk]);
				}
				dist[ii][jj] = Math.sqrt(d);
			}
		}
		return(dist);
	}
	

	// printing methods *******************************************
	
	
		public static void println() {
			System.out.println();
		}
	public static void println(Object x) {
		System.out.println(x.toString());
		// IJ.log((String) x);
	}
	public static void print(Object x) {
		System.out.print(x.toString());
		// IJ.log((String) x);
	}
	
	public static void printArray(double[][] ar) {
		for (int ii=0; ii<ar.length; ii++ ) {
			for (int jj=0; jj<ar[ii].length; jj++ ) {
				System.out.print(ar[ii][jj] + "\t");
			}
			System.out.println();		
		}
	}
	public static void printArray(double[] ar) {
		for (int ii=0; ii<ar.length; ii++ ) {
			System.out.print(ar[ii] + "\t");		
		}
		System.out.println();
	}
	public static void printArray(int[] ar) {
		for (int ii=0; ii<ar.length; ii++ ) {
			System.out.print(ar[ii] + "\t");		
		}
		System.out.println();
	}

}
