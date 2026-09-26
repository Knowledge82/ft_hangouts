package com.fortytwo.ft_hangouts;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ContactDao contactDao;
    private ContactAdapter contactAdapter;
    private ListView contactListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactDao = new ContactDao(this);
        contactListView = findViewById(R.id.contactListView);

        List<Contact> contacts = contactDao.getAllContacts();
        contactAdapter = new ContactAdapter(this, contacts);
        contactListView.setAdapter(contactAdapter);
    }
}