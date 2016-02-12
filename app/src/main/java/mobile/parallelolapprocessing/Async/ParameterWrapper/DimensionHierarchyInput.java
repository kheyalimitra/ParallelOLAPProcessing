package mobile.parallelolapprocessing.Async.ParameterWrapper;

import DataStructure.TreeNode;

/**
 * Created by KheyaliMitra on 1/31/2016.
 */
public class DimensionHierarchyInput {
    String userSelection;
    TreeNode rootNode;
    public DimensionHierarchyInput(TreeNode node,String input)
    {
        this.userSelection =  input;
        this.rootNode = node;
    }
}
