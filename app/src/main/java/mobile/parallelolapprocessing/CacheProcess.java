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
import mobile.parallelolapprocessing.Async.CacheProcessUpto1Level;

/**
 * Created by KheyaliMitra on 1/31/2016.
 */
public class CacheProcess extends AsyncTask<Void,Void,String> {//implements Runnable {// this is for passing parameters in doInBackground method
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
    public CacheProcess(){
        if(CacheProcess.inflatedQueries == null)
        {
            CacheProcess.inflatedQueries = new ArrayList<>();
        }
        if(CacheProcessUpto1Level.inflatedQueries == null)
        {
            CacheProcessUpto1Level.inflatedQueries = new ArrayList<>();
        }
    }
    public List<List<List<Integer>>> generateNewAxisDetails(List<List<HashMap<Integer, TreeNode>>> allLeaves, List<List<List<Integer>>> allAxisDetails) {
        List<List<List<Integer>>> newAxisDetails= new ArrayList<>();
        List<List<Integer>> newAxis= new ArrayList<>();

        //Add measures to the first axis
        newAxis.add(allAxisDetails.get(0).get(0));
        int dimenAxis =1;
        for(int i=0;i<allLeaves.size();i++){
            //Individual axis
            List<HashMap<Integer,TreeNode>> axis = allLeaves.get(i);
            List<Integer> dimensionList = new ArrayList<>();
            for(int j=0;j<axis.size();j++){
                dimensionList.addAll(axis.get(j).keySet());
            }
            dimensionList.addAll(allAxisDetails.get(0).get(dimenAxis++));

            newAxis.add(dimensionList);
        }

        newAxisDetails.add(newAxis);



        return newAxisDetails;
    }

    public List<List<HashMap<Integer, TreeNode>>>getLeavesPerAxis(HashMap<Integer, TreeNode> keyValPairsForDimension,
                                                       List<List<Integer>> allAxisDetails) {
        List<List<HashMap<Integer, TreeNode>> > Leaves = new ArrayList<>();
        this.parentEntiresPerAxis =  new ArrayList<>();
        for (int i = 1; i < allAxisDetails.size(); i++)// 0 th entry is for measures
        {
            List<TreeNode> parents = new ArrayList<>();
            List<HashMap<Integer, TreeNode>> axisWiseList = new ArrayList<>();
            for (int j = 0; j < allAxisDetails.get(i).size(); j++) {

                TreeNode child = keyValPairsForDimension.get(allAxisDetails.get(i).get(j));
                if (child != null) {
                    List<TreeNode> children = child.getChildren();
                    if(children!=null && children.size()==0)
                    {
                    // 1st time this is running and the parent node itself is leaf node:
                     // then go back 1 level up and run for its parent
                        child = child.getParent();

                    }
                    else{
                        // the following section deals with situations like: Education. All Customers.  We know that the server will send values for all under 'All custmers' sections
                        // so if we go to its parent that is Education, its children will be 'All customers' which must be replaced by all those children under it.
                        // Education.Children--> All customers but we need Bachelors, Hgh School  Graduates etc

                        if(children!=null && children.size()>0 && children.size()<=1) {
                            String childName = children.get(0).getReference().toString();
                            if (!childName.contains("All")) {
                                child = child.getParent();
                            } else {
                                child = children.get(0);

                            }
                        }
                    }
                    // old approach
                    //parents.add(child);
                    HashMap<Integer,TreeNode> allLeaves = getLeaves(child, new HashMap<Integer, TreeNode>());
                    axisWiseList.add(allLeaves);
                }
            }
            Leaves.add(axisWiseList);
            parentEntiresPerAxis.add(parents);
        }
        return Leaves;
    }
    public  HashMap<Integer,TreeNode> getLeaves(TreeNode parent,HashMap<Integer,TreeNode> oldLeaves){

        return new CacheProcessUpto1Level().iterateTreeToGenerateChildren(parent);

    }


    private void run() {
        try {
            start = System.currentTimeMillis();

            Cube c = new Cube(olapServerURL);
            MDXQProcessor mdxQ = new MDXQProcessor();

            List<List<HashMap<Integer, TreeNode>>> allLeaves = getLeavesPerAxis(keyValPairsForDimension, allAxisDetails.get(0));
            List<List<List<Integer>>> newAxisDetails = generateNewAxisDetails(allLeaves, allAxisDetails);
            List<List<String>> cellOrdinalCombinations = new ArrayList<>();
            int queryCount = allAxisDetails.size();
            for (int i = 0; i < queryCount; i++) {
                cellOrdinalCombinations.add(mdxQ.GenerateCellOrdinal(newAxisDetails.get(i)));
            }

            mdxQ.GenerateQueryString(allAxisDetails, selectedMeasures, measureMap,
                    keyValPairsForDimension, true,false);
            if(CacheProcess.inflatedQueries!=null && CacheProcess.inflatedQueries.size()>0) {//Issue
                List<List<Long>> cubeInflated = c.GetCubeData(CacheProcess.inflatedQueries.get(CacheProcess.inflatedQueries.size()-1));

                mdxQ.CheckAndPopulateCache(cellOrdinalCombinations.get(0), this.parentEntiresPerAxis, cubeInflated);// assuming only 1 query entry
            }
        }
        catch(Exception e) {
            String ex = e.getMessage();
        }
    }

    @Override
    protected String doInBackground(Void... params) {
        this.run();

        return "Success";
    }
}
