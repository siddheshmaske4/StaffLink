    package com.example.stafflink;

    import android.Manifest;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.pm.PackageManager;
    import android.location.Location;
    import android.net.wifi.WifiInfo;
    import android.net.wifi.WifiManager;
    import android.os.Bundle;
    import android.view.*;
    import android.widget.*;

    import androidx.annotation.NonNull;
    import androidx.core.app.ActivityCompat;
    import androidx.fragment.app.Fragment;

    import com.google.android.gms.location.FusedLocationProviderClient;
    import com.google.android.gms.location.LocationServices;
    import com.google.firebase.database.*;

    import java.text.SimpleDateFormat;
    import java.util.*;

    public class DashboardFragment extends Fragment {

        TextView txtCircle, txtUsername, txtEmail, txtStatus;
        Button btnPunchIn, btnPunchOut;
        ImageButton btnLogout, inboxIcon;

        DatabaseReference attendanceRef;
        String companyCode, empId;
        private String adminNodeKey; // needed to fetch attendanceRules

        FusedLocationProviderClient fusedLocationClient;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        private static final int LOCATION_PERMISSION_REQUEST = 2001;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

            txtCircle   = view.findViewById(R.id.txtCircle);
            txtUsername = view.findViewById(R.id.txtUsername);
            txtEmail    = view.findViewById(R.id.txtEmail);
            txtStatus   = view.findViewById(R.id.txtStatus);
            btnPunchIn  = view.findViewById(R.id.btnPunchIn);
            btnPunchOut = view.findViewById(R.id.btnPunchOut);
            btnLogout   = view.findViewById(R.id.btnLogout);
            inboxIcon   = view.findViewById(R.id.inboxIcon);

            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences("StafflinkPrefs", getContext().MODE_PRIVATE);

            String email = prefs.getString("email", "");
            companyCode  = prefs.getString("company_code", "");
            empId        = prefs.getString("emp_id", "");

            // Get adminNodeKey by finding which admin manages this company
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

            if (!email.isEmpty()) {
                String first = email.substring(0, 1).toUpperCase();
                txtCircle.setText(first);
                txtUsername.setText(first + email.substring(1, email.indexOf("@")));
                txtEmail.setText(email);
            }

            attendanceRef = FirebaseDatabase.getInstance()
                    .getReference("companies")
                    .child(companyCode)
                    .child("employees")
                    .child(empId)
                    .child("attendance");

            btnPunchIn.setOnClickListener(v -> checkFencingAndPunchIn());
            btnPunchOut.setOnClickListener(v -> punchOut());

            btnLogout.setOnClickListener(v -> {
                prefs.edit().clear().apply();
                startActivity(new Intent(getContext(), Employee_page.class));
                requireActivity().finish();
            });

            inboxIcon.setOnClickListener(v ->
                    startActivity(new Intent(getContext(), EmployeeInboxActivity.class)));

            checkTodayStatus();
            return view;
        }


        // =========================================================================
        // PUNCH IN — with fencing check
        // =========================================================================

        private void checkFencingAndPunchIn() {
            // Fetch attendanceRules from admin node
            FirebaseDatabase.getInstance()
                    .getReference("Stafflink")
                    .child("admins")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            // Find admin for this company
                            for (DataSnapshot adminSnap : snapshot.getChildren()) {
                                String adminCompany = adminSnap.child("companyCode")
                                        .getValue(String.class);
                                if (companyCode.equals(adminCompany)) {
                                    DataSnapshot rules = adminSnap.child("attendanceRules");
                                    validateFencingRules(rules);
                                    return;
                                }
                            }
                            // No rules found — allow punch in directly
                            doPunchIn();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Rules fetch failed — allow punch in
                            doPunchIn();
                        }
                    });
        }

        private void validateFencingRules(DataSnapshot rules) {
            if (rules == null || !rules.exists()) {
                doPunchIn(); // no rules set → allow
                return;
            }

            DataSnapshot geoSnap = rules.child("geofencing");
            DataSnapshot netSnap = rules.child("networkFencing");

            boolean geoEnabled = Boolean.TRUE.equals(geoSnap.child("enabled").getValue(Boolean.class));
            boolean netEnabled = Boolean.TRUE.equals(netSnap.child("enabled").getValue(Boolean.class));

            if (!geoEnabled && !netEnabled) {
                doPunchIn(); // both disabled → allow
                return;
            }

            if (netEnabled) {
                // Check WiFi first — synchronous
                String allowedSSID = netSnap.child("allowedSSID").getValue(String.class);
                String currentSSID = getCurrentWifiSSID();

                if (allowedSSID != null && !allowedSSID.isEmpty()) {
                    // Strip quotes Android sometimes adds to SSID
                    String cleanSSID = currentSSID != null
                            ? currentSSID.replace("\"", "") : "";
                    String cleanAllowed = allowedSSID.replace("\"", "");

                    if (!cleanSSID.equals(cleanAllowed)) {
                        Toast.makeText(getContext(),
                                "❌ Not on office WiFi. Connect to: " + cleanAllowed,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }

            if (geoEnabled) {
                // Check GPS location — async
                Double officeLat = geoSnap.child("latitude").getValue(Double.class);
                Double officeLng = geoSnap.child("longitude").getValue(Double.class);
                Long radiusLong  = geoSnap.child("radiusMeters").getValue(Long.class);
                int radius       = radiusLong != null ? radiusLong.intValue() : 100;

                if (officeLat == null || officeLng == null) {
                    doPunchIn(); // no coords set → allow
                    return;
                }

                checkGpsAndPunchIn(officeLat, officeLng, radius);
            } else {
                // Only network fencing was enabled and passed
                doPunchIn();
            }
        }

        private void checkGpsAndPunchIn(double officeLat, double officeLng, int radius) {
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST);
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location == null) {
                            Toast.makeText(getContext(),
                                    "Could not get your location. Enable GPS and try again.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        float[] results = new float[1];
                        Location.distanceBetween(
                                location.getLatitude(), location.getLongitude(),
                                officeLat, officeLng,
                                results
                        );

                        float distanceMeters = results[0];

                        if (distanceMeters <= radius) {
                            doPunchIn(); // ✅ within range
                        } else {
                            int distInt = Math.round(distanceMeters);
                            Toast.makeText(getContext(),
                                    "❌ You are " + distInt + "m away from office.\n" +
                                            "Must be within " + radius + "m to punch in.",
                                    Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Location error. Try again.", Toast.LENGTH_SHORT).show());
        }

        private String getCurrentWifiSSID() {
            try {
                WifiManager wifiManager = (WifiManager) requireContext()
                        .getApplicationContext()
                        .getSystemService(android.content.Context.WIFI_SERVICE);
                if (wifiManager != null && wifiManager.isWifiEnabled()) {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (wifiInfo != null) {
                        return wifiInfo.getSSID();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        // =========================================================================
        // ACTUAL PUNCH IN — called after all checks pass
        // =========================================================================

        private void doPunchIn() {
            String today = dateFormat.format(new Date());
            String time  = timeFormat.format(new Date());

            attendanceRef.child(today).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snap) {
                    if (snap.exists()) {
                        Toast.makeText(getContext(), "Already punched in today", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("checkIn",      time);
                    data.put("status",       "Present");
                    data.put("overtimeHours", 0);

                    attendanceRef.child(today).setValue(data);
                    txtStatus.setText("Status: Present");
                    Toast.makeText(getContext(), "✅ Punch In Successful", Toast.LENGTH_SHORT).show();
                }

                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }


        // =========================================================================
        // PUNCH OUT — unchanged
        // =========================================================================

        private void punchOut() {
            String today = dateFormat.format(new Date());
            String time  = timeFormat.format(new Date());

            attendanceRef.child(today).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snap) {
                    if (!snap.exists()) {
                        Toast.makeText(getContext(), "Punch in first!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snap.child("checkOut").exists()) {
                        Toast.makeText(getContext(), "Already punched out today", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String punchInTime = snap.child("checkIn").getValue(String.class);
                    if (punchInTime == null) return;

                    int workedHours = calculateHours(punchInTime, time);
                    int overtime    = Math.max(0, workedHours - 8);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("checkOut",     time);
                    updates.put("workedHours",  workedHours);
                    updates.put("overtimeHours", overtime);

                    attendanceRef.child(today).updateChildren(updates);
                    Toast.makeText(getContext(), "✅ Punch Out Successful", Toast.LENGTH_SHORT).show();
                }

                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }


        // =========================================================================
        // STATUS CHECK
        // =========================================================================

        private void checkTodayStatus() {
            String today = dateFormat.format(new Date());
            attendanceRef.child(today).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snap) {
                    txtStatus.setText(snap.exists() ? "Status: Present" : "Status: Not Punched In");
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }


        // =========================================================================
        // TIME CALC
        // =========================================================================

        private int calculateHours(String in, String out) {
            try {
                Date inTime  = timeFormat.parse(in);
                Date outTime = timeFormat.parse(out);
                long diff    = outTime.getTime() - inTime.getTime();
                return (int) (diff / (1000 * 60 * 60));
            } catch (Exception e) {
                return 0;
            }
        }
    }