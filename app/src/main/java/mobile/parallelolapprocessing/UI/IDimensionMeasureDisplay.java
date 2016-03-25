package mobile.parallelolapprocessing.UI;

import android.support.annotation.IntegerRes;

import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

import DataStructure.TreeNode;

/**
 * Created by jayma on 3/13/2016.
 */
public interface IDimensionMeasureDisplay {

    /***
     * Get Display ui for user query
     * @param CacheContent
     * @param UserSelectedKeyCombinations
     * @param UserSeletedMeasures
     * @param DimensionReference
     * @param MeasureReference
     * @return
     */
    String GetDisplay(HashMap<String,HashMap<Integer,Long>> CacheContent,
                      List<String> UserSelectedKeyCombinations,
                      List<Integer> UserSeletedMeasures,
                      HashMap<Integer,TreeNode> DimensionReference,
                      HashMap<Integer,String> MeasureReference);
}
