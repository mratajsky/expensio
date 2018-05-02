package com.michalratajsky.repollo;

import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends CursorRecyclerAdapter<ExpenseAdapter.ViewHolder> {
    private static final String TAG = "ExpenseAdapter";

    // Database date format
    private SimpleDateFormat mDateFormatDb = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    // Output date format
    private DateFormat mDateFormat = DateFormat.getDateInstance(DateFormat.LONG);

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        private TextView mDescription;
        private TextView mAmount;
        private TextView mDate;
        private ImageView mArrowUp;
        private ImageView mArrowDown;

        ViewHolder(View v) {
            super(v);
            mDescription = (TextView) v.findViewById(R.id.item_description);
            mAmount = (TextView) v.findViewById(R.id.item_amount);
            mDate = (TextView) v.findViewById(R.id.item_date);
            mArrowUp = (ImageView) v.findViewById(R.id.item_arrow_up);
            mArrowDown = (ImageView) v.findViewById(R.id.item_arrow_down);
        }
    }

    public ExpenseAdapter(Cursor cursor) {
        super(cursor);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ExpenseAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.expense_view, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view
    @Override
    public void onBindViewHolderCursor(ViewHolder holder, Cursor cursor) {
        int column;

        column = cursor.getColumnIndex(ExpensioContract.ExpenseEntry.COLUMN_DESCRIPTION);
        holder.mDescription.setText(cursor.getString(column));

        column = cursor.getColumnIndex(ExpensioContract.ExpenseEntry.COLUMN_AMOUNT);

        double amount = Double.parseDouble(cursor.getString(column));
        NumberFormat nf = NumberFormat.getCurrencyInstance();

        column = cursor.getColumnIndex(ExpensioContract.ExpenseEntry.COLUMN_TYPE);
        if (cursor.getInt(column) == ExpensioContract.ExpenseEntry.TYPE_EXPENSE) {
            holder.mArrowUp.setVisibility(View.GONE);
            holder.mArrowDown.setVisibility(View.VISIBLE);
            holder.mAmount.setTextColor(Color.RED);
            holder.mAmount.setText("- " + nf.format(amount));
        } else {
            holder.mArrowUp.setVisibility(View.VISIBLE);
            holder.mArrowDown.setVisibility(View.GONE);
            holder.mAmount.setTextColor(Color.rgb(0, 100, 0));
            holder.mAmount.setText("+ " + nf.format(amount));
        }
        column = cursor.getColumnIndex(ExpensioContract.ExpenseEntry.COLUMN_DATE);
        try {
            Date date = mDateFormatDb.parse(cursor.getString(column));
            holder.mDate.setText(mDateFormat.format(date));
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        holder.itemView.setActivated(isSelected(holder.getAdapterPosition()));
    }

    private SparseBooleanArray selectedItems = new SparseBooleanArray();

    public boolean isSelected(int position) {
        return selectedItems.get(position);
    }

    public void toggleSelection(int position) {
        if (selectedItems.get(position, false))
            selectedItems.delete(position);
        else
            selectedItems.put(position, true);

        notifyItemChanged(position);
    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Long> getSelectedItems() {
        List<Long> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(getItemId(selectedItems.keyAt(i)));
        }
        return items;
    }
}
