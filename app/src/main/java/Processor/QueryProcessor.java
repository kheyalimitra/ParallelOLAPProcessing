package Processor;

import android.support.annotation.NonNull;

import DataRetrieval.*;
import DataStructure.TreeNode;

import java.util.*;

import MDXQueryProcessor.*;
import mobile.parallelolapprocessing.Async.CacheProcessUpto1Level;
import mobile.parallelolapprocessing.Async.Call.MDXUserQuery;
import mobile.parallelolapprocessing.CacheProcess;
import mobile.parallelolapprocessing.DimensionTree;
import mobile.parallelolapprocessing.GoogleDisplayLogic;
import mobile.parallelolapprocessing.MainActivity;
import mobile.parallelolapprocessing.UI.DimensionMeasureGoogleHTMLTable;


public class QueryProcessor {

    public static final String olapServiceURL = "http://webolap.cmpt.sfu.ca/ElaWebService/Service.asmx";//"http://192.168.0.207/OLAPService/AdventureWorks.asmx";
    public static HashMap<String, HashMap<String, Long>> resultSet = new HashMap<>();
    public static float hitcount = 0;
    public void QueryProcessor() {
    }

    public static TreeNode GetRootDimension() throws Exception {
        //Populate dimension tree up to two level
        Dimension dimensionObj = new Dimension(olapServiceURL);
        return dimensionObj.GetRootDimension();
    }

    public static TreeNode GetHierarchyDimension(TreeNode rootDimensionTree, String selection) throws Exception {
        //Populate dimension tree up to two level
        Dimension dimensionObj = new Dimension(olapServiceURL);
        //User clicked on Account>Account Number
        dimensionObj.PopulateLeafNode(rootDimensionTree, selection);
        return rootDimensionTree;
    }

    public static HashMap<Integer, String> GetMeasures() throws Exception {
        Measures measuresObj = new Measures(olapServiceURL);
        return measuresObj.GetMeasures();
    }


    public boolean GetUserRequestedData(int[] entryPerDimension,
                                        TreeNode rootDimensionTree,
                                        List<String> hardcodedInputDim,
                                        Measures measuresObj,
                                        HashMap<Integer, String> measureMap,
                                        List<String> hardcodedInputMeasures) throws Exception {
        //Initialize data cube axis
        DataCubeAxis dca = new DataCubeAxis();

        try {
            //Integer represent axis and List of TreeNode denotes dimensions on that axis
            HashMap<Integer, List<TreeNode>> dimensionsInAxes = dca.GetTreeNodeListForEachAxis(rootDimensionTree, hardcodedInputDim,
                    entryPerDimension);
            //Measure id to measure string map
            HashMap<Integer, String> selectedMeasureMap = measuresObj.GetHashKeyforSelecteditems(hardcodedInputMeasures, measureMap);


            //generates Key combinations
            MDXQProcessor mdxQueryProcessorObj = new MDXQProcessor();
            List<String> generatedKeys = mdxQueryProcessorObj.GenerateKeyCombination(dimensionsInAxes);

            DimensionTree.UserSelectedDimensionCombinations = new ArrayList<>(generatedKeys);
            List<String> generatedKeyCopy = new ArrayList<>(generatedKeys);
            DimensionTree.UserSelectedMeasures = getCloneOfGivenList(new ArrayList<>(selectedMeasureMap.keySet()));
            Set<Integer> selectedMeasureMapCopy  = new HashSet<>(selectedMeasureMap.keySet());

            // takes care of past dimensions selected and if needed flushing cache
            _recordQueryHistory();
            MDXUserQuery.allAxisDetails = new MDXQProcessor().GetAxisDetails(
                    new ArrayList<>(selectedMeasureMap.keySet()),
                    generatedKeys);
            //Start timer
            DimensionTree.startTimer = System.currentTimeMillis();
            //check from cache
            DimensionKeyCombinationAndMeasures nonCachedSelections = mdxQueryProcessorObj.GetAllDimensionAndMeasuresToFetch(generatedKeyCopy,
                                                                                        selectedMeasureMapCopy);
            List<Integer> nonCachedMeasures = new ArrayList<>(nonCachedSelections.Measures);
            List<String> nonCachedKeys = new ArrayList<>(nonCachedSelections.KeyCombinations);
            // % of hit in cache
            float originalSize = DimensionTree.UserSelectedDimensionCombinations.size()*DimensionTree.UserSelectedMeasures.size();
            float hit = originalSize - (nonCachedKeys.size()*nonCachedMeasures.size());
            hitcount =(float) Math.round((hit / originalSize)*100) /100;
            if (nonCachedKeys.size() > 0) {
                mdxQueryProcessorObj.ProcessUserQuery(nonCachedMeasures, selectedMeasureMap,dimensionsInAxes,
                        nonCachedKeys, false, true);

                    MDXUserQuery.isComplete = true;
                    return true;
            }

        } catch (Exception ex) {

            return false;
        }
        MDXUserQuery.isComplete = true;
        return true;
    }
    private void _recordQueryHistory() {
        // if 0% hit and usr is moving to different direction that previous one flush memory
        if(!_isCurrentSelectionRelatedToPastDimensions())
        {
            if(MainActivity.CachedDataCubes.size()>0) {
                MainActivity.CachedDataCubes.clear();
                MDXQProcessor.lastTenSelectedDimensions.clear();
                CacheProcess.inflatedQueries.clear();
                CacheProcessUpto1Level.inflatedQueries.clear();
                System.gc();
            }
        }
        _addNewEntryinPasDimensionQueryHistory();
    }

    private void _addNewEntryinPasDimensionQueryHistory(){
        for (String dimensions : DimensionTree.UserSelectedDimensionCombinations) {
            String eachDimension[] = dimensions.split("#");
            for (String key : eachDimension) {
                Integer parentKey = _findRootParent(key);
                MDXQProcessor.lastTenSelectedDimensions.add(parentKey);

            }
        }
    }
    private  boolean _isCurrentSelectionRelatedToPastDimensions(){
        boolean isExists =  false;
        for (String dimensions : DimensionTree.UserSelectedDimensionCombinations) {
            String eachDimension[] = dimensions.split("#");
            for (String key : eachDimension) {
                Integer parentKey = _findRootParent(key);
                if(MDXQProcessor.lastTenSelectedDimensions.size()>0) {
                    if (MDXQProcessor.lastTenSelectedDimensions.contains(parentKey)) {
                        isExists = true;
                    }
                }
                else
                {
                   // MDXQProcessor.lastTenSelectedDimensions.add(parentKey);
                    isExists = true;
                }
            }

        }
        return isExists;

    }

    private Integer _findRootParent(String key) {
        DataStructure.TreeNode node = Dimension.dimensionHiearchyMap.get(Integer.parseInt(key));
        Integer parentNodeKey = -1;
        if(node!=null){
            while(node.getParent().getReference() != "Dimension"){
                node = node.getParent();
            }
            parentNodeKey = node.getNodeCounter();
        }
        return  parentNodeKey;
    }

    @NonNull
    private <T> List<T> getCloneOfGivenList(List<T> generatedKeys) {
        List<T> generatedKeysClone =  new ArrayList<>();
        for(T item: generatedKeys){
            generatedKeysClone.add(item);
        }
        return generatedKeysClone;
    }

    private HashMap<Integer, String> _getSelectedMeasureFromMap(HashMap<Integer, String> measureMap, List<Integer> selectedMesureKeyList) {
        HashMap<Integer, String> _measureKeyVal = new HashMap<>();
        for (int i = 0; i < selectedMesureKeyList.size(); i++) {
            int key = selectedMesureKeyList.get(i);
            _measureKeyVal.put(key, measureMap.get(key).toString());
        }
        return _measureKeyVal;
    }
}
