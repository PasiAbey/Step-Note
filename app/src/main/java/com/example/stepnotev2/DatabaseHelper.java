package com.example.stepnotev2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "stepnote_v2.db";
    private static final int DATABASE_VERSION = 5; // Increment again to force upgrade

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String USER_ID = "id";
    private static final String USER_NAME = "name";
    private static final String USER_EMAIL = "email";
    private static final String USER_PASSWORD = "password";
    private static final String USER_PROFILE_IMAGE_PATH = "profile_image_path";
    private static final String USER_JOIN_DATE = "join_date";
    private static final String USER_IS_LOGGED_IN = "is_logged_in";
    private static final String USER_CREATED_AT = "created_at";

    // Flashcards table
    private static final String TABLE_FLASHCARDS = "flashcards";
    private static final String FLASHCARD_ID = "id";
    private static final String FLASHCARD_QUESTION = "question";
    private static final String FLASHCARD_ANSWER = "answer";
    private static final String FLASHCARD_CREATED_DATE = "created_date";

    // Audio notes table
    private static final String TABLE_AUDIO_NOTES = "audio_notes";
    private static final String AUDIO_ID = "id";
    private static final String AUDIO_TITLE = "title";
    private static final String AUDIO_DURATION = "duration";
    private static final String AUDIO_FILE_PATH = "file_path";
    private static final String AUDIO_FILE_NAME = "file_name";
    private static final String AUDIO_FILE_SIZE = "file_size";
    private static final String AUDIO_CREATED_DATE = "created_date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "DatabaseHelper initialized with version: " + DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database tables from scratch...");

        createAllTables(db);
        Log.d(TAG, "All tables created successfully");
    }

    private void createAllTables(SQLiteDatabase db) {
        // Create Users table
        String createUsersTable = "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + "("
                + USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + USER_NAME + " TEXT NOT NULL,"
                + USER_EMAIL + " TEXT UNIQUE NOT NULL,"
                + USER_PASSWORD + " TEXT NOT NULL,"
                + USER_PROFILE_IMAGE_PATH + " TEXT,"
                + USER_JOIN_DATE + " TEXT,"
                + USER_IS_LOGGED_IN + " INTEGER DEFAULT 0,"
                + USER_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(createUsersTable);
        Log.d(TAG, "Users table created");

        // Create Flashcards table
        String createFlashcardsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_FLASHCARDS + "("
                + FLASHCARD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FLASHCARD_QUESTION + " TEXT NOT NULL,"
                + FLASHCARD_ANSWER + " TEXT NOT NULL,"
                + FLASHCARD_CREATED_DATE + " TEXT NOT NULL"
                + ")";
        db.execSQL(createFlashcardsTable);
        Log.d(TAG, "Flashcards table created");

        // Create Audio Notes table - THIS IS THE MISSING TABLE!
        String createAudioNotesTable = "CREATE TABLE IF NOT EXISTS " + TABLE_AUDIO_NOTES + "("
                + AUDIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + AUDIO_TITLE + " TEXT NOT NULL,"
                + AUDIO_DURATION + " TEXT NOT NULL,"
                + AUDIO_FILE_PATH + " TEXT,"
                + AUDIO_FILE_NAME + " TEXT,"
                + AUDIO_FILE_SIZE + " INTEGER DEFAULT 0,"
                + AUDIO_CREATED_DATE + " TEXT NOT NULL"
                + ")";
        db.execSQL(createAudioNotesTable);
        Log.d(TAG, "Audio Notes table created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        // Force recreation of all tables to ensure they exist
        Log.d(TAG, "Force creating all missing tables...");
        createAllTables(db);

        Log.d(TAG, "Database upgrade completed successfully");
    }

    // Check if table exists method
    private boolean tableExists(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{tableName}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        Log.d(TAG, "Table " + tableName + " exists: " + exists);
        return exists;
    }

    // Safe audio notes methods with table existence check
    public List<AudioNote> getAllAudioNotes() {
        List<AudioNote> audioNotes = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();

            // Check if table exists first
            if (!tableExists(db, TABLE_AUDIO_NOTES)) {
                Log.w(TAG, "Audio notes table doesn't exist, creating it...");
                createAllTables(db);
            }

            Log.d(TAG, "Querying audio notes...");

            cursor = db.query(TABLE_AUDIO_NOTES, null, null, null, null, null,
                    AUDIO_ID + " DESC");

            Log.d(TAG, "Audio notes cursor count: " + cursor.getCount());

            if (cursor.moveToFirst()) {
                do {
                    try {
                        AudioNote audioNote = new AudioNote();

                        audioNote.setId(cursor.getLong(cursor.getColumnIndexOrThrow(AUDIO_ID)));
                        audioNote.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(AUDIO_TITLE)));
                        audioNote.setDuration(cursor.getString(cursor.getColumnIndexOrThrow(AUDIO_DURATION)));

                        // Handle potentially null columns safely
                        int filePathIndex = cursor.getColumnIndex(AUDIO_FILE_PATH);
                        if (filePathIndex != -1) {
                            audioNote.setFilePath(cursor.getString(filePathIndex));
                        }

                        int fileNameIndex = cursor.getColumnIndex(AUDIO_FILE_NAME);
                        if (fileNameIndex != -1) {
                            audioNote.setFileName(cursor.getString(fileNameIndex));
                        }

                        int fileSizeIndex = cursor.getColumnIndex(AUDIO_FILE_SIZE);
                        if (fileSizeIndex != -1) {
                            audioNote.setFileSize(cursor.getLong(fileSizeIndex));
                        }

                        audioNote.setCreatedDate(cursor.getString(cursor.getColumnIndexOrThrow(AUDIO_CREATED_DATE)));

                        audioNotes.add(audioNote);
                        Log.d(TAG, "Added audio note: " + audioNote.getTitle());

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing audio note: " + e.getMessage());
                        e.printStackTrace();
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "No audio notes found in database");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting audio notes: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.d(TAG, "Returning " + audioNotes.size() + " audio notes");
        return audioNotes;
    }

    public long addAudioNote(String title, String duration) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Ensure table exists
        if (!tableExists(db, TABLE_AUDIO_NOTES)) {
            createAllTables(db);
        }

        ContentValues values = new ContentValues();
        values.put(AUDIO_TITLE, title);
        values.put(AUDIO_DURATION, duration);
        values.put(AUDIO_CREATED_DATE, getCurrentDateTime());

        long id = db.insert(TABLE_AUDIO_NOTES, null, values);
        Log.d(TAG, "Added audio note with ID: " + id);
        return id;
    }

    public long addAudioNote(String title, String duration, String filePath, String fileName, long fileSize) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Ensure table exists
        if (!tableExists(db, TABLE_AUDIO_NOTES)) {
            createAllTables(db);
        }

        ContentValues values = new ContentValues();
        values.put(AUDIO_TITLE, title);
        values.put(AUDIO_DURATION, duration);
        values.put(AUDIO_FILE_PATH, filePath);
        values.put(AUDIO_FILE_NAME, fileName);
        values.put(AUDIO_FILE_SIZE, fileSize);
        values.put(AUDIO_CREATED_DATE, getCurrentDateTime());

        long id = db.insert(TABLE_AUDIO_NOTES, null, values);
        Log.d(TAG, "Added audio note with file, ID: " + id);
        return id;
    }

    public int updateAudioNote(long id, String title, String duration) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Ensure table exists
        if (!tableExists(db, TABLE_AUDIO_NOTES)) {
            createAllTables(db);
            return 0; // No rows to update if table was just created
        }

        ContentValues values = new ContentValues();
        values.put(AUDIO_TITLE, title);
        values.put(AUDIO_DURATION, duration);

        int result = db.update(TABLE_AUDIO_NOTES, values, AUDIO_ID + " = ?", new String[]{String.valueOf(id)});
        Log.d(TAG, "Updated audio note ID " + id + ", result: " + result);
        return result;
    }

    public void deleteAudioNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Ensure table exists
        if (!tableExists(db, TABLE_AUDIO_NOTES)) {
            Log.w(TAG, "Cannot delete from non-existent table");
            return;
        }

        int result = db.delete(TABLE_AUDIO_NOTES, AUDIO_ID + " = ?", new String[]{String.valueOf(id)});
        Log.d(TAG, "Deleted audio note ID " + id + ", result: " + result);
    }

    // Keep all your existing User and Flashcard methods exactly as they are...
    // [Include all the existing methods from your original DatabaseHelper]

    // User Management Methods
    public long registerUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(USER_NAME, name);
        values.put(USER_EMAIL, email);
        values.put(USER_PASSWORD, password);
        values.put(USER_JOIN_DATE, getCurrentDate());
        values.put(USER_IS_LOGGED_IN, 1);

        return db.insert(TABLE_USERS, null, values);
    }

    public User authenticateUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {USER_ID, USER_NAME, USER_EMAIL, USER_PROFILE_IMAGE_PATH, USER_JOIN_DATE};
        String selection = USER_EMAIL + " = ? AND " + USER_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            User user = new User();
            user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(USER_ID)));
            user.setName(cursor.getString(cursor.getColumnIndexOrThrow(USER_NAME)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(USER_EMAIL)));
            user.setProfileImagePath(cursor.getString(cursor.getColumnIndexOrThrow(USER_PROFILE_IMAGE_PATH)));
            user.setJoinDate(cursor.getString(cursor.getColumnIndexOrThrow(USER_JOIN_DATE)));

            cursor.close();

            // Update login status
            updateUserLoginStatus(user.getId(), true);

            return user;
        }

        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    public boolean isEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {USER_ID};
        String selection = USER_EMAIL + " = ?";
        String[] selectionArgs = {email};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        boolean exists = cursor != null && cursor.getCount() > 0;

        if (cursor != null) {
            cursor.close();
        }

        return exists;
    }

    public User getCurrentLoggedInUser() {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {USER_ID, USER_NAME, USER_EMAIL, USER_PROFILE_IMAGE_PATH, USER_JOIN_DATE};
        String selection = USER_IS_LOGGED_IN + " = ?";
        String[] selectionArgs = {"1"};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            User user = new User();
            user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(USER_ID)));
            user.setName(cursor.getString(cursor.getColumnIndexOrThrow(USER_NAME)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(USER_EMAIL)));
            user.setProfileImagePath(cursor.getString(cursor.getColumnIndexOrThrow(USER_PROFILE_IMAGE_PATH)));
            user.setJoinDate(cursor.getString(cursor.getColumnIndexOrThrow(USER_JOIN_DATE)));

            cursor.close();
            return user;
        }

        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    public void updateUserLoginStatus(long userId, boolean isLoggedIn) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(USER_IS_LOGGED_IN, isLoggedIn ? 1 : 0);

        db.update(TABLE_USERS, values, USER_ID + " = ?", new String[]{String.valueOf(userId)});
    }

    public void logoutAllUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(USER_IS_LOGGED_IN, 0);

        db.update(TABLE_USERS, values, null, null);
    }

    public int updateUserProfile(long userId, String name, String email, String profileImagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(USER_NAME, name);
        values.put(USER_EMAIL, email);
        if (profileImagePath != null && !profileImagePath.isEmpty()) {
            values.put(USER_PROFILE_IMAGE_PATH, profileImagePath);
        }

        return db.update(TABLE_USERS, values, USER_ID + " = ?", new String[]{String.valueOf(userId)});
    }

    public int updateUserPassword(long userId, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(USER_PASSWORD, newPassword);

        return db.update(TABLE_USERS, values, USER_ID + " = ?", new String[]{String.valueOf(userId)});
    }

    // Existing Flashcard methods
    public long addFlashcard(String question, String answer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(FLASHCARD_QUESTION, question);
        values.put(FLASHCARD_ANSWER, answer);
        values.put(FLASHCARD_CREATED_DATE, getCurrentDateTime());

        return db.insert(TABLE_FLASHCARDS, null, values);
    }

    public List<Flashcard> getAllFlashcards() {
        List<Flashcard> flashcards = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_FLASHCARDS, null, null, null, null, null,
                FLASHCARD_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Flashcard flashcard = new Flashcard();
                flashcard.setId(cursor.getLong(cursor.getColumnIndexOrThrow(FLASHCARD_ID)));
                flashcard.setQuestion(cursor.getString(cursor.getColumnIndexOrThrow(FLASHCARD_QUESTION)));
                flashcard.setAnswer(cursor.getString(cursor.getColumnIndexOrThrow(FLASHCARD_ANSWER)));
                flashcard.setCreatedDate(cursor.getString(cursor.getColumnIndexOrThrow(FLASHCARD_CREATED_DATE)));

                flashcards.add(flashcard);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return flashcards;
    }

    public int updateFlashcard(long id, String question, String answer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(FLASHCARD_QUESTION, question);
        values.put(FLASHCARD_ANSWER, answer);

        return db.update(TABLE_FLASHCARDS, values, FLASHCARD_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteFlashcard(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FLASHCARDS, FLASHCARD_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // Utility methods
    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
}