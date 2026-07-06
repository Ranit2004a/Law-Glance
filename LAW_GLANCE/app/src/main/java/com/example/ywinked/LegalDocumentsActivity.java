package com.example.ywinked;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class LegalDocumentsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal_documents);

        ImageView backBtn = findViewById(R.id.ic_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        findViewById(R.id.cardRentAgreement).setOnClickListener(v -> openGenerator("rent"));
        findViewById(R.id.cardAffidavit).setOnClickListener(v -> openGenerator("affidavit"));
        findViewById(R.id.cardNDA).setOnClickListener(v -> openGenerator("nda"));
        findViewById(R.id.cardEmployment).setOnClickListener(v -> openGenerator("employment"));
        findViewById(R.id.cardLoan).setOnClickListener(v -> openGenerator("loan"));
        findViewById(R.id.cardPartnership).setOnClickListener(v -> openGenerator("partnership"));
        findViewById(R.id.cardService).setOnClickListener(v -> openGenerator("service"));
        findViewById(R.id.cardPowerOfAttorney).setOnClickListener(v -> openGenerator("power_of_attorney"));
        findViewById(R.id.cardWill).setOnClickListener(v -> openGenerator("will"));
        findViewById(R.id.cardLegalNotice).setOnClickListener(v -> openGenerator("legal_notice"));
    }

    private void openGenerator(String docType) {
        Intent intent = new Intent(this, DocumentGeneratorActivity.class);
        intent.putExtra("doc_type", docType);
        startActivity(intent);
    }
}
