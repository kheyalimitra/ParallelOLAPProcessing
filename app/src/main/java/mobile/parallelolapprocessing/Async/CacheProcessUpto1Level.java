package mobile.parallelolapprocessing.Async;

import java.util.HashMap;
import java.util.List;

import DataStructure.TreeNode;

/**
 * Created by jayma on 2/28/2016.
 */
public class CacheProcessUpto1Level {
    public HashMap<Integer,TreeNode> iterateTreeToGenerateChildren(TreeNode parent) {
        HashMap<Integer,TreeNode> allLeaves=new HashMap<>();
        List<TreeNode> children = parent.getChildren();
        if (children.size() == 0) {
            allLeaves.put(parent.getNodeCounter(),parent);

        } else {
            for (int i = 0; i < children.size(); i++) {
                allLeaves.put(children.get(i).getNodeCounter(),children.get(i));
            }
        }
        return allLeaves;
    }
}
