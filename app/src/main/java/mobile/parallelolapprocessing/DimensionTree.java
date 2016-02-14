package mobile.parallelolapprocessing;

/**
 * Created by KheyaliMitra on 3/25/2015.
 */

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import DataCaching.DataCaching;
import mobile.parallelolapprocessing.Async.Call.DimensionHierarchy;
import mobile.parallelolapprocessing.Async.IconTreeItem;

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
        final ListView selectedQuery = (ListView)rootView.findViewById(R.id.queryView);
        final TextView finalSelection =(TextView)rootView.findViewById(R.id.finalSelections);
        execBtn.setVisibility(View.INVISIBLE);
        selectedQuery.setVisibility(View.INVISIBLE);
        finalSelection.setVisibility(View.INVISIBLE);
        try {

            View.OnClickListener buttonListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    _setVisibilitySettings(containerView, measureContainer, execBtn, selectedQuery, finalSelection);
                    final LinkedHashMap<String, String> listItems = new LinkedHashMap<>();

                    listItems.put("First","Selected Dimensions are given below:");

                    for( int i=0;i<SelectedDimensions.size();i++) {
                        listItems.put(SelectedDimensions.get(i), SelectedDimensions.get(i));
                    }

                    listItems.put("Second","Selected Measures are given below:");

                    for( int i=0;i<SelectedMeasures.size();i++)
                        listItems.put(SelectedMeasures.get(i), SelectedMeasures.get(i));
                    List<String> list = new ArrayList(listItems.values());
                    listMenuPos_Mes = list.lastIndexOf("Selected Measures are given below:");
                    listMenuPos_Dimen = list.lastIndexOf("Selected Dimensions are given below:");
                    queryList = list;
                    _setAdapterOnClickListener(listItems, list, selectedQuery);

                }

            };
            analyzeBtn.setOnClickListener(buttonListener);
            // get dimension and measures from the server
            main.getDimensionAndMeasures();
            //Populate Dimension List view
            TreeNode root = main.PopulateTreeHierarchy();
            _displayDimensionAndMeasureTreeView(savedInstanceState, main, containerView, measureContainer, root);
        } catch (Exception e) {
            Toast.makeText(MainActivity.MainContext, "Error in selection. Please retry.", Toast.LENGTH_LONG).show();
        }
        return rootView;
    }

    private void _setVisibilitySettings(ViewGroup containerView, ViewGroup measureContainer, Button execBtn, ListView selectedQuery, TextView finalSelection) {
        containerView.setVisibility(View.INVISIBLE);
        measureContainer.setVisibility(View.INVISIBLE);
        dimNode.setVisibility(View.INVISIBLE);
        mesNode.setVisibility(View.INVISIBLE);
        execBtn.setVisibility(View.VISIBLE);
        selectedQuery.setVisibility(View.VISIBLE);
        finalSelection.setVisibility(View.VISIBLE);
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
                TreeNode n = node.getRoot();
                TreeNode p = node.getParent();
                TreeNode grandP = p.getParent();
                String root = (String)n.getValue();
                String parentNode = (String) p.getValue();
                String grandParentNode="";
                if(grandP !=null)
                    grandParentNode= (String)grandP.getValue();
                String param="";
                if(root.equals("Dimension")) {
                    param="[Dimension].[";
                    dimNode.setText("Selected: "+grandParentNode + "." + parentNode + "." + child);

                    if( !parentNode.equals("Dimension/Hierarchy:")&& !parentNode.equals("Dimension")) {// if reparation is ro be avoided for multiple service call use (level==3 && node.getChildren().size()<1))
                        param+=parentNode+"].["+child+"]";
                        if(grandParentNode.equals("Dimension")) {
                            SelectedDimensions.add(child);
                        }
                        else {
                            if(grandParentNode.equals("Dimension/Hierarchy:"))
                                SelectedDimensions.add(parentNode + "." + child);
                            else
                                SelectedDimensions.add(grandParentNode+"."+parentNode + "." + child);
                        }
                        List<TreeNode> children = node.getChildren();
                        if(children.size()==0 && node.getLevel()==3) {

                            DimensionHierarchy dimenHierarchyObj = new DimensionHierarchy(param);//.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, param);
                            dimenHierarchyObj.join();
                            dimenHierarchyObj.start();
                            // wait for asynchronous call to fetch the records.
                            while(DimensionTree.HierarchyNode==null){
                                Thread.sleep(10);
                            }
                            List<DataStructure.TreeNode> AdventureWorksHierarchyDetails = HierarchyNode.getChildren();
                            try {

                                Iterator itr = AdventureWorksHierarchyDetails.iterator();
                                while (itr.hasNext()) {
                                    DataStructure.TreeNode innerNode = (DataStructure.TreeNode) itr.next();
                                    com.unnamed.b.atv.model.TreeNode parent = new TreeNode((String)innerNode.getReference());
                                    List<TreeNode> leaves = innerNode.getChildren();
                                    if(leaves.size()>0) {
                                        Iterator grandChildrenItr = leaves.iterator();
                                        while (grandChildrenItr.hasNext()) {
                                            DataStructure.TreeNode grandNode = (DataStructure.TreeNode) grandChildrenItr.next();
                                            com.unnamed.b.atv.model.TreeNode leaf = new TreeNode((String) grandNode.getReference());
                                            node.addChild(leaf);

                                        }
                                    }
                                    //node.addChild(parent);
                                }
                            } catch (Exception e)

                            {
                                Toast.makeText(MainActivity.MainContext, "Some Error occurred. Please retry", Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                }
                else
                {
                    if(root.equals("Measure"))
                    {
                        mesNode.setText("Selected: " + child);
                        if(!child.equals("Measures:") && !child.equals("Measure") )
                            SelectedMeasures.add(child);
                    }


                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.MainContext, "Can not retrieve data from service.", Toast.LENGTH_LONG).show();

            }
        }
    };
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
