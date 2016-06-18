package mobile.parallelolapprocessing.Async;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import DataRetrieval.Cube;
import DataStructure.TreeNode;
import MDXQueryProcessor.MDXQProcessor;

import mobile.parallelolapprocessing.Async.Call.MDXUserQuery;
import mobile.parallelolapprocessing.CacheProcess;
import mobile.parallelolapprocessing.Inflated1;
import mobile.parallelolapprocessing.Inflated2;
import mobile.parallelolapprocessing.Inflated3;

/**
 * Created by jayma on 2/28/2016.
 */
public class CacheProcessUpto1Level implements Runnable{///extends AsyncTask<Void,Void,String> {
    public List<List<List<Integer>>> allAxisDetails;
    public List<Integer> selectedMeasures;
    public HashMap<Integer, String> measureMap;
    public HashMap<Integer, TreeNode> keyValPairsForDimension;
    public List<List<String>>cellOrdinalCombinations;
    public String olapServerURL;
    private Thread inflatedDataDnldThread;
    private List<List<HashMap<Integer, TreeNode>>> allParents;
    List<List<TreeNode>> parentEntiresPerAxis;
    public static HashSet<String> inflatedQueries;
    public CacheProcessUpto1Level(List<List<List<Integer>>> allAxisDetails, List<Integer> selectedMeasures, HashMap<Integer, String> measureMap,
                        HashMap<Integer, TreeNode> keyValPairsForDimension, List<List<String>> cellOrdinalCombinations, String olapURL, List<List<HashMap<Integer, TreeNode>>> allParents)
    {
        this.allAxisDetails = allAxisDetails;
        this.selectedMeasures =selectedMeasures;
        this.measureMap = measureMap;
        this.keyValPairsForDimension = keyValPairsForDimension;
        this.cellOrdinalCombinations = cellOrdinalCombinations;
        this.olapServerURL = olapURL;
        this.allParents =  allParents;
        instatiateOtherThreads();
    }
    public void instatiateOtherThreads(){
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
        if(Inflated3.inflatedQueries == null)
        {
            Inflated3.inflatedQueries = new HashSet<>();
        }
    }
    public void run() {
        try {

            Cube c = new Cube(olapServerURL);
            MDXQProcessor mdxQ = new MDXQProcessor();
            if(allParents.size()>0) {
                List<List<List<Integer>>> newAxisDetails = generateNewAxisDetails(allParents, allAxisDetails);
                List<List<String>> cellOrdinalCombinations = new ArrayList<>();
                List<String> queryListForSiblings = _generateQueryString(allAxisDetails, selectedMeasures, measureMap,
                        keyValPairsForDimension);

                    if (!inflatedQueries.contains(queryListForSiblings.get(0)) &&
                            !Inflated1.inflatedQueries.contains(queryListForSiblings.get(0)) &&
                            !Inflated2.inflatedQueries.contains(queryListForSiblings.get(0)) &&
                            !Inflated3.inflatedQueries.contains(queryListForSiblings.get(0))) {
                        inflatedQueries.add(queryListForSiblings.get(0));

                        int queryCount = allAxisDetails.size();
                        for (int i = 0; i < queryCount; i++) {
                            cellOrdinalCombinations.add(mdxQ.GenerateCellOrdinal(newAxisDetails.get(i)));
                        }
                        Log.d("Inflated Query4", "Start data download  " + String.valueOf(System.currentTimeMillis()));
                        Log.d("Inflated Query4", "MDX query:" + String.valueOf(queryListForSiblings.get(0)));

                        List<List<Long>> cubeInflated = c.GetCubeData(queryListForSiblings.get(0));// since last entry is the current one
                        Log.d("Inflated Query4", "MDX query down load ends: " + String.valueOf(System.currentTimeMillis()));
                        mdxQ.CheckAndPopulateCache(cellOrdinalCombinations.get(0), this.parentEntiresPerAxis, cubeInflated, false);// assuming only 1 query entry
                        Log.d("Inflated Query4", "Process ends  " + String.valueOf(System.currentTimeMillis()));

                    }
                }

        }
        catch(Exception e) {
            String ex = e.getMessage();
        }
    }
    private List<String> _generateQueryString(List<List<List<Integer>>> queryDetails,List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,
                                            HashMap<Integer,TreeNode> keyValPairsForDimension ){
        int nQueryCount =queryDetails.size();
        List<String> subQueries =  new ArrayList<>();
        for(int i=0;i<nQueryCount;i++)
        {
            String querystring = this._generateSubQueryString(queryDetails.get(i), selectedMeasures, measureMap, keyValPairsForDimension);
            subQueries.add(querystring);

        }
        return subQueries;
    }

    private String _generateSubQueryString(List<List<Integer>> queryDetails,List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,
                                           HashMap<Integer,TreeNode> keyValPairsForDimension){
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
            TreeNode  node;//= keyValPairsForDimension.get(dimensionkeyList.get(0));
            for(int j=0;j<dimensionCount;j++)
            {
                node= keyValPairsForDimension.get(dimensionkeyList.get(j));
                TreeNode pNode = node.getParent();
                String dimensionName = pNode.getHierarchyName();
                //we discard root node values
                if(dimensionName.equals("[Dimension]")) {
                    dimensionName = node.getHierarchyName();
                }
                dimensionName =  dimensionName.substring(dimensionName.indexOf(".")+1);// removing [Dimension] part from the string : else it will not execute
                dimensionName += ".children";
                if(!dimensionList.contains(dimensionName))
                    dimensionList.add(dimensionName);
            }

            axisWiseQuery.add("{" + TextUtils.join(",", dimensionList) + "} on axis(" + (i) + ") ");

        }
        String sqlStatement = mdxQ.generateSubQuery(axisWiseQuery);
        return sqlStatement;
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
        }
        newAxisDetails.add(newAxis);
        return newAxisDetails;
    }


}
