package DataRetrieval;

import DataStructure.TreeNode;
import java.util.HashMap;
import java.util.List;

/**
 * Created by KheyaliMitra on 1/5/2016.
 */
public class DataCubeAxis {

    /**
     * Put all dimensions in each axis based on input array entryPerAxis
     * @param tree Tree node to store Dimension hierarchy
     * @param dimensionList Example: [[Account.all Account][Customer.all customer]][[Geography.All geography][Customer. gender]]
     * @param entryPerAxis Number of dimension is stored in each array element where index of that element represents Axis Number
     *                     Example: :[2,1] => Signifies there are two axis and in axis 0 it has 2 dimension and in axis 1 it has 1 dimension
     * @return             HashMap where key represents axis of the cube and Value represents list of dimensions
     *                      Example: 1 => List [[TreeNode1],[TreeNode2]]
     *                               2 => List [[TreeNode4],[TreeNode7]]
     */
    public HashMap<Integer,List<TreeNode>> GetTreeNodeListForEachAxis(TreeNode tree, List<String> dimensionList, int[] entryPerAxis){
        //Gets the axis size
        int numberOfAxis = entryPerAxis.length;
        //Gets total dimensions
        int totalDimensions = dimensionList.size();

        HashMap<Integer,List<TreeNode>> selectedDimen= new HashMap<Integer,List<TreeNode>>();
        int start=0;
        Dimension dm = new Dimension();
        for( int i=0;i<numberOfAxis;i++) {
            selectedDimen.put(i,dm.GetTreeListFromDimensionList(tree, dimensionList.subList(start,start+ entryPerAxis[i])));
            start += entryPerAxis[i];
        }
        return selectedDimen;
    }
}
