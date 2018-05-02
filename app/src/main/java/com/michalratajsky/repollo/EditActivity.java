package com.michalratajsky.repollo;

import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class EditActivity extends AppCompatActivity implements
        DatePickerDialog.OnDateSetListener,
        ExpenseCategoryDialogFragment.DialogListener {
    private static final String TAG = "EditActivity";
    private static final String COLUMN_CATEGORIES = "categories";

    private long mInitialCategory;

    // Database ID of the current item given by the parent activity
    private long mCurrentItemId;

    // Current date in the date input field
    private Calendar mCurrentDate;

    private DatabaseHelper mDatabase;
    private List<Long> mCategories;
    private boolean mCategoriesUpdated;

    // Database date format
    private SimpleDateFormat mDateFormatDb = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    // Output date format
    private DateFormat mDateFormat = DateFormat.getDateInstance(DateFormat.LONG);

    private TextInputLayout mInputDescription;
    private TextInputLayout mInputAmount;
    private TextInputLayout mInputDate;
    private TextInputLayout mInputCategories;
    private TextInputLayout mInputNotes;
    private RadioButton mInputRadioExpense;
    private RadioButton mInputRadioIncome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        mDatabase = new DatabaseHelper(this);

        Intent intent = getIntent();
        // ID of the edited item
        mCurrentItemId = intent.getLongExtra(MainActivity.REQUEST_ID, 0L);

        // ID of the initial category
        mInitialCategory = intent.getLongExtra(MainActivity.REQUEST_CATEGORY, 0L);

        initUi();
        initForm();
    }

    private void initUi() {
        mInputDescription = (TextInputLayout) findViewById(R.id.input_description);
        mInputAmount = (TextInputLayout) findViewById(R.id.input_amount);
        mInputNotes = (TextInputLayout) findViewById(R.id.input_notes);

        mInputDate = (TextInputLayout) findViewById(R.id.input_date);
        mInputDate.getEditText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Creating date picker");
                DatePickerDialog dialog = DatePickerDialog.newInstance(
                        EditActivity.this,
                        mCurrentDate.get(Calendar.YEAR),
                        mCurrentDate.get(Calendar.MONTH),
                        mCurrentDate.get(Calendar.DAY_OF_MONTH)
                );
                dialog.show(getFragmentManager(), "DatePickerDialog");
            }
        });

        mInputCategories = (TextInputLayout) findViewById(R.id.input_categories);
        mInputCategories.getEditText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Creating category selector");

                Bundle bundle = new Bundle();
                bundle.putLong(ExpenseCategoryDialogFragment.ARG_ID, mCurrentItemId);
                if (mCategoriesUpdated) {
                    long[] ids = new long[mCategories.size()];
                    Iterator<Long> iterator = mCategories.iterator();
                    for (int i = 0; i < ids.length; i++)
                        ids[i] = iterator.next();

                    bundle.putLongArray(ExpenseCategoryDialogFragment.ARG_CATEGORIES, ids);
                }

                ExpenseCategoryDialogFragment dialog = new ExpenseCategoryDialogFragment();
                dialog.setArguments(bundle);
                dialog.show(getFragmentManager(), "ExpenseCategoryDialogFragment");
            }
        });

        mInputRadioExpense = (RadioButton) findViewById(R.id.input_radio_expense);
        mInputRadioIncome = (RadioButton) findViewById(R.id.input_radio_income);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void initForm() {
        mCurrentDate = Calendar.getInstance();

        if (mCurrentItemId > 0L) {
            // Read current item from database
            try {
                SQLiteDatabase db = mDatabase.getReadableDatabase();

                Cursor cursor = db.rawQuery("SELECT e.*, " +
                        "(SELECT GROUP_CONCAT(c.name, ', ') FROM " +
                        ExpensioContract.CategoryEntry.TABLE_NAME + " c JOIN " +
                        ExpensioContract.ExpenseCategoryEntry.TABLE_NAME + " ec ON c." +
                        ExpensioContract.CategoryEntry.COLUMN_ID + " = ec." +
                        ExpensioContract.ExpenseCategoryEntry.COLUMN_CATEGORY_ID + " AND ec." +
                        ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID + " = e." +
                        ExpensioContract.ExpenseEntry.COLUMN_ID + ") AS " + COLUMN_CATEGORIES + " FROM " +
                        ExpensioContract.ExpenseEntry.TABLE_NAME + " e WHERE e." +
                        ExpensioContract.ExpenseEntry.COLUMN_ID + " = ?",
                        new String[] { Long.toString(mCurrentItemId) });

                if (cursor.moveToFirst()) {
                    // Select the correct type
                    int value = cursor.getInt(cursor.getColumnIndex(
                            ExpensioContract.ExpenseEntry.COLUMN_TYPE));
                    switch (value) {
                        case ExpensioContract.ExpenseEntry.TYPE_EXPENSE:
                            mInputRadioExpense.setChecked(true);
                            break;
                        case ExpensioContract.ExpenseEntry.TYPE_INCOME:
                        default:
                            mInputRadioIncome.setChecked(true);
                            break;
                    }

                    // Fill textual values
                    String text;
                    text = cursor.getString(cursor.getColumnIndex(
                            ExpensioContract.ExpenseEntry.COLUMN_DESCRIPTION));
                    mInputDescription.getEditText().setText(text);
                    text = cursor.getString(cursor.getColumnIndex(
                            ExpensioContract.ExpenseEntry.COLUMN_AMOUNT));
                    mInputAmount.getEditText().setText(text);
                    text = cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORIES));
                    mInputCategories.getEditText().setText(text);
                    text = cursor.getString(cursor.getColumnIndex(
                            ExpensioContract.ExpenseEntry.COLUMN_NOTES));
                    mInputNotes.getEditText().setText(text);

                    text = cursor.getString(cursor.getColumnIndex(
                            ExpensioContract.ExpenseEntry.COLUMN_DATE));
                    try {
                        mCurrentDate.setTime(mDateFormatDb.parse(text.split(" ")[0]));
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse date of item ID " + mCurrentItemId
                                + ": " + text, e);
                        // Keep the default - current date in case of error
                    }
                    setCurrentDate();
                } else {
                    Log.e(TAG, "Invalid item ID " + mCurrentItemId);
                    mCurrentItemId = 0L;
                }
                cursor.close();
            } catch (SQLiteException e) {
                Log.e(TAG, "Failed to query database", e);
            }
        } else {
            // Adding a new item
            if (mInitialCategory > 0L) {
                String name = mDatabase.getCategoryNameById(mInitialCategory);
                if (name != null) {
                    mCategories = new ArrayList<>();
                    mCategories.add(mInitialCategory);
                    mInputCategories.getEditText().setText(name);
                }
            }
            setCurrentDate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_edit_main, menu);
        return true;
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        setCurrentDate(year, monthOfYear, dayOfMonth);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_save) {
            if (validateInput()) {
                boolean result;
                if (mCurrentItemId > 0L)
                    result = updateItem(mCurrentItemId);
                else
                    result = insertItem();
                if (result)
                    setResult(MainActivity.RESULT_EDIT_OK);
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean validateInput() {
        String text;
        boolean hasError = false;
        text = mInputDescription.getEditText().getText().toString();
        if (text.isEmpty()) {
            mInputDescription.setError(getString(R.string.input_err_description));
            hasError = true;
        } else
            mInputDescription.setErrorEnabled(false);
        text = mInputAmount.getEditText().getText().toString();
        if (text.isEmpty()) {
            mInputAmount.setError(getString(R.string.input_err_amount));
            hasError = true;
        } else
            mInputAmount.setErrorEnabled(false);
        return !hasError;
    }

    // Get type constant for database type field according to currently selected type
    private int getCurrentType() {
        if (mInputRadioExpense.isChecked())
            return ExpensioContract.ExpenseEntry.TYPE_EXPENSE;
        else
            return ExpensioContract.ExpenseEntry.TYPE_INCOME;
    }

    // Update the date field according to the currently set date
    private void setCurrentDate() {
        mInputDate.getEditText().setText(mDateFormat.format(mCurrentDate.getTime()));
    }

    // Update the date field according to the passed date
    private void setCurrentDate(int year, int month, int day) {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.MONTH, month);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, day);
        setCurrentDate();
    }

    private ContentValues createContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(ExpensioContract.ExpenseEntry.COLUMN_DESCRIPTION,
                mInputDescription
                        .getEditText()
                        .getText().toString());
        cv.put(ExpensioContract.ExpenseEntry.COLUMN_TYPE,
                getCurrentType());
        cv.put(ExpensioContract.ExpenseEntry.COLUMN_AMOUNT,
                mInputAmount
                        .getEditText()
                        .getText().toString());
        cv.put(ExpensioContract.ExpenseEntry.COLUMN_DATE,
                mDateFormatDb.format(mCurrentDate.getTime()));
        cv.put(ExpensioContract.ExpenseEntry.COLUMN_NOTES,
                mInputNotes
                        .getEditText()
                        .getText().toString());
        return cv;
    }

    private boolean insertItem() {
        ContentValues cv = createContentValues();

        Log.d(TAG, "Inserting item: " + cv.toString());

        SQLiteDatabase db = mDatabase.getWritableDatabase();
        try {
            db.beginTransaction();
            long id = db.insertOrThrow(
                    ExpensioContract.ExpenseEntry.TABLE_NAME,
                    null,
                    cv);
            if (id != -1L) {
                if (mCategoriesUpdated) {
                    for (long categoryId : mCategories) {
                        ContentValues cvCategory = new ContentValues();
                        cvCategory.put(
                                ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID,
                                id);
                        cvCategory.put(
                                ExpensioContract.ExpenseCategoryEntry.COLUMN_CATEGORY_ID,
                                categoryId);
                        db.insert(
                                ExpensioContract.ExpenseCategoryEntry.TABLE_NAME,
                                null,
                                cvCategory);
                    }
                }
                Log.d(TAG, "Done inserting item ID " + id);
                db.setTransactionSuccessful();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert item", e);
        } finally {
            db.endTransaction();
        }
        return false;
    }

    private boolean updateItem(long id) {
        ContentValues cv = createContentValues();

        Log.d(TAG, "Updating item ID " + id + ": " + cv.toString());

        SQLiteDatabase db = mDatabase.getWritableDatabase();
        try {
            db.beginTransaction();
            int updated = db.update(
                    ExpensioContract.ExpenseEntry.TABLE_NAME,
                    cv,
                    ExpensioContract.ExpenseEntry.COLUMN_ID + " = ?",
                    new String[] { Long.toString(id) });
            if (updated > 0) {
                if (mCategoriesUpdated) {
                    db.delete(
                            ExpensioContract.ExpenseCategoryEntry.TABLE_NAME,
                            ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID + " = ?",
                            new String[] { Long.toString(id) });
                    for (long categoryId : mCategories) {
                        ContentValues cvCategory = new ContentValues();
                        cvCategory.put(
                                ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID,
                                id);
                        cvCategory.put(
                                ExpensioContract.ExpenseCategoryEntry.COLUMN_CATEGORY_ID,
                                categoryId);
                        db.insert(
                                ExpensioContract.ExpenseCategoryEntry.TABLE_NAME,
                                null,
                                cvCategory);
                    }
                }
                db.setTransactionSuccessful();
                Log.d(TAG, "Done updating item");
            }
            if (updated > 0)
                return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to update item", e);
        } finally {
            db.endTransaction();
        }
        return false;
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, List<Long> selectedIds,
                                      String selectedText) {
        mCategories = selectedIds;
        mCategoriesUpdated = true;

        mInputCategories.getEditText().setText(selectedText);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
    }
}
