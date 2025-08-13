package com.example.stepnotev2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.FlashcardViewHolder> {

    private List<Flashcard> flashcardList;
    private OnFlashcardClickListener listener;

    // Interface for click handling
    public interface OnFlashcardClickListener {
        void onFlashcardClick(Flashcard flashcard, int position);
        void onFlashcardEdit(Flashcard flashcard, int position);
        void onFlashcardDelete(Flashcard flashcard, int position);
    }

    public FlashcardAdapter(List<Flashcard> flashcardList) {
        this.flashcardList = flashcardList;
    }

    public void setOnFlashcardClickListener(OnFlashcardClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FlashcardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flashcard, parent, false);
        return new FlashcardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlashcardViewHolder holder, int position) {
        Flashcard flashcard = flashcardList.get(position);

        // Set card number
        holder.tvCardNumber.setText("#" + (position + 1));

        // Set question (front text)
        holder.tvQuestion.setText(flashcard.getFrontText());

        // Set answer (back text)
        holder.tvAnswer.setText(flashcard.getBackText());

        // Set created date
        if (flashcard.getCreatedAt() != null && !flashcard.getCreatedAt().isEmpty()) {
            // Extract just the date part if it's a full datetime string
            String dateOnly = flashcard.getCreatedAt();
            if (dateOnly.contains(" ")) {
                dateOnly = dateOnly.split(" ")[0]; // Get just the date part
            }
            holder.tvCreatedDate.setText(dateOnly);
        } else {
            holder.tvCreatedDate.setText("2025-08-13");
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFlashcardClick(flashcard, position);
            }
        });

        // Edit button click
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFlashcardEdit(flashcard, position);
            }
        });

        // Delete button click
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFlashcardDelete(flashcard, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return flashcardList != null ? flashcardList.size() : 0;
    }

    // Method to update the list
    public void updateFlashcards(List<Flashcard> newFlashcards) {
        this.flashcardList = newFlashcards;
        notifyDataSetChanged();
    }

    // Method to add a flashcard
    public void addFlashcard(Flashcard flashcard) {
        flashcardList.add(0, flashcard); // Add to top
        notifyItemInserted(0);
        // Update card numbers for all items
        notifyItemRangeChanged(0, flashcardList.size());
    }

    // Method to remove a flashcard
    public void removeFlashcard(int position) {
        if (position >= 0 && position < flashcardList.size()) {
            flashcardList.remove(position);
            notifyItemRemoved(position);
            // Update card numbers for remaining items
            notifyItemRangeChanged(position, flashcardList.size() - position);
        }
    }

    // Method to update a flashcard
    public void updateFlashcard(int position, Flashcard updatedFlashcard) {
        if (position >= 0 && position < flashcardList.size()) {
            flashcardList.set(position, updatedFlashcard);
            notifyItemChanged(position);
        }
    }

    // Method to get flashcard at position
    public Flashcard getFlashcard(int position) {
        if (position >= 0 && position < flashcardList.size()) {
            return flashcardList.get(position);
        }
        return null;
    }

    // ViewHolder class matching your layout
    public static class FlashcardViewHolder extends RecyclerView.ViewHolder {
        TextView tvCardNumber;
        TextView tvCreatedDate;
        TextView tvQuestion;
        TextView tvAnswer;
        TextView btnEdit;
        TextView btnDelete;

        public FlashcardViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views matching your layout IDs
            tvCardNumber = itemView.findViewById(R.id.tvCardNumber);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvAnswer = itemView.findViewById(R.id.tvAnswer);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}