/** Generates the template file "feature_model_table.txt" from a given weka data file (.arff). This file can then be used for the 
 *  rest of the LLAMA pipeline. 
 *
 *  TODO:
 *  - Derivative features (Difference_Of_Gaussian can be derived?)
 *
 *  @param arffLocation 
 *  full path to the .arff file
 *  @param modelName 
 *  model name (.model) to be included in the feature model table
 *  @param outputLocation 
 *  full path to the desired output location (e.g "/home/Desktop/feature_model_table.txt")
 *  @param downSample 
 *  Boolean Flag, will downsample large features if true
 */

#@ String arffLocation
#@ String modelName
#@ String outputLocation
#@ Boolean downSample

import weka.core.converters.ConverterUtils.DataSource;
import weka.core.Instances
import java.util.Arrays

import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;

ArrayList<String> IJFilterList = Arrays.asList("Gaussian","Minimum","Maximum", "Mean", "Median", "Variance"); // IJFilters

println('Starting to look for feature details');

DataSource source = new DataSource(arffLocation);
Instances data = source.getDataSet();

println('Writing attribute details to file');

try (PrintWriter writer = new PrintWriter(outputLocation)) {
    // container for attributes
    StringBuilder featureModelTable = new StringBuilder();

    // Header
    ArrayList<String> header = Arrays.asList("feature_name\t", "operation\t", "parameter\t", "sigma\t", "group\t", "downsample\t", modelName+"\n");
    for (String columnName: header) {
        featureModelTable.append(columnName);
        }

    for (int i=0;i<data.numAttributes();i++) {
        ArrayList<String> attribute = new ArrayList<String>(7);

        if (data.attribute(i).name() != "class") {

            name = data.attribute(i).name();
            attribute.add(name+"\t"); // 1. feature_name

            // catch for IJFilter formatting

            String operation = name.split("_",2)[0];

            if (IJFilterList.contains(operation)){
                if (operation == 'Gaussian') {
                    operation += " Blur";
                }
                
                operation += " 3D...";
            }

            // catch for structure_n
            if (operation.contains('Structure')) {
                int periodindex = name.indexOf(".");
                int secondperiodindex = name.indexOf(".", periodindex+1);
                if (secondperiodindex != -1) {
                    operation += "_" + name[secondperiodindex-1];
                }
            }

            if (operation == 'original') {
                operation = "";
            }

            attribute.add(operation+"\t"); // 2. operation

            String parameter = "";

            if (name.contains('largest')) {
                parameter = "0";

            }
            else if (name.contains('middle')) {
                parameter = "1";

            }
            else if (name.contains('smallest')) {
                parameter = "2";

            }

            if (name.split("_",3).length > 1) {
                if (name.split("_",3)[1].length() == 1) {
                parameter = name.split("_",3)[1];
                } 
            }

            attribute.add(parameter+"\t"); // 3.parameter
            
            String sigma = "0";

            if (name.indexOf(".") != -1) {
                int periodindex = name.indexOf(".");
                sigma = name.charAt(periodindex-1);
            }
            attribute.add(sigma+"\t"); // 4.sigma

            String group = "";

            if (operation == "") {
                group = "original";
            }

            else {
                group = "ImageScience";
            }

            for (String filter: IJFilterList) {
                if (name.startsWith(filter)) {
                    group = "IJ_filter";
                }
            }

            attribute.add(group+"\t"); // 5.group
            
            // 6. downsample
            if (downSample) {
                if (Integer.parseInt(sigma) >= 8) {
                    attribute.add("2_2_1\t");
                    attribute.set(0, name+"2_2_1\t")
                }
                else {
                    attribute.add("\t");
                }
            }

            else {
                attribute.add("\t");
            }
            // 7. Presence (1 or 0)
            attribute.add("1\n");

            // add to StringBuilder
            for (String value: attribute) {
                featureModelTable.append(value);
            }

        }
    }
    writer.write(featureModelTable.toString());

    }
catch (FileNotFoundException e) {
      println(e.getMessage());
    }

