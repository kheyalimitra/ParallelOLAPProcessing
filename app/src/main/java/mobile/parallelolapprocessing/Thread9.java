package mobile.parallelolapprocessing;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import DataRetrieval.Cube;
import DataStructure.TreeNode;
import MDXQueryProcessor.MDXQProcessor;
import mobile.parallelolapprocessing.Async.CacheProcessUpto1Level;

/**
 * Created by jayma on 4/7/2016.
 */
public class Thread9 extends AsyncTask {
    public List<List<List<Integer>>> allAxisDetails;
    public List<Integer> selectedMeasures;
    public HashMap<Integer, String> measureMap;
    public HashMap<Integer, TreeNode> keyValPairsForDimension;
    public List<List<String>>cellOrdinalCombinations;
    public String olapServerURL;

    public static List<String> inflatedQueries;
    private long start=0;
    List<List<TreeNode>> parentEntiresPerAxis;
    public Thread9(List<List<List<Integer>>> allAxisDetails, List<Integer> selectedMeasures, HashMap<Integer, String> measureMap,
                   HashMap<Integer, TreeNode> keyValPairsForDimension, List<List<String>> cellOrdinalCombinations, String olapURL)
    {
        this.allAxisDetails = allAxisDetails;
        this.selectedMeasures =selectedMeasures;
        this.measureMap = measureMap;
        this.keyValPairsForDimension = keyValPairsForDimension;
        this.cellOrdinalCombinations = cellOrdinalCombinations;
        this.olapServerURL = olapURL;
    }
    public Thread9()
    {
        if(Thread9.inflatedQueries == null)
        {
            Thread9.inflatedQueries = new ArrayList<>();
        }
        if(CacheProcess.inflatedQueries == null)
        {
            CacheProcess.inflatedQueries = new ArrayList<>();
        }

    }

    @Override
    protected Object doInBackground(Object[] params) {
        Log.d("Inflated Query 8:", "Time taken to start job " + String.valueOf(System.currentTimeMillis()));
        this.run();
        long end = System.currentTimeMillis();
        Log.d("Inflated Query 8:", "Time taken to finish job " + String.valueOf(end));


        return "Success";
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
    public void run() {
        try {

            Cube c = new Cube(olapServerURL);
            MDXQProcessor mdxQ = new MDXQProcessor();
            CacheProcess cpObj = new CacheProcess();
            Thread9 th9 = new Thread9();
            List<List<HashMap<Integer, TreeNode>>> allLeaves = _getSiblingsPerAxis(keyValPairsForDimension, allAxisDetails.get(0));
            if(allLeaves.size()>0) {
                List<List<List<Integer>>> newAxisDetails = cpObj.generateNewAxisDetails(allLeaves, allAxisDetails);
                List<List<String>> cellOrdinalCombinations = new ArrayList<>();
                int queryCount = allAxisDetails.size();
                for (int i = 0; i < queryCount; i++) {
                    cellOrdinalCombinations.add(mdxQ.GenerateCellOrdinal(newAxisDetails.get(i)));
                }

                List<String>  queryListForSiblings = mdxQ.GenerateQueryString(allAxisDetails, selectedMeasures, measureMap,
                        keyValPairsForDimension, true, true);
                if(!Thread9.inflatedQueries.contains(queryListForSiblings.get(0)) ) {
                    Thread9.inflatedQueries.add(queryListForSiblings.get(0));
                    String query =  "select {[Measures].[Internet Sales Amount]} on axis(0), DESCENDANTS({[Geography].[Geography].[All Geographies].[France]},2,LEAVES) on axis(1) ,DESCENDANTS({[Employee].[Employee Department].[All Employees]},1,LEAVES) on axis(2) from [Adventure Works]";

                    // "select {[Measures].[Internet Sales Amount]} on axis(0), DESCENDANTS({[Geography].[Geography].[All Geographies].[France]},2,LEAVES) on axis(1) ,DESCENDANTS({[Employee].[Employee Department].[All Employees]},3,LEAVES) on axis(2) from [Adventure Works]";
                    //String query// = "select {[Measures].[Internet Sales Amount]} on axis(0), DESCENDANTS({[Employee].[Employee Department].[All Employees].[Document Control],[Employee].[Employee Department].[All Employees].[Engineering],[Employee].[Employee Department].[All Employees].[Executive],[Employee].[Employee Department].[All Employees].[Facilities and Maintenance],[Employee].[Employee Department].[All Employees].[Finance],[Employee].[Employee Department].[All Employees].[Human Resources],[Employee].[Employee Department].[All Employees].[Information Services],[Employee].[Employee Department].[All Employees].[Marketing]},2,LEAVES) on axis(1) ,DESCENDANTS({[Geography].[Geography].[All Geographies].[Australia],[Geography].[Geography].[All Geographies].[Canada],[Geography].[Geography].[All Geographies].[France],[Geography].[Geography].[All Geographies].[United Kingdom],[Geography].[Geography].[All Geographies].[United States]},3,LEAVES) on axis(2) from [Adventure Works]";
                    List<List<Long>> cubeInflated = c.GetCubeData(query);// since last entry is the current one
                    mdxQ.CheckAndPopulateCache(cellOrdinalCombinations.get(0), this.parentEntiresPerAxis, cubeInflated,false);// assuming only 1 query entry
                }
            }
        }
        catch(Exception e) {
            String ex = e.getMessage();
        }

    }
    private List<List<HashMap<Integer,TreeNode>>> _getSiblingsPerAxis(HashMap<Integer, TreeNode> keyValPairsForDimension,
                                                                      List<List<Integer>> allAxisDetails) {
        List<List<HashMap<Integer, TreeNode>>> Leaves = new ArrayList<>();
        boolean isRootNode = false;
        for (int i = 1; i < allAxisDetails.size(); i++)// 0 th entry is for measures
        {
            List<HashMap<Integer, TreeNode>> axisWiseList = new ArrayList<>();
            for (int j = 0; j < allAxisDetails.get(i).size(); j++) {
                int key = allAxisDetails.get(i).get(j);

                TreeNode node = keyValPairsForDimension.get(key);
                if (node != null) {
                    node = node.getParent();
                    if (node.getReference().toString() != "Dimensions") {
                        //if this is in root level, we do not need to add that
                        HashMap<Integer, TreeNode> allLeaves = iterateTreeToGenerateChildren(node);
                        axisWiseList.add(allLeaves);
                    }
                    //}
                } else {
                    isRootNode = true;
                    break;
                }
            }
            Leaves.add(axisWiseList);
            if (isRootNode)
                break;
        }
        //if this is in root level, we do not need to add that : simply return blank list
        if(isRootNode){
            return new ArrayList<>();
        }
        return Leaves;
    }


}
