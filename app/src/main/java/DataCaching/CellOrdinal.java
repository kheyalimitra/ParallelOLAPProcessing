package DataCaching;

import DataStructure.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by KheyaliMitra on 1/22/2016.
 */
public class CellOrdinal {
    public List<List<Integer>> GetLeafNodeNumbersForSelectedDimensions(List<String>compressedKeys,
                                                                       HashMap<Integer,List<TreeNode>> Dimensions,
                                                                       HashMap<Integer,String> measures) {
        List<List<Integer>> memberList =  new ArrayList<>();
        HashMap<Integer,TreeNode>selectedTreeNodes =  new HashMap<>();

        //just to take all selected dimensions irrespective of axis entries :
        // this is to make calculation easier while looking for leaf nodes
        for(int index=0;index<Dimensions.size();index++)
        {
            for(int innerEntry=0;innerEntry<Dimensions.get(index).size();innerEntry++) {
                TreeNode node =Dimensions.get(index).get(innerEntry);
                selectedTreeNodes.put(node.getNodeCounter(),node);
            }
        }
        // just take measures keys to get cell ordinals
        List<Integer> measureKeys = new ArrayList<>();
        measureKeys.addAll(measures.keySet());
        List<Integer> tempKeys = new ArrayList<>();
        for (int i = 0; i < compressedKeys.size(); ++i) {
            // subKeys: [X1, X2], [Y1, Y2]
            String[] subKeys = compressedKeys.get(i).split("#");
            for (int j = 0; j < subKeys.length; ++j) {
                String subSubKeys = subKeys[j];
                if (!tempKeys.contains(Integer.parseInt(subSubKeys))) {
                    tempKeys.add(Integer.parseInt(subSubKeys));
                }
            }
        }
         memberList = _getChildren(tempKeys, selectedTreeNodes);
        Integer[] currnet = _calculateCellOrdinal(memberList,measureKeys);
        return  memberList;
    }

    private Integer[] _calculateCellOrdinal(List<List<Integer>> memberList , List<Integer>measures) {
        List<Integer> line =new ArrayList<>();
        //int cubeLength = measures.size();//cube.celMeasure.length;
        Integer[] S=new Integer[memberList.size()];
        Integer[] dimensions = new Integer[memberList.size()];
        line = measures;
        int celordinal = line.get(line.size() - 1);
        for (int j = 0; j < memberList.size(); j++) {
            S[j] = celordinal % (memberList.get(j).size());

            dimensions[j] = memberList.get(j).get(S[j]);
            celordinal = (celordinal - S[j]) / memberList.get(j).size();

        }
        return dimensions;
    }

    private List<List<Integer>> _getChildren(List<Integer>keys, HashMap<Integer,TreeNode>Dimensions) {
            //List<List<TreeNode>> children = new ArrayList<>();
        List<List<Integer>> children = new ArrayList<>();
            for (int i = 0; i < keys.size(); ++i) {
                    List<Integer> innerNodes = new ArrayList<>();
                    List<Integer> leaves = new ArrayList<>();
                    int key = keys.get(i);
                        TreeNode node = Dimensions.get(key);
                        if (node != null) {
                            if (node.getChildren().size() == 0) {// it was ===
                                innerNodes.addAll(Arrays.asList(node.getNodeCounter()));
                            } else {
                                leaves.addAll(_getLeaves(node));

                            }
                            if (leaves.size() > 0) {
                                children.add(leaves);
                            } else {
                                if (innerNodes.size() > 0) {
                                    children.add(innerNodes);
                                }
                            }
                        }
                    }
            return children;
        }
    private List<Integer> _getLeaves(TreeNode node) {
        List<Integer> leaves = new ArrayList<>();
        List<TreeNode> children = node.getChildren();
        if (children.size()== 0) {
            leaves.add(node.getNodeCounter());
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
