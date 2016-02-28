package DataRetrieval;
import android.text.TextUtils;

import DataStructure.TreeNode;
import mobile.parallelolapprocessing.DimensionTree;

import org.json.simple.parser.JSONParser;
import java.util.*;


/**
 * Get all dimesion in JSON object format
 * Assumption is JSOn string will be wrapped within an XML root string
 */
public class Dimension {
    private String _soap_Address;// = "http://192.168.0.207/OLAPService/AdventureWorks.asmx";//"http://webolap.cmpt.sfu.ca/ElaWebService/Service.asmx";
    private static int _nodeCounter=1;
    private String _populateLeafNode_ParameterName="Hierarchy";
    private  String _populateLeafNode_OperationName="MetaData2";
    private String _rootOperationName = "Dimen";
    /**
     * this a mapping dictionary for integer key and string hierarchy name. this is going to be used for future reference of any tree node selected by client
     */
    private HashMap<Integer,String> _treeHiearchyMap= new HashMap<>();
    public static HashMap<Integer,TreeNode> dimensionHiearchyMap= new HashMap<>();

    /***
     * Default Constructor which sets own Soap Address and dimension operation string
     * Default Soap Address: http://192.168.0.207/OLAPService/AdventureWorks.asmx
     * Default Operation Name:Dimen
     */
    public Dimension() {
    }

    /**
     * Constructor which sets own dimension operation string
     * Default Soap Address: http://192.168.0.207/OLAPService/AdventureWorks.asmx
     *
     * @param URL
     */
    public Dimension(String URL) {
        this._soap_Address = URL;
    }


    /***
     * Get Root Dimension form database after query to the server
     * @return
     * @throws Exception
     */
    public TreeNode GetRootDimension(String OperationName) throws Exception {
        String dimensionString = this._getJSONString(OperationName);

        //create JSON parser object
        JSONParser parser = new JSONParser();
        //parse dimension string returned SOAP
        Object jsonObject = parser.parse(dimensionString);
        return _generateTreeFromJSONObject(jsonObject, "Dimension");
    }

    /***
     * Get Root Dimension form database after query to the server
     * Assumption of operation name is "Dimen"
     * @return
     * @throws Exception
     */
    public TreeNode GetRootDimension() throws Exception {
        return this.GetRootDimension(this._rootOperationName);
    }

    /**
     * Populates leaf node of a give parent in a tree
     * @param RootTree
     * @param ParamName
     * @param ParamVal
     * @param OperationName
     * @return
     * @throws Exception
     */
    public void PopulateLeafNode(TreeNode RootTree, String ParamName,String ParamVal,String OperationName) throws Exception{
        String jsonString = this._getJSONString(OperationName,ParamName,ParamVal);

        //create JSON parser object
        JSONParser parser = new JSONParser();
        //parse dimension string returned SOAP
        Object jsonObject = parser.parse(jsonString);
        this._setLeafNode(RootTree, jsonObject, ParamVal);
    }
    public void PopulateLeafNode(TreeNode RootTree, String ParamVal) throws Exception {
        this.PopulateLeafNode(RootTree,this._populateLeafNode_ParameterName,ParamVal,this._populateLeafNode_OperationName);
    }

        /**
         * DFS path traversal of dimension tree
         * @param tree
         */
    public TreeNode FindNodeInTree(TreeNode tree, String name) {
        // if name is ull return null
        if(name == "") {
            return null;
        }

        String[] hierarchyArray = name.split("\\.");
        String firstNode=hierarchyArray[0];
        //if whole name matches the last part of it (after .)  then it means it is a single word and its for only root level node like
        // [dimension] or [account] then just return the node.
        if(name.equals(hierarchyArray[hierarchyArray.length-1])) {
            return tree;
        }else{
            hierarchyArray = name.split("\\.");
            //avoid 1st part ([dimension] word)
            firstNode=hierarchyArray[1];
        }

        // traverse children
        List<TreeNode>children = tree.getChildren();
        if (children.size() == 0) {
            return null;
        } else {
            for (int i = 0; i < children.size(); i++) {
                TreeNode child = children.get(i);
                if(child!=null) {
                    // check depth wise every node to match with this string
                    if (firstNode.equals("[" + child.getReference().toString() + "]")) {
                        hierarchyArray = Arrays.copyOfRange(hierarchyArray, 1, hierarchyArray.length);
                        return FindNodeInTree(child, TextUtils.join(".", hierarchyArray));
                    }
                }
            }

        }
        // if nothing matches return null
        return null;
    }

    /**
     * List all the tree nodes for corresponding list
     * @param tree
     * @param hardcodedInputDim
     * @return
     */
    public List<TreeNode> GetTreeListFromDimensionList(TreeNode tree, List<String> hardcodedInputDim) {
        List<TreeNode> selectedDimen = new ArrayList<TreeNode>();

        for(int i=0;i<hardcodedInputDim.size();i++)
        {
            String dimen = hardcodedInputDim.get(i);
            TreeNode node = FindNodeInTree(tree,dimen);
            if(node!=null) {
                selectedDimen.add(node);
            }
        }
        return selectedDimen;
    }

    /**
     * Generates tree structure based on parsed JSON Object
     * @param jsonObject
     * @param dimension
     * @return
     */
    private TreeNode _generateTreeFromJSONObject(Object jsonObject, String dimension) {
        //populate dimension tree using parsed JSON Object
        Map<String,Object> treeDetails = (Map<String,Object>)jsonObject;

        //Root Node of Dimension Tree
        TreeNode rootNode = new TreeNode(dimension);

        //Iterate through JSON Object to generate parent and its child nodes and then finally attach to its root node.
        Iterator parentIterator = treeDetails.entrySet().iterator();
        while (parentIterator.hasNext()) {

            //Takes key value pair using iterator
            Map.Entry pair = (Map.Entry)parentIterator.next();
            //Get value for child node
            Map<String,Object> child = (Map<String,Object>)pair.getValue();

            //Gets inner child of the child
            Iterator childIterator = child.entrySet().iterator();

            //Initializes tree node for grand children with child name and its hierarchy name
            TreeNode parentNode =new TreeNode(pair.getKey().toString(),rootNode.getHierarchyName()+".["+pair.getKey().toString()+"]");
            while(childIterator.hasNext())
            {
                Map.Entry grandChild = (Map.Entry)childIterator.next();
                // these following checking is for special cases like Date.Date.calender year
                // this will check if this [Date.calender year] part has more than 1 entry after splitting  based on '.'
                // if so it will check if 0the entry matches with parent name : if so, take rest part  else take whole value
                String grandchildName=grandChild.getKey().toString();
                String []splitString = grandchildName.split("\\.");

                if(splitString.length>1 && splitString[0].equals(pair.getKey().toString()))
                {
                    grandchildName = grandchildName.substring(grandchildName.indexOf(".")+1);
                }
                //sets grand child node name
                TreeNode node = new TreeNode(grandchildName);
                //add each child node to its immediate parent node
                this._nodeCounter = parentNode.addChildNode(node,this._nodeCounter);
                // add node name entry to map table
                this._treeHiearchyMap.put(this._nodeCounter,node.getHierarchyName());
                this.dimensionHiearchyMap.put(this._nodeCounter,node);
            }
            // adds every parent node(including its children) to the root node to generate the tree
            this._nodeCounter = rootNode.addChildNode(parentNode,this._nodeCounter );

            //these two flush each iterator object and points next one in every turn, used better to avoid exception related to modification inside Map entry
            childIterator.remove();
            parentIterator.remove();
        }
        return rootNode;
    }

    /**
     * get JSON string from SOAP model from web service
     * @param OperationName
     * @param ParameterName
     * @param ParameterValue
     * @return
     * @throws Exception
     */
    private String _getJSONString(String OperationName,String ParameterName, String ParameterValue )throws Exception{
        //Creates soap retrieval object
        Soap soapRetrieval = new Soap();

        //set parameters to make it usable for both root node (parameters=null ) and child nodes (key,value pairs)
        HashMap<String,Object> parameters =null;
        if(ParameterName!=null){
            String[] parameterValHier = ParameterValue.split("\\.");
            for(int i=1;i<parameterValHier.length;i++){
                if(parameterValHier[i].startsWith("[") && parameterValHier[i].endsWith("]")){
                    parameterValHier[i] = parameterValHier[i].substring(1,parameterValHier[i].length()-1);
                }
            }
            ParameterValue = TextUtils.join(".",Arrays.copyOfRange(parameterValHier,1,parameterValHier.length));

            //Creates parameter
            parameters = new HashMap<>();
            parameters.put(ParameterName,ParameterValue);
        }
        //call getJSON String for root and child tree generation
        String dimensionString = soapRetrieval.GetJSONString(this._soap_Address, OperationName, parameters);
        return dimensionString;
    }

    /**
     * Get JSON String for root node of Dimension tree
     * @param OperationName
     * @return
     * @throws Exception
     */
    private String _getJSONString(String OperationName )throws Exception{
        return this._getJSONString(OperationName,null,null );
    }

    /**
     * Adds leaf nodes to existing tree
     * @param rootTree
     * @param jsonObject
     * @param hierarchy
     * @return
     */
    private TreeNode _setLeafNode(TreeNode rootTree, Object jsonObject, String hierarchy) {
        TreeNode grandChildNode = this._getLeafTreeNodeFromroot(rootTree,hierarchy);
        DimensionTree.HierarchyNode = grandChildNode;
        TreeNode gGrandChildTree = this._setGGrandTree(jsonObject,grandChildNode);
        this._nodeCounter  = grandChildNode.addChildNode(gGrandChildTree,this._nodeCounter );
        this._treeHiearchyMap.put(this._nodeCounter,gGrandChildTree.getHierarchyName());
        this.dimensionHiearchyMap.put(this._nodeCounter,gGrandChildTree);
        return rootTree;
    }

    /**
     * Adds great grand child node to its immediate parent
     * @param jsonObject
     * @param relativeRootNode
     * @return
     */
    private TreeNode _setGGrandTree(Object jsonObject,TreeNode relativeRootNode) {
        //populate dimension tree using parsed JSON Object
        List<Object> treeDetails = (List<Object>)jsonObject;

        //Root Node of Dimension Tree
        TreeNode rootNode = null;
        TreeNode childNode1 = null;
        TreeNode childNode2 = null;
        TreeNode childNode3 = null;
        TreeNode childNode4 = null;

        //Iterate through JSON Object to generate parent and its child nodes and then finally attach to its root node.
        Iterator parentIterator = treeDetails.iterator();
        while (parentIterator.hasNext()) {

            //Takes key value pair using iterator
            Object pair = parentIterator.next();
            String nodeName = pair.toString();
            String [] nodeNameHierarchy = nodeName.split("\\|");
            //check for length: to avoid blank entries like "1||"-> after split it will give ' 1 ' so no entry name
            // we are not considering any null or blank entry here
            if(nodeNameHierarchy.length>2) {
                String currentNodeName = nodeNameHierarchy[1];
                // level 0
                if (this._getGGrandRootLevel(nodeName) == 0) {
                    // sets hierarchy node names for this root node
                    rootNode = new TreeNode(currentNodeName, (relativeRootNode!=null? relativeRootNode.getHierarchyName():"") + ".[" + currentNodeName + "]");
                    // add full hierarchy name along with hash code
                    this._treeHiearchyMap.put(this._nodeCounter, rootNode.getHierarchyName());
                    this.dimensionHiearchyMap.put(this._nodeCounter,rootNode);
                } else
                { // as seen in JSON object, all child nodes are appearing sequentially 1.>2.>3> 4 for a particular parent
                    // it is sorted, so we do not need to search for whose children is who/
                    // level 1

                    if(this._getGGrandRootLevel(nodeName)==1){
                        childNode1 = new TreeNode(currentNodeName);
                        //get hierarchy name of child node  and add to map table
                        this._nodeCounter = rootNode.addChildNode(childNode1, this._nodeCounter);
                        this._treeHiearchyMap.put(this._nodeCounter, childNode1.getHierarchyName());
                        this.dimensionHiearchyMap.put(this._nodeCounter,childNode1);
                    }
                    else{
                        // level 2
                        if(this._getGGrandRootLevel(nodeName)==2){
                            childNode2 = new TreeNode(currentNodeName);
                            //get hierarchy name of  node  and add to map table
                            this._nodeCounter = childNode1.addChildNode(childNode2, this._nodeCounter);
                            this._treeHiearchyMap.put(this._nodeCounter, childNode2.getHierarchyName());
                            this.dimensionHiearchyMap.put(this._nodeCounter,childNode2);

                        }
                        else {
                            // level 3

                            if(this._getGGrandRootLevel(nodeName)==3){
                                //get hierarchy name of  node  and add to map table
                                childNode3 = new TreeNode(currentNodeName);
                                this._nodeCounter = childNode2.addChildNode(childNode3, this._nodeCounter);
                                this._treeHiearchyMap.put(this._nodeCounter, childNode3.getHierarchyName());
                                this.dimensionHiearchyMap.put(this._nodeCounter,childNode3);

                            }
                            else
                            {
                                // level 4
                                if(this._getGGrandRootLevel(nodeName)==4){
                                    childNode4 = new TreeNode(currentNodeName);
                                    //get hierarchy name of  node  and add to map table
                                    this._nodeCounter = childNode3.addChildNode(childNode4, this._nodeCounter);
                                    this._treeHiearchyMap.put(this._nodeCounter, childNode4.getHierarchyName());
                                    this.dimensionHiearchyMap.put(this._nodeCounter,childNode4);

                                }
                            }
                        }
                    }
                }
            }

            //these two flush each iterator object and points next one in every turn, used better to avoid exception related to modification inside Map entry
            parentIterator.remove();
        }

        return rootNode;
    }

    /**
     * Node contains Level|Value[|Value] , we retrieve only level
     * Example 0|All Accounts OR  0|12|12
     * @param leafNodeName
     * @return
     */
    private int _getGGrandRootLevel(String leafNodeName) {
        return Integer.parseInt(leafNodeName.split("\\|")[0]);
    }


    /**
     * Get Left node only two level down
     * @param rootTree
     * @param hierarchy Hierarchy must be two level only
     * @return
     */
    private TreeNode _getLeafTreeNodeFromroot(TreeNode rootTree,String hierarchy){
        return FindNodeInTree(rootTree,hierarchy);
    }
}
