package com.example.ywinked;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "user_db";
    private static final int DATABASE_VERSION = 2;
    
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_NAME = "name";

    private static final String TABLE_REMINDERS = "reminders";
    private static final String COLUMN_REM_ID = "id";
    private static final String COLUMN_REM_TITLE = "case_title";
    private static final String COLUMN_REM_COURT = "court_name";
    private static final String COLUMN_REM_DATE = "hearing_date";
    private static final String COLUMN_REM_TIME = "hearing_time";

    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_EMAIL + " TEXT UNIQUE, "
            + COLUMN_PASSWORD + " TEXT, "
            + COLUMN_NAME + " TEXT)";

    private static final String CREATE_TABLE_REMINDERS = "CREATE TABLE " + TABLE_REMINDERS + " ("
            + COLUMN_REM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_REM_TITLE + " TEXT, "
            + COLUMN_REM_COURT + " TEXT, "
            + COLUMN_REM_DATE + " TEXT, "
            + COLUMN_REM_TIME + " TEXT)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_REMINDERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMINDERS);
        onCreate(db);
    }

    // Add a new user
    public long addUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);
        
        return db.insert(TABLE_USERS, null, values);
    }

    // Check if user exists
    public boolean checkUser(String email, String password) {
        String[] columns = {
            COLUMN_ID
        };
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = COLUMN_EMAIL + " = ?" + " AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = { email, password };
        
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        
        return count > 0;
    }

    // Check if email exists
    public boolean checkEmail(String email) {
        String[] columns = {
            COLUMN_ID
        };
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = COLUMN_EMAIL + " = ?";
        String[] selectionArgs = { email };
        
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        
        return count > 0;
    }

    // Add Court Reminder
    public long addReminder(String title, String court, String date, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REM_TITLE, title);
        values.put(COLUMN_REM_COURT, court);
        values.put(COLUMN_REM_DATE, date);
        values.put(COLUMN_REM_TIME, time);
        long id = db.insert(TABLE_REMINDERS, null, values);
        db.close();
        return id;
    }

    // Get All Reminders
    public List<ReminderModel> getAllReminders() {
        List<ReminderModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_REMINDERS + " ORDER BY " + COLUMN_REM_ID + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REM_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REM_TITLE));
                String court = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REM_COURT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REM_DATE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REM_TIME));
                list.add(new ReminderModel(id, title, court, date, time));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // Delete Reminder
    public void deleteReminder(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_REMINDERS, COLUMN_REM_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}
