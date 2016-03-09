package mobile.parallelolapprocessing.Async;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import DataRetrieval.Cube;
import DataStructure.TreeNode;
import MDXQueryProcessor.MDXQProcessor;

import mobile.parallelolapprocessing.CacheProcess;

/**
 * Created by jayma on 2/28/2016.
 */
public class CacheProcessUpto1Level extends AsyncTask<Void,Void,String> {
    public List<List<List<Integer>>> allAxisDetails;
    public List<Integer> selectedMeasures;
    public HashMap<Integer, String> measureMap;
    public HashMap<Integer, TreeNode> keyValPairsForDimension;
    public List<List<String>>cellOrdinalCombinations;
    public String olapServerURL;
    private Thread inflatedDataDnldThread;
    public static List<String> inflatedQueries;
    private long start=0;
    List<List<TreeNode>> parentEntiresPerAxis;
    public CacheProcessUpto1Level(List<List<List<Integer>>> allAxisDetails, List<Integer> selectedMeasures, HashMap<Integer, String> measureMap,
                        HashMap<Integer, TreeNode> keyValPairsForDimension, List<List<String>> cellOrdinalCombinations, String olapURL)
    {
        this.allAxisDetails = allAxisDetails;
        this.selectedMeasures =selectedMeasures;
        this.measureMap = measureMap;
        this.keyValPairsForDimension = keyValPairsForDimension;
        this.cellOrdinalCombinations = cellOrdinalCombinations;
        this.olapServerURL = olapURL;
    }
    public CacheProcessUpto1Level()
    {
        if(CacheProcessUpto1Level.inflatedQueries == null)
        {
            CacheProcessUpto1Level.inflatedQueries = new ArrayList<>();
        }
    }

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
    private void run() {
        try {

            Cube c = new Cube(olapServerURL);
            MDXQProcessor mdxQ = new MDXQProcessor();
            CacheProcess cpObj = new CacheProcess();
            List<HashMap<Integer, TreeNode>> allLeaves = _getSiblingsPerAxis(keyValPairsForDimension, allAxisDetails.get(0));
            if(allLeaves.size()>0) {
                List<List<List<Integer>>> newAxisDetails = cpObj.generateNewAxisDetails(allLeaves, allAxisDetails);
                List<List<String>> cellOrdinalCombinations = new ArrayList<>();
                int queryCount = allAxisDetails.size();
                for (int i = 0; i < queryCount; i++) {
                    cellOrdinalCombinations.add(mdxQ.GenerateCellOrdinal(newAxisDetails.get(i)));
                }

                mdxQ.GenerateQueryString(allAxisDetails, selectedMeasures, measureMap,
                        keyValPairsForDimension, true,true);
                List<List<Long>> cubeInflated = c.GetCubeData(inflatedQueries.get(0));
                mdxQ.CheckAndPopulateCache(cellOrdinalCombinations.get(0), this.parentEntiresPerAxis, cubeInflated);// assuming only 1 query entry
            }
        }
        catch(Exception e) {
            String ex = e.getMessage();
        }
    }
    private List<HashMap<Integer,TreeNode>> _getSiblingsPerAxis(HashMap<Integer, TreeNode> keyValPairsForDimension,
                                                               List<List<Integer>> allAxisDetails) {
        List<HashMap<Integer, TreeNode>> Leaves = new ArrayList<>();
        boolean isRootNode = false;
        for (int i = 1; i < allAxisDetails.size(); i++)// 0 th entry is for measures
        {
            for (int j = 0; j < allAxisDetails.get(i).size(); j++) {

                TreeNode node = keyValPairsForDimension.get(allAxisDetails.get(i).get(j));
                if (node != null) {
                    node = node.getParent();
                    if(node.getReference().toString()!="Dimensions") {
                        //if this is in root level, we do not need to add that
                        HashMap<Integer, TreeNode> allLeaves = new CacheProcess().getLeaves(node, new HashMap<Integer, TreeNode>());
                        Leaves.add(allLeaves);
                    }
                    else {
                        isRootNode = true;
                        break;
                    }
                }

            }
            if(isRootNode)
                break;
        }
        //if this is in root level, we do not need to add that : simply return blank list
        if(isRootNode){
            return new ArrayList<>();
        }
        return Leaves;
    }
    @Override
    protected String doInBackground(Void... params) {
        this.run();

        return "Success";
    }

}
