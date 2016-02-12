package mobile.parallelolapprocessing;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.lang.ArrayUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import DataRetrieval.Cube;
import DataRetrieval.Dimension;
import DataStructure.TreeNode;
import MDXQueryProcessor.MDXQProcessor;
import Processor.QueryProcessor;

/**
 * Created by KheyaliMitra on 1/31/2016.
 */
public class CacheProcess implements Runnable {//AsyncTask<Void,Void,String> {//implements Runnable {// this is for passing parameters in doInBackground method
    public List<List<List<Integer>>> allAxisDetails;
    public List<Integer> selectedMeasures;
    public HashMap<Integer, String> measureMap;
    public HashMap<Integer, TreeNode> keyValPairsForDimension;
    public List<List<String>>cellOrdinalCombinations;
    public String olapServerURL;
    private Thread inflatedDataDnldThread;
    private long start=0;
    public CacheProcess(List<List<List<Integer>>> allAxisDetails, List<Integer> selectedMeasures, HashMap<Integer, String> measureMap,
                               HashMap<Integer, TreeNode> keyValPairsForDimension, List<List<String>> cellOrdinalCombinations, String olapURL)
    {
        this.allAxisDetails = allAxisDetails;
        this.selectedMeasures =selectedMeasures;
        this.measureMap = measureMap;
        this.keyValPairsForDimension = keyValPairsForDimension;
        this.cellOrdinalCombinations = cellOrdinalCombinations;
        this.olapServerURL = olapURL;
    }
     private String callonPostExecute(String val)
    {
        //messageHandler.sendEmptyMessage(0);
        //MainActivity.CachedDataCubes.get(0);
        long endTime = System.currentTimeMillis() - start;
        if (!Log.isLoggable("MDXQueryDownload", Log.VERBOSE))
            Log.v("MyApplicationTag", "Inflated End MDXQueryDownloaded. Time spent: "+ endTime);
        return "Success";
    }



    //@Override
    protected String doInBackground(Void... params) {
        try {
            start = System.currentTimeMillis();
            if (!Log.isLoggable("MDXQueryDownload", Log.VERBOSE))
                Log.v("MyApplicationTag", "Inflated StartMDXQueryDownload started:");
            Cube c =  new Cube(olapServerURL);
            MDXQProcessor mdxQ = new MDXQProcessor();
            List<HashMap<Integer,TreeNode>> allLeaves =_getLeavesPerAxis(keyValPairsForDimension, allAxisDetails.get(0));
            List<List<List<Integer>>> newAxisDetails = _generateNewAxisDetails(allLeaves,allAxisDetails);
            List<List<String>> cellOrdinalCombinations= new ArrayList<>();
            int queryCount =allAxisDetails.size();
            for(int i=0;i<queryCount;i++) {
                cellOrdinalCombinations.add(mdxQ.GenerateCellOrdinal(newAxisDetails.get(i)));
            }

            List<String> inflatedQueries = mdxQ.GenerateQueryString(allAxisDetails, selectedMeasures, measureMap,
                    keyValPairsForDimension, true);
            List<List<Long>>cubeInflated = c.GetCubeData(inflatedQueries.get(0));
            long endTime = System.currentTimeMillis() - start;
            if (!Log.isLoggable("MDXQueryDownload", Log.VERBOSE))
                Log.v("MyApplicationTag", "Inflated query down load time from server: "+ endTime);

            mdxQ.CheckAndPopulateCache(cellOrdinalCombinations.get(0), cubeInflated);// assuming only 1 query entry

        }
        catch(Exception ex){
            String e =  ex.getMessage();
        }
        String backGroundThread = "Back ground thread";
        return callonPostExecute(backGroundThread);
    }

    private List<List<List<Integer>>> _generateNewAxisDetails(List<HashMap<Integer, TreeNode>> allLeaves, List<List<List<Integer>>> allAxisDetails) {
        List<List<List<Integer>>> newAxisDetails= new ArrayList<>();
        List<List<Integer>> newAxis= new ArrayList<>();
        int hashIndex=0;
        newAxis.add(allAxisDetails.get(0).get(0));
        for(int i=0;i<allAxisDetails.size(); i++){// total queries
            for (int j=1;j<allAxisDetails.get(i).size();j++){// no of axis
                List<Integer> dimen =  new ArrayList<>();
                for(int k=0;k<allAxisDetails.get(i).get(j).size();k++)// entry per axis
                {
                    dimen=new ArrayList<Integer>(allLeaves.get(hashIndex++).keySet());
                    newAxis.add(dimen);
                }
            }
            newAxisDetails.add(newAxis);
        }

        return newAxisDetails;
    }

    private List<HashMap<Integer,TreeNode>> _getLeavesPerAxis(HashMap<Integer, TreeNode> keyValPairsForDimension,
                                                       List<List<Integer>> allAxisDetails) {
        List<HashMap<Integer, TreeNode>> Leaves = new ArrayList<>();
        for (int i = 1; i < allAxisDetails.size(); i++)// 0 th entry is for measures
        {
            for (int j = 0; j < allAxisDetails.get(i).size(); j++) {
                TreeNode child = keyValPairsForDimension.get(allAxisDetails.get(i).get(j));
                if (child != null) {
                    if(child.getChildren().size()==0)
                    {
                    // 1st time this is running and the parent node itself is leaf node:
                    // then go back 1 level up and run for its parent
                        child = child.getParent();
                    }
                    HashMap<Integer,TreeNode> allLeaves = _getLeaves(child, new HashMap<Integer, TreeNode>());
                    Leaves.add(allLeaves);
                }
            }
        }
        return Leaves;
    }
    private  HashMap<Integer,TreeNode> _getLeaves(TreeNode parent,HashMap<Integer,TreeNode> oldLeaves){
        return _iterateTreeToGenerateLeaves(parent,oldLeaves);

    }

    private HashMap<Integer,TreeNode> _iterateTreeToGenerateLeaves(TreeNode parent, HashMap<Integer,TreeNode> oldLeaves) {
        HashMap<Integer,TreeNode> allLeaves=oldLeaves;
        List<TreeNode>children = parent.getChildren();
        if (children.size() == 0) {
            allLeaves.put(parent.getNodeCounter(),parent);

        } else {
            for (int i = 0; i < children.size(); i++) {
                TreeNode child = children.get(i);
                if(child!=null) {
                    _iterateTreeToGenerateLeaves(child,allLeaves);
                }
            }
        }
        return allLeaves;
    }
     @Override
    public void run() {
        try {
            Cube c =  new Cube(QueryProcessor.olapServiceURL);
            MDXQProcessor mdxQ = new MDXQProcessor();
            List<String> inflatedQueries = mdxQ.GenerateQueryString(allAxisDetails, selectedMeasures, measureMap,
                    keyValPairsForDimension, true);
            List<List<Long>>cubeInflated = c.GetCubeData(inflatedQueries.get(0));
            long endTime = System.currentTimeMillis() -start;
            if (!Log.isLoggable("MDXQueryDownload", Log.VERBOSE))
                Log.v("MyApplicationTag", "Inflated query down load time from server: "+ endTime);

            mdxQ.CheckAndPopulateCache(cellOrdinalCombinations.get(0), cubeInflated);// assuming only 1 query entry
            long cachingTime = System.currentTimeMillis() -start;
            if (!Log.isLoggable("MDXQueryDownload", Log.VERBOSE))
                Log.v("MyApplicationTag", "Inflated query caching time: "+ (cachingTime-endTime));

            MainActivity.CachedDataCubes.get(0);
        }
        catch(Exception ex){
            String e =  ex.getMessage();
        }
    }
    public void start ()
    {
        if (inflatedDataDnldThread == null)
        {
            inflatedDataDnldThread = new Thread (this);
            inflatedDataDnldThread.start ();
            start = System.currentTimeMillis();

        }
    }


}
