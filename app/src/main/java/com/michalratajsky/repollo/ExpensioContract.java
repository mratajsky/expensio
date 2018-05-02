package com.michalratajsky.repollo;

import android.provider.BaseColumns;

final class ExpensioContract {
    private ExpensioContract() {}

    static final class ExpenseEntry implements BaseColumns {
        static final String TABLE_NAME = "expenses";
        static final String COLUMN_ID = "_id";
        static final String COLUMN_DESCRIPTION = "description";
        static final String COLUMN_TYPE = "type";
        static final String COLUMN_AMOUNT = "amount";
        static final String COLUMN_DATE = "date";
        static final String COLUMN_NOTES = "notes";

        // Supported type values for COLUMN_TYPE
        static final int TYPE_EXPENSE = 0;
        static final int TYPE_INCOME = 1;
    }

    static final class CategoryEntry implements BaseColumns {
        static final String TABLE_NAME = "categories";
        static final String COLUMN_ID = "_id";
        static final String COLUMN_NAME = "name";
        static final String COLUMN_COLOR = "color";
        static final String COLUMN_NOTES = "notes";
        static final String COLUMN_INITIAL = "initial";
    }

    static final class ExpenseCategoryEntry implements BaseColumns {
        static final String TABLE_NAME = "expenses_categories";
        static final String COLUMN_ID = "_id";
        static final String COLUMN_EXPENSE_ID = "expense_id";
        static final String COLUMN_CATEGORY_ID = "category_id";
    }
}
