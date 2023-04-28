package com.indiza.smsi.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.indiza.smsi.R;
import com.indiza.smsi.SenderActivity;
import com.indiza.smsi.data.ContactResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Edited by: Ranit Raj Ganguly on 17/04/21 & Envy on 23/04/2023
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {
    public Context mContext;
    private List<ContactResponse> contactsList ;
    public static List<ContactResponse> contactsToSend = new ArrayList<>();
    // Constructor
    public ContactsAdapter(List<ContactResponse> list) {
        this.contactsList = list;
    }
    public boolean isClickable = true;
    public void selectAll(boolean check){
      try{
          if(contactsList != null){
              for (int i=0; i<contactsList.size();i++) {
                  contactsList.get(i).isSelected();
                  contactsList.get(i).setSelected(check);
              }
          }
          notifyDataSetChanged();
      }catch(Exception ex){

      }
    }
    @NonNull
    @Override
    public ContactsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ContactsAdapter.ViewHolder holder, int position) {
        ContactResponse currentItem = contactsList.get(position);
        //in some cases, it will prevent unwanted situations
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(currentItem.isSelected());

        holder.cbSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    contactsToSend.add(currentItem);
                }else {

                    contactsToSend.remove(currentItem);
                }
                Set<ContactResponse> set = new HashSet<>(contactsToSend);
                contactsToSend.clear();
                contactsToSend.addAll(set);
                currentItem.setSelected(isChecked);
            }
        });
        holder.getContactNameTextView().setText(currentItem.getName());
        holder.getContactNumberTextView().setText(currentItem.getPhoneNumberList().get(0).getNumber());
        holder.getContactCbSelect().setSelected(currentItem.isSelected());
    }
    public void onClick(View view) {
        if(!isClickable)
            return;
        // do your click stuff
    }
    @Override
    public int getItemCount() {
        return Math.max(contactsList.size(), 0);
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView contactNameTextView;
        private TextView contactNumberTextView;
        private CheckBox cbSelect;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = (CheckBox) itemView.findViewById(R.id.cbSelect);
            contactNameTextView = itemView.findViewById(R.id.contact_name);
            contactNumberTextView = itemView.findViewById(R.id.contact_number);
        }
        public TextView getContactNameTextView() {
            return contactNameTextView;
        }
        public TextView getContactNumberTextView() {
            return contactNumberTextView;
        }
        public CheckBox getContactCbSelect(){ return cbSelect;}
    }
}
