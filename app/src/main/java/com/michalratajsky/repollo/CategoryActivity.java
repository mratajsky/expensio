package com.michalratajsky.repollo;

import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

public class CategoryActivity extends AppCompatActivity implements
        CategoryDialogFragment.DialogListener {
    private static final String TAG = "CategoryActivity";

    private CategoryAdapter mAdapter;
    private DatabaseHelper mDatabase;

    private ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            mode.getMenuInflater().inflate(R.menu.activity_main_context, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode,
        // but may be called multiple times if the mode is invalidated.
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
        setContentView(R.layout.activity_category);

        mDatabase = new DatabaseHelper(this);

        initUi();
    }

    private void initUi() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView view = (RecyclerView) findViewById(R.id.recycler);

        // Use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        view.setHasFixedSize(true);

        // Use a linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        view.setLayoutManager(layoutManager);

        // Specify an adapter
        mAdapter = new CategoryAdapter(getCursor());
        view.setAdapter(mAdapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                layoutManager.getOrientation());
        view.addItemDecoration(dividerItemDecoration);

        ItemClickSupport.addTo(view)
                .setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        if (mActionMode != null) {
                            // In item selection mode
                            selectItem(position);
                        } else {
                            long id = mAdapter.getItemId(position);

                            Log.d(TAG, "Clicked item ID " + id);

                            Bundle bundle = new Bundle();
                            bundle.putLong(CategoryDialogFragment.ARG_ID, id);
                            bundle.putString(CategoryDialogFragment.ARG_NAME,
                                    mDatabase.getCategoryNameById(id));

                            CategoryDialogFragment dialog = new CategoryDialogFragment();
                            dialog.setArguments(bundle);
                            dialog.show(getFragmentManager(), "CategoryDialogFragment");
                        }
                    }
                }).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
                    selectItem(position);
                    return true;
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CategoryDialogFragment dialog = new CategoryDialogFragment();
                dialog.show(getFragmentManager(), "CategoryDialogFragment");
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private Cursor getCursor() {
        SQLiteDatabase db = mDatabase.getReadableDatabase();

        return db.query(
                ExpensioContract.CategoryEntry.TABLE_NAME,
                null,
                null, null,
                null, null,
                ExpensioContract.CategoryEntry.COLUMN_NAME);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, long id, String name) {
        ContentValues cv = createContentValues(name);

        boolean result;
        if (id > 0)
            result = updateItem(id, cv);
        else
            result = insertItem(cv);
        if (result)
            refreshList();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
    }

    private ContentValues createContentValues(String name) {
        ContentValues cv = new ContentValues();
        cv.put(ExpensioContract.CategoryEntry.COLUMN_NAME, name);
        return cv;
    }

    private boolean insertItem(ContentValues cv) {
        Log.d(TAG, "Inserting item: " + cv.toString());

        try {
            SQLiteDatabase db = mDatabase.getWritableDatabase();
            long id = db.insertOrThrow(
                    ExpensioContract.CategoryEntry.TABLE_NAME,
                    null,
                    cv);
            if (id != -1L) {
                Log.d(TAG, "Done inserting item ID " + id);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert item", e);
        }
        return false;
    }

    private boolean updateItem(long id, ContentValues cv) {
        Log.d(TAG, "Updating item ID " + id + ": " + cv.toString());

        try {
            SQLiteDatabase db = mDatabase.getWritableDatabase();

            int updated = db.update(
                    ExpensioContract.CategoryEntry.TABLE_NAME,
                    cv,
                    ExpensioContract.CategoryEntry.COLUMN_ID + " = ?",
                    new String[] { Long.toString(id) });
            if (updated > 0) {
                Log.d(TAG, "Done updating item");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update item", e);
        }
        return false;
    }

    private void refreshList() {
        mAdapter.changeCursor(getCursor());
        mAdapter.notifyDataSetChanged();
    }

    private void deleteSelectedItems() {
        final List<Long> items = mAdapter.getSelectedItems();
        if (items.isEmpty())
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage(R.string.category_dialog_confirm)
                .setPositiveButton(R.string.mdtp_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "Deleting selected items: " + items.toString());

                        int deleted = mDatabase.deleteIds(
                                ExpensioContract.CategoryEntry.TABLE_NAME,
                                items);
                        Log.d(TAG, "Deleted " + deleted + " items");
                        if (deleted > 0) {
                            CoordinatorLayout layout = (CoordinatorLayout)
                                    findViewById(R.id.categories_layout);
                            Snackbar.make(layout, R.string.deleted, Snackbar.LENGTH_SHORT)
                                    .show();
                            refreshList();
                        }
                    }
                })
                .setNegativeButton(R.string.mdtp_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .show();
    }

    private void selectItem(int position) {
        if (mActionMode == null)
            mActionMode = startSupportActionMode(mActionModeCallback);

        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectedItemCount();
        String title = getResources().getQuantityString(
                R.plurals.selected_count,
                count,
                count);

        mActionMode.setTitle(title);
    }
}
