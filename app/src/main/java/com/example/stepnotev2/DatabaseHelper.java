package com.example.stepnotev2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database info
    private static final String DATABASE_NAME = "stepnote.db";
    private static final int DATABASE_VERSION = 4; // Updated version for new changes

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_FLASHCARDS = "flashcards";
    private static final String TABLE_AUDIO_NOTES = "audio_notes";
    private static final String TABLE_STEPS = "daily_steps";
    private static final String TABLE_USER_STATS = "user_stats";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createUsersTable(db);
        createFlashcardsTable(db);
        createAudioNotesTable(db);
        createStepsTable(db);
        createUserStatsTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Add new tables if they don't exist
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_STEPS + "("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "user_id INTEGER,"
                    + "date TEXT,"
                    + "step_count INTEGER DEFAULT 0,"
                    + "goal INTEGER DEFAULT 10000,"
                    + "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id))");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USER_STATS + "("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "user_id INTEGER UNIQUE,"
                    + "first_use_date TEXT,"
                    + "total_steps INTEGER DEFAULT 0,"
                    + "total_days INTEGER DEFAULT 0,"
                    + "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id))");
        }

        if (oldVersion < 4) {
            // Add join_date column to existing users table if it doesn't exist
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN join_date TEXT");
                // Update existing users with their created_at date as join_date
                db.execSQL("UPDATE " + TABLE_USERS + " SET join_date = created_at WHERE join_date IS NULL");
            } catch (Exception e) {
                // Column might already exist, ignore
            }
        }
    }

    // ===== TABLE CREATION METHODS =====

    private void createUsersTable(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "email TEXT UNIQUE NOT NULL,"
                + "password TEXT NOT NULL,"
                + "profile_image_path TEXT,"
                + "is_logged_in INTEGER DEFAULT 0,"
                + "created_at TEXT,"
                + "join_date TEXT"  // Added join_date column
                + ")";
        db.execSQL(CREATE_USERS_TABLE);
    }

    private void createFlashcardsTable(SQLiteDatabase db) {
        String CREATE_FLASHCARDS_TABLE = "CREATE TABLE " + TABLE_FLASHCARDS + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER,"
                + "front_text TEXT NOT NULL,"
                + "back_text TEXT NOT NULL,"
                + "created_at TEXT,"
                + "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id)"
                + ")";
        db.execSQL(CREATE_FLASHCARDS_TABLE);
    }

    private void createAudioNotesTable(SQLiteDatabase db) {
        String CREATE_AUDIO_NOTES_TABLE = "CREATE TABLE " + TABLE_AUDIO_NOTES + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER,"
                + "title TEXT NOT NULL,"
                + "file_path TEXT NOT NULL,"
                + "duration TEXT DEFAULT '00:00',"
                + "created_at TEXT,"
                + "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id)"
                + ")";
        db.execSQL(CREATE_AUDIO_NOTES_TABLE);
    }

    private void createStepsTable(SQLiteDatabase db) {
        String CREATE_STEPS_TABLE = "CREATE TABLE " + TABLE_STEPS + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER,"
                + "date TEXT,"
                + "step_count INTEGER DEFAULT 0,"
                + "goal INTEGER DEFAULT 10000,"
                + "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id)"
                + ")";
        db.execSQL(CREATE_STEPS_TABLE);
    }

    private void createUserStatsTable(SQLiteDatabase db) {
        String CREATE_USER_STATS_TABLE = "CREATE TABLE " + TABLE_USER_STATS + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER UNIQUE,"
                + "first_use_date TEXT,"
                + "total_steps INTEGER DEFAULT 0,"
                + "total_days INTEGER DEFAULT 0,"
                + "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id)"
                + ")";
        db.execSQL(CREATE_USER_STATS_TABLE);
    }

    // ===== USER METHODS =====

    public long addUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);

        String currentDateTime = getCurrentDateTime();
        values.put("created_at", currentDateTime);
        values.put("join_date", currentDateTime); // Set join date when user registers

        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result;
    }

    public User loginUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        // First, set all users as logged out
        ContentValues logoutValues = new ContentValues();
        logoutValues.put("is_logged_in", 0);
        db.update(TABLE_USERS, logoutValues, null, null);

        // Check credentials
        Cursor cursor = db.query(TABLE_USERS, null,
                "email = ? AND password = ?",
                new String[]{email, password},
                null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = new User();
            user.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            user.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
            user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow("password"))); // Add password for ProfileFragment

            String profilePath = cursor.getString(cursor.getColumnIndexOrThrow("profile_image_path"));
            user.setProfileImagePath(profilePath);

            // Get join date with fallback
            int joinDateIndex = cursor.getColumnIndex("join_date");
            if (joinDateIndex != -1) {
                String joinDate = cursor.getString(joinDateIndex);
                user.setJoinDate(joinDate != null ? joinDate : cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
            } else {
                user.setJoinDate(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
            }

            // Set user as logged in
            ContentValues loginValues = new ContentValues();
            loginValues.put("is_logged_in", 1);
            db.update(TABLE_USERS, loginValues, "id = ?", new String[]{String.valueOf(user.getId())});
        }

        cursor.close();
        db.close();
        return user;
    }

    public User getCurrentLoggedInUser() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_USERS, null,
                "is_logged_in = 1",
                null, null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = new User();
            user.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            user.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
            user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow("password"))); // Add password for ProfileFragment

            String profilePath = cursor.getString(cursor.getColumnIndexOrThrow("profile_image_path"));
            user.setProfileImagePath(profilePath);

            // Get join date with fallback
            int joinDateIndex = cursor.getColumnIndex("join_date");
            if (joinDateIndex != -1) {
                String joinDate = cursor.getString(joinDateIndex);
                user.setJoinDate(joinDate != null ? joinDate : cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
            } else {
                user.setJoinDate(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
            }
        }

        cursor.close();
        db.close();
        return user;
    }

    public void logoutUser() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_logged_in", 0);
        db.update(TABLE_USERS, values, null, null);
        db.close();
    }

    // Updated for ProfileFragment compatibility
    public int updateUserProfile(int userId, String name, String email, String profileImagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        if (profileImagePath != null) {
            values.put("profile_image_path", profileImagePath);
        }

        int rowsAffected = db.update(TABLE_USERS, values, "id = ?", new String[]{String.valueOf(userId)});
        db.close();
        return rowsAffected;
    }

    // Overloaded method for backward compatibility
    public boolean updateUserProfile(int userId, String name, String profileImagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        if (profileImagePath != null) {
            values.put("profile_image_path", profileImagePath);
        }

        int rowsAffected = db.update(TABLE_USERS, values, "id = ?", new String[]{String.valueOf(userId)});
        db.close();
        return rowsAffected > 0;
    }

    // New method for ProfileFragment password update
    public int updateUserPassword(int userId, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", newPassword);

        int rowsAffected = db.update(TABLE_USERS, values, "id = ?", new String[]{String.valueOf(userId)});
        db.close();
        return rowsAffected;
    }

    // New method for ProfileFragment login status update
    public void updateUserLoginStatus(int userId, boolean isLoggedIn) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_logged_in", isLoggedIn ? 1 : 0);
        db.update(TABLE_USERS, values, "id = ?", new String[]{String.valueOf(userId)});
        db.close();
    }

    // Renamed method for ProfileFragment compatibility
    public boolean isEmailExists(String email) {
        return emailExists(email);
    }

    public boolean emailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                "email = ?", new String[]{email},
                null, null, null);

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // ===== FLASHCARD METHODS =====

    public long addFlashcard(int userId, String frontText, String backText) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("front_text", frontText);
        values.put("back_text", backText);
        values.put("created_at", getCurrentDateTime());

        long result = db.insert(TABLE_FLASHCARDS, null, values);
        db.close();
        return result;
    }

    public List<Flashcard> getUserFlashcards(int userId) {
        List<Flashcard> flashcards = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_FLASHCARDS, null,
                "user_id = ?",
                new String[]{String.valueOf(userId)},
                null, null, "created_at DESC");

        if (cursor.moveToFirst()) {
            do {
                Flashcard flashcard = new Flashcard();
                flashcard.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                flashcard.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
                flashcard.setFrontText(cursor.getString(cursor.getColumnIndexOrThrow("front_text")));
                flashcard.setBackText(cursor.getString(cursor.getColumnIndexOrThrow("back_text")));
                flashcard.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                flashcards.add(flashcard);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return flashcards;
    }

    // New method for ProfileFragment statistics
    public int getUserFlashcardsCount(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_FLASHCARDS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }

    public boolean updateFlashcard(int flashcardId, String frontText, String backText) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("front_text", frontText);
        values.put("back_text", backText);

        int rowsAffected = db.update(TABLE_FLASHCARDS, values, "id = ?", new String[]{String.valueOf(flashcardId)});
        db.close();
        return rowsAffected > 0;
    }

    public boolean deleteFlashcard(int flashcardId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_FLASHCARDS, "id = ?", new String[]{String.valueOf(flashcardId)});
        db.close();
        return rowsDeleted > 0;
    }

    // ===== AUDIO NOTES METHODS =====

    public long addAudioNote(int userId, String title, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("title", title);
        values.put("file_path", filePath);
        values.put("duration", "00:00");
        values.put("created_at", getCurrentDateTime());

        long result = db.insert(TABLE_AUDIO_NOTES, null, values);
        db.close();
        return result;
    }

    public List<AudioNote> getUserAudioNotes(int userId) {
        List<AudioNote> audioNotes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_AUDIO_NOTES, null,
                "user_id = ?",
                new String[]{String.valueOf(userId)},
                null, null, "created_at DESC");

        if (cursor.moveToFirst()) {
            do {
                AudioNote audioNote = new AudioNote();
                audioNote.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                audioNote.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
                audioNote.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                audioNote.setFilePath(cursor.getString(cursor.getColumnIndexOrThrow("file_path")));
                audioNote.setDuration(cursor.getString(cursor.getColumnIndexOrThrow("duration")));
                audioNote.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                audioNotes.add(audioNote);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return audioNotes;
    }

    // New method for ProfileFragment statistics
    public int getUserAudioNotesCount(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_AUDIO_NOTES + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }

    public boolean deleteAudioNote(int audioNoteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_AUDIO_NOTES, "id = ?", new String[]{String.valueOf(audioNoteId)});
        db.close();
        return rowsDeleted > 0;
    }

    public boolean updateAudioNote(int audioNoteId, String title, String duration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("duration", duration);

        int rowsAffected = db.update(TABLE_AUDIO_NOTES, values, "id = ?", new String[]{String.valueOf(audioNoteId)});
        db.close();
        return rowsAffected > 0;
    }

    // ===== STEP TRACKING METHODS =====

    public void updateTodaySteps(int userId, int steps) {
        SQLiteDatabase db = this.getWritableDatabase();
        String today = getCurrentDate();

        Cursor cursor = db.query(TABLE_STEPS, null,
                "user_id = ? AND date = ?",
                new String[]{String.valueOf(userId), today},
                null, null, null);

        ContentValues values = new ContentValues();
        values.put("step_count", steps);

        if (cursor.moveToFirst()) {
            db.update(TABLE_STEPS, values,
                    "user_id = ? AND date = ?",
                    new String[]{String.valueOf(userId), today});
        } else {
            values.put("user_id", userId);
            values.put("date", today);
            values.put("goal", 10000);
            db.insert(TABLE_STEPS, null, values);
        }

        cursor.close();
        updateUserStats(userId);
        db.close();
    }

    public int getTodaySteps(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = getCurrentDate();

        Cursor cursor = db.query(TABLE_STEPS, new String[]{"step_count"},
                "user_id = ? AND date = ?",
                new String[]{String.valueOf(userId), today},
                null, null, null);

        int steps = 0;
        if (cursor.moveToFirst()) {
            steps = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return steps;
    }

    public int getTodayGoal(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = getCurrentDate();

        Cursor cursor = db.query(TABLE_STEPS, new String[]{"goal"},
                "user_id = ? AND date = ?",
                new String[]{String.valueOf(userId), today},
                null, null, null);

        int goal = 10000;
        if (cursor.moveToFirst()) {
            goal = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return goal;
    }

    private void updateUserStats(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Calculate total steps
        Cursor stepsCursor = db.rawQuery(
                "SELECT SUM(step_count) FROM " + TABLE_STEPS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)});

        int totalSteps = 0;
        if (stepsCursor.moveToFirst()) {
            totalSteps = stepsCursor.getInt(0);
        }
        stepsCursor.close();

        // Calculate total days
        Cursor daysCursor = db.rawQuery(
                "SELECT COUNT(DISTINCT date) FROM " + TABLE_STEPS + " WHERE user_id = ? AND step_count > 0",
                new String[]{String.valueOf(userId)});

        int totalDays = 0;
        if (daysCursor.moveToFirst()) {
            totalDays = daysCursor.getInt(0);
        }
        daysCursor.close();

        // Update or insert user stats
        ContentValues values = new ContentValues();
        values.put("total_steps", totalSteps);
        values.put("total_days", totalDays);

        Cursor userStatsCursor = db.query(TABLE_USER_STATS, null,
                "user_id = ?", new String[]{String.valueOf(userId)},
                null, null, null);

        if (userStatsCursor.moveToFirst()) {
            db.update(TABLE_USER_STATS, values,
                    "user_id = ?", new String[]{String.valueOf(userId)});
        } else {
            values.put("user_id", userId);
            values.put("first_use_date", getCurrentDate());
            db.insert(TABLE_USER_STATS, null, values);
        }

        userStatsCursor.close();
    }

    public UserStats getUserStats(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        UserStats stats = new UserStats();

        Cursor cursor = db.query(TABLE_USER_STATS, null,
                "user_id = ?", new String[]{String.valueOf(userId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            stats.totalSteps = cursor.getInt(cursor.getColumnIndexOrThrow("total_steps"));
            stats.totalDays = cursor.getInt(cursor.getColumnIndexOrThrow("total_days"));
            stats.firstUseDate = cursor.getString(cursor.getColumnIndexOrThrow("first_use_date"));
        }

        cursor.close();
        db.close();
        return stats;
    }

    // ===== UTILITY METHODS =====

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Methods for ProfileFragment compatibility
    public void clearLoggedInUser() {
        logoutUser();
    }

    // Inner class for user statistics
    public static class UserStats {
        public int totalSteps = 0;
        public int totalDays = 0;
        public String firstUseDate = "";
    }
}