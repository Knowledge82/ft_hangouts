package com.fortytwo.ft_hangouts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import static com.fortytwo.ft_hangouts.DatabaseContract.*;

public class ContactDao {

    private final DatabaseHelper dbHelper;

    public ContactDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public long addContact(Contact contact) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ContactEntry.COLUMN_NAME, contact.getName());
        values.put(ContactEntry.COLUMN_SURNAME, contact.getSurname());
        values.put(ContactEntry.COLUMN_PHONE, contact.getPhone());
        values.put(ContactEntry.COLUMN_EMAIL, contact.getEmail());
        values.put(ContactEntry.COLUMN_BIRTHDAY, contact.getBirthday());

        long newId = db.insert(ContactEntry.TABLE_NAME, null, values);
        db.close();
        return newId;
    }

    public List<Contact> getAllContacts() {
        List<Contact> contacts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT * FROM " + ContactEntry.TABLE_NAME +
                " ORDER BY " + ContactEntry.COLUMN_NAME + " ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Contact contact = new Contact(
                        cursor.getLong(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_SURNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_PHONE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_EMAIL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_BIRTHDAY))
                );
                contacts.add(contact);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return contacts;
    }

    public int updateContact(Contact contact) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ContactEntry.COLUMN_NAME, contact.getName());
        values.put(ContactEntry.COLUMN_SURNAME, contact.getSurname());
        values.put(ContactEntry.COLUMN_PHONE, contact.getPhone());
        values.put(ContactEntry.COLUMN_EMAIL, contact.getEmail());
        values.put(ContactEntry.COLUMN_BIRTHDAY, contact.getBirthday());

        int rowsAffected = db.update(
                ContactEntry.TABLE_NAME,
                values,
                ContactEntry.COLUMN_ID + " = ?",
                new String[]{String.valueOf(contact.getId())}
        );

        db.close();
        return rowsAffected;
    }

    public void deleteContact(long contactId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                ContactEntry.TABLE_NAME,
                ContactEntry.COLUMN_ID + " = ?",
                new String[]{String.valueOf(contactId)}
        );
        db.close();
    }
}
