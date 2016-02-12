package mobile.parallelolapprocessing.Async.ParameterWrapper;

import java.util.HashMap;
import java.util.List;

import DataRetrieval.Measures;
import DataStructure.TreeNode;

/**
 * Created by KheyaliMitra on 2/1/2016.
 */
public class MDXUserQueryInput {
    public int[] entryPerDimension;
    public TreeNode rootDimensionTree;
    public List<String> hardcodedInputDim;
    public Measures measuresObj;
    public HashMap<Integer,String> measureMap;
    public List<String> hardcodedInputMeasures ;
    public MDXUserQueryInput(int[] entryPerDimension, TreeNode rootDimensionTree,List<String>hardcodedInputDim,Measures measuresObj,HashMap<Integer,String>measureMap,
                             List<String>   hardcodedInputMeasures) {
        this.entryPerDimension = entryPerDimension;
        this.rootDimensionTree = rootDimensionTree;
        this.hardcodedInputDim = hardcodedInputDim;
        this.measuresObj = measuresObj;
        this.measureMap= measureMap;
        this.hardcodedInputMeasures = hardcodedInputMeasures;

    }
}
