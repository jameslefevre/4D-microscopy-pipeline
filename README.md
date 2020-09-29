# 4D-microscopy-pipeline

This project contains executables and source code for an automated microscopy data analysis pipeline based on Fiji/ImageJ.

Briefly, semantic segmentation is performed using a machine learning algorithm, then objects are separated and quantified within each class, and tracked. The pipeline is modular an designed for easy and flexible deployment. Example scripts for deployment on an hpc system using PBS job system are provided without documentation.

A visualiser designed to be used with this pipeline is provided here:
<http://github.com/jameslefevre/visualiser-4D-microscopy-analysis>



## Installation

1. Install [FIJI](https://imagej.net/Fiji) on the machine to be used for computation.
2. Install the plugins Trainable Weka / Trainable_Segmentation, 3D_ImageJ_Suite / mcib3d-suite, MorphoLibJ, Skeletonize3D, AnalyzeSkeleton.
3. Copy the file Segmented_Image_Analysis.jar into the jar folder within the FIJI folder.
4. Copy the contents of the scripts folder onto the analyis machine.

