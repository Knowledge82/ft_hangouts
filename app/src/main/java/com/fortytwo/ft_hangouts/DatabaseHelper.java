package com.fortytwo.ft_hangouts;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.fortytwo.ft_hangouts.DatabaseContract.*;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createContactsTable = "CREATE TABLE " + ContactEntry.TABLE_NAME + " (" +
                ContactEntry.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ContactEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                ContactEntry.COLUMN_SURNAME + " TEXT, " +
                ContactEntry.COLUMN_PHONE + " TEXT NOT NULL, " +
                ContactEntry.COLUMN_EMAIL + " TEXT, " +
                ContactEntry.COLUMN_BIRTHDAY + " TEXT)";

        String createMessagesTable = "CREATE TABLE " + MessageEntry.TABLE_NAME + " (" +
                MessageEntry.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MessageEntry.COLUMN_CONTACT_ID + " INTEGER NOT NULL, " +
                MessageEntry.COLUMN_BODY + " TEXT NOT NULL, " +
                MessageEntry.COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                MessageEntry.COLUMN_DIRECTION + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + MessageEntry.COLUMN_CONTACT_ID + ") REFERENCES " +
                ContactEntry.TABLE_NAME + "(" + ContactEntry.COLUMN_ID + "))";

        db.execSQL(createContactsTable);
        db.execSQL(createMessagesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MessageEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ContactEntry.TABLE_NAME);
        onCreate(db);
    }
}
