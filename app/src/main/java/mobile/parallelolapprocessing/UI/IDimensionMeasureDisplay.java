package mobile.parallelolapprocessing.UI;

import java.util.HashMap;

import DataStructure.TreeNode;

/**
 * Created by jayma on 3/13/2016.
 */
public interface IDimensionMeasureDisplay {

    /**
     * Get String to display
     * @param ResultSet
     * @param DimensionRef
     * @param MeasuresRef
     * @return
     */
    String GetDisplay(HashMap<String,HashMap<String,Long>>  ResultSet,
                             HashMap<Integer, TreeNode> DimensionRef,
                             HashMap<Integer,String> MeasuresRef);
}
