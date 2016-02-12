package DataCaching;

import DataRetrieval.Cube;
import DataRetrieval.Dimension;
import DataRetrieval.Soap;
import DataStructure.TreeNode;
import MDXQueryProcessor.MDXQProcessor;
import java.util.*;
import java.lang.*;
import org.json.simple.parser.JSONParser;

/**
 * Created by KheyaliMitra on 1/14/2016.
 */
public class DataCaching {





   private List<List<TreeNode>> _generateChildren(List<String[]>keys, HashMap<Integer,List<TreeNode>>Dimensions) {
        List<List<TreeNode>> children = new ArrayList<>();

        for (int i = 0; i < keys.size(); ++i) {
            List<TreeNode>innerNodes =  new ArrayList<>();
            List<TreeNode> leaves= new ArrayList<>();
            for (int j = 0; j < keys.get(i).length; ++j) {
               TreeNode node = Dimensions.get(i).get(j);
                if (node.getChildren().size()== 0) {// it was ===
                    innerNodes.addAll(Arrays.asList(node));
                } else {
                    leaves.addAll(_getLeaves(node));

                }
            }
            if(leaves.size()>0) {
                children.add(leaves);
            }
            else
            {
                if(innerNodes.size()>0)
                {
                    children.add(innerNodes);
                }
            }
        }

        return children;
    }
    private List<TreeNode> _getLeaves(TreeNode node) {
        List<TreeNode> leaves = new ArrayList<>();
        List<TreeNode> children = node.getChildren();
        if (children.size()== 0) {
            leaves.add(node);
            return leaves;
        }
        else {
            for (int i = 0; i < children.size(); i++) {
                leaves.addAll(_getLeaves(children.get(i)));
            }
            return leaves;
        }
    }
}

