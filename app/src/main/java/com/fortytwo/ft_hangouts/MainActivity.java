package com.fortytwo.ft_hangouts;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.widget.ListView;
import android.view.View;
import java.util.List;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity {

    private ContactDao contactDao;
    private ContactAdapter contactAdapter;
    private ListView contactListView;
    private Toolbar toolbar;
    private static final String PREFS_NAME = "ft_hangouts_prefs";
    private static final String KEY_TOOLBAR_COLOR = "toolbar_color";

    private void refreshContactList() {
        List<Contact> contacts = contactDao.getAllContacts();
        contactAdapter.clear();
        contactAdapter.addAll(contacts);
        contactAdapter.notifyDataSetChanged();
    }

    private void showColorPickerDialog() {
        String[] colorNames = {"Персиковый (по умолчанию)", "Синий", "Зелёный", "Фиолетовый", "Красный"};
        int[] colorValues = {
                getColor(R.color.primary),
                Color.parseColor("#4C6EF5"),
                Color.parseColor("#40C057"),
                Color.parseColor("#7950F2"),
                Color.parseColor("#FA5252")
        };

        new AlertDialog.Builder(this)
                .setTitle("Цвет шапки")
                .setItems(colorNames, (dialog, which) -> {
                    int chosenColor = colorValues[which];
                    toolbar.setBackgroundColor(chosenColor);

                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().putInt(KEY_TOOLBAR_COLOR, chosenColor).apply();
                })
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true);
        final int actionBarHeightPx = TypedValue.complexToDimensionPixelSize(
                typedValue.data, getResources().getDisplayMetrics());

        toolbar.setOnApplyWindowInsetsListener((v, insets) -> {
            int statusBarInset = insets.getSystemWindowInsetTop();

            v.setPadding(v.getPaddingLeft(), statusBarInset, v.getPaddingRight(), v.getPaddingBottom());

            ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = actionBarHeightPx + statusBarInset;
            v.setLayoutParams(params);

            return insets;
        });

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedColor = prefs.getInt(KEY_TOOLBAR_COLOR, getColor(R.color.primary));
        toolbar.setBackgroundColor(savedColor);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("ft_hangouts");

        contactDao = new ContactDao(this);
        contactListView = findViewById(R.id.contactListView);

        contactAdapter = new ContactAdapter(this, contactDao.getAllContacts());
        contactListView.setAdapter(contactAdapter);

        TextView addContactButton = findViewById(R.id.addContactButton);
        addContactButton.setOnClickListener(v -> startActivity(new Intent(this, AddEditContactActivity.class)));

        final int baseMarginPx = (int) (24 * getResources().getDisplayMetrics().density);

        addContactButton.setOnApplyWindowInsetsListener((v, insets) -> {
            int navBarInset = insets.getSystemWindowInsetBottom();

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = baseMarginPx + navBarInset;
            v.setLayoutParams(params);

            return insets;
        });

        contactListView.setOnItemClickListener((parent, view, position, id) -> {
            Contact contact = contactAdapter.getItem(position);
            Intent intent = new Intent(MainActivity.this, AddEditContactActivity.class);
            intent.putExtra(AddEditContactActivity.EXTRA_CONTACT_ID, contact.getId());
            startActivity(intent);
        });

        contactListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Contact contact = contactAdapter.getItem(position);

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Удалить контакт")
                    .setMessage("Удалить " + contact.getName() + " " + contact.getSurname() + "?")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        contactDao.deleteContact(contact.getId());
                        refreshContactList();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();

            return true;
        });
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
        if (item.getItemId() == R.id.action_change_color) {
            showColorPickerDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}