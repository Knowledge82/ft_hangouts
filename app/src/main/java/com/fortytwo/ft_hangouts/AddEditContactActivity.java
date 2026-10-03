package com.fortytwo.ft_hangouts;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.DatePickerDialog;
import java.util.Calendar;
import java.util.Locale;
import android.view.View;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;

public class AddEditContactActivity extends AppCompatActivity {

    public static final String EXTRA_CONTACT_ID = "contact_id";

    private final Calendar birthdayCalendar = Calendar.getInstance();
    private EditText nameEditText, surnameEditText, phoneEditText, emailEditText, birthdayEditText;
    private ContactDao contactDao;

    private long contactId = -1;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_contact);

        Toolbar toolbar = findViewById(R.id.toolbar);

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

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        TextView toolbarTitle = findViewById(R.id.toolbarTitle);

        contactDao = new ContactDao(this);

        nameEditText = findViewById(R.id.nameEditText);
        surnameEditText = findViewById(R.id.surnameEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        birthdayEditText = findViewById(R.id.birthdayEditText);
        birthdayEditText.setOnClickListener(v -> showDatePicker());

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> saveContact());

        contactId = getIntent().getLongExtra(EXTRA_CONTACT_ID, -1);
        isEditMode = contactId != -1;

        if (isEditMode) {
            toolbarTitle.setText(R.string.title_edit_contact);
            loadContact();
        } else {
            toolbarTitle.setText(R.string.title_new_contact);
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    birthdayCalendar.set(year, month, dayOfMonth);
                    String formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    birthdayEditText.setText(formatted);
                },
                birthdayCalendar.get(Calendar.YEAR),
                birthdayCalendar.get(Calendar.MONTH),
                birthdayCalendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
    private void loadContact() {
        Contact contact = contactDao.getContactById(contactId);
        if (contact == null) return;

        nameEditText.setText(contact.getName());
        surnameEditText.setText(contact.getSurname());
        phoneEditText.setText(contact.getPhone());
        emailEditText.setText(contact.getEmail());
        birthdayEditText.setText(contact.getBirthday());
        if (contact.getBirthday() != null && !contact.getBirthday().isEmpty()) {
            try {
                String[] parts = contact.getBirthday().split("-");
                birthdayCalendar.set(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]) - 1,
                        Integer.parseInt(parts[2])
                );
            } catch (Exception ignored) {
                // некорректный формат в базе — просто откроется сегодняшняя дата
            }
        }
    }

    private void saveContact() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, R.string.error_name_phone_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String surname = surnameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String birthday = birthdayEditText.getText().toString().trim();

        if (isEditMode) {
            Contact contact = new Contact(contactId, name, surname, phone, email, birthday);
            contactDao.updateContact(contact);
        } else {
            Contact contact = new Contact(name, surname, phone, email, birthday);
            contactDao.addContact(contact);
        }

        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}