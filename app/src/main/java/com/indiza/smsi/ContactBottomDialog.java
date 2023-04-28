package com.indiza.smsi;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.indiza.smsi.databinding.ActivitySenderBinding;
import com.indiza.smsi.view.adapter.ContactsAdapter;
import com.indiza.smsi.viewModel.MainActivityViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

public class ContactBottomDialog extends BottomSheetDialogFragment
        implements View.OnClickListener {
    public static final String TAG = "ActionBottomDialog";
    private ItemClickListener mListener;
    private RecyclerView recycleViewCtc;
    private MainActivityViewModel mViewModel;
    private ConstraintLayout constraintLayout;


    public static ContactBottomDialog newInstance() {
        return new ContactBottomDialog();
    }
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_btcontact, container, false);
        recycleViewCtc =  view.findViewById(R.id.display_contacts_recycler_view);
        ContactsAdapter mAdapter = new ContactsAdapter(ContactsAdapter.contactsToSend );

        recycleViewCtc.setHasFixedSize(true);
        recycleViewCtc.setLayoutManager(new LinearLayoutManager(getContext()));
        recycleViewCtc.setAdapter(mAdapter);
        return view ;
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ItemClickListener) {
            mListener = (ItemClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ItemClickListener");
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    @Override public void onClick(View view) {
        TextView tvSelected = (TextView) view;
        mListener.onItemClick(tvSelected.getText().toString());
        dismiss();
    }
    public interface ItemClickListener {
        void onItemClick(String item);
    }
}
