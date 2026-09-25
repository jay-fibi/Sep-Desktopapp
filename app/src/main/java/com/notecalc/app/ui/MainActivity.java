package com.notecalc.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.notecalc.app.R;
import com.notecalc.app.data.NoteItem;
import com.notecalc.app.data.NoteRepository;
import com.notecalc.app.engine.LineResult;
import com.notecalc.app.engine.NoteCalcEngine;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private EditText etNoteTitle;
    private EditText etEditor;
    private TextView tvResultsColumn;
    private TextView tvGrandTotal;
    private TextView tvItemsCount;
    private ScrollView scrollViewEditor;
    private ScrollView scrollViewResults;

    private NoteCalcEngine engine;
    private NoteRepository repository;
    private String currentNoteId = null;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable calculationRunnable;

    private static final String SAMPLE_CONTENT =
            "Monthly Expenses\n" +
            "Apartment Rent: 1200\n" +
            "Electricity & Water: 85 + 45\n" +
            "High-speed Internet: 60\n" +
            "Groceries: 120 * 4\n" +
            "Dining Out: 175\n" +
            "total\n" +
            "\n" +
            "Income & Savings\n" +
            "Salary: 3800\n" +
            "Tax: Salary * 18%\n" +
            "Net Income = Salary - Tax\n" +
            "Savings = Net Income - total";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        engine = new NoteCalcEngine();
        repository = new NoteRepository(this);

        initViews();
        setupSyncScrolling();
        setupEditorWatcher();
        setupQuickToolbar();
        setupActions();

        // Load initial note or sample if clean start
        List<NoteItem> notes = repository.getAllNotes();
        if (!notes.isEmpty()) {
            loadNote(notes.get(0));
        } else {
            loadSampleNote();
        }
    }

    private void initViews() {
        etNoteTitle = findViewById(R.id.etNoteTitle);
        etEditor = findViewById(R.id.etEditor);
        tvResultsColumn = findViewById(R.id.tvResultsColumn);
        tvGrandTotal = findViewById(R.id.tvGrandTotal);
        tvItemsCount = findViewById(R.id.tvItemsCount);
        scrollViewEditor = findViewById(R.id.scrollViewEditor);
        scrollViewResults = findViewById(R.id.scrollViewResults);
    }

    private void setupSyncScrolling() {
        scrollViewEditor.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            scrollViewResults.scrollTo(scrollX, scrollY);
        });
    }

    private void setupEditorWatcher() {
        calculationRunnable = this::recalculate;

        etEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                debounceHandler.removeCallbacks(calculationRunnable);
                debounceHandler.postDelayed(calculationRunnable, 100);
            }
        });
    }

    private void recalculate() {
        String content = etEditor.getText() != null ? etEditor.getText().toString() : "";
        NoteCalcEngine.EvaluationResult result = engine.evaluate(content);

        StringBuilder sb = new StringBuilder();
        int mathLinesCount = 0;

        for (LineResult lr : result.getLineResults()) {
            if (lr.hasResult()) {
                sb.append(lr.getFormattedResult()).append("\n");
                if (lr.getNumericValue() != null) {
                    mathLinesCount++;
                }
            } else {
                sb.append("\n");
            }
        }

        tvResultsColumn.setText(sb.toString());
        tvGrandTotal.setText(result.getFormattedGrandTotal());
        tvItemsCount.setText(mathLinesCount + (mathLinesCount == 1 ? " calculation" : " calculations"));
    }

    private void setupQuickToolbar() {
        bindQuickKey(R.id.btnQuickPlus, " + ");
        bindQuickKey(R.id.btnQuickMinus, " - ");
        bindQuickKey(R.id.btnQuickMult, " * ");
        bindQuickKey(R.id.btnQuickDiv, " / ");
        bindQuickKey(R.id.btnQuickPercent, "%");
        bindQuickKey(R.id.btnQuickEquals, " = ");
        bindQuickKey(R.id.btnQuickColon, ": ");
        bindQuickKey(R.id.btnQuickTotal, "\ntotal\n");
        bindQuickKey(R.id.btnQuickAvg, "\naverage\n");
        bindQuickKey(R.id.btnQuickSqrt, "sqrt()");
    }

    private void bindQuickKey(int buttonId, String textToInsert) {
        Button btn = findViewById(buttonId);
        if (btn != null) {
            btn.setOnClickListener(v -> insertTextAtCursor(textToInsert));
        }
    }

    private void insertTextAtCursor(String text) {
        int start = Math.max(etEditor.getSelectionStart(), 0);
        int end = Math.max(etEditor.getSelectionEnd(), 0);
        etEditor.getText().replace(Math.min(start, end), Math.max(start, end), text, 0, text.length());

        if (text.equals("sqrt()")) {
            etEditor.setSelection(start + 5);
        }
    }

    private void setupActions() {
        ImageButton btnNew = findViewById(R.id.btnNew);
        btnNew.setOnClickListener(v -> createNewNote());

        MaterialButton btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveCurrentNote());

        MaterialButton btnSample = findViewById(R.id.btnSample);
        btnSample.setOnClickListener(v -> loadSampleNote());

        ImageButton btnLibrary = findViewById(R.id.btnLibrary);
        btnLibrary.setOnClickListener(v -> showLibraryDialog());

        ImageButton btnShare = findViewById(R.id.btnShare);
        btnShare.setOnClickListener(v -> shareNoteAndCalculations());
    }

    private void createNewNote() {
        currentNoteId = UUID.randomUUID().toString();
        etNoteTitle.setText("");
        etEditor.setText("");
        etNoteTitle.requestFocus();
        Toast.makeText(this, "New Note started", Toast.LENGTH_SHORT).show();
    }

    private void saveCurrentNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etEditor.getText().toString();

        if (title.isEmpty() && content.trim().isEmpty()) {
            Toast.makeText(this, "Note is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty()) {
            // First line of note as title fallback
            String[] lines = content.split("\n", 2);
            title = lines[0].trim().isEmpty() ? "Untitled Note" : lines[0].trim();
            if (title.length() > 30) {
                title = title.substring(0, 30) + "...";
            }
            etNoteTitle.setText(title);
        }

        if (currentNoteId == null) {
            currentNoteId = UUID.randomUUID().toString();
        }

        NoteItem item = new NoteItem(currentNoteId, title, content, System.currentTimeMillis());
        repository.saveNote(item);
        Toast.makeText(this, "Note saved to Jacks calc!", Toast.LENGTH_SHORT).show();
    }

    private void loadSampleNote() {
        currentNoteId = "sample_note_id";
        etNoteTitle.setText("Monthly Budget Demo");
        etEditor.setText(SAMPLE_CONTENT);
        recalculate();
    }

    private void loadNote(NoteItem item) {
        currentNoteId = item.getId();
        etNoteTitle.setText(item.getTitle());
        etEditor.setText(item.getContent());
        recalculate();
    }

    private void showLibraryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notes_library, null);
        builder.setView(dialogView);

        RecyclerView recycler = dialogView.findViewById(R.id.recyclerNotes);
        TextView tvEmpty = dialogView.findViewById(R.id.tvEmptyNotes);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        List<NoteItem> notes = repository.getAllNotes();
        if (notes.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
        }

        AlertDialog dialog = builder.create();

        NotesAdapter adapter = new NotesAdapter(notes, new NotesAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(NoteItem note) {
                loadNote(note);
                dialog.dismiss();
            }

            @Override
            public void onNoteDelete(NoteItem note) {
                repository.deleteNote(note.getId());
                notes.remove(note);
                if (notes.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recycler.setVisibility(View.GONE);
                }
                recycler.getAdapter().notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
            }
        });

        recycler.setAdapter(adapter);
        dialog.show();
    }

    private void shareNoteAndCalculations() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etEditor.getText().toString();
        String grandTotal = tvGrandTotal.getText().toString();

        NoteCalcEngine.EvaluationResult eval = engine.evaluate(content);
        StringBuilder fullReport = new StringBuilder();
        if (!title.isEmpty()) {
            fullReport.append("=== ").append(title).append(" ===\n\n");
        }
        for (LineResult lr : eval.getLineResults()) {
            fullReport.append(lr.getOriginalText());
            if (lr.hasResult()) {
                fullReport.append("  =>  ").append(lr.getFormattedResult());
            }
            fullReport.append("\n");
        }
        fullReport.append("\n-------------------------\n");
        fullReport.append("TOTAL: ").append(grandTotal).append("\n");
        fullReport.append("Calculated with Jacks calc");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, title.isEmpty() ? "Jacks calc Note" : title);
        intent.putExtra(Intent.EXTRA_TEXT, fullReport.toString());
        startActivity(Intent.createChooser(intent, "Share Note"));
    }
}
