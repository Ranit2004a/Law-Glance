package com.example.ywinked;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.List;

public class LegalDocumentsActivity extends AppCompatActivity {

    // Holds all document entries for filtering
    private static class DocEntry {
        CardView card;
        String title;
        String docType;
        DocEntry(CardView card, String title, String docType) {
            this.card = card; this.title = title; this.docType = docType;
        }
    }

    private final List<DocEntry> allDocs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal_documents);

        // Back button
        ImageView backBtn = findViewById(R.id.ic_back);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        // Register all document cards with their titles and types
        allDocs.add(new DocEntry(findViewById(R.id.cardEmployment),      "Employment Agreement",     "employment"));
        allDocs.add(new DocEntry(findViewById(R.id.cardLoan),            "Loan Agreement",           "loan"));
        allDocs.add(new DocEntry(findViewById(R.id.cardPartnership),     "Partnership Agreement",    "partnership"));
        allDocs.add(new DocEntry(findViewById(R.id.cardService),         "Service Agreement",        "service"));
        allDocs.add(new DocEntry(findViewById(R.id.cardPowerOfAttorney), "Power of Attorney",        "power_of_attorney"));
        allDocs.add(new DocEntry(findViewById(R.id.cardWill),            "Last Will & Testament",    "will"));
        allDocs.add(new DocEntry(findViewById(R.id.cardLegalNotice),     "Legal Notice",             "legal_notice"));
        allDocs.add(new DocEntry(findViewById(R.id.cardRentAgreement),   "Rent / Lease Agreement",   "rent"));
        allDocs.add(new DocEntry(findViewById(R.id.cardAffidavit),       "General Affidavit",        "affidavit"));
        allDocs.add(new DocEntry(findViewById(R.id.cardNDA),             "Non-Disclosure Agreement", "nda"));

        // Set click listeners for each card
        for (DocEntry doc : allDocs) {
            if (doc.card != null) {
                final String type = doc.docType;
                doc.card.setOnClickListener(v -> openGenerator(type));
            }
        }

        // Search bar — real-time filtering
        EditText searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDocs(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Also trigger search on keyboard "Search" action
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            filterDocs(v.getText().toString().trim());
            return true;
        });
    }

    /** Show only cards whose title contains the query (case-insensitive). */
    private void filterDocs(String query) {
        boolean anyVisible = false;
        for (DocEntry doc : allDocs) {
            if (doc.card == null) continue;
            boolean matches = query.isEmpty() ||
                    doc.title.toLowerCase().contains(query.toLowerCase());
            doc.card.setVisibility(matches ? View.VISIBLE : View.GONE);
            if (matches) anyVisible = true;
        }
    }

    private void openGenerator(String docType) {
        Intent intent = new Intent(this, DocumentGeneratorActivity.class);
        intent.putExtra("doc_type", docType);
        startActivity(intent);
    }
}
