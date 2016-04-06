package mobile.parallelolapprocessing.Async;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import DataRetrieval.Cube;
import DataStructure.TreeNode;
import MDXQueryProcessor.MDXQProcessor;

import mobile.parallelolapprocessing.CacheProcess;
import mobile.parallelolapprocessing.MainActivity;

/**
 * Created by jayma on 2/28/2016.
 */
public class CacheProcessUpto1Level extends AsyncTask<Void,Void,String> { //comment this when use 2 thread or 3 thread test//extends AsyncTask<Void,Void,String> {
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
        if(CacheProcess.inflatedQueries == null)
        {
            CacheProcess.inflatedQueries = new ArrayList<>();
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
    public void run() {
        try {

            Cube c = new Cube(olapServerURL);
            MDXQProcessor mdxQ = new MDXQProcessor();
            CacheProcess cpObj = new CacheProcess();
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
                if(!CacheProcessUpto1Level.inflatedQueries.contains(queryListForSiblings.get(0)) && !CacheProcess.inflatedQueries.contains(queryListForSiblings.get(0))) {
                   CacheProcessUpto1Level.inflatedQueries.add(queryListForSiblings.get(0));
                   List<List<Long>> cubeInflated = c.GetCubeData(CacheProcessUpto1Level.inflatedQueries.get(CacheProcessUpto1Level.inflatedQueries.size()-1));// since last entry is the current one
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
                        /*// the following section deals with situations like: Education. All Customers.  We know that the server will send values for all under 'All custmers' sections
                        // so if we go to its parent that is Education, its children will be 'All customers' which must be replaced by all those children under it.
                        // Education.Children--> All customers but we need Bachelors, Hgh School  Graduates etc
                        List<TreeNode> children = node.getChildren();
                        if(children!=null && children.size()>0 && children.size()<=1)
                        {
                            String childName = children.get(0).getReference().toString();
                            if(!childName.contains("All")){
                                node = node.getParent();
                            }
                            else{
                                node = children.get(0);

                            }
                        }
                        else{*/
                            node = node.getParent();
                       // }

                        int nodeNo = node.getNodeCounter();
                        // if same parent has already processed... we do not need that (avoiding 198#198#202#202 case -> 198#202 is right)

                       // if (!uniqueDimensions.contains(nodeNo)) {
                         //   uniqueDimensions.add(nodeNo);
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


    @Override
    protected String doInBackground(Void... params) {

        //start = System.currentTimeMillis();
       // List<Long> timing = new ArrayList<>();
        //timing.add(start);
        Log.d("Inflated Query 2:", "Time taken to start job " + String.valueOf( System.currentTimeMillis()));
        this.run();
        long end = System.currentTimeMillis();
        Log.d("Inflated Query 2:", "Time taken to finish job " + String.valueOf(end));
        //timing.add(end);
       //MainActivity.ThreadProcesshingDetails.put("Inflated_Query_2_Async",timing);

        return "Success";
    }

}
