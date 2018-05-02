package com.michalratajsky.repollo;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG  = "DatabaseHelper";
    private static final String DATABASE_NAME = "Data.db";
    private static final int DATABASE_VERSION = 1;

    private static final String SQL_CREATE_EXPENSES = "" +
            "CREATE TABLE " + ExpensioContract.ExpenseEntry.TABLE_NAME + " (" +
            "_id INTEGER PRIMARY KEY," +
            ExpensioContract.ExpenseEntry.COLUMN_DESCRIPTION + " TEXT," +
            ExpensioContract.ExpenseEntry.COLUMN_TYPE + " INTEGER," +
            ExpensioContract.ExpenseEntry.COLUMN_AMOUNT + " INTEGER," +
            ExpensioContract.ExpenseEntry.COLUMN_DATE + " TEXT," +
            ExpensioContract.ExpenseEntry.COLUMN_NOTES + " TEXT)";

    private static final String SQL_CREATE_CATEGORIES = "" +
            "CREATE TABLE " + ExpensioContract.CategoryEntry.TABLE_NAME + " (" +
            "_id INTEGER PRIMARY KEY," +
            ExpensioContract.CategoryEntry.COLUMN_NAME + " TEXT," +
            ExpensioContract.CategoryEntry.COLUMN_COLOR + " TEXT," +
            ExpensioContract.CategoryEntry.COLUMN_NOTES + " TEXT," +
            ExpensioContract.CategoryEntry.COLUMN_INITIAL + " INTEGER)";

    private static final String SQL_CREATE_EXPENSES_CATEGORIES = "" +
            "CREATE TABLE " + ExpensioContract.ExpenseCategoryEntry.TABLE_NAME + " (" +
            "_id INTEGER PRIMARY KEY," +
            ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID + " INTEGER " +
            "REFERENCES " + ExpensioContract.ExpenseEntry.TABLE_NAME + "(_id) ON DELETE CASCADE," +
            ExpensioContract.ExpenseCategoryEntry.COLUMN_CATEGORY_ID + " INTEGER " +
            "REFERENCES " + ExpensioContract.CategoryEntry.TABLE_NAME + "(_id) ON DELETE CASCADE)";

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database tables");
        db.execSQL(SQL_CREATE_EXPENSES);
        db.execSQL(SQL_CREATE_CATEGORIES);
        db.execSQL(SQL_CREATE_EXPENSES_CATEGORIES);
        Log.d(TAG, "Done creating database tables");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to version " + newVersion);

        Log.d(TAG, "Done upgrading database");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    public int deleteIds(String table, List<Long> ids) {
        SQLiteDatabase db = getWritableDatabase();

        int deleted = 0;
        try {
            deleted = db.delete(table, "_id IN (" + TextUtils.join(",", ids) + ")", null);
        } catch (SQLiteException e) {
            Log.w(TAG, e);
        }
        return deleted;
    }

    public String getCategoryNameById(long id) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT "
                        + ExpensioContract.CategoryEntry.COLUMN_NAME + " FROM "
                        + ExpensioContract.CategoryEntry.TABLE_NAME + " WHERE "
                        + ExpensioContract.CategoryEntry.COLUMN_ID + " = ?",
                new String[] { Long.toString(id)} );
        String name = null;
        if (cursor.moveToFirst()) {
            int column = cursor.getColumnIndex(ExpensioContract.CategoryEntry.COLUMN_NAME);
            name = cursor.getString(column);
        }
        cursor.close();
        return name;
    }
}
