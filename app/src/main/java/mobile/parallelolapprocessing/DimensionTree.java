package mobile.parallelolapprocessing;

/**
 * Created by KheyaliMitra on 3/25/2015.
 */

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import DataRetrieval.Dimension;
import MDXQueryProcessor.MDXQProcessor;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.Async.CacheProcessUpto1Level;
import mobile.parallelolapprocessing.Async.Call.DimensionHierarchy;
import mobile.parallelolapprocessing.Async.Call.MDXUserQuery;
import mobile.parallelolapprocessing.Async.Call.RootDimension;
import mobile.parallelolapprocessing.Async.IconTreeItem;
import mobile.parallelolapprocessing.Async.ParameterWrapper.MDXUserQueryInput;
import mobile.parallelolapprocessing.UI.DimensionMeasureGoogleHTMLTable;
import mobile.parallelolapprocessing.UI.IDimensionMeasureDisplay;

public class DimensionTree extends Fragment{

    private TextView dimNode;
    private TextView mesNode;
    private  AndroidTreeView mView;
    private AndroidTreeView tView;
    private int listMenuPos_Dimen = 0;
    private int listMenuPos_Mes = 0;
    private List<String> queryList ;
    public static DataStructure.TreeNode HierarchyNode;
    private ArrayList<String> SelectedDimensions;
    private ArrayList<String> SelectedMeasures;

    public static List<Integer> UserSelectedMeasures;
    public static List<String> UserSelectedDimensionCombinations;

    public static long startTimer=0;
    public static long endTimer =0;
    public  static  long timeTaken =0;
    private class SimpleArrayAdapter extends ArrayAdapter<String> {
        public SimpleArrayAdapter(Context context, List<String> objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
        }

        @Override
        public boolean isEnabled(int position) {
            if(position == listMenuPos_Dimen || position == listMenuPos_Mes) {
                return false;
            }
            return true;
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        SelectedDimensions =  new ArrayList<String>();
        SelectedMeasures = new ArrayList<String>();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {

        MainActivity main = new MainActivity();
        //Start Service Thread
        //Get All layout from treeview.xml
        View rootView = inflater.inflate(R.layout.treeview, null, false);
        final ViewGroup containerView = (ViewGroup) rootView.findViewById(R.id.DimensionListView);
        dimNode = (TextView) rootView.findViewById(R.id.DimensionTreeNodeNode);
        mesNode = (TextView) rootView.findViewById(R.id.MeasureListNode);
        final ViewGroup measureContainer = (ViewGroup) rootView.findViewById(R.id.MeasureListView);
        final Button analyzeBtn = (Button)rootView.findViewById(R.id.AnalyzeButton);
        final Button execBtn = (Button)rootView.findViewById(R.id.executeButton);
        final Button backBtn = (Button)rootView.findViewById(R.id.backButton);
        final ListView selectedQuery = (ListView)rootView.findViewById(R.id.queryView);
        final TextView finalSelection =(TextView)rootView.findViewById(R.id.finalSelections);
        final EditText axis = (EditText) rootView.findViewById(R.id.editTextAxis);
        final EditText dimesionPerAxis =(EditText) rootView.findViewById(R.id.editTextDimension);
        execBtn.setVisibility(View.INVISIBLE);
        selectedQuery.setVisibility(View.INVISIBLE);
        finalSelection.setVisibility(View.INVISIBLE);
        backBtn.setVisibility(View.INVISIBLE);
        Runtime runtime = Runtime.getRuntime();
//        long maxMemory=runtime.maxMemory();
//        long VMmemory  = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//
//        long VMTotalSize = Runtime.getRuntime().totalMemory();
//        Log.d("Heap Size","VM Heap Size Limit  "+String.valueOf(maxMemory));
//        Log.d("Heap Size","Allocated VM Memory  "+String.valueOf(VMmemory));
//        Log.d("Heap Size","VM Heap Size "+String.valueOf(VMTotalSize));

        //axis.setVisibility(View.INVISIBLE);
        //dimesionPerAxis.setVisibility(View.INVISIBLE);
        try {

            View.OnClickListener buttonListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    _captureUserSelectionForDimensionMeasures(containerView, measureContainer, execBtn, backBtn, selectedQuery, finalSelection,axis,dimesionPerAxis);

                }

            };
            analyzeBtn.setOnClickListener(buttonListener);
            // get dimension and measures from the server
            main.getDimensionAndMeasures();
            //Populate Dimension List view
            TreeNode root = main.PopulateTreeHierarchy();
            _displayDimensionAndMeasureTreeView(savedInstanceState, main, containerView, measureContainer, root);

            // button to execute user's final selection as MDX query and fetch data either from cache or from server
            View.OnClickListener executeListener = new View.OnClickListener()
            {
                @Override
                public void onClick(View v) {
                    _resetStaticVariablesRelatedToQuery();
                    String totalAxis = axis.getText().toString();
                    String totalDimensionPerAxis = dimesionPerAxis.getText().toString();
                    // process MXD query here
                    _processUserMDXquerySelection(selectedQuery,totalAxis,totalDimensionPerAxis);




                }

            };
            execBtn.setOnClickListener(executeListener);

            // controls actions for back button so that user can reselect and regenerate queries
            View.OnClickListener backButtonListener =new View.OnClickListener()
            {
                @Override
                public void onClick(View v) {
                    _manageFramControlVisibilityFromBackButton(containerView, measureContainer, execBtn, selectedQuery, finalSelection);
                    // flush  previous selection
                    SelectedDimensions =  new ArrayList<>();
                    SelectedMeasures =  new ArrayList<>();
                    _resetStaticVariablesRelatedToQuery();

                }
            };
            backBtn.setOnClickListener(backButtonListener);

        } catch (Exception e) {
            Toast.makeText(MainActivity.MainContext, "Error in selection. Please retry.", Toast.LENGTH_LONG).show();
        }
        return rootView;
    }

    private void _resetStaticVariablesRelatedToQuery() {
        MDXUserQuery.allAxisDetails =  new ArrayList<>();
        MDXUserQuery.selectedMeasures =  new ArrayList<>();
        MDXUserQuery.measureMap =  new HashMap<>();
        MDXUserQuery.keyValPairsForDimension =  new HashMap<>();
        MDXUserQuery.cellOrdinalCombinations =  new ArrayList<>();
        QueryProcessor.resultSet = new HashMap<>();
        QueryProcessor.hitcount = 0;
        UserSelectedDimensionCombinations = new ArrayList<>();
        UserSelectedMeasures  =  new ArrayList<>();
        endTimer =0;
        startTimer=0;
        timeTaken=0;

    }

    private void _captureUserSelectionForDimensionMeasures(ViewGroup containerView, ViewGroup measureContainer, Button execBtn, Button backBtn, ListView selectedQuery, TextView finalSelection, EditText axis,EditText dimesionPerAxis) {

        _setVisibilitySettings(containerView, measureContainer, execBtn, backBtn, selectedQuery, finalSelection, axis, dimesionPerAxis);
        final LinkedHashMap<String, String> listItems = new LinkedHashMap<>();

        listItems.put("First","Selected Dimensions are given below:");

        for( int i=0;i<SelectedDimensions.size();i++) {
            listItems.put(SelectedDimensions.get(i), SelectedDimensions.get(i));
        }

        listItems.put("Second", "Selected Measures are given below:");

        for( int i=0;i<SelectedMeasures.size();i++)
            listItems.put(SelectedMeasures.get(i), SelectedMeasures.get(i));
        List<String> list = new ArrayList(listItems.values());
        listMenuPos_Mes = list.lastIndexOf("Selected Measures are given below:");
        listMenuPos_Dimen = list.lastIndexOf("Selected Dimensions are given below:");
        queryList = list;
        _setAdapterOnClickListener(listItems, list, selectedQuery);
    }

    private void _manageFramControlVisibilityFromBackButton(ViewGroup containerView, ViewGroup measureContainer, Button execBtn, ListView selectedQuery, TextView finalSelection) {
        containerView.setVisibility(View.VISIBLE);
        measureContainer.setVisibility(View.VISIBLE);
        dimNode.setVisibility(View.VISIBLE);
        mesNode.setVisibility(View.VISIBLE);
        execBtn.setVisibility(View.INVISIBLE);
        selectedQuery.setVisibility(View.INVISIBLE);
        finalSelection.setVisibility(View.INVISIBLE);
    }

    private void _processUserMDXquerySelection(ListView selectedQuery, String totalAxis, String totaldimensionPerAxis) {
        int measure_pos = queryList.lastIndexOf("Selected Measures are given below:");
        List<String> measures = new ArrayList<>(queryList.subList(measure_pos+1,queryList.size()));
        int dimen_pos = queryList.lastIndexOf("Selected Dimensions are given below:");
        List<String> dimensions = new ArrayList<>(queryList.subList(dimen_pos+1,measure_pos));
         // so far hard coded

        String[] result = totaldimensionPerAxis.split(",");
        int [] entryPerDimension =new int[result.length] ;
        int i=0;
        for (String str : result)
            entryPerDimension[i++] = Integer.parseInt(str);

        DataRetrieval.Measures measuresObj = new DataRetrieval.Measures(QueryProcessor.olapServiceURL);//pass url
        MDXUserQueryInput mdxInputObj = new MDXUserQueryInput(entryPerDimension, MainActivity.DimensionTreeNode,dimensions,
                measuresObj,MainActivity.MeasuresList,measures) ;

        MDXUserQuery MDXObj = new MDXUserQuery(mdxInputObj);
        // decide if caching is needed or not.
        //MDXObj.compareWithPreviousQueryDimensions(entryPerDimension,dimensions);
        this.startTimer =0;
        this.endTimer =0;
        try {
            MDXObj.start();
            //MDXObj.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            // start asynchronous thread
            //startAsyncThreads();
             while (!MDXUserQuery.isComplete) {
                Thread.sleep(1);
            }
            this.endTimer = System.currentTimeMillis();

            timeTaken = this.endTimer - this.startTimer;
            _populateListView(selectedQuery, this.endTimer - this.startTimer);





        }
        catch (Exception e){

        }
    }


//    public void startAsyncThreads(){
//        try {
//            // start parallel thread to fetch inflated data for leaf levels
//            CacheProcess cache = new CacheProcess(MDXUserQuery.allAxisDetails, MDXUserQuery.selectedMeasures, MDXUserQuery.measureMap, MDXUserQuery.keyValPairsForDimension,
//                    MDXUserQuery.cellOrdinalCombinations, QueryProcessor.olapServiceURL);
//
//            cache.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//
//            // start another thread to fetch siblings data
//            CacheProcessUpto1Level cacheParentLevelObj = new CacheProcessUpto1Level(MDXUserQuery.allAxisDetails, MDXUserQuery.selectedMeasures, MDXUserQuery.measureMap, MDXUserQuery.keyValPairsForDimension,
//                    MDXUserQuery.cellOrdinalCombinations, QueryProcessor.olapServiceURL);
//
//            cacheParentLevelObj.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//
//        }
//        catch(Exception e)
//        {
//            String s = e.getMessage();
//        }
//    }


    private void _populateListView(ListView selectedQuery, long timeTaken) {
        // if result set has value, display it
        if (QueryProcessor.resultSet.size()>0){
            //<Long> results = new ArrayList<Long>(QueryProcessor.resultSet.values());
            List<String> newList = new ArrayList<String>();
            //for (Long myInt : results) {
             //   newList.add(String.valueOf(myInt));
           // }
            newList.add("total Time taken (ms): "+String.valueOf(timeTaken));
            SimpleArrayAdapter a = new SimpleArrayAdapter(MainActivity.MainContext,newList);
            //Sets Adapter
            selectedQuery.setAdapter(a);

        }
        try {
            Intent intent = new Intent(MainActivity.MainContext, GoogleDisplayLogic.class);
            DimensionTree.this.startActivity(intent);
        } catch (Exception e) {
            String s = e.getMessage();
        }



        this.startTimer =0;
        this.endTimer =0;

    }

    private void _setVisibilitySettings(ViewGroup containerView, ViewGroup measureContainer, Button execBtn,Button backBtn, ListView selectedQuery, TextView finalSelection,EditText axis, EditText dimensionPerAxis) {
        containerView.setVisibility(View.INVISIBLE);
        measureContainer.setVisibility(View.INVISIBLE);
        dimNode.setVisibility(View.INVISIBLE);
        mesNode.setVisibility(View.INVISIBLE);
        execBtn.setVisibility(View.VISIBLE);
        backBtn.setVisibility(View.VISIBLE);
        selectedQuery.setVisibility(View.VISIBLE);
        finalSelection.setVisibility(View.VISIBLE);
        axis.setVisibility(View.VISIBLE);
        dimensionPerAxis.setVisibility(View.VISIBLE);
    }

    private void _setAdapterOnClickListener(final LinkedHashMap<String, String> listItems, List<String> list, final ListView selectedQuery) {
        SimpleArrayAdapter adapter = new SimpleArrayAdapter(MainActivity.MainContext, list);
        //Sets Adapter
        selectedQuery.setAdapter(adapter);

        //Onclick of List view
        selectedQuery.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                                                 @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               _listViewOnlickEventTask(position, selectedQuery, listItems);
            }

        }
        );
    }

    private void _displayDimensionAndMeasureTreeView(Bundle savedInstanceState, MainActivity main, ViewGroup containerView, ViewGroup measureContainer, TreeNode root) {
        IconTreeItem nodeItem = new IconTreeItem(0, "Dimension/Hierarchy:");

        root.setViewHolder(new IconTreeItemHolder(main.MainContext));
        root.setClickListener(nodeClickListener);

        tView = new AndroidTreeView(getActivity(), root);
        tView.setDefaultAnimation(true);
        tView.setDefaultContainerStyle(R.style.TreeNodeStyleCustom);
        tView.setDefaultNodeClickListener(nodeClickListener);
        containerView.addView(tView.getView());
        tView.collapseAll();

        if (savedInstanceState != null) {
            String state = savedInstanceState.getString("tState");
            if (!TextUtils.isEmpty(state)) {
                tView.restoreState(state);
            }
        }

        TreeNode measuresRoot = main.PopulateMeasures();
        //IconTreeItem mesItem =  new IconTreeItem(1, "Measures");
        measuresRoot.setViewHolder(new IconTreeItemHolder(main.MainContext));
        measuresRoot.setClickListener(nodeClickListener);
        mView = new AndroidTreeView(getActivity(), measuresRoot);
        mView.setDefaultAnimation(true);
        mView.setDefaultContainerStyle(R.style.TreeNodeStyleCustom);
        mView.setDefaultNodeClickListener(nodeClickListener);
        measureContainer.addView(mView.getView());
        if (savedInstanceState != null) {
            String state = savedInstanceState.getString("tState");
            if (!TextUtils.isEmpty(state)) {
                mView.restoreState(state);
            }
        }
        mView.collapseAll();
    }

    private void _listViewOnlickEventTask(int position, ListView selectedQuery, LinkedHashMap<String, String> listItems) {
        try {
            queryList=null;
            int i= position;
            String itemSelected = (String) (selectedQuery.getItemAtPosition(position));
            listItems.remove(itemSelected);
            List<String> l = new ArrayList(listItems.values());
            queryList=l;
            // set both the items non clickable using adapter
            listMenuPos_Mes = l.lastIndexOf("Selected Measures are given below:");
            listMenuPos_Dimen = l.lastIndexOf("Selected Dimensions are given below:");
            SimpleArrayAdapter a = new SimpleArrayAdapter(MainActivity.MainContext, l);
            //Sets Adapter
            selectedQuery.setAdapter(a);
            Toast.makeText(MainActivity.MainContext, itemSelected + "is removed.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.MainContext, "Some Error occurred. Please retry", Toast.LENGTH_LONG).show();

        }
    }

    private TreeNode.TreeNodeClickListener nodeClickListener = new TreeNode.TreeNodeClickListener() {
        @Override
        public void onClick(TreeNode node, Object value) {
            HierarchyNode =null;
            try {
                String child = (String) node.getValue();
                String root ="";
                if(node.getRoot()!=null) {
                    root =(String) node.getRoot().getValue();
                }
                List<TreeNode>parentList = _getParentListsFromDimensionSelection(node);
                if(root.equals("Dimension")) {
                    _populateDimensionLeaves(node, child, parentList);

                }
                else
                {
                    _storeMeasureClicks(child, root);
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.MainContext, "Can not retrieve data from service.", Toast.LENGTH_LONG).show();

            }
        }
    };
    private List<TreeNode> _getParentListsFromDimensionSelection(TreeNode node){
        List<TreeNode> parents= new ArrayList<>();
        while(node.getParent()!=null){
            TreeNode parentNode = node.getParent();
            parents.add(parentNode);
            node = parentNode;
        }
        return parents;
    }
    private void _storeMeasureClicks(String child, String root) {
        if(root.equals("Measures"))
        {
            mesNode.setText("Selected: " + child);
            if(!child.equals("Measures:") && !child.equals("Measures") )
                SelectedMeasures.add(child);
        }
    }

    private void _populateDimensionLeaves(TreeNode node, String child, List<TreeNode>parentList) throws InterruptedException {
        String param="";
        List<String> parentNames = new ArrayList<>();
        for (int i=parentList.size()-1;i>=0;i--){
            String val = (String)parentList.get(i).getValue();
            param +=val+".";
            if(!val.equals("Dimension/Hierarchy:")&& !val.equals("Dimension")) {
                param +=val+".";
                parentNames.add((val));
            }
        }
        if(param.contains(".")) {
            param = param.substring(0, param.lastIndexOf("."));
        }
        // if reparation is to be avoided for multiple service call use (level==3 && node.getChildren().size()<1))
        param = _getSelectedDimensionNodeEntry(child, parentNames);
        dimNode.setText("Selected: "+param);
        _populateNodeHierarchy(node, param);

    }

    private String _getSelectedDimensionNodeEntry(String child, List<String> parentNames) {
        String param="[Dimension].[";
        for(int i=0;i<parentNames.size();i++){
            param+=parentNames.get(i)+"].[";
        }
        param +=child+"]";
        SelectedDimensions.add(param);
        return  param;
    }

    private void _populateNodeHierarchy(TreeNode node, String param) throws InterruptedException {
        List<TreeNode> children = node.getChildren();
        if(children.size()==0 && node.getLevel()==3) {
            MainActivity mainObj = new MainActivity();
            mainObj.fetchHierarchyRecordsFromServer(param);
            //_fetchHierarchyRecordsFromServer(param);
            List<DataStructure.TreeNode> AdventureWorksHierarchyDetails = HierarchyNode.getChildren();
            try {
                _iterateThroughLeaves(node, AdventureWorksHierarchyDetails);
            }
            catch (Exception e)
            {
                Toast.makeText(MainActivity.MainContext, "Some Error occurred. Please retry", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void _iterateThroughLeaves(TreeNode node, List<DataStructure.TreeNode> adventureWorksHierarchyDetails) {
        Iterator itr = adventureWorksHierarchyDetails.iterator();
        DataStructure.TreeNode t = (DataStructure.TreeNode) itr.next();
        TreeNode parentNode =new TreeNode(t.getReference());
        List<DataStructure.TreeNode> leaves = t.getChildren();
        Iterator leafItr = leaves.iterator();
        int maxdisplay=50;// just showing upto 50 leaves: easier to fit in to small screen
        while (leafItr.hasNext() && maxdisplay>=0) {
            DataStructure.TreeNode lNode = (DataStructure.TreeNode) leafItr.next();
            // iterate through child levels (as it is coming from json 0 till 5)

            TreeNode childTreeNode = new TreeNode(lNode.getReference());
            List<DataStructure.TreeNode> children = lNode.getChildren();
            if(children.size()>0){
                childTreeNode = _iterateThroughchildLevels(children,childTreeNode);
            }
            parentNode.addChild(childTreeNode);
            maxdisplay--;
        }
        node.addChild(parentNode);
    }

    private TreeNode _iterateThroughchildLevels(List<DataStructure.TreeNode> children, TreeNode childTreeNode) {

        TreeNode newChildTreeNode=childTreeNode;
        Iterator leafItr = children.iterator();
        int maxdisplay=50;// just showing upto 50 leaves: easier to fit in to small screen
        while (leafItr.hasNext() && maxdisplay>=0) {
            DataStructure.TreeNode lNode = (DataStructure.TreeNode) leafItr.next();
            // iterate through upto 5 levels (as it is coming from json 0 till 5)

            TreeNode childNode = new TreeNode(lNode.getReference());
            List<DataStructure.TreeNode> gchildren = lNode.getChildren();
            if(gchildren.size()>0){
                childNode = _iterateThroughchildLevels(gchildren,childNode);
            }
            newChildTreeNode.addChild(childNode);
            maxdisplay--;
        }

            return newChildTreeNode;
    }

public void executeSerially(){
    CacheProcess cache = new CacheProcess(MDXUserQuery.allAxisDetails, MDXUserQuery.selectedMeasures, MDXUserQuery.measureMap, MDXUserQuery.keyValPairsForDimension,
            MDXUserQuery.cellOrdinalCombinations, QueryProcessor.olapServiceURL);

    cache.run();

    // start another thread to fetch siblings data
    CacheProcessUpto1Level cacheParentLevelObj = new CacheProcessUpto1Level(MDXUserQuery.allAxisDetails, MDXUserQuery.selectedMeasures, MDXUserQuery.measureMap, MDXUserQuery.keyValPairsForDimension,
            MDXUserQuery.cellOrdinalCombinations, QueryProcessor.olapServiceURL);

    cacheParentLevelObj.run();
}
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.expandAll:
                tView.expandAll();
                break;

            case R.id.collapseAll:
                tView.collapseAll();
                break;
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tState", tView.getSaveState());
    }
}
