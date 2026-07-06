package com.example.ywinked;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DocumentGeneratorActivity extends AppCompatActivity {

    private String docType;
    private EditText inputPartyA, inputPartyB, inputAddress, inputRentValue;
    private Button btnGenerate, btnCopy, btnShare;
    private TextView txtDocTitle, txtGeneratedDoc;
    private LinearLayout layoutResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_generator);

        docType = getIntent().getStringExtra("doc_type");
        if (docType == null) docType = "rent";

        ImageView backBtn = findViewById(R.id.ic_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        txtDocTitle = findViewById(R.id.txtDocTitle);
        inputPartyA = findViewById(R.id.inputPartyA);
        inputPartyB = findViewById(R.id.inputPartyB);
        inputAddress = findViewById(R.id.inputAddress);
        inputRentValue = findViewById(R.id.inputRentValue);
        btnGenerate = findViewById(R.id.btnGenerate);
        layoutResult = findViewById(R.id.layoutResult);
        txtGeneratedDoc = findViewById(R.id.txtGeneratedDoc);
        btnCopy = findViewById(R.id.btnCopy);
        btnShare = findViewById(R.id.btnShare);

        setupUI();

        btnGenerate.setOnClickListener(v -> generateDraft());

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Legal Draft", txtGeneratedDoc.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Draft copied to clipboard!", Toast.LENGTH_SHORT).show();
        });

        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, txtDocTitle.getText().toString());
            intent.putExtra(Intent.EXTRA_TEXT, txtGeneratedDoc.getText().toString());
            startActivity(Intent.createChooser(intent, "Share Draft via"));
        });
    }

    private void setupUI() {
        switch (docType) {
            case "rent":
                txtDocTitle.setText("Rent Agreement Generator");
                inputPartyA.setHint("Landlord Name (Party A)");
                inputPartyB.setHint("Tenant Name (Party B)");
                inputAddress.setHint("Rented Property Address");
                inputRentValue.setHint("Monthly Rent Amount (e.g. ₹15,000)");
                break;
            case "affidavit":
                txtDocTitle.setText("Affidavit Generator");
                inputPartyA.setHint("Declarant Name (Party A)");
                inputPartyB.setHint("Father's/Spouse Name");
                inputAddress.setHint("Address of Declarant");
                inputRentValue.setHint("Declaration Purpose (e.g. Name Correction)");
                break;
            case "nda":
                txtDocTitle.setText("NDA Generator");
                inputPartyA.setHint("Disclosing Party Name (Party A)");
                inputPartyB.setHint("Receiving Party Name (Party B)");
                inputAddress.setHint("Governing Jurisdiction (e.g. New Delhi)");
                inputRentValue.setHint("Confidential Information Subject");
                break;
            case "employment":
                txtDocTitle.setText("Employment Agreement Generator");
                inputPartyA.setHint("Employer Company Name (Party A)");
                inputPartyB.setHint("Employee Name (Party B)");
                inputAddress.setHint("Workplace Address / State");
                inputRentValue.setHint("Job Title & Monthly Salary (e.g. Developer, ₹60,000)");
                break;
            case "loan":
                txtDocTitle.setText("Loan Agreement Generator");
                inputPartyA.setHint("Lender Name (Party A)");
                inputPartyB.setHint("Borrower Name (Party B)");
                inputAddress.setHint("Governing State / Address");
                inputRentValue.setHint("Loan Amount & Interest Rate (e.g. ₹50,000 at 5%)");
                break;
            case "partnership":
                txtDocTitle.setText("Partnership Agreement Generator");
                inputPartyA.setHint("Partner A Name (Party A)");
                inputPartyB.setHint("Partner B Name (Party B)");
                inputAddress.setHint("Business Head Office Address");
                inputRentValue.setHint("Profit Sharing Ratio (e.g. 50:50)");
                break;
            case "service":
                txtDocTitle.setText("Service Agreement Generator");
                inputPartyA.setHint("Service Provider Name (Party A)");
                inputPartyB.setHint("Client Name (Party B)");
                inputAddress.setHint("Service Location / Address");
                inputRentValue.setHint("Scope of Service & Fee (e.g. Web Design for ₹25,000)");
                break;
            case "power_of_attorney":
                txtDocTitle.setText("Power of Attorney Generator");
                inputPartyA.setHint("Principal Name (Party A)");
                inputPartyB.setHint("Agent/Attorney Name (Party B)");
                inputAddress.setHint("Principal's Residential Address");
                inputRentValue.setHint("Authorized Powers (e.g. Property Management)");
                break;
            case "will":
                txtDocTitle.setText("Last Will & Testament Generator");
                inputPartyA.setHint("Testator Name (Party A)");
                inputPartyB.setHint("Executor/Beneficiary Name (Party B)");
                inputAddress.setHint("Testator's Residential Address");
                inputRentValue.setHint("Asset Distribution Details");
                break;
            case "legal_notice":
                txtDocTitle.setText("Legal Notice Generator");
                inputPartyA.setHint("Sender Name (Party A)");
                inputPartyB.setHint("Recipient Name (Party B)");
                inputAddress.setHint("Recipient's Office/Residence Address");
                inputRentValue.setHint("Dispute Subject (e.g. Unpaid Dues of ₹40,000)");
                break;
        }
    }

    private void generateDraft() {
        String pA = inputPartyA.getText().toString().trim();
        String pB = inputPartyB.getText().toString().trim();
        String addr = inputAddress.getText().toString().trim();
        String val = inputRentValue.getText().toString().trim();

        if (pA.isEmpty() || pB.isEmpty() || addr.isEmpty() || val.isEmpty()) {
            Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show();
            return;
        }

        String draft = "";
        switch (docType) {
            case "rent":
                draft = "RENT AGREEMENT\n\n" +
                        "This Rent Agreement is made and executed on this 4th day of July, 2026, at " + addr + " between:\n\n" +
                        "LANDLORD: " + pA + ", residing at " + addr + ", hereinafter called the Party of the First Part.\n" +
                        "AND\n" +
                        "TENANT: " + pB + ", hereinafter called the Party of the Second Part.\n\n" +
                        "WHEREAS the First Part is the absolute owner of the premises located at " + addr + " and has agreed to let out the same to the Second Part for residential purposes.\n\n" +
                        "NOW THIS DEED WITNESSETH AS UNDER:\n" +
                        "1. That the monthly rent of the premises is agreed to be " + val + " per month, excluding electricity and water charges.\n" +
                        "2. That the Tenant shall pay the monthly rent on or before the 5th of each calendar month.\n" +
                        "3. That the tenancy is valid for a period of 11 months starting from today.\n" +
                        "4. That the Tenant shall keep the premises in good and clean condition.\n\n" +
                        "IN WITNESS WHEREOF, the Landlord and Tenant have signed this agreement in presence of witnesses.\n\n" +
                        "First Part (Landlord): ____________________\n" +
                        "Second Part (Tenant):  ____________________\n\n" +
                        "Witness 1: _________________\n" +
                        "Witness 2: _________________";
                break;

            case "affidavit":
                draft = "GENERAL AFFIDAVIT\n\n" +
                        "I, " + pA + ", son/daughter of " + pB + ", residing at " + addr + ", do hereby solemnly affirm and declare on oath as under:\n\n" +
                        "1. That I am a citizen of India and competent to make this declaration.\n" +
                        "2. That I am making this affidavit for the purpose of: " + val + ".\n" +
                        "3. That the facts declared above are true and correct to the best of my knowledge and belief, and nothing material has been concealed therefrom.\n\n" +
                        "DEPONENT: ____________________\n\n" +
                        "VERIFICATION:\n" +
                        "Verified at " + addr + " on this 4th day of July, 2026, that the contents of the above affidavit are true and correct.\n\n" +
                        "DEPONENT: ____________________";
                break;

            case "nda":
                draft = "NON-DISCLOSURE AGREEMENT (NDA)\n\n" +
                        "This Non-Disclosure Agreement is entered into on this 4th day of July, 2026, by and between:\n\n" +
                        "DISCLOSING PARTY: " + pA + ", hereinafter referred to as the First Part.\n" +
                        "AND\n" +
                        "RECEIVING PARTY: " + pB + ", hereinafter referred to as the Second Part.\n\n" +
                        "WHEREAS the First Part intends to disclose confidential information related to: \"" + val + "\" to the Second Part for business discussions.\n\n" +
                        "NOW, THEREFORE, IT IS MUTUALLY AGREED:\n" +
                        "1. The Second Part shall maintain strictly confidential all information disclosed by the First Part.\n" +
                        "2. The Second Part shall not use the confidential information for any purpose other than the evaluation of the business relationship.\n" +
                        "3. This Agreement shall be governed by the laws of " + addr + ".\n\n" +
                        "Disclosing Party (Party A): ____________________\n" +
                        "Receiving Party (Party B):  ____________________\n\n" +
                        "Date: 4th July, 2026";
                break;

            case "employment":
                draft = "EMPLOYMENT AGREEMENT\n\n" +
                        "This Employment Agreement is made and entered into on this 4th day of July, 2026, by and between:\n\n" +
                        "EMPLOYER: " + pA + ", located at " + addr + ", hereinafter referred to as the Employer.\n" +
                        "AND\n" +
                        "EMPLOYEE: " + pB + ", hereinafter referred to as the Employee.\n\n" +
                        "NOW, THEREFORE, IT IS MUTUALLY AGREED AS FOLLOWS:\n" +
                        "1. Position: The Employee is hired for the position of \"" + val + "\".\n" +
                        "2. Workplace: The Employee shall perform duties at " + addr + ".\n" +
                        "3. Compensation: The Employer shall pay the Employee the compensation specified in \"" + val + "\" on or before the 1st of each calendar month.\n" +
                        "4. Termination: Either party may terminate this agreement with a 30-day written notice.\n\n" +
                        "IN WITNESS WHEREOF, the parties hereto have signed this Agreement.\n\n" +
                        "Employer: ____________________\n" +
                        "Employee: ____________________";
                break;

            case "loan":
                draft = "LOAN CONTRACT AGREEMENT\n\n" +
                        "This Loan Contract Agreement is entered into on this 4th day of July, 2026, by and between:\n\n" +
                        "LENDER: " + pA + ", residing at " + addr + ", hereinafter referred to as the Lender.\n" +
                        "AND\n" +
                        "BORROWER: " + pB + ", hereinafter referred to as the Borrower.\n\n" +
                        "NOW THIS AGREED WITNESSETH AS UNDER:\n" +
                        "1. Principal Amount: The Lender agrees to lend the Borrower the principal sum as specified: \"" + val + "\", receipt of which is hereby acknowledged.\n" +
                        "2. Repayment: The Borrower promises to repay the principal sum along with the agreed interest in \"" + val + "\" within 12 months from today.\n" +
                        "3. Default: In case of default, the Lender reserves the right to take legal action under the jurisdiction of " + addr + ".\n\n" +
                        "Lender: ____________________\n" +
                        "Borrower: ____________________";
                break;

            case "partnership":
                draft = "PARTNERSHIP AGREEMENT\n\n" +
                        "This Partnership Agreement is made on this 4th day of July, 2026, by and between:\n\n" +
                        "PARTNER A: " + pA + ", residing at " + addr + ".\n" +
                        "AND\n" +
                        "PARTNER B: " + pB + ", residing at " + addr + ".\n\n" +
                        "The parties agree to enter into partnership on the following terms:\n" +
                        "1. Business Purpose: The partnership business shall be conducted at " + addr + ".\n" +
                        "2. Profit/Loss Sharing: The partners shall share profits and losses in the ratio of: " + val + ".\n" +
                        "3. Management: Both partners have equal rights in the management and operations of the partnership business.\n\n" +
                        "Partner A: ____________________\n" +
                        "Partner B: ____________________";
                break;

            case "service":
                draft = "SERVICE AGREEMENT\n\n" +
                        "This Service Agreement is made on this 4th day of July, 2026, between:\n\n" +
                        "PROVIDER: " + pA + ", operating at " + addr + ".\n" +
                        "AND\n" +
                        "CLIENT: " + pB + ".\n\n" +
                        "NOW IT IS MUTUALLY AGREED AS FOLLOWS:\n" +
                        "1. Services: The Provider agrees to perform the professional services specified as: \"" + val + "\".\n" +
                        "2. Payment: The Client shall pay the Provider the fee specified in \"" + val + "\" upon successful completion of the services.\n" +
                        "3. Dispute Resolution: Any disputes arising under this contract shall be settled under the jurisdiction of " + addr + ".\n\n" +
                        "Provider: ____________________\n" +
                        "Client:   ____________________";
                break;

            case "power_of_attorney":
                draft = "GENERAL POWER OF ATTORNEY\n\n" +
                        "KNOW ALL MEN BY THESE PRESENTS, that I, " + pA + ", residing at " + addr + ", do hereby appoint " + pB + " as my true and lawful Attorney-in-Fact.\n\n" +
                        "My Agent is authorized to act on my behalf to perform all legal acts, specifically: \"" + val + "\".\n" +
                        "I hereby ratify and confirm all acts done by my attorney under this power.\n" +
                        "This Power of Attorney shall remain valid until revoked in writing by me.\n\n" +
                        "Principal:       ____________________\n" +
                        "Agent/Attorney: ____________________";
                break;

            case "will":
                draft = "LAST WILL AND TESTAMENT\n\n" +
                        "I, " + pA + ", residing at " + addr + ", being of sound mind, do hereby declare this to be my Last Will and Testament.\n\n" +
                        "1. Revocation: I revoke all prior wills and codicils.\n" +
                        "2. Distribution: I devise and bequeath all my property and assets as follows: \"" + val + "\" to my beneficiary " + pB + ".\n" +
                        "3. Executor: I appoint " + pB + " as the sole Executor of this my Last Will.\n\n" +
                        "Testator:  ____________________\n" +
                        "Witness 1: ____________________\n" +
                        "Witness 2: ____________________";
                break;

            case "legal_notice":
                draft = "FORMAL LEGAL NOTICE\n\n" +
                        "To,\n" +
                        pB + "\n" +
                        addr + "\n\n" +
                        "Under instructions from my client " + pA + ", I hereby serve you with this formal Legal Notice:\n" +
                        "1. That you have failed to resolve the dispute regarding: \"" + val + "\".\n" +
                        "2. You are hereby called upon to settle this matter or pay the dues of \"" + val + "\" within 15 days of receipt of this notice.\n" +
                        "3. Failing which, my client will be compelled to initiate civil/criminal proceedings against you at your risk and cost.\n\n" +
                        "Sender (Party A):               ____________________\n" +
                        "Advocate on behalf of Sender: ____________________";
                break;
        }

        txtGeneratedDoc.setText(draft);
        layoutResult.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Draft generated successfully!", Toast.LENGTH_SHORT).show();
    }
}
