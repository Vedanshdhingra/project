package com.example.project;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private List<Note> notes;
    private NotesAdapter notesAdapter;
    private AppDao dao;
    private View blurOverlay;
    private FrameLayout focusedContainer;
    private RecyclerView rvNotes;
    private TextView btnSecretFilter, btnAllFilter;
    
    // OCR components
    private TextRecognizer textRecognizer;
    private EditText activeEditText;
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Biometric components
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    // Frosted Glass Colors (80% opacity)
    private final int[] noteColors = {
        0xCCFFF9C4, // Yellow
        0xCCE3F2FD, // Blue
        0xCCFCE4EC, // Pink
        0xCCE8F5E9, // Green
        0xCCFFF3E0, // Orange
        0xCCFFEBEE  // Red
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        initOCR();
        initBiometric();
        initViews();
        setupFilters();
        dao = AppDatabase.getInstance(this).appDao();
        setupNotes();
    }

    private void initOCR() {
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null && activeEditText != null) {
                processImage(bitmap);
            }
        });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                cameraLauncher.launch(null);
            } else {
                Toast.makeText(this, "Camera permission required for OCR", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initBiometric() {
        executor = ContextCompat.getMainExecutor(this);
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Project Universe Authentication")
                .setSubtitle("Authenticate to unlock your secret universe")
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void authenticateNote(Note note, int position) {
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                note.sessionUnlocked = true;
                notesAdapter.notifyItemChanged(position);
                showNoteEditMode(note, position);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        biometricPrompt.authenticate(promptInfo);
    }

    private void processImage(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String resultText = visionText.getText();
                    if (!resultText.isEmpty()) {
                        String currentText = activeEditText.getText().toString();
                        activeEditText.setText(currentText + (currentText.isEmpty() ? "" : "\n") + resultText);
                    } else {
                        Toast.makeText(this, "No text detected", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "OCR failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void initViews() {
        blurOverlay = findViewById(R.id.blurOverlay);
        focusedContainer = findViewById(R.id.focusedContainer);
        rvNotes = findViewById(R.id.recyclerViewNotes);
        btnSecretFilter = findViewById(R.id.btnSecretFilter);
        btnAllFilter = findViewById(R.id.btnAllFilter);
        View fingerprintBtn = findViewById(R.id.fingerprintContainer);

        fingerprintBtn.setOnClickListener(v -> createNewNote());
        blurOverlay.setOnClickListener(v -> hideEditMode());
    }

    private void setupFilters() {
        btnSecretFilter.setOnClickListener(v -> {
            notesAdapter.setShowOnlySecret(true);
            btnSecretFilter.setAlpha(1.0f);
            btnAllFilter.setAlpha(0.5f);
        });

        btnAllFilter.setOnClickListener(v -> {
            notesAdapter.setShowOnlySecret(false);
            btnSecretFilter.setAlpha(0.5f);
            btnAllFilter.setAlpha(1.0f);
        });
        
        btnAllFilter.setAlpha(1.0f);
        btnSecretFilter.setAlpha(0.5f);
    }

    private void setupNotes() {
        notes = dao.getAllNotes();
        
        if (notes.isEmpty()) {
            // First time setup - add default notes
            Note n1 = new Note("Secret note", "Top secret mission details.", 0xCCFFEBEE);
            n1.isSecret = true;
            n1.id = (int) dao.insertNote(n1);
            notes.add(n1);

            Note n2 = new Note("Secret note", "Another hidden universe.", 0xCCE3F2FD);
            n2.isSecret = true;
            n2.id = (int) dao.insertNote(n2);
            notes.add(n2);

            Note n3 = new Note("Private Journal", "Personal reflections & daily notes. Today: feeling inspired after the product kickoff. Team morale is high.", 0xCCE3F2FD);
            n3.isSecret = true;
            n3.sessionUnlocked = true;
            n3.id = (int) dao.insertNote(n3);
            notes.add(n3);
        }
        
        notesAdapter = new NotesAdapter(notes, (note, position) -> {
            if (note.isSecret && !note.sessionUnlocked) {
                authenticateNote(note, position);
            } else {
                showNoteEditMode(note, position);
            }
        });
        rvNotes.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rvNotes.setAdapter(notesAdapter);
        notesAdapter.sortAndNotify();
    }

    private void createNewNote() {
        int randomColor = noteColors[new Random().nextInt(noteColors.length)];
        Note newNote = new Note("", "", randomColor);
        newNote.id = (int) dao.insertNote(newNote);
        notes.add(0, newNote);
        notesAdapter.sortAndNotify();
        showNoteEditMode(newNote, 0);
    }

    private void showNoteEditMode(Note note, int position) {
        focusedContainer.removeAllViews();
        View editView = LayoutInflater.from(this).inflate(R.layout.edit_note, focusedContainer, false);
        
        View container = editView.findViewById(R.id.editNoteContainer);
        EditText editTitle = editView.findViewById(R.id.editNoteTitle);
        EditText editContent = editView.findViewById(R.id.editNoteContent);
        Button btnDelete = editView.findViewById(R.id.btnDelete);
        ImageButton btnPin = editView.findViewById(R.id.btnPin);
        ImageButton btnDeadline = editView.findViewById(R.id.btnDeadline);
        ImageButton btnScan = editView.findViewById(R.id.btnScan);
        ImageButton btnLock = editView.findViewById(R.id.btnLock);
        LinearLayout colorPicker = editView.findViewById(R.id.colorPickerContainer);

        container.setBackgroundColor(note.color);
        editTitle.setText(note.title);
        editContent.setText(note.content);
        updatePinButton(btnPin, note.isPinned);
        updateLockButton(btnLock, note.isSecret);

        TextWatcher autoSaveWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                note.title = editTitle.getText().toString();
                note.content = editContent.getText().toString();
                note.lastModified = System.currentTimeMillis();
                dao.updateNote(note);
                notesAdapter.notifyItemChanged(position);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        editTitle.addTextChangedListener(autoSaveWatcher);
        editContent.addTextChangedListener(autoSaveWatcher);

        btnPin.setOnClickListener(v -> {
            note.isPinned = !note.isPinned;
            note.lastModified = System.currentTimeMillis();
            dao.updateNote(note);
            updatePinButton(btnPin, note.isPinned);
            notesAdapter.sortAndNotify();
        });

        btnLock.setOnClickListener(v -> {
            note.isSecret = !note.isSecret;
            note.lastModified = System.currentTimeMillis();
            dao.updateNote(note);
            updateLockButton(btnLock, note.isSecret);
            notesAdapter.notifyItemChanged(position);
        });

        btnDeadline.setOnClickListener(v -> showDateTimePicker((date) -> {
            note.deadline = date;
            note.lastModified = System.currentTimeMillis();
            dao.updateNote(note);
            notesAdapter.sortAndNotify();
        }));

        btnScan.setOnClickListener(v -> {
            activeEditText = editContent;
            checkCameraPermission();
        });

        btnDelete.setOnClickListener(v -> {
            dao.deleteNote(note);
            notes.remove(note);
            notesAdapter.sortAndNotify();
            hideEditMode();
        });

        // Color Picker setup
        for (int color : noteColors) {
            View colorView = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(60, 60);
            params.setMargins(10, 0, 10, 0);
            colorView.setLayoutParams(params);
            colorView.setBackgroundColor(color);
            colorView.setOnClickListener(v -> {
                note.color = color;
                note.lastModified = System.currentTimeMillis();
                dao.updateNote(note);
                container.setBackgroundColor(color);
                notesAdapter.notifyItemChanged(position);
            });
            colorPicker.addView(colorView);
        }

        displayFocusedView(editView);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private interface OnDateSelectedListener {
        void onDateSelected(long date);
    }

    private void showDateTimePicker(OnDateSelectedListener listener) {
        final Calendar currentDate = Calendar.getInstance();
        final Calendar date = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            date.set(year, month, dayOfMonth);
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                date.set(Calendar.MINUTE, minute);
                listener.onDateSelected(date.getTimeInMillis());
            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show();
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }

    private void displayFocusedView(View view) {
        focusedContainer.addView(view);
        blurOverlay.setVisibility(View.VISIBLE);
        focusedContainer.setVisibility(View.VISIBLE);
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        view.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
    }

    private void updatePinButton(ImageButton btn, boolean isPinned) {
        btn.setColorFilter(isPinned ? 0xFFFFD54F : 0xFF757575);
    }

    private void updateLockButton(ImageButton btn, boolean isSecret) {
        btn.setColorFilter(isSecret ? 0xFFF44336 : 0xFF757575);
    }

    private void hideEditMode() {
        blurOverlay.setVisibility(View.GONE);
        focusedContainer.setVisibility(View.GONE);
        focusedContainer.removeAllViews();
    }
}