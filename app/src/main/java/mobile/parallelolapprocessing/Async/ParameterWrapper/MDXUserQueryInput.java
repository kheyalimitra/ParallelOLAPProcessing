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
    public List<String> DimensionInput;
    public Measures measuresObj;
    public HashMap<Integer,String> measureMap;
    public List<String> measureInput ;
    public MDXUserQueryInput(int[] entryPerDimension, TreeNode rootDimensionTree,List<String>inputDim,Measures measuresObj,HashMap<Integer,String>measureMap,
                             List<String> mInput) {
        this.entryPerDimension = entryPerDimension;
        this.rootDimensionTree = rootDimensionTree;
        this.DimensionInput = inputDim;
        this.measuresObj = measuresObj;
        this.measureMap= measureMap;
        this.measureInput = mInput;

    }
}
