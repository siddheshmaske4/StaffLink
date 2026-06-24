package com.example.stafflink;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

public class PayrollFragment extends Fragment {

    private Spinner spinnerMonth;
    private TextView txtBaseSalary, txtPresentDays, txtLeaves, txtOvertime, txtPfEsi, txtFinalSalary;
    private Button btnDownload;

    private DatabaseReference payrollRef;
    private String companyCode, empId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_payroll, container, false);

        // Bind views
        spinnerMonth = view.findViewById(R.id.spinnerMonth);
        txtBaseSalary = view.findViewById(R.id.txtBaseSalary);
        txtPresentDays = view.findViewById(R.id.txtPresentDays);
        txtLeaves = view.findViewById(R.id.txtLeaves);
        txtOvertime = view.findViewById(R.id.txtOvertime);
        txtPfEsi = view.findViewById(R.id.txtPfEsi);
        txtFinalSalary = view.findViewById(R.id.txtFinalSalary);
        btnDownload = view.findViewById(R.id.btnDownloadPayslip);

        // Load session
        SharedPreferences sp = requireActivity()
                .getSharedPreferences("StafflinkPrefs", android.content.Context.MODE_PRIVATE);

        companyCode = sp.getString("company_code", null);
        empId = sp.getString("emp_id", null);

        if (companyCode == null || empId == null) {
            Toast.makeText(getContext(), "Session expired, please login again!", Toast.LENGTH_LONG).show();
            return view;
        }

        payrollRef = FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyCode)
                .child("employees")
                .child(empId)
                .child("payroll")
                .child("calculated");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                getLast12Months()
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(adapter);

        spinnerMonth.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int pos, long id) {
                loadPayroll(parent.getItemAtPosition(pos).toString());
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnDownload.setOnClickListener(v ->
                generatePayslipPDF(spinnerMonth.getSelectedItem().toString())
        );

        return view;
    }

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

                int base = getInt(snap,"baseSalary");
                int present = getInt(snap,"presentDays");
                int fullLeaves = getInt(snap,"fullLeaves");
                int halfLeaves = getInt(snap,"halfDays");
                int overtime = getInt(snap,"overtimeHours");
                int pf = getInt(snap,"pfDeducted");
                int esi = getInt(snap,"esiDeducted");
                int finalSalary = getInt(snap,"finalSalary");

                txtBaseSalary.setText("Base Salary: ₹" + base);
                txtPresentDays.setText("Present Days: " + present);
                txtLeaves.setText("Leaves: Full " + fullLeaves + ", Half " + halfLeaves);
                txtOvertime.setText("Overtime: " + overtime + " hrs");
                txtPfEsi.setText("PF + ESI: ₹" + (pf + esi));
                txtFinalSalary.setText("Final Salary: ₹" + finalSalary);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private int getInt(DataSnapshot snap, String key) {
        Integer v = snap.child(key).getValue(Integer.class);
        return v == null ? 0 : v;
    }

    private void generatePayslipPDF(String month) {
        try {
            PdfDocument pdf = new PdfDocument();
            PdfDocument.Page page = pdf.startPage(
                    new PdfDocument.PageInfo.Builder(300, 600, 1).create()
            );

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

            File file = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Payslip_" + month + ".pdf"
            );

            pdf.writeTo(new FileOutputStream(file));
            pdf.close();

            Toast.makeText(getContext(), "Payslip downloaded", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(getContext(), "PDF failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String[] getLast12Months() {
        String[] months = new String[12];
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 12; i++) {
            months[i] = String.format("%04d-%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1);
            cal.add(Calendar.MONTH, -1);
        }
        return months;
    }
}
