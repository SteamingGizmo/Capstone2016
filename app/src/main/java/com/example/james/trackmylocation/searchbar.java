package com.example.james.trackmylocation;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

/**
 * Created by James on 2/15/2017.
 */

public class searchbar extends ListActivity {

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }
    public void onNewIntent(Intent intent){
        setIntent(intent);
        handleIntent(intent);
    }
    public void onListItemClick(ListView l, View v, int position, long id){
        //detail activity to call
    }
    private void handleIntent(Intent intent){
        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            String query = intent.getStringExtra(SearchManager.QUERY);
            doSearch(query);
        }
    }
    private void doSearch(String queryStr){
        //get a Cursor, prepare listadapter
        //and set it
    }
}
