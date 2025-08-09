package com.example.stepnotev2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.FlashcardViewHolder> {

    private List<Flashcard> flashcards;
    private Context context;
    private OnFlashcardClickListener listener;

    public interface OnFlashcardClickListener {
        void onFlashcardClick(Flashcard flashcard, int position);
        void onEditClick(Flashcard flashcard, int position);
        void onDeleteClick(Flashcard flashcard, int position);
    }

    public FlashcardAdapter(Context context, List<Flashcard> flashcards) {
        this.context = context;
        this.flashcards = flashcards;
    }

    public void setOnFlashcardClickListener(OnFlashcardClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FlashcardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_flashcard, parent, false);
        return new FlashcardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlashcardViewHolder holder, int position) {
        Flashcard flashcard = flashcards.get(position);

        // Bind data
        holder.tvCardNumber.setText("#" + (position + 1));
        holder.tvQuestion.setText(flashcard.getQuestion());
        holder.tvAnswer.setText(flashcard.getAnswer());

        // Format date (show only date part)
        String createdDate = flashcard.getCreatedDate();
        if (createdDate != null && createdDate.contains(" ")) {
            createdDate = createdDate.split(" ")[0]; // Get only date part
        }
        holder.tvCreatedDate.setText(createdDate);

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFlashcardClick(flashcard, position);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(flashcard, position);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(flashcard, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return flashcards.size();
    }

    public void updateFlashcards(List<Flashcard> newFlashcards) {
        this.flashcards = newFlashcards;
        notifyDataSetChanged();
    }

    public static class FlashcardViewHolder extends RecyclerView.ViewHolder {
        TextView tvCardNumber, tvQuestion, tvAnswer, tvCreatedDate, btnEdit, btnDelete;

        public FlashcardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCardNumber = itemView.findViewById(R.id.tvCardNumber);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvAnswer = itemView.findViewById(R.id.tvAnswer);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}