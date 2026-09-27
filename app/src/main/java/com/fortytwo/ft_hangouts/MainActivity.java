package com.fortytwo.ft_hangouts;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Мои контакты");

        contactDao = new ContactDao(this);
        contactListView = findViewById(R.id.contactListView);

        contactAdapter = new ContactAdapter(this, contactDao.getAllContacts());
        contactListView.setAdapter(contactAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        contactAdapter.clear();
        contactAdapter.addAll(contactDao.getAllContacts());
        contactAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_contact) {
            startActivity(new Intent(this, AddEditContactActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}