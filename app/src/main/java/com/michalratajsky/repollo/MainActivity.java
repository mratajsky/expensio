package com.michalratajsky.repollo;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
            NavigationView.OnNavigationItemSelectedListener,
        SearchView.OnQueryTextListener {
    private static final String TAG = "MainActivity";

    public static final int REQUEST_EDIT_INSERT = 1;
    public static final int REQUEST_EDIT_UPDATE = 2;

    public static final String REQUEST_ID = "ID";
    public static final String REQUEST_CATEGORY = "CATEGORY";

    public static final int RESULT_EDIT_OK = 1;

    private DatabaseHelper mDatabase;
    private RecyclerView mRecycler;
    private ExpenseAdapter mAdapter;
    private DrawerLayout mDrawer;
    private String mSearch;
    private ActionMode mActionMode;
    private MenuItem mCurrentNavigationItem;
    private SharedPreferences mPreferences;
    private HashMap<MenuItem, Long> mDrawerCategoryMap = new HashMap<>();
    private HashMap<Long, String> mCategoryNameMap = new HashMap<>();
    private long mCurrentCategory;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            mode.getMenuInflater().inflate(R.menu.activity_main_context, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteSelectedItems();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mAdapter.clearSelections();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = new DatabaseHelper(this);

        mPreferences = getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);

        initUi();
        // Must happen before drawer
        initAdapter();
        initDrawer();
    }

    private void initUi() {
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this);

        mRecycler = (RecyclerView) findViewById(R.id.recycler);

        // Use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecycler.setHasFixedSize(true);

        // Use a linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecycler.setLayoutManager(layoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                layoutManager.getOrientation());
        mRecycler.addItemDecoration(dividerItemDecoration);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                if (mCurrentCategory > 0L)
                    intent.putExtra(REQUEST_CATEGORY, mCurrentCategory);
                startActivityForResult(intent, REQUEST_EDIT_INSERT);
            }
        });
    }

    private void initDrawer() {
        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);

        MenuItem item = navView.getMenu().findItem(R.id.nav_category_holder);
        SubMenu sm = item.getSubMenu();

        long savedCategory = mPreferences.getLong(
                getString(R.string.preference_current_category), 0);

        Log.d(TAG, "Saved category is " + savedCategory);
        boolean foundSavedCategory = false;

        // Read list of categories
        Cursor cursor = getCategories();
        if (cursor.moveToFirst()) {
            int columnId = cursor.getColumnIndex(ExpensioContract.CategoryEntry.COLUMN_ID);
            int columnName = cursor.getColumnIndex(ExpensioContract.CategoryEntry.COLUMN_NAME);
            do {
                String name = cursor.getString(columnName);
                item = sm.add(name);
                item.setCheckable(true);

                // Store database ID as menu item tag
                long id = cursor.getLong(columnId);
                mDrawerCategoryMap.put(item, id);
                mCategoryNameMap.put(id, name);

                if (savedCategory == id) {
                    Log.d(TAG, "Found saved category " + item.getTitle());
                    mCurrentNavigationItem = item;
                    mCurrentNavigationItem.setChecked(true);
                    setCurrentCategory(id);
                    foundSavedCategory = true;
                }
            } while (cursor.moveToNext());
        }
        if (!foundSavedCategory) {
            mCurrentNavigationItem = sm.getItem(0);
            mCurrentNavigationItem.setChecked(true);
            setCurrentCategory(0);
        }
        cursor.close();
    }

    private void initAdapter() {
        mAdapter = new ExpenseAdapter(getCursor());
        mRecycler.setAdapter(mAdapter);

        ItemClickSupport.addTo(mRecycler)
                .setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        if (mActionMode != null) {
                            // In item selection mode
                            selectItem(position);
                        } else {
                            long id = mAdapter.getItemId(position);

                            Log.d(TAG, "Selected item ID " + id);

                            Intent intent = new Intent(MainActivity.this, EditActivity.class);
                            intent.putExtra(REQUEST_ID, id);
                            startActivityForResult(intent, REQUEST_EDIT_UPDATE);
                        }
                    }
                }).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
                        selectItem(position);
                        return true;
                    }
        });
    }

    private Cursor getCategories() {
        SQLiteDatabase db = mDatabase.getReadableDatabase();

        return db.query(
                ExpensioContract.CategoryEntry.TABLE_NAME,
                new String[] {
                        ExpensioContract.CategoryEntry.COLUMN_ID,
                        ExpensioContract.CategoryEntry.COLUMN_NAME
                },
                null, null,
                null, null,
                ExpensioContract.CategoryEntry.COLUMN_NAME);
    }

    private Cursor getCursor() {
        SQLiteDatabase db = mDatabase.getReadableDatabase();

        String query = "SELECT e.* FROM " + ExpensioContract.ExpenseEntry.TABLE_NAME + " e";
        String[] queryArgs = null;

        if (mCurrentCategory > 0 || mSearch != null) {
            ArrayList<String> args = new ArrayList<>();
            if (mCurrentCategory > 0) {
                query += " JOIN " + ExpensioContract.ExpenseCategoryEntry.TABLE_NAME +
                        " ec ON e." +
                        ExpensioContract.ExpenseEntry.COLUMN_ID + " = ec." +
                        ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID + " AND ec." +
                        ExpensioContract.ExpenseCategoryEntry.COLUMN_CATEGORY_ID + " = ?";
                args.add(Long.toString(mCurrentCategory));
            }
            if (mSearch != null) {
                query += " WHERE " + ExpensioContract.ExpenseEntry.COLUMN_DESCRIPTION + " LIKE ?";
                args.add('%' + mSearch + '%');
            }
            queryArgs = args.toArray(new String[0]);
        }
        query += " ORDER BY " + ExpensioContract.ExpenseEntry.COLUMN_DATE + " DESC";

        return db.rawQuery(query, queryArgs);
    }

    private void deleteSelectedItems() {
        final List<Long> items = mAdapter.getSelectedItems();
        if (items.isEmpty())
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage(R.string.expense_dialog_confirm)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "Deleting selected items: " + items.toString());

                        int deleted = mDatabase.deleteIds(
                                ExpensioContract.ExpenseEntry.TABLE_NAME,
                                items);
                        Log.d(TAG, "Deleted " + deleted + " items");
                        if (deleted > 0) {
                            CoordinatorLayout layout = (CoordinatorLayout)
                                    findViewById(R.id.main_layout);
                            Snackbar.make(layout, R.string.deleted, Snackbar.LENGTH_SHORT)
                                    .show();
                            refreshList();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .show();
    }

    private void refreshList() {
        mAdapter.changeCursor(getCursor());
        mAdapter.notifyDataSetChanged();
    }

    private void selectItem(int pos) {
        if (mActionMode == null)
            mActionMode = startSupportActionMode(mActionModeCallback);

        mAdapter.toggleSelection(pos);
        int count = mAdapter.getSelectedItemCount();
        String title = getResources().getQuantityString(
                R.plurals.selected_count,
                count,
                count);

        mActionMode.setTitle(title);
    }

    private void setCurrentCategory(long id) {
        String name = mCategoryNameMap.get(id);
        if (name != null)
            setTitle(name);
        else
            setTitle(R.string.app_name);
        mCurrentCategory = id;
        refreshList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_EDIT_INSERT:
            case REQUEST_EDIT_UPDATE:
                if (resultCode == RESULT_EDIT_OK)
                    refreshList();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START))
            mDrawer.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        // Do not iconify the widget; expand it by default
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_chart) {
            Intent intent = new Intent(this, ChartActivity.class);
            if (mCurrentCategory > 0L)
                intent.putExtra(REQUEST_CATEGORY, mCurrentCategory);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_categories:
                Intent intent = new Intent(this, CategoryActivity.class);
                startActivity(intent);
                break;
            default:
                if (mCurrentNavigationItem != null)
                    mCurrentNavigationItem.setChecked(false);

                item.setChecked(true);
                mCurrentNavigationItem = item;

                long categoryId = 0;
                try {
                    // Database ID of the selected category
                    categoryId = mDrawerCategoryMap.get(item);
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
                setCurrentCategory(categoryId);

                // Store the ID in preferences too
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putLong(getString(R.string.preference_current_category),
                        mCurrentCategory);
                editor.apply();
                break;
        }
        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.isEmpty())
            mSearch = null;
        else
            mSearch = newText;

        refreshList();
        return false;
    }
}
