package com.fortytwo.ft_hangouts;

public final class DatabaseContract {

    private DatabaseContract() {}

    public static final String DATABASE_NAME = "ft_hangouts.db";
    public static final int DATABASE_VERSION = 1;

    public static final class ContactEntry {
        public static final String TABLE_NAME = "contacts";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_SURNAME = "surname";
        public static final String COLUMN_PHONE = "phone";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_BIRTHDAY = "birthday";
    }

    public static final class MessageEntry {
        public static final String TABLE_NAME = "messages";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_CONTACT_ID = "contact_id";
        public static final String COLUMN_BODY = "body";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_DIRECTION = "direction";
    }
}
