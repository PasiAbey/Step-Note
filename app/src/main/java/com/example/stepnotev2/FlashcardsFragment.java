package com.example.stepnotev2;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.List;

public class FlashcardsFragment extends Fragment {

    private TextView tvCardContent, tvCardProgress, tvFlashcardCount, tvTapToFlip;
    private LinearLayout flashcardContainer, btnAddFlashcard;
    private LinearLayout flashcardsContainer;

    private DatabaseHelper databaseHelper;
    private List<Flashcard> flashcards;
    private int currentCardIndex = 0;
    private boolean isShowingQuestion = true;
    private boolean isAnimating = false; // Prevent multiple simultaneous animations

    // Gesture detection
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 80;
    private static final int SWIPE_VELOCITY_THRESHOLD = 80;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flashcards, container, false);

        databaseHelper = new DatabaseHelper(getContext());
        loadFlashcardsFromDatabase();

        initViews(view);
        setupGestureDetector();
        setupDynamicFlashcardList();
        updateFlashcardCount();
        setupFlashcardListeners();
        updateFlashcard();

        return view;
    }

    private void loadFlashcardsFromDatabase() {
        flashcards = databaseHelper.getAllFlashcards();

        // Debug logging
        android.util.Log.d("FLASHCARDS_DEBUG", "Total flashcards loaded: " + flashcards.size());
        for (int i = 0; i < flashcards.size(); i++) {
            android.util.Log.d("FLASHCARDS_DEBUG", "Card " + (i+1) + ": " + flashcards.get(i).getQuestion());
        }

        if (flashcards.isEmpty()) {
            Toast.makeText(getContext(), "No flashcards available. Add your first flashcard!", Toast.LENGTH_LONG).show();
        }
    }

    private void initViews(View view) {
        tvCardContent = view.findViewById(R.id.tvCardContent);
        tvCardProgress = view.findViewById(R.id.tvCardProgress);
        tvFlashcardCount = view.findViewById(R.id.tvFlashcardCount);
        tvTapToFlip = view.findViewById(R.id.tvTapToFlip);
        flashcardContainer = view.findViewById(R.id.flashcardContainer);
        btnAddFlashcard = view.findViewById(R.id.btnAddFlashcard);
        flashcardsContainer = view.findViewById(R.id.flashcardsContainer);
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null || isAnimating) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // Check if horizontal swipe is more significant than vertical
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe right - go to previous card
                            onSwipeRight();
                        } else {
                            // Swipe left - go to next card
                            onSwipeLeft();
                        }
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // Handle tap to flip (only if not animating)
                if (!flashcards.isEmpty() && !isAnimating) {
                    flipCard();
                }
                return true;
            }
        });
    }

    private void onSwipeLeft() {
        // Go to next card with loop
        if (flashcards.isEmpty() || isAnimating) {
            if (flashcards.isEmpty()) {
                Toast.makeText(getContext(), "No flashcards available", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (currentCardIndex < flashcards.size() - 1) {
            // Normal next card
            animateCardTransition(true, () -> {
                currentCardIndex++;
                isShowingQuestion = true;
                updateFlashcard();
            });
        } else {
            // At last card, loop to first card
            animateCardTransition(true, () -> {
                currentCardIndex = 0;
                isShowingQuestion = true;
                updateFlashcard();
                showLoopMessage("🔄 Back to first card");
            });
        }
    }

    private void onSwipeRight() {
        // Go to previous card with loop
        if (flashcards.isEmpty() || isAnimating) {
            if (flashcards.isEmpty()) {
                Toast.makeText(getContext(), "No flashcards available", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (currentCardIndex > 0) {
            // Normal previous card
            animateCardTransition(false, () -> {
                currentCardIndex--;
                isShowingQuestion = true;
                updateFlashcard();
            });
        } else {
            // At first card, loop to last card
            animateCardTransition(false, () -> {
                currentCardIndex = flashcards.size() - 1;
                isShowingQuestion = true;
                updateFlashcard();
                showLoopMessage("🔄 Jumped to last card");
            });
        }
    }

    private void showLoopMessage(String message) {
        // Show a subtle indication that we've looped
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void animateCardTransition(boolean isNext, Runnable onComplete) {
        if (isAnimating) return;
        isAnimating = true;

        // Calculate slide distances
        float slideDistance = flashcardContainer.getWidth() * 1.2f;
        float exitX = isNext ? -slideDistance : slideDistance;
        float enterX = isNext ? slideDistance : -slideDistance;

        // Phase 1: Slide out current card with rotation and scale
        flashcardContainer.animate()
                .translationX(exitX)
                .rotation(isNext ? -15f : 15f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    // Update content while card is off-screen
                    onComplete.run();

                    // Reset transformations and position for entry
                    flashcardContainer.setRotation(isNext ? 15f : -15f);
                    flashcardContainer.setScaleX(0.8f);
                    flashcardContainer.setScaleY(0.8f);
                    flashcardContainer.setAlpha(0f);
                    flashcardContainer.setTranslationX(enterX);

                    // Phase 2: Slide in new card with spring effect
                    flashcardContainer.animate()
                            .translationX(0f)
                            .rotation(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(400)
                            .setInterpolator(new OvershootInterpolator(0.8f))
                            .withEndAction(() -> {
                                isAnimating = false;
                            })
                            .start();
                })
                .start();
    }

    private void updateFlashcardAppearance() {
        if (isShowingQuestion) {
            // Blue background, white text for questions
            flashcardContainer.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.flashcard_background_blue));
            tvCardContent.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            tvTapToFlip.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            tvTapToFlip.setText("Tap to see answer");
        } else {
            // White background, blue text for answers (exact border color match)
            flashcardContainer.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.flashcard_background_white));
            tvCardContent.setTextColor(ContextCompat.getColor(getContext(), R.color.flashcard_blue_text)); // Exact border color
            tvTapToFlip.setTextColor(ContextCompat.getColor(getContext(), R.color.flashcard_blue_instruction)); // Lighter for instruction
            tvTapToFlip.setText("Tap to see question");
        }
    }

    private void setupDynamicFlashcardList() {
        if (flashcardsContainer == null) return;

        flashcardsContainer.removeAllViews();

        if (flashcards.isEmpty()) {
            // Show empty state
            TextView emptyView = new TextView(getContext());
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

        // No highlighting - all cards have the same appearance
        view.setBackground(getResources().getDrawable(R.drawable.card_background, null));
        view.setAlpha(1.0f);

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
        // Set touch listener for gesture detection
        flashcardContainer.setOnTouchListener((v, event) -> {
            return gestureDetector.onTouchEvent(event);
        });

        // Add new flashcard
        btnAddFlashcard.setOnClickListener(v -> showAddFlashcardDialog());
    }

    private void flipCard() {
        if (isAnimating) return;
        isAnimating = true;

        // Enhanced flip animation with 3D effect
        flashcardContainer.animate()
                .scaleX(0f)
                .rotationY(90f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    isShowingQuestion = !isShowingQuestion;
                    updateCardContent();
                    updateFlashcardAppearance(); // Update colors when flipping

                    // Flip back with spring effect
                    flashcardContainer.animate()
                            .scaleX(1f)
                            .rotationY(0f)
                            .setDuration(200)
                            .setInterpolator(new OvershootInterpolator(0.5f))
                            .withEndAction(() -> {
                                isAnimating = false;
                            })
                            .start();
                })
                .start();
    }

    private void updateFlashcard() {
        if (flashcards.isEmpty()) {
            tvCardContent.setText("No flashcards available\n\nTap 'Add New Flashcard' to get started!");
            tvCardProgress.setText("0 cards");
            // Set default blue appearance for empty state
            flashcardContainer.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.flashcard_background_blue));
            tvCardContent.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            return;
        }

        updateCardContent();
        updateFlashcardAppearance(); // Update colors based on question/answer state

        // Animate progress text update
        tvCardProgress.animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction(() -> {
                    tvCardProgress.setText("Card " + (currentCardIndex + 1) + " of " + flashcards.size());
                    tvCardProgress.animate()
                            .alpha(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add New Flashcard");

        // Create dialog layout
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // Question input
        EditText questionInput = new EditText(getContext());
        questionInput.setHint("Enter question");
        questionInput.setMinLines(2);
        layout.addView(questionInput);

        // Answer input
        EditText answerInput = new EditText(getContext());
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
                    Toast.makeText(getContext(), "Flashcard added successfully!", Toast.LENGTH_SHORT).show();
                    refreshData();
                    currentCardIndex = 0; // Go to newest card (first in DESC order)
                    isShowingQuestion = true;
                } else {
                    Toast.makeText(getContext(), "Error adding flashcard", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Flashcard click handlers
    public void onFlashcardClick(Flashcard flashcard, int position) {
        if (isAnimating) return; // Prevent action during animation

        // Jump to this flashcard in the main viewer with smooth transition
        if (position != currentCardIndex) {
            boolean isNext = position > currentCardIndex;
            animateCardTransition(isNext, () -> {
                currentCardIndex = position;
                isShowingQuestion = true;
                updateFlashcard();
            });
            Toast.makeText(getContext(), "Jumped to: " + flashcard.getQuestion(), Toast.LENGTH_SHORT).show();
        } else {
            // Same card, just flip it
            flipCard();
        }
    }

    public void onEditClick(Flashcard flashcard, int position) {
        if (isAnimating) return;
        showEditFlashcardDialog(flashcard, position);
    }

    public void onDeleteClick(Flashcard flashcard, int position) {
        if (isAnimating) return;
        showDeleteConfirmationDialog(flashcard, position);
    }

    private void showEditFlashcardDialog(Flashcard flashcard, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Flashcard");

        // Create dialog layout
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // Pre-filled question input
        EditText questionInput = new EditText(getContext());
        questionInput.setHint("Enter question");
        questionInput.setText(flashcard.getQuestion());
        questionInput.setMinLines(2);
        layout.addView(questionInput);

        // Pre-filled answer input
        EditText answerInput = new EditText(getContext());
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
                    Toast.makeText(getContext(), "Flashcard updated successfully!", Toast.LENGTH_SHORT).show();
                    refreshData();
                    // Stay on the same card position
                    currentCardIndex = position;
                    isShowingQuestion = true;
                    updateFlashcard();
                } else {
                    Toast.makeText(getContext(), "Error updating flashcard", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteConfirmationDialog(Flashcard flashcard, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Flashcard")
                .setMessage("Are you sure you want to delete this flashcard?\n\nQ: " + flashcard.getQuestion() + "\nA: " + flashcard.getAnswer())
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseHelper.deleteFlashcard(flashcard.getId());
                    Toast.makeText(getContext(), "Flashcard deleted successfully!", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        if (databaseHelper != null) {
            refreshData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}