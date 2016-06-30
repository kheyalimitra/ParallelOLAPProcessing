package mobile.parallelolapprocessing;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import DataRetrieval.Cube;
import DataStructure.TreeNode;
import MDXQueryProcessor.MDXQProcessor;
import mobile.parallelolapprocessing.Async.CacheProcessUpto1Level;

/**
 * Created by KheyaliMitra on 6/17/2016.
 */
public class Inflated3 {//implements Runnable{

    public List<List<List<Integer>>> allAxisDetails;
    public List<Integer> selectedMeasures;
    public HashMap<Integer, String> measureMap;
    public HashMap<Integer, TreeNode> keyValPairsForDimension;
    public List<List<String>>cellOrdinalCombinations;
    public String olapServerURL;
    public List<Boolean> isAddChildrenToDimension;
    private long start=0;
    List<List<HashMap<Integer, TreeNode>>> allLeaves;
    List<List<TreeNode>> parentEntiresPerAxis;
    public static HashSet<String> inflatedQueries;
    public Inflated3(List<List<List<Integer>>> allAxisDetails, List<Integer> selectedMeasures, HashMap<Integer, String> measureMap,
                     HashMap<Integer, TreeNode> keyValPairsForDimension, List<List<String>> cellOrdinalCombinations, String olapURL, List<List<HashMap<Integer, TreeNode>>> allLeaves,List<List<TreeNode>> parentEntiresPerAxis)
    {
        this.allAxisDetails = allAxisDetails;
        this.selectedMeasures =selectedMeasures;
        this.measureMap = measureMap;
        this.keyValPairsForDimension = keyValPairsForDimension;
        this.cellOrdinalCombinations = cellOrdinalCombinations;
        this.olapServerURL = olapURL;
        this.allLeaves = allLeaves;
        this.parentEntiresPerAxis = parentEntiresPerAxis;
        isAddChildrenToDimension = new ArrayList<>();

        instatiateOtherThreads();
    }
    public void instatiateOtherThreads(){
        if(inflatedQueries == null)
        {
            inflatedQueries = new HashSet<>();
        }
        if(inflatedQueries == null)
        {
            inflatedQueries = new HashSet<>();
        }
        if(Inflated1.inflatedQueries == null)
        {
            Inflated1.inflatedQueries = new HashSet<>();
        }
        if(Inflated2.inflatedQueries == null)
        {
            Inflated2.inflatedQueries = new HashSet<>();
        }
        if(CacheProcessUpto1Level.inflatedQueries == null)
        {
            CacheProcessUpto1Level.inflatedQueries = new HashSet<>();
        }

    }
       private List<List<List<Integer>>> generateNewAxisDetails(List<List<HashMap<Integer, TreeNode>>> allLeaves, List<List<List<Integer>>> allAxisDetails) {
        List<List<List<Integer>>> newAxisDetails= new ArrayList<>();
        List<List<Integer>> newAxis= new ArrayList<>();
        //Add measures to the first axis
        newAxis.add(allAxisDetails.get(0).get(0));
        int leavesSize = allLeaves.size();
        for (int i = 0; i <leavesSize; i++) {
            //Individual axis
            List<HashMap<Integer, TreeNode>> axis = allLeaves.get(i);
            List<Integer> dimensionList = new ArrayList<>();
            for (int j = 0; j < axis.size(); j++) {
                dimensionList.addAll(axis.get(j).keySet());
            }
            newAxis.add(dimensionList);
            isAddChildrenToDimension.add(true);
        }
        newAxisDetails.add(newAxis);
        return newAxisDetails;
    }


    public void run() {
        try {
            start = System.currentTimeMillis();

            Cube c = new Cube(olapServerURL);
            MDXQProcessor mdxQ = new MDXQProcessor();
            List<List<List<Integer>>> newAxisDetails = generateNewAxisDetails(allLeaves, allAxisDetails);
            List<List<String>> cellOrdinalCombinations = new ArrayList<>();

            List<String>  queryListForChildren  = _generateQueryString(allAxisDetails, selectedMeasures, measureMap,
                    keyValPairsForDimension, true,false);
            /*if (!inflatedQueries.contains(queryListForChildren.get(0)) &&
                    !Inflated1.inflatedQueries.contains(queryListForChildren.get(0)) &&
                    !Inflated2.inflatedQueries.contains(queryListForChildren.get(0)) &&
                    !CacheProcessUpto1Level.inflatedQueries.contains(queryListForChildren.get(0))){
            */    {int queryCount = allAxisDetails.size();
                    for (int i = 0; i < queryCount; i++) {
                        cellOrdinalCombinations.add(mdxQ.GenerateCellOrdinal(newAxisDetails.get(i)));
                    }

                    Log.d("Inflated Query3.1", "Start data download  " + String.valueOf(System.currentTimeMillis()));
                    inflatedQueries.add(queryListForChildren.get(0));
                    Log.d("Inflated Query3.1", "MDX query: " + String.valueOf(queryListForChildren.get(0)));
                    List<List<Long>> cubeInflated = c.GetCubeData(queryListForChildren.get(0));
                    Log.d("Inflated Query3.1", "MDX query down load ends: " + String.valueOf(System.currentTimeMillis()));
                    mdxQ.CheckAndPopulateCache(cellOrdinalCombinations.get(0), this.parentEntiresPerAxis, cubeInflated, false);// assuming only 1 query entry
                    Log.d("Inflated Query3.1", "process ends " + String.valueOf(System.currentTimeMillis()));
                }

        }
        catch(Exception e) {
            String ex = e.getMessage();
        }
    }
    private List<String>_generateQueryString(List<List<List<Integer>>> queryDetails,List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,
                                            HashMap<Integer,TreeNode> keyValPairsForDimension, boolean isAddDescendant,boolean isAddInflatedSiblings ){
        int nQueryCount =queryDetails.size();
        List<String> subQueries =  new ArrayList<>();
        for(int i=0;i<nQueryCount;i++)
        {
            String querystring = this._generateSubQueryString(queryDetails.get(i), selectedMeasures, measureMap, keyValPairsForDimension, isAddDescendant, isAddInflatedSiblings);
            subQueries.add(querystring);

        }
        return subQueries;
    }

    private String _generateSubQueryString(List<List<Integer>> queryDetails,List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,
                                           HashMap<Integer,TreeNode> keyValPairsForDimension, boolean isAddDescendant,boolean isFindSiblings ){
        MDXQProcessor mdxQ = new MDXQProcessor();
        String sqlMeasureStatement = mdxQ.generateQueryForMeasures(selectedMeasures, measureMap);
        int axisCount =queryDetails.size();
        List<String> axisWiseQuery =  new ArrayList<>();
        axisWiseQuery.add(sqlMeasureStatement);
        for(int i=1;i<axisCount;i++)// assuming axis 0 is always for measures
        {
            List<Integer> dimensionkeyList = queryDetails.get(i);
            List<String> dimensionList =  new ArrayList<>();
            int dimensionCount = dimensionkeyList.size();
            List<String> originalDimensions = new ArrayList<>();
            TreeNode  node;
            for(int j=0;j<dimensionCount;j++)
            {
                node= keyValPairsForDimension.get(dimensionkeyList.get(j));
                if(isAddDescendant) {
                    // this one is for all dimensions which are currently pointed as leaf node, if that is so, go 1 level up and fetch records for all leaves of same parents
                    originalDimensions.add(node.getHierarchyName().substring(node.getHierarchyName().indexOf(".")+1));
                    if((node.getChildren().size() == 0)|| (isFindSiblings)) {
                        node = node.getParent();
                    }

                }
                String dimensionName = node.getHierarchyName();
                dimensionName =  dimensionName.substring(dimensionName.indexOf(".")+1);// removing [Dimension] part from the string : else it will not execute

                if (isAddChildrenToDimension.get(i-1)) {// since it is not capturing measures details in this list
                    dimensionName += ".children";
                }
                if(!dimensionList.contains(dimensionName))
                    dimensionList.add(dimensionName);

            }
              axisWiseQuery.add("{" + TextUtils.join(",", dimensionList) + "} on axis(" + (i) + ") ");

        }
        String sqlStatement = mdxQ.generateSubQuery(axisWiseQuery);
        return sqlStatement;
    }




}
