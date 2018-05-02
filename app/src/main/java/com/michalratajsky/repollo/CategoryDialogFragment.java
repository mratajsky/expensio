package com.michalratajsky.repollo;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;

public class CategoryDialogFragment extends DialogFragment {
    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DialogListener {
        void onDialogPositiveClick(DialogFragment dialog, long id, String name);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    // Bundle arguments
    public static final String ARG_ID = "ID";
    public static final String ARG_NAME = "NAME";

    // Use this instance of the interface to deliver action events
    DialogListener mListener;

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
        final String name =
                bundle != null ? bundle.getString(ARG_NAME) : null;

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(id > 0
                        ? R.string.category_dialog_title_edit
                        : R.string.category_dialog_title_add)
                .setView(getActivity()
                        .getLayoutInflater()
                        .inflate(R.layout.category_dialog, null))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int _id) {
                        // Send the positive button event back to the host activity
                        AlertDialog ad = (AlertDialog) dialog;
                        EditText editText = (EditText) ad.findViewById(R.id.input_name);
                        mListener.onDialogPositiveClick(CategoryDialogFragment.this,
                                id,
                                editText.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int _id) {
                        // Send the negative button event back to the host activity
                        mListener.onDialogNegativeClick(CategoryDialogFragment.this);
                    }
                });

        // Create the AlertDialog object
        final Dialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                final AlertDialog ad = (AlertDialog) dialogInterface;

                // Disable the OK button if there is no initial name
                ad.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(name != null && !name.isEmpty());

                EditText editText = (EditText) dialog.findViewById(R.id.input_name);
                if (name != null)
                    editText.setText(name);
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        // Enable the OK button if the name is not empty
                        if (TextUtils.isEmpty(editable))
                            ad.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        else
                            ad.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                });
            }
        });
        return dialog;
    }
}
