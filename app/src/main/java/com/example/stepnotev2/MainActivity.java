package com.example.stepnotev2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Navigation clicks
        findViewById(R.id.navHome).setOnClickListener(v ->
                Toast.makeText(this, "You're already on Home!", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.navFlashcards).setOnClickListener(v -> {
            startActivity(new Intent(this, FlashcardsActivity.class));
        });

        findViewById(R.id.navAudioNotes).setOnClickListener(v ->
                Toast.makeText(this, "Audio Notes - Coming Soon!", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.navProfile).setOnClickListener(v ->
                Toast.makeText(this, "Profile - Coming Soon!", Toast.LENGTH_SHORT).show()
        );

        // Section clicks
        findViewById(R.id.sectionFlashcards).setOnClickListener(v -> {
            startActivity(new Intent(this, FlashcardsActivity.class));
        });

        findViewById(R.id.sectionAudioNotes).setOnClickListener(v ->
                Toast.makeText(this, "Audio Notes - Coming Soon!", Toast.LENGTH_SHORT).show()
        );
    }
}