package com.michalratajsky.repollo;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class ChartActivity extends AppCompatActivity implements
        ChartPeriodDialogFragment.DialogListener,
        ChartTypeDialogFragment.DialogListener {
    private static final String TAG = "ChartActivity";
    private static final int COLOR_GREEN = Color.rgb(0, 100, 0);
    private static final int TYPE_MONTHLY = 0;
    private static final int TYPE_YEARLY = 1;

    private SimpleDateFormat mDateFormatDb = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private SimpleDateFormat mDateFormatDay = new SimpleDateFormat("MMM dd");
    private SimpleDateFormat mDateFormatMonth = new SimpleDateFormat("LLLL yyyy");

    private DatabaseHelper mDatabase;
    private SharedPreferences mPreferences;
    private BarChart mChart;
    private TextView mLabel;

    private HashMap<Integer, String> mXKeys = new HashMap<>();
    private HashMap<Integer, Date> mMonthlyMap = new HashMap<>();
    private HashMap<Integer, Integer> mYearlyMap = new HashMap<>();

    private int mMonthlyMonth;
    private int mMonthlyYear;
    private int mYearlyYear;
    private int mType;
    private long mCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        mDatabase = new DatabaseHelper(this);

        mPreferences = getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        mType = mPreferences.getInt(getString(R.string.preference_current_chart_type), 0);

        // ID of the initial category
        mCategory = getIntent().getLongExtra(MainActivity.REQUEST_CATEGORY, 0L);
        if (mCategory > 0) {
            String name = mDatabase.getCategoryNameById(mCategory);
            if (name != null) {
                TextView subLabel = (TextView) findViewById(R.id.chart_sublabel);
                subLabel.setVisibility(View.VISIBLE);
                subLabel.setText(name);
            }
        }
        mLabel = (TextView) findViewById(R.id.chart_label);

        initChart();
        switch (mType) {
            case TYPE_YEARLY:
                plotYearly();
                break;
            case TYPE_MONTHLY:
            default:
                plotMonthly();
        }
    }

    private void initChart() {
        mChart = (BarChart) findViewById(R.id.chart);

        mChart.getAxisRight().setEnabled(false);
        mChart.getDescription().setEnabled(false);
        mChart.getLegend().setEnabled(false);
        mChart.setDrawGridBackground(false);
        mChart.setDrawBarShadow(false);
        mChart.setDrawValueAboveBar(true);
        mChart.setPinchZoom(false);
        mChart.setTouchEnabled(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return mXKeys.get((int) value);
            }
        });
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis yAxis = mChart.getAxisLeft();
        yAxis.setDrawAxisLine(false);
        yAxis.setDrawGridLines(true);
        yAxis.setDrawZeroLine(true);
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxis.setSpaceTop(15f);
    }

    private CharSequence[] getMonths() {
        SQLiteDatabase db = mDatabase.getReadableDatabase();

        Cursor cursor;
        if (mCategory > 0L)
            cursor = db.rawQuery("SELECT DISTINCT strftime('%Y-%m-01', " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ") FROM " +
                            ExpensioContract.ExpenseEntry.TABLE_NAME + " e JOIN " +
                            ExpensioContract.ExpenseCategoryEntry.TABLE_NAME + " ec ON e." +
                            ExpensioContract.ExpenseEntry.COLUMN_ID + " = ec." +
                            ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID + " AND ec." +
                            ExpensioContract.ExpenseCategoryEntry.COLUMN_CATEGORY_ID + " = ? " +
                            "ORDER BY strftime('%Y-%m', " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ")",
                    new String[] { Long.toString(mCategory) });
        else
            cursor = db.rawQuery("SELECT DISTINCT strftime('%Y-%m-01', " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ") FROM " +
                            ExpensioContract.ExpenseEntry.TABLE_NAME + " ORDER BY strftime('%Y-%m', " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ")",
                    null);

        CharSequence[] items = new CharSequence[cursor.getCount()];
        mMonthlyMap.clear();
        if (cursor.moveToFirst()) {
            int i = 0;
            do {
                try {
                    Date date = mDateFormatDb.parse(cursor.getString(0));
                    items[i] = mDateFormatMonth.format(date);
                    mMonthlyMap.put(i, date);
                    i++;
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    private CharSequence[] getYears() {
        SQLiteDatabase db = mDatabase.getReadableDatabase();

        Cursor cursor;
        if (mCategory > 0L)
            cursor = db.rawQuery("SELECT DISTINCT strftime('%Y', " +
                    ExpensioContract.ExpenseEntry.COLUMN_DATE + ") FROM " +

                    ExpensioContract.ExpenseEntry.TABLE_NAME + " e JOIN " +
                    ExpensioContract.ExpenseCategoryEntry.TABLE_NAME + " ec ON e." +
                    ExpensioContract.ExpenseEntry.COLUMN_ID + " = ec." +
                    ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID + " AND ec." +
                    ExpensioContract.ExpenseCategoryEntry.COLUMN_CATEGORY_ID + " = ? " +
                    "ORDER BY strftime('%Y', " +
                    ExpensioContract.ExpenseEntry.COLUMN_DATE + ")",
                    new String[] { Long.toString(mCategory) });
        else
            cursor = db.rawQuery("SELECT DISTINCT strftime('%Y', " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ") FROM " +
                            ExpensioContract.ExpenseEntry.TABLE_NAME + " ORDER BY strftime('%Y', " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ")",
                    null);

        CharSequence[] items = new CharSequence[cursor.getCount()];
        mYearlyMap.clear();
        if (cursor.moveToFirst()) {
            int i = 0;
            do {
                int year = cursor.getInt(0);
                items[i] = getString(R.string.chart_year, year);
                mYearlyMap.put(i, year);
                i++;
            } while (cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    private void plotMonthly() {
        setTitle(getString(R.string.chart_monthly_overview));

        Calendar cal = Calendar.getInstance();
        if (mMonthlyMonth == 0 || mMonthlyYear == 0) {
            // Select current month and year
            mMonthlyMonth = cal.get(Calendar.MONTH) + 1;
            mMonthlyYear = cal.get(Calendar.YEAR);
        } else {
            cal.set(Calendar.MONTH, mMonthlyMonth - 1);
            cal.set(Calendar.YEAR, mMonthlyYear);
        }
        mLabel.setText(mDateFormatMonth.format(cal.getTime()));

        SQLiteDatabase db = mDatabase.getReadableDatabase();
        Cursor cursor;
        String period = String.format(Locale.US, "%d-%02d", mMonthlyYear, mMonthlyMonth);
        if (mCategory > 0L)
            cursor = db.rawQuery("SELECT " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ", " +
                            ExpensioContract.ExpenseEntry.COLUMN_TYPE + ", SUM(" +
                            ExpensioContract.ExpenseEntry.COLUMN_AMOUNT + ") FROM " +
                            ExpensioContract.ExpenseEntry.TABLE_NAME + " e JOIN " +
                            ExpensioContract.ExpenseCategoryEntry.TABLE_NAME + " ec ON e." +
                            ExpensioContract.ExpenseEntry.COLUMN_ID + " = ec." +
                            ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID + " AND ec." +
                            ExpensioContract.ExpenseCategoryEntry.COLUMN_CATEGORY_ID + " = ? WHERE strftime('%Y-%m', " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ") = ? GROUP BY " +
                            ExpensioContract.ExpenseEntry.COLUMN_TYPE + ", strftime('%Y-%m-%d', " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ") ORDER BY strftime('%d', " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ")",
                    new String[] {
                            Long.toString(mCategory),
                            period
                    });
        else
            cursor = db.rawQuery("SELECT " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ", " +
                            ExpensioContract.ExpenseEntry.COLUMN_TYPE + ", SUM(" +
                            ExpensioContract.ExpenseEntry.COLUMN_AMOUNT + ") FROM " +
                            ExpensioContract.ExpenseEntry.TABLE_NAME + " WHERE strftime('%Y-%m', " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ") = ? GROUP BY " +
                            ExpensioContract.ExpenseEntry.COLUMN_TYPE + ", strftime('%Y-%m-%d', " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ") ORDER BY strftime('%d', " +
                            ExpensioContract.ExpenseEntry.COLUMN_DATE + ")",
                    new String[] { period });

        SortedMap<String, Float> values = new TreeMap<>();
        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(0);
                float value = cursor.getFloat(2);
                float current = 0;
                if (values.containsKey(date))
                    current = values.get(date);
                if (cursor.getInt(1) == ExpensioContract.ExpenseEntry.TYPE_EXPENSE)
                    current -= value;
                else
                    current += value;
                values.put(date, current);
            } while (cursor.moveToNext());
        }
        cursor.close();
        mXKeys.clear();

        List<BarEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Float> entry : values.entrySet()) {
            try {
                Date date = mDateFormatDb.parse(entry.getKey());
                mXKeys.put(i, mDateFormatDay.format(date));
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            entries.add(new BarEntry(i, entry.getValue()));
            if (entry.getValue() >= 0)
                colors.add(COLOR_GREEN);
            else
                colors.add(Color.RED);
            i++;
        }
        BarDataSet set = new BarDataSet(entries, "BarDataSet");
        set.setColors(colors);
        mChart.setData(new BarData(set));
        mChart.invalidate();
        mChart.fitScreen();
    }

    private void plotYearly() {
        setTitle(getString(R.string.chart_yearly_overview));

        if (mYearlyYear == 0) {
            // Select current year
            Calendar cal = Calendar.getInstance();
            mYearlyYear = cal.get(Calendar.YEAR);
        }
        mLabel.setText(getString(R.string.chart_year, mYearlyYear));

        SQLiteDatabase db = mDatabase.getReadableDatabase();
        Cursor cursor;
        if (mCategory > 0L)
            cursor = db.rawQuery("SELECT strftime('%Y-%m-01', " +
                    ExpensioContract.ExpenseEntry.COLUMN_DATE + "), " +
                    ExpensioContract.ExpenseEntry.COLUMN_TYPE + ", SUM(" +
                    ExpensioContract.ExpenseEntry.COLUMN_AMOUNT + ") FROM " +
                    ExpensioContract.ExpenseEntry.TABLE_NAME + " e JOIN " +
                    ExpensioContract.ExpenseCategoryEntry.TABLE_NAME + " ec ON e." +
                    ExpensioContract.ExpenseEntry.COLUMN_ID + " = ec." +
                    ExpensioContract.ExpenseCategoryEntry.COLUMN_EXPENSE_ID + " AND ec." +
                    ExpensioContract.ExpenseCategoryEntry.COLUMN_CATEGORY_ID + " = ? WHERE strftime('%Y', " +
                    ExpensioContract.ExpenseEntry.COLUMN_DATE + ") = ? GROUP BY " +
                    ExpensioContract.ExpenseEntry.COLUMN_TYPE + ", strftime('%Y-%m', " +
                    ExpensioContract.ExpenseEntry.COLUMN_DATE + ") ORDER BY strftime('%m', " +
                    ExpensioContract.ExpenseEntry.COLUMN_DATE + ")",
                    new String[] {
                            Long.toString(mCategory),
                            Integer.toString(mYearlyYear)
                    });
        else
            cursor = db.rawQuery("SELECT strftime('%Y-%m-01', " +
                    ExpensioContract.ExpenseEntry.COLUMN_DATE + "), " +
                    ExpensioContract.ExpenseEntry.COLUMN_TYPE + ", SUM(" +
                    ExpensioContract.ExpenseEntry.COLUMN_AMOUNT + ") FROM " +
                    ExpensioContract.ExpenseEntry.TABLE_NAME + " WHERE strftime('%Y', " +
                    ExpensioContract.ExpenseEntry.COLUMN_DATE + ") = ? GROUP BY " +
                    ExpensioContract.ExpenseEntry.COLUMN_TYPE + ", strftime('%Y-%m', " +
                    ExpensioContract.ExpenseEntry.COLUMN_DATE + ") ORDER BY strftime('%m', " +
                    ExpensioContract.ExpenseEntry.COLUMN_DATE + ")",
                    new String[] { Integer.toString(mYearlyYear) });

        SortedMap<String, Float> values = new TreeMap<>();
        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(0);
                float value = cursor.getFloat(2);
                float current = 0;
                if (values.containsKey(date))
                    current = values.get(date);
                if (cursor.getInt(1) == ExpensioContract.ExpenseEntry.TYPE_EXPENSE)
                    current -= value;
                else
                    current += value;
                values.put(date, current);
            } while (cursor.moveToNext());
        }
        cursor.close();
        mXKeys.clear();

        List<BarEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Float> entry : values.entrySet()) {
            try {
                Date date = mDateFormatDb.parse(entry.getKey());
                mXKeys.put(i, mDateFormatMonth.format(date));
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            entries.add(new BarEntry(i, entry.getValue()));
            if (entry.getValue() >= 0)
                colors.add(COLOR_GREEN);
            else
                colors.add(Color.RED);
            i++;
        }
        BarDataSet set = new BarDataSet(entries, "BarDataSet");
        set.setColors(colors);
        mChart.setData(new BarData(set));
        mChart.invalidate();
        mChart.fitScreen();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_chart_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_select_period) {
            ChartPeriodDialogFragment dialog = new ChartPeriodDialogFragment();
            Bundle bundle = new Bundle();
            CharSequence[] items;
            if (mType == TYPE_MONTHLY)
                items = getMonths();
            else
                items = getYears();
            bundle.putCharSequenceArray(ChartPeriodDialogFragment.ARG_ITEMS, items);
            dialog.setArguments(bundle);
            dialog.show(getFragmentManager(), "dialog");
            return true;
        }
        if (id == R.id.action_select_type) {
            ChartTypeDialogFragment dialog = new ChartTypeDialogFragment();
            dialog.show(getFragmentManager(), "dialog");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPeriodDialogClick(int item) {
        switch (mType) {
            case TYPE_MONTHLY:
                Date date = mMonthlyMap.get(item);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                mMonthlyMonth = cal.get(Calendar.MONTH) + 1;
                mMonthlyYear = cal.get(Calendar.YEAR);
                plotMonthly();
                break;
            case TYPE_YEARLY:
                mYearlyYear = mYearlyMap.get(item);
                plotYearly();
        }
    }

    @Override
    public void onTypeDialogClick(int item) {
        switch (item) {
            case TYPE_MONTHLY:
                mType = TYPE_MONTHLY;
                plotMonthly();
                break;
            case TYPE_YEARLY:
                mType = TYPE_YEARLY;
                plotYearly();
                break;
            default:
                return;
        }

        // Store the type in preferences too
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(getString(R.string.preference_current_chart_type), item);
        editor.apply();
    }
}
