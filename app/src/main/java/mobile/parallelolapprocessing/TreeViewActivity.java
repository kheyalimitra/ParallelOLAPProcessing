package mobile.parallelolapprocessing;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
/**
 * Created by KheyaliMitra on 2/12/2016.
 */
public class TreeViewActivity extends  ActionBarActivity{public final static String View_PARAM = "fragment";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.treehierarchy);

        Bundle b = getIntent().getExtras();
        Class<?> treeViewClass = (Class<?>) b.get(View_PARAM);
        if (bundle == null) {
            Fragment f = Fragment.instantiate(this, treeViewClass.getName());
            f.setArguments(b);
            getFragmentManager().beginTransaction().replace(R.id.tHierarchy, f, treeViewClass.getName()).commit();
        }
    }
}
