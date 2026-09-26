package com.fortytwo.ft_hangouts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ContactAdapter extends ArrayAdapter<Contact> {

    public ContactAdapter(Context context, List<Contact> contacts) {
        super(context, 0, contacts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Contact contact = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.contact_list_item, parent, false);
        }

        TextView nameView = convertView.findViewById(R.id.contactNameTextView);
        TextView phoneView = convertView.findViewById(R.id.contactPhoneTextView);

        nameView.setText(contact.getName() + " " + contact.getSurname());
        phoneView.setText(contact.getPhone());

        return convertView;
    }
}