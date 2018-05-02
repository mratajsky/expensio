package com.michalratajsky.repollo;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends CursorRecyclerAdapter<CategoryAdapter.ViewHolder> {
    private static final String TAG = "CategoryAdapter";

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        private TextView mName;

        ViewHolder(View v) {
            super(v);
            mName = (TextView) v.findViewById(R.id.item_name);
        }
    }

    public CategoryAdapter(Cursor cursor) {
        super(cursor);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public CategoryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.category_view, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view
    @Override
    public void onBindViewHolderCursor(ViewHolder holder, Cursor cursor) {
        int column = cursor.getColumnIndex(ExpensioContract.CategoryEntry.COLUMN_NAME);

        holder.mName.setText(cursor.getString(column));
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
        for (int i = 0; i < selectedItems.size(); i++)
            items.add(getItemId(selectedItems.keyAt(i)));

        return items;
    }
}
