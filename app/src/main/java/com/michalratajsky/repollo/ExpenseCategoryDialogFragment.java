package com.michalratajsky.repollo;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ExpenseCategoryDialogFragment extends DialogFragment {
    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DialogListener {
        void onDialogPositiveClick(DialogFragment dialog, List<Long> selectedIds,
                                   String selectedText);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    // Bundle arguments
    public static final String ARG_ID = "ID";
    public static final String ARG_CATEGORIES = "CATEGORIES";

    private static final String COLUMN_IS_CHECKED = "is_checked";

    private ArrayList<Long> mSelectedItems = new ArrayList<>();
    private HashMap<Long, String> mCategoryNames = new HashMap<>();

    // Use this instance of the interface to deliver action events
    private DialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (DialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final long id =
                bundle != null ? bundle.getLong(ARG_ID) : 0;
        final long[] selectedIds =
                bundle != null ? bundle.getLongArray(ARG_CATEGORIES) : null;

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.expense_category_dialog_select)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int _id) {
                        // Send the positive button event back to the host activity
                        ArrayList<String> names = new ArrayList<>();
                        for (long id : mSelectedItems)
                            names.add(mCategoryNames.get(id));

                        Collections.sort(names);
                        String selectedText = TextUtils.join(", ", names);

                        mListener.onDialogPositiveClick(ExpenseCategoryDialogFragment.this,
                                mSelectedItems, selectedText);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int _id) {
                        // Send the negative button event back to the host activity
                        mListener.onDialogNegativeClick(ExpenseCategoryDialogFragment.this);
                    }
                });

        setDialogCategories(builder, id, selectedIds);
        return builder.create();
    }

    private void setDialogCategories(AlertDialog.Builder builder,
                                     long expenseId,
                                     long[] selectedIds) {
        SQLiteDatabase db = new DatabaseHelper(getActivity()).getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT c." +
                ExpensioContract.CategoryEntry.COLUMN_ID + ", c." +
                ExpensioContract.CategoryEntry.COLUMN_NAME + ", ec." +
                ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID + " IS NOT NULL AS " +
                COLUMN_IS_CHECKED + " FROM " +
                ExpensioContract.CategoryEntry.TABLE_NAME + " c LEFT JOIN " +
                ExpensioContract.ExpenseCategoryEntry.TABLE_NAME + " ec ON c." +
                ExpensioContract.CategoryEntry.COLUMN_ID + " = ec." +
                ExpensioContract.ExpenseCategoryEntry.COLUMN_CATEGORY_ID + " AND ec." +
                ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID + " = ? " +
                "LEFT JOIN " +
                ExpensioContract.ExpenseEntry.TABLE_NAME + " e ON e." +
                ExpensioContract.ExpenseEntry.COLUMN_ID + " = ec." +
                ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID + " ORDER BY c." +
                ExpensioContract.CategoryEntry.COLUMN_NAME, new String[] { Long.toString(expenseId) });

        int count = cursor.getCount();

        final long[] ids = new long[count];
        final CharSequence[] items = new CharSequence[count];
        final boolean[] selected = new boolean[count];
        if (selectedIds != null)
            Arrays.sort(selectedIds);

        mCategoryNames.clear();
        mSelectedItems.clear();

        if (cursor.moveToFirst()) {
            int i = 0;
            int colId = cursor.getColumnIndex(ExpensioContract.CategoryEntry.COLUMN_ID);
            int colName = cursor.getColumnIndex(ExpensioContract.CategoryEntry.COLUMN_NAME);
            int colSelected = cursor.getColumnIndex(COLUMN_IS_CHECKED);
            do {
                long id = cursor.getLong(colId);
                String name = cursor.getString(colName);
                ids[i] = id;
                items[i] = name;
                if (selectedIds != null)
                    selected[i] = Arrays.binarySearch(selectedIds, id) >= 0;
                else
                    selected[i] = cursor.getInt(colSelected) == 1;
                if (selected[i])
                    mSelectedItems.add(id);
                mCategoryNames.put(id, name);
                i++;
            } while (cursor.moveToNext());
        }
        cursor.close();

        DialogInterface.OnMultiChoiceClickListener listener =
                new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                if (b) {
                    mSelectedItems.add(ids[i]);
                } else
                    mSelectedItems.remove(mSelectedItems.indexOf(ids[i]));
            }
        };

        builder.setMultiChoiceItems(items, selected, listener);
    }
}
