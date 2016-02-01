package mobile.parallelolapprocessing;

import java.util.HashMap;
import java.util.List;

import DataStructure.TreeNode;

/**
 * Created by KheyaliMitra on 1/31/2016.
 */
public class MDXParameterWrapper {
   public List<List<List<Integer>>> allAxisDetails;
    public List<Integer> selectedMeasures;
    public HashMap<Integer, String> measureMap;
    public HashMap<Integer, TreeNode> keyValPairsForDimension;
    public List<List<String>>cellOrdinalCombinations;
    public String olapServerURL;
    public MDXParameterWrapper(List<List<List<Integer>>> allAxisDetails, List<Integer> selectedMeasures, HashMap<Integer, String> measureMap,
                               HashMap<Integer, TreeNode> keyValPairsForDimension, List<List<String>> cellOrdinalCombinations, String olapURL)
    {
        this.allAxisDetails = allAxisDetails;
        this.selectedMeasures =selectedMeasures;
        this.measureMap = measureMap;
        this.keyValPairsForDimension = keyValPairsForDimension;
        this.cellOrdinalCombinations = cellOrdinalCombinations;
        this.olapServerURL = olapURL;
    }
}
