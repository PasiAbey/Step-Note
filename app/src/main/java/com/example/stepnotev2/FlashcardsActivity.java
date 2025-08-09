package com.example.stepnotev2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.EditText;
import java.util.List;

public class FlashcardsActivity extends AppCompatActivity {

    private TextView tvCardContent, tvCardProgress, tvFlashcardCount;
    private LinearLayout flashcardContainer, btnPrevious, btnNext, btnAddFlashcard;
    private LinearLayout flashcardsContainer;

    private DatabaseHelper databaseHelper;
    private List<Flashcard> flashcards;
    private int currentCardIndex = 0;
    private boolean isShowingQuestion = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        databaseHelper = new DatabaseHelper(this);
        loadFlashcardsFromDatabase();

        initViews();
        setupDynamicFlashcardList();
        updateFlashcardCount();
        setupNavigationListeners();
        setupFlashcardListeners();
        updateFlashcard();
    }

    private void loadFlashcardsFromDatabase() {
        flashcards = databaseHelper.getAllFlashcards();

        // Debug logging
        android.util.Log.d("FLASHCARDS_DEBUG", "Total flashcards loaded: " + flashcards.size());
        for (int i = 0; i < flashcards.size(); i++) {
            android.util.Log.d("FLASHCARDS_DEBUG", "Card " + (i+1) + ": " + flashcards.get(i).getQuestion());
        }

        if (flashcards.isEmpty()) {
            Toast.makeText(this, "No flashcards available. Add your first flashcard!", Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {
        tvCardContent = findViewById(R.id.tvCardContent);
        tvCardProgress = findViewById(R.id.tvCardProgress);
        tvFlashcardCount = findViewById(R.id.tvFlashcardCount);
        flashcardContainer = findViewById(R.id.flashcardContainer);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnAddFlashcard = findViewById(R.id.btnAddFlashcard);
        flashcardsContainer = findViewById(R.id.flashcardsContainer);
    }

    private void setupDynamicFlashcardList() {
        if (flashcardsContainer == null) return;

        flashcardsContainer.removeAllViews();

        if (flashcards.isEmpty()) {
            // Show empty state
            TextView emptyView = new TextView(this);
            emptyView.setText("No flashcards yet.\nAdd your first flashcard above!");
            emptyView.setTextColor(getResources().getColor(R.color.text_secondary, null));
            emptyView.setTextSize(16);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setPadding(32, 64, 32, 64);
            flashcardsContainer.addView(emptyView);
            return;
        }

        for (int i = 0; i < flashcards.size(); i++) {
            View cardView = createFlashcardItemView(flashcards.get(i), i);
            flashcardsContainer.addView(cardView);
        }
    }

    private View createFlashcardItemView(Flashcard flashcard, int position) {
        View view = getLayoutInflater().inflate(R.layout.item_flashcard, flashcardsContainer, false);

        TextView tvCardNumber = view.findViewById(R.id.tvCardNumber);
        TextView tvQuestion = view.findViewById(R.id.tvQuestion);
        TextView tvAnswer = view.findViewById(R.id.tvAnswer);
        TextView tvCreatedDate = view.findViewById(R.id.tvCreatedDate);
        TextView btnEdit = view.findViewById(R.id.btnEdit);
        TextView btnDelete = view.findViewById(R.id.btnDelete);

        // Bind data
        tvCardNumber.setText("#" + (position + 1));
        tvQuestion.setText(flashcard.getQuestion());
        tvAnswer.setText(flashcard.getAnswer());

        // Format date to show only date part
        String createdDate = flashcard.getCreatedDate();
        if (createdDate != null && createdDate.contains(" ")) {
            createdDate = createdDate.split(" ")[0];
        }
        tvCreatedDate.setText(createdDate != null ? createdDate : "Unknown");

        // Set click listeners
        view.setOnClickListener(v -> onFlashcardClick(flashcard, position));
        btnEdit.setOnClickListener(v -> onEditClick(flashcard, position));
        btnDelete.setOnClickListener(v -> onDeleteClick(flashcard, position));

        return view;
    }

    private void updateFlashcardCount() {
        if (tvFlashcardCount != null) {
            tvFlashcardCount.setText(flashcards.size() + " cards");
        }
    }

    private void setupFlashcardListeners() {
        // Flip card when tapped
        flashcardContainer.setOnClickListener(v -> {
            if (!flashcards.isEmpty()) {
                flipCard();
            }
        });

        // Previous card
        btnPrevious.setOnClickListener(v -> {
            if (!flashcards.isEmpty() && currentCardIndex > 0) {
                currentCardIndex--;
                isShowingQuestion = true;
                updateFlashcard();
            } else if (flashcards.isEmpty()) {
                Toast.makeText(this, "No flashcards available", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "This is the first card", Toast.LENGTH_SHORT).show();
            }
        });

        // Next card
        btnNext.setOnClickListener(v -> {
            if (!flashcards.isEmpty() && currentCardIndex < flashcards.size() - 1) {
                currentCardIndex++;
                isShowingQuestion = true;
                updateFlashcard();
            } else if (flashcards.isEmpty()) {
                Toast.makeText(this, "No flashcards available", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "This is the last card", Toast.LENGTH_SHORT).show();
            }
        });

        // Add new flashcard
        btnAddFlashcard.setOnClickListener(v -> showAddFlashcardDialog());
    }

    private void flipCard() {
        // Animate flip effect
        flashcardContainer.animate()
                .scaleX(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    isShowingQuestion = !isShowingQuestion;
                    updateCardContent();
                    flashcardContainer.animate()
                            .scaleX(1f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    private void updateFlashcard() {
        if (flashcards.isEmpty()) {
            tvCardContent.setText("No flashcards available\n\nTap 'Add New Flashcard' to get started!");
            tvCardProgress.setText("0 cards");
            return;
        }

        updateCardContent();
        tvCardProgress.setText("Card " + (currentCardIndex + 1) + " of " + flashcards.size());
    }

    private void updateCardContent() {
        if (flashcards.isEmpty()) return;

        Flashcard currentCard = flashcards.get(currentCardIndex);
        if (isShowingQuestion) {
            tvCardContent.setText(currentCard.getQuestion());
        } else {
            tvCardContent.setText(currentCard.getAnswer());
        }
    }

    private void refreshData() {
        loadFlashcardsFromDatabase();
        setupDynamicFlashcardList();
        updateFlashcardCount();

        // Adjust current card index if needed
        if (currentCardIndex >= flashcards.size() && !flashcards.isEmpty()) {
            currentCardIndex = flashcards.size() - 1;
        } else if (flashcards.isEmpty()) {
            currentCardIndex = 0;
        }

        updateFlashcard();
    }

    private void showAddFlashcardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Flashcard");

        // Create dialog layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // Question input
        EditText questionInput = new EditText(this);
        questionInput.setHint("Enter question");
        questionInput.setMinLines(2);
        layout.addView(questionInput);

        // Answer input
        EditText answerInput = new EditText(this);
        answerInput.setHint("Enter answer");
        answerInput.setMinLines(2);
        answerInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) answerInput.getLayoutParams()).topMargin = 20;
        layout.addView(answerInput);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String question = questionInput.getText().toString().trim();
            String answer = answerInput.getText().toString().trim();

            if (!question.isEmpty() && !answer.isEmpty()) {
                long id = databaseHelper.addFlashcard(question, answer);
                if (id != -1) {
                    Toast.makeText(this, "Flashcard added successfully!", Toast.LENGTH_SHORT).show();
                    refreshData();
                    currentCardIndex = 0; // Go to newest card (first in DESC order)
                    isShowingQuestion = true;
                } else {
                    Toast.makeText(this, "Error adding flashcard", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Flashcard click handlers
    public void onFlashcardClick(Flashcard flashcard, int position) {
        // Jump to this flashcard in the main viewer
        currentCardIndex = position;
        isShowingQuestion = true;
        updateFlashcard();

        // Scroll to top to show the main flashcard viewer
        findViewById(R.id.flashcardContainer).requestFocus();
        Toast.makeText(this, "Jumped to: " + flashcard.getQuestion(), Toast.LENGTH_SHORT).show();
    }

    public void onEditClick(Flashcard flashcard, int position) {
        showEditFlashcardDialog(flashcard, position);
    }

    public void onDeleteClick(Flashcard flashcard, int position) {
        showDeleteConfirmationDialog(flashcard, position);
    }

    private void showEditFlashcardDialog(Flashcard flashcard, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Flashcard");

        // Create dialog layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // Pre-filled question input
        EditText questionInput = new EditText(this);
        questionInput.setHint("Enter question");
        questionInput.setText(flashcard.getQuestion());
        questionInput.setMinLines(2);
        layout.addView(questionInput);

        // Pre-filled answer input
        EditText answerInput = new EditText(this);
        answerInput.setHint("Enter answer");
        answerInput.setText(flashcard.getAnswer());
        answerInput.setMinLines(2);
        answerInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) answerInput.getLayoutParams()).topMargin = 20;
        layout.addView(answerInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String question = questionInput.getText().toString().trim();
            String answer = answerInput.getText().toString().trim();

            if (!question.isEmpty() && !answer.isEmpty()) {
                int result = databaseHelper.updateFlashcard(flashcard.getId(), question, answer);
                if (result > 0) {
                    Toast.makeText(this, "Flashcard updated successfully!", Toast.LENGTH_SHORT).show();
                    refreshData();
                    // Stay on the same card position
                    currentCardIndex = position;
                    isShowingQuestion = true;
                    updateFlashcard();
                } else {
                    Toast.makeText(this, "Error updating flashcard", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteConfirmationDialog(Flashcard flashcard, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Flashcard")
                .setMessage("Are you sure you want to delete this flashcard?\n\nQ: " + flashcard.getQuestion() + "\nA: " + flashcard.getAnswer())
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseHelper.deleteFlashcard(flashcard.getId());
                    Toast.makeText(this, "Flashcard deleted successfully!", Toast.LENGTH_SHORT).show();
                    refreshData();

                    // Adjust current card index after deletion
                    if (flashcards.isEmpty()) {
                        currentCardIndex = 0;
                    } else if (currentCardIndex >= flashcards.size()) {
                        currentCardIndex = flashcards.size() - 1;
                    }
                    updateFlashcard();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupNavigationListeners() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.navFlashcards).setOnClickListener(v ->
                Toast.makeText(this, "You're already on Flashcards!", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.navAudioNotes).setOnClickListener(v ->
                Toast.makeText(this, "Audio Notes - Coming Soon!", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.navProfile).setOnClickListener(v ->
                Toast.makeText(this, "Profile - Coming Soon!", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        refreshData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}