package mobile.parallelolapprocessing;


/**
 * Created by KheyaliMitra on 3/25/2015.
 */
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.github.johnkil.print.PrintView;
import com.unnamed.b.atv.model.TreeNode;

import mobile.parallelolapprocessing.Async.IconTreeItem;

public class IconTreeItemHolder extends TreeNode.BaseNodeViewHolder<IconTreeItem> {
    private TextView tvValue;
    private PrintView arrowView;


    public IconTreeItemHolder(Context context) {
        super(context);
    }


    @Override
    public View createNodeView(final TreeNode node, IconTreeItem value) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.layout_icon_node, null, false);
        tvValue = (TextView) view.findViewById(R.id.node_value);
        tvValue.setText(value.text);

        final View iconView = (View) view.findViewById(R.id.icon);
        //iconView.setIconText(context.getResources().getString(value.icon));

       // arrowView = (PrintView) view.findViewById(R.id.arrow_icon);

            /*view.findViewById(R.id.btn_addFolder).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TreeNode newFolder = new TreeNode(new IconTreeItemHolder.IconTreeItem(R.string.ic_folder, "New Folder"));
                    getTreeView().addNode(node, newFolder);
                }
            });

            view.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getTreeView().removeNode(node);
                }
            });

            //if My computer
            if (node.getLevel() == 1) {
                view.findViewById(R.id.btn_delete).setVisibility(View.GONE);
            }*/

        return view;
    }



}