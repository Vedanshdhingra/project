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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private List<Note> notes;
    private List<Task> tasks;
    private NotesAdapter notesAdapter;
    private TasksAdapter tasksAdapter;
    
    private View blurOverlay;
    private FrameLayout focusedContainer;
    private RecyclerView rvNotes, rvTasks;
    private TextView tabNotes, tabTasks;
    private View indicatorNotes, indicatorTasks;
    private boolean isNotesActive = true;
    
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
        setupTabs();
        setupNotes();
        setupTasks();
        setupSearch();
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
                .setTitle("Secret Note Access")
                .setSubtitle("Authenticate to view your secret note")
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void authenticateSecretNote(Note note, int position) {
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
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
        rvTasks = findViewById(R.id.recyclerViewTasks);
        tabNotes = findViewById(R.id.tabNotes);
        tabTasks = findViewById(R.id.tabTasks);
        indicatorNotes = findViewById(R.id.indicatorNotes);
        indicatorTasks = findViewById(R.id.indicatorTasks);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        fabAdd.setOnClickListener(v -> {
            if (isNotesActive) createNewNote();
            else createNewTask();
        });

        blurOverlay.setOnClickListener(v -> hideEditMode());
    }

    private void setupTabs() {
        tabNotes.setOnClickListener(v -> switchTab(true));
        tabTasks.setOnClickListener(v -> switchTab(false));
    }

    private void switchTab(boolean toNotes) {
        isNotesActive = toNotes;
        
        // Typography & Indicators
        tabNotes.setTextColor(toNotes ? 0xFFFFFFFF : 0x40FFFFFF);
        tabTasks.setTextColor(toNotes ? 0x40FFFFFF : 0xFFFFFFFF);
        indicatorNotes.setVisibility(toNotes ? View.VISIBLE : View.INVISIBLE);
        indicatorTasks.setVisibility(toNotes ? View.INVISIBLE : View.VISIBLE);
        
        rvNotes.setVisibility(toNotes ? View.VISIBLE : View.GONE);
        rvTasks.setVisibility(toNotes ? View.GONE : View.VISIBLE);
        
        EditText search = findViewById(R.id.searchEditText);
        search.setHint(toNotes ? "Search notes..." : "Search tasks...");
    }

    private void setupNotes() {
        notes = new ArrayList<>();
        notes.add(new Note("Design System", "Implement glassmorphism theme with deep ocean gradients.", 0xCCE3F2FD));
        notes.add(new Note("Priority Task", "The deadline system now supports visual scaling for urgent items.", 0xCCFFEBEE));
        notes.get(1).deadline = System.currentTimeMillis() + 3600000;
        notes.get(1).isPinned = true;
        
        notesAdapter = new NotesAdapter(notes, (note, position) -> {
            if (note.isSecret) {
                authenticateSecretNote(note, position);
            } else {
                showNoteEditMode(note, position);
            }
        });
        rvNotes.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rvNotes.setAdapter(notesAdapter);
        
        // Enhanced 3D Perspective
        rvNotes.setCameraDistance(15000f);
        rvNotes.setRotationY(-12f);
        rvNotes.setRotationX(6f);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                Collections.swap(notes, fromPos, toPos);
                notesAdapter.notifyItemMoved(fromPos, toPos);
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        }).attachToRecyclerView(rvNotes);
    }

    private void setupTasks() {
        tasks = new ArrayList<>();
        tasks.add(new Task("Check out the new Task feature", "Tasks have deadlines and reminders!", null));
        
        tasksAdapter = new TasksAdapter(tasks, new TasksAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task, int position) {
                showTaskEditMode(task, position);
            }
            @Override
            public void onTaskStatusChanged(Task task, int position) {
                // Handle completion
            }
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(tasksAdapter);
    }

    private void setupSearch() {
        EditText search = findViewById(R.id.searchEditText);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isNotesActive) notesAdapter.filter(s.toString());
                // Simple task filter logic could be added here
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void createNewNote() {
        int randomColor = noteColors[new Random().nextInt(noteColors.length)];
        Note newNote = new Note("", "", randomColor);
        notes.add(0, newNote);
        notesAdapter.sortAndNotify();
        showNoteEditMode(newNote, notes.indexOf(newNote));
    }

    private void createNewTask() {
        Task newTask = new Task("", "", null);
        tasks.add(0, newTask);
        tasksAdapter.notifyItemInserted(0);
        showTaskEditMode(newTask, 0);
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
                notesAdapter.notifyItemChanged(position);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        editTitle.addTextChangedListener(autoSaveWatcher);
        editContent.addTextChangedListener(autoSaveWatcher);

        btnPin.setOnClickListener(v -> {
            note.isPinned = !note.isPinned;
            updatePinButton(btnPin, note.isPinned);
            notesAdapter.sortAndNotify();
        });

        btnLock.setOnClickListener(v -> {
            note.isSecret = !note.isSecret;
            updateLockButton(btnLock, note.isSecret);
            notesAdapter.notifyItemChanged(position);
        });

        btnDeadline.setOnClickListener(v -> showDateTimePicker((date) -> {
            note.deadline = date;
            notesAdapter.sortAndNotify();
        }));

        btnScan.setOnClickListener(v -> {
            activeEditText = editContent;
            checkCameraPermission();
        });

        btnDelete.setOnClickListener(v -> {
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

    private void showTaskEditMode(Task task, int position) {
        focusedContainer.removeAllViews();
        View editView = LayoutInflater.from(this).inflate(R.layout.edit_task, focusedContainer, false);
        
        EditText editTitle = editView.findViewById(R.id.editTaskTitle);
        EditText editDesc = editView.findViewById(R.id.editTaskDescription);
        TextView textDeadline = editView.findViewById(R.id.textDeadlineDisplay);
        ImageButton btnSetDeadline = editView.findViewById(R.id.btnSetDeadline);
        CheckBox checkReminder = editView.findViewById(R.id.checkReminder);
        Button btnDelete = editView.findViewById(R.id.btnDeleteTask);
        Button btnSave = editView.findViewById(R.id.btnSaveTask);

        editTitle.setText(task.title);
        editDesc.setText(task.description);
        checkReminder.setChecked(task.reminderEnabled);
        
        if (task.deadline != null) {
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(task.deadline);
            textDeadline.setText(DateFormat.format("dd MMM, hh:mm a", cal));
        }

        btnSetDeadline.setOnClickListener(v -> showDateTimePicker((date) -> {
            task.deadline = date;
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(date);
            textDeadline.setText(DateFormat.format("dd MMM, hh:mm a", cal));
            tasksAdapter.notifyItemChanged(position);
        }));

        TextWatcher taskAutoSaveWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                task.title = editTitle.getText().toString();
                task.description = editDesc.getText().toString();
                tasksAdapter.notifyItemChanged(position);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        editTitle.addTextChangedListener(taskAutoSaveWatcher);
        editDesc.addTextChangedListener(taskAutoSaveWatcher);

        checkReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.reminderEnabled = isChecked;
            if (isChecked && task.deadline != null) {
                simulateReminder(task);
            }
            tasksAdapter.notifyItemChanged(position);
        });

        btnSave.setOnClickListener(v -> {
            if (task.title.isEmpty()) {
                Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
                return;
            }
            hideEditMode();
        });

        btnDelete.setOnClickListener(v -> {
            tasks.remove(position);
            tasksAdapter.notifyItemRemoved(position);
            hideEditMode();
        });

        displayFocusedView(editView);
    }

    private void simulateReminder(Task task) {
        long reminderTime = task.deadline - (60 * 60 * 1000);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(reminderTime);
        String timeStr = DateFormat.format("hh:mm a", cal).toString();
        Toast.makeText(this, "Reminder set for " + timeStr + " (1h before deadline)", Toast.LENGTH_LONG).show();
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