package com.example.stepnotev2;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class FlashcardsFragment extends Fragment {

    private static final String TAG = "FlashcardsFragment";

    // UI Components (matching your XML)
    private LinearLayout flashcardContainer;
    private TextView tvCardContent;
    private TextView tvTapToFlip;
    private TextView tvCardProgress;
    private LinearLayout btnAddFlashcard;
    private TextView tvFlashcardCount;
    private LinearLayout flashcardsContainer;

    // Flashcard Study Mode
    private List<Flashcard> flashcardsList;
    private int currentCardIndex = 0;
    private boolean isShowingFront = true;
    private GestureDetector gestureDetector;

    // Database
    private DatabaseHelper databaseHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flashcards, container, false);

        databaseHelper = new DatabaseHelper(getContext());
        flashcardsList = new ArrayList<>();

        initViews(view);
        setupGestureDetector();
        loadFlashcards();

        return view;
    }

    private void initViews(View view) {
        // Initialize views matching your XML IDs
        flashcardContainer = view.findViewById(R.id.flashcardContainer);
        tvCardContent = view.findViewById(R.id.tvCardContent);
        tvTapToFlip = view.findViewById(R.id.tvTapToFlip);
        tvCardProgress = view.findViewById(R.id.tvCardProgress);
        btnAddFlashcard = view.findViewById(R.id.btnAddFlashcard);
        tvFlashcardCount = view.findViewById(R.id.tvFlashcardCount);
        flashcardsContainer = view.findViewById(R.id.flashcardsContainer);

        // Set click listeners
        flashcardContainer.setOnClickListener(v -> flipCard());
        btnAddFlashcard.setOnClickListener(v -> showAddFlashcardDialog());
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();

                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                            if (diffX > 0) {
                                // Swipe right - previous card
                                showPreviousCard();
                            } else {
                                // Swipe left - next card
                                showNextCard();
                            }
                            return true;
                        }
                    }
                } catch (Exception exception) {
                    Log.e(TAG, "Error in gesture detection", exception);
                }
                return false;
            }
        });

        flashcardContainer.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void loadFlashcards() {
        User currentUser = databaseHelper.getCurrentLoggedInUser();
        if (currentUser != null) {
            flashcardsList = databaseHelper.getUserFlashcards((int) currentUser.getId());
            updateFlashcardCount();
            displayCurrentCard();
            displayAllFlashcards();
        }
    }

    private void updateFlashcardCount() {
        int count = flashcardsList.size();
        tvFlashcardCount.setText(count + (count == 1 ? " card" : " cards"));
    }

    private void displayCurrentCard() {
        if (flashcardsList.isEmpty()) {
            // Show empty state
            tvCardContent.setText("No flashcards yet!\nTap 'Add New Flashcard' to get started");
            tvTapToFlip.setText("Create your first flashcard");
            tvCardProgress.setText("0 cards");
            return;
        }

        Flashcard currentCard = flashcardsList.get(currentCardIndex);

        if (isShowingFront) {
            tvCardContent.setText(currentCard.getFrontText());
            tvTapToFlip.setText("Tap to reveal answer");
        } else {
            tvCardContent.setText(currentCard.getBackText());
            tvTapToFlip.setText("Tap to show question");
        }

        // Update progress
        tvCardProgress.setText("Card " + (currentCardIndex + 1) + " of " + flashcardsList.size());
    }

    private void flipCard() {
        if (flashcardsList.isEmpty()) {
            showAddFlashcardDialog();
            return;
        }

        isShowingFront = !isShowingFront;
        displayCurrentCard();
    }

    private void showNextCard() {
        if (flashcardsList.isEmpty()) return;

        currentCardIndex = (currentCardIndex + 1) % flashcardsList.size(); // Loop back to start
        isShowingFront = true; // Always start with front when switching cards
        displayCurrentCard();
    }

    private void showPreviousCard() {
        if (flashcardsList.isEmpty()) return;

        currentCardIndex = (currentCardIndex - 1 + flashcardsList.size()) % flashcardsList.size(); // Loop to end
        isShowingFront = true; // Always start with front when switching cards
        displayCurrentCard();
    }

    private void displayAllFlashcards() {
        flashcardsContainer.removeAllViews();

        if (flashcardsList.isEmpty()) {
            // Show empty state
            TextView emptyText = new TextView(getContext());
            emptyText.setText("No flashcards yet! Create your first flashcard using the button above.");
            emptyText.setTextSize(16);
            emptyText.setTextColor(getResources().getColor(R.color.text_secondary));
            emptyText.setPadding(32, 64, 32, 64);
            emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            flashcardsContainer.addView(emptyText);
            return;
        }

        for (int i = 0; i < flashcardsList.size(); i++) {
            Flashcard flashcard = flashcardsList.get(i);
            View flashcardView = createFlashcardItemView(flashcard, i);
            flashcardsContainer.addView(flashcardView);
        }
    }

    private View createFlashcardItemView(Flashcard flashcard, int position) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_flashcard, flashcardsContainer, false);

        TextView tvCardNumber = view.findViewById(R.id.tvCardNumber);
        TextView tvCreatedDate = view.findViewById(R.id.tvCreatedDate);
        TextView tvQuestion = view.findViewById(R.id.tvQuestion);
        TextView tvAnswer = view.findViewById(R.id.tvAnswer);
        TextView btnEdit = view.findViewById(R.id.btnEdit);
        TextView btnDelete = view.findViewById(R.id.btnDelete);

        // Set data
        tvCardNumber.setText("#" + (position + 1));
        tvQuestion.setText(flashcard.getFrontText());
        tvAnswer.setText(flashcard.getBackText());

        // Set date (extract date part from datetime)
        if (flashcard.getCreatedAt() != null && !flashcard.getCreatedAt().isEmpty()) {
            String dateOnly = flashcard.getCreatedAt();
            if (dateOnly.contains(" ")) {
                dateOnly = dateOnly.split(" ")[0]; // Get just the date part
            }
            tvCreatedDate.setText(dateOnly);
        } else {
            tvCreatedDate.setText("2025-08-13");
        }

        // Set click listeners
        view.setOnClickListener(v -> {
            // Jump to this card in study mode
            currentCardIndex = position;
            isShowingFront = true;
            displayCurrentCard();
            Toast.makeText(getContext(), "Jumped to card " + (position + 1), Toast.LENGTH_SHORT).show();
        });

        btnEdit.setOnClickListener(v -> showEditFlashcardDialog(flashcard, position));
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog(flashcard, position));

        return view;
    }

    private void showAddFlashcardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add New Flashcard");

        // Create custom layout for dialog
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Question input
        TextView questionLabel = new TextView(getContext());
        questionLabel.setText("Question (Front):");
        questionLabel.setTextSize(14);
        questionLabel.setTextColor(getResources().getColor(R.color.text_primary));
        questionLabel.setPadding(0, 0, 0, 8);

        EditText questionInput = new EditText(getContext());
        questionInput.setHint("Enter your question here...");
        questionInput.setMinLines(2);
        questionInput.setMaxLines(4);

        // Answer input
        TextView answerLabel = new TextView(getContext());
        answerLabel.setText("Answer (Back):");
        answerLabel.setTextSize(14);
        answerLabel.setTextColor(getResources().getColor(R.color.text_primary));
        answerLabel.setPadding(0, 24, 0, 8);

        EditText answerInput = new EditText(getContext());
        answerInput.setHint("Enter your answer here...");
        answerInput.setMinLines(2);
        answerInput.setMaxLines(4);

        layout.addView(questionLabel);
        layout.addView(questionInput);
        layout.addView(answerLabel);
        layout.addView(answerInput);

        builder.setView(layout);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String question = questionInput.getText().toString().trim();
            String answer = answerInput.getText().toString().trim();

            if (TextUtils.isEmpty(question)) {
                Toast.makeText(getContext(), "Please enter a question", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(answer)) {
                Toast.makeText(getContext(), "Please enter an answer", Toast.LENGTH_SHORT).show();
                return;
            }

            createFlashcard(question, answer);
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void showEditFlashcardDialog(Flashcard flashcard, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Flashcard");

        // Create custom layout for dialog
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Question input
        TextView questionLabel = new TextView(getContext());
        questionLabel.setText("Question (Front):");
        questionLabel.setTextSize(14);
        questionLabel.setTextColor(getResources().getColor(R.color.text_primary));
        questionLabel.setPadding(0, 0, 0, 8);

        EditText questionInput = new EditText(getContext());
        questionInput.setText(flashcard.getFrontText());
        questionInput.setMinLines(2);
        questionInput.setMaxLines(4);

        // Answer input
        TextView answerLabel = new TextView(getContext());
        answerLabel.setText("Answer (Back):");
        answerLabel.setTextSize(14);
        answerLabel.setTextColor(getResources().getColor(R.color.text_primary));
        answerLabel.setPadding(0, 24, 0, 8);

        EditText answerInput = new EditText(getContext());
        answerInput.setText(flashcard.getBackText());
        answerInput.setMinLines(2);
        answerInput.setMaxLines(4);

        layout.addView(questionLabel);
        layout.addView(questionInput);
        layout.addView(answerLabel);
        layout.addView(answerInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String question = questionInput.getText().toString().trim();
            String answer = answerInput.getText().toString().trim();

            if (TextUtils.isEmpty(question)) {
                Toast.makeText(getContext(), "Please enter a question", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(answer)) {
                Toast.makeText(getContext(), "Please enter an answer", Toast.LENGTH_SHORT).show();
                return;
            }

            updateFlashcard(flashcard.getId(), question, answer, position);
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void showDeleteConfirmDialog(Flashcard flashcard, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Flashcard")
                .setMessage("Are you sure you want to delete this flashcard?\n\nQ: " + flashcard.getFrontText())
                .setPositiveButton("Delete", (dialog, which) -> deleteFlashcard(flashcard.getId(), position))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void createFlashcard(String question, String answer) {
        User currentUser = databaseHelper.getCurrentLoggedInUser();
        if (currentUser != null) {
            long result = databaseHelper.addFlashcard((int) currentUser.getId(), question, answer);

            if (result > 0) {
                Toast.makeText(getContext(), "Flashcard created successfully!", Toast.LENGTH_SHORT).show();
                loadFlashcards(); // Refresh everything

                // If this is the first card, show it immediately
                if (flashcardsList.size() == 1) {
                    currentCardIndex = 0;
                    isShowingFront = true;
                    displayCurrentCard();
                }

                Log.d(TAG, "Flashcard created successfully");
            } else {
                Toast.makeText(getContext(), "Failed to create flashcard", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to create flashcard");
            }
        } else {
            Toast.makeText(getContext(), "Please login to create flashcards", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFlashcard(int flashcardId, String question, String answer, int position) {
        boolean success = databaseHelper.updateFlashcard(flashcardId, question, answer);

        if (success) {
            Toast.makeText(getContext(), "Flashcard updated successfully!", Toast.LENGTH_SHORT).show();
            loadFlashcards(); // Refresh everything

            // Keep current position if we're on the updated card
            if (currentCardIndex == position) {
                displayCurrentCard();
            }

            Log.d(TAG, "Flashcard updated successfully");
        } else {
            Toast.makeText(getContext(), "Failed to update flashcard", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to update flashcard");
        }
    }

    private void deleteFlashcard(int flashcardId, int position) {
        boolean success = databaseHelper.deleteFlashcard(flashcardId);

        if (success) {
            Toast.makeText(getContext(), "Flashcard deleted", Toast.LENGTH_SHORT).show();

            // Adjust current index if needed
            if (currentCardIndex >= position && currentCardIndex > 0) {
                currentCardIndex--;
            }

            loadFlashcards(); // Refresh everything

            // Reset to first card if we deleted the current one
            if (currentCardIndex >= flashcardsList.size() && !flashcardsList.isEmpty()) {
                currentCardIndex = 0;
            }

            displayCurrentCard();
            Log.d(TAG, "Flashcard deleted successfully");
        } else {
            Toast.makeText(getContext(), "Failed to delete flashcard", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to delete flashcard");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFlashcards(); // Refresh when returning to fragment
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}