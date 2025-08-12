package com.example.stepnotev2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private TextView tvWelcomeMessage;
    private LinearLayout sectionFlashcards, sectionAudioNotes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupInteractions();
        setupWelcomeMessage();

        return view;
    }

    private void initViews(View view) {
        tvWelcomeMessage = view.findViewById(R.id.tvWelcomeMessage);
        sectionFlashcards = view.findViewById(R.id.sectionFlashcards);
        sectionAudioNotes = view.findViewById(R.id.sectionAudioNotes);
    }

    private void setupInteractions() {
        // Flashcards section click
        sectionFlashcards.setOnClickListener(v -> {
            // Navigate to flashcards (switch to flashcards tab)
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToFlashcards();
            }
        });

        // Audio Notes section click
        sectionAudioNotes.setOnClickListener(v -> {
            // Navigate to audio notes (switch to audio notes tab)
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToAudioNotes();
            }
        });
    }

    private void setupWelcomeMessage() {
        // You can customize this based on user data
        String userName = "PasiAbey"; // Replace with actual user name
        tvWelcomeMessage.setText("Welcome back, " + userName + "!");
    }
}