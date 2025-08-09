package com.example.stepnotev2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "stepnote_v2.db";
    private static final int DATABASE_VERSION = 1;

    // Flashcards table
    private static final String TABLE_FLASHCARDS = "flashcards";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_QUESTION = "question";
    private static final String COLUMN_ANSWER = "answer";
    private static final String COLUMN_CREATED_DATE = "created_date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create flashcards table
        String createFlashcardsTable = "CREATE TABLE " + TABLE_FLASHCARDS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_QUESTION + " TEXT NOT NULL,"
                + COLUMN_ANSWER + " TEXT NOT NULL,"
                + COLUMN_CREATED_DATE + " TEXT NOT NULL"
                + ")";
        db.execSQL(createFlashcardsTable);

        // Insert sample data
        insertSampleData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FLASHCARDS);
        onCreate(db);
    }

    private void insertSampleData(SQLiteDatabase db) {
        String currentDate = getCurrentDateTime();

        insertSampleFlashcard(db, "What is the capital of France?", "Paris", currentDate);
        insertSampleFlashcard(db, "What is 2 + 2?", "4", currentDate);
        insertSampleFlashcard(db, "What is the largest planet?", "Jupiter", currentDate);
        insertSampleFlashcard(db, "What year did World War II end?", "1945", currentDate);
        insertSampleFlashcard(db, "What is the chemical symbol for water?", "H2O", currentDate);
    }

    private void insertSampleFlashcard(SQLiteDatabase db, String question, String answer, String date) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUESTION, question);
        values.put(COLUMN_ANSWER, answer);
        values.put(COLUMN_CREATED_DATE, date);
        db.insert(TABLE_FLASHCARDS, null, values);
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Add new flashcard
    public long addFlashcard(String question, String answer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_QUESTION, question);
        values.put(COLUMN_ANSWER, answer);
        values.put(COLUMN_CREATED_DATE, getCurrentDateTime());

        long id = db.insert(TABLE_FLASHCARDS, null, values);
        db.close();
        return id;
    }

    // Get all flashcards
    public List<Flashcard> getAllFlashcards() {
        List<Flashcard> flashcards = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_FLASHCARDS + " ORDER BY " + COLUMN_ID + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Flashcard flashcard = new Flashcard();
                flashcard.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                flashcard.setQuestion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUESTION)));
                flashcard.setAnswer(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANSWER)));
                flashcard.setCreatedDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_DATE)));
                flashcards.add(flashcard);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return flashcards;
    }

    // Get flashcard count
    public int getFlashcardCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_FLASHCARDS, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    // Delete flashcard
    public void deleteFlashcard(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FLASHCARDS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Update flashcard
    public int updateFlashcard(int id, String question, String answer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUESTION, question);
        values.put(COLUMN_ANSWER, answer);

        int result = db.update(TABLE_FLASHCARDS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    // Get single flashcard
    public Flashcard getFlashcard(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FLASHCARDS,
                new String[]{COLUMN_ID, COLUMN_QUESTION, COLUMN_ANSWER, COLUMN_CREATED_DATE},
                COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Flashcard flashcard = new Flashcard(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUESTION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANSWER)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_DATE))
            );
            cursor.close();
            db.close();
            return flashcard;
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return null;
    }
}