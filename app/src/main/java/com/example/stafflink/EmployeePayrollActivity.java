package com.example.stafflink;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

public class EmployeePayrollActivity extends AppCompatActivity {

    private Spinner spinnerMonth;
    private TextView txtBaseSalary, txtPresentDays, txtLeaves, txtOvertime, txtPfEsi, txtFinalSalary;
    private Button btnDownload;

    private DatabaseReference payrollRef;
    private String companyCode, empId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_payroll);

        // Bind views
        spinnerMonth = findViewById(R.id.spinnerMonth);
        txtBaseSalary = findViewById(R.id.txtBaseSalary);
        txtPresentDays = findViewById(R.id.txtPresentDays);
        txtLeaves = findViewById(R.id.txtLeaves);
        txtOvertime = findViewById(R.id.txtOvertime);
        txtPfEsi = findViewById(R.id.txtPfEsi);
        txtFinalSalary = findViewById(R.id.txtFinalSalary);
        btnDownload = findViewById(R.id.btnDownloadPayslip);

        // Load session
        SharedPreferences sp = getSharedPreferences("StafflinkPrefs", MODE_PRIVATE);
        companyCode = sp.getString("company_code", null);
        empId = sp.getString("emp_id", null);

        if (companyCode == null || empId == null) {
            Toast.makeText(this, "Session expired, please login again!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Setup payroll reference
        payrollRef = FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyCode)
                .child("employees")
                .child(empId)
                .child("payroll")
                .child("calculated");

        // Populate month spinner (last 12 months)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, getLast12Months());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(adapter);

        // Load payroll for selected month
        spinnerMonth.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String month = parent.getItemAtPosition(position).toString();
                loadPayroll(month);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Download payslip
        btnDownload.setOnClickListener(v -> {
            String month = spinnerMonth.getSelectedItem().toString();
            generatePayslipPDF(month);
        });
    }

    // Fetch payroll from Firebase
    private void loadPayroll(String month) {
        payrollRef.child(month).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) {
                    txtBaseSalary.setText("Base Salary: ₹0");
                    txtPresentDays.setText("Present Days: 0");
                    txtLeaves.setText("Leaves: 0");
                    txtOvertime.setText("Overtime: 0 hrs");
                    txtPfEsi.setText("PF + ESI: ₹0");
                    txtFinalSalary.setText("Final Salary: ₹0");
                    return;
                }

                int base = snap.child("baseSalary").getValue(Integer.class) != null ? snap.child("baseSalary").getValue(Integer.class) : 0;
                int present = snap.child("presentDays").getValue(Integer.class) != null ? snap.child("presentDays").getValue(Integer.class) : 0;
                int fullLeaves = snap.child("fullLeaves").getValue(Integer.class) != null ? snap.child("fullLeaves").getValue(Integer.class) : 0;
                int halfLeaves = snap.child("halfDays").getValue(Integer.class) != null ? snap.child("halfDays").getValue(Integer.class) : 0;
                int overtimeHrs = snap.child("overtimeHours").getValue(Integer.class) != null ? snap.child("overtimeHours").getValue(Integer.class) : 0;
                int pf = snap.child("pfDeducted").getValue(Integer.class) != null ? snap.child("pfDeducted").getValue(Integer.class) : 0;
                int esi = snap.child("esiDeducted").getValue(Integer.class) != null ? snap.child("esiDeducted").getValue(Integer.class) : 0;
                int finalSalary = snap.child("finalSalary").getValue(Integer.class) != null ? snap.child("finalSalary").getValue(Integer.class) : 0;

                txtBaseSalary.setText("Base Salary: ₹" + base);
                txtPresentDays.setText("Present Days: " + present);
                txtLeaves.setText("Leaves: Full " + fullLeaves + ", Half " + halfLeaves);
                txtOvertime.setText("Overtime: " + overtimeHrs + " hrs");
                txtPfEsi.setText("PF + ESI: ₹" + (pf + esi));
                txtFinalSalary.setText("Final Salary: ₹" + finalSalary);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Generate PDF
    private void generatePayslipPDF(String month) {
        try {
            PdfDocument pdf = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();

            paint.setTextSize(14);
            canvas.drawText("PAYSLIP - " + month, 80, 40, paint);

            paint.setTextSize(12);
            canvas.drawText(txtBaseSalary.getText().toString(), 20, 80, paint);
            canvas.drawText(txtPresentDays.getText().toString(), 20, 110, paint);
            canvas.drawText(txtLeaves.getText().toString(), 20, 140, paint);
            canvas.drawText(txtOvertime.getText().toString(), 20, 170, paint);
            canvas.drawText(txtPfEsi.getText().toString(), 20, 200, paint);

            paint.setTextSize(14);
            canvas.drawText(txtFinalSalary.getText().toString(), 20, 250, paint);

            pdf.finishPage(page);

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Payslip_" + month + ".pdf");

            pdf.writeTo(new FileOutputStream(file));
            pdf.close();

            Toast.makeText(this, "Payslip downloaded", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Helper to get last 12 months for spinner
    private String[] getLast12Months() {
        String[] months = new String[12];
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 12; i++) {
            months[i] = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
            cal.add(Calendar.MONTH, -1);
        }
        return months;
    }
}
