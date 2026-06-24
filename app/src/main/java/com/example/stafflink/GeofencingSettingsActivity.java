package com.example.stafflink;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * GeofencingSettingsActivity — Admin sets office location and fencing rules.
 *
 * Firebase write path:
 *   Stafflink/admins/{adminId}/attendanceRules/geofencing/
 *   Stafflink/admins/{adminId}/attendanceRules/networkFencing/
 */
public class GeofencingSettingsActivity extends AppCompatActivity {

    // ─── UI ──────────────────────────────────────────────────────────────────
    private ImageButton btnBack;
    private Switch      switchGeofencing, switchNetworkFencing;
    private EditText    etLatitude, etLongitude, etRadius, etSSID;
    private Button      btnUseMyLocation, btnSave;

    // ─── Data ─────────────────────────────────────────────────────────────────
    private String adminKey;
    private DatabaseReference rulesRef;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geofencing_settings);

        // Session
        SharedPreferences sp = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);
        adminKey = sp.getString("adminNode", null);

        if (adminKey == null) {
            Toast.makeText(this, "Session error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rulesRef = FirebaseDatabase.getInstance()
                .getReference("Stafflink")
                .child("admins")
                .child(adminKey)
                .child("attendanceRules");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        bindViews();
        loadExistingRules();
        setupListeners();
    }

    private void bindViews() {
        btnBack             = findViewById(R.id.btnBack);
        switchGeofencing    = findViewById(R.id.switchGeofencing);
        switchNetworkFencing = findViewById(R.id.switchNetworkFencing);
        etLatitude          = findViewById(R.id.etLatitude);
        etLongitude         = findViewById(R.id.etLongitude);
        etRadius            = findViewById(R.id.etRadius);
        etSSID              = findViewById(R.id.etSSID);
        btnUseMyLocation    = findViewById(R.id.btnUseMyLocation);
        btnSave             = findViewById(R.id.btnSave);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnUseMyLocation.setOnClickListener(v -> fetchCurrentLocation());

        btnSave.setOnClickListener(v -> saveRules());
    }

    // =========================================================================
    // LOAD existing rules from Firebase
    // =========================================================================

    private void loadExistingRules() {
        rulesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Geofencing
                DataSnapshot geo = snapshot.child("geofencing");
                if (geo.exists()) {
                    Boolean geoEnabled = geo.child("enabled").getValue(Boolean.class);
                    switchGeofencing.setChecked(geoEnabled != null && geoEnabled);

                    Double lat = geo.child("latitude").getValue(Double.class);
                    Double lng = geo.child("longitude").getValue(Double.class);
                    Long radius = geo.child("radiusMeters").getValue(Long.class);

                    if (lat != null) etLatitude.setText(String.valueOf(lat));
                    if (lng != null) etLongitude.setText(String.valueOf(lng));
                    if (radius != null) etRadius.setText(String.valueOf(radius));
                }

                // Network fencing
                DataSnapshot net = snapshot.child("networkFencing");
                if (net.exists()) {
                    Boolean netEnabled = net.child("enabled").getValue(Boolean.class);
                    switchNetworkFencing.setChecked(netEnabled != null && netEnabled);

                    String ssid = net.child("allowedSSID").getValue(String.class);
                    if (ssid != null) etSSID.setText(ssid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GeofencingSettingsActivity.this,
                        "Failed to load rules", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================================
    // GET current location and fill lat/lng fields
    // =========================================================================

    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        etLatitude.setText(String.valueOf(location.getLatitude()));
                        etLongitude.setText(String.valueOf(location.getLongitude()));
                        Toast.makeText(this, "Location fetched ✓", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "Could not get location. Make sure GPS is on.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // SAVE rules to Firebase
    // =========================================================================

    private void saveRules() {
        // ── Geofencing validation ─────────────────────────────────────────────
        if (switchGeofencing.isChecked()) {
            String latStr    = etLatitude.getText().toString().trim();
            String lngStr    = etLongitude.getText().toString().trim();
            String radiusStr = etRadius.getText().toString().trim();

            if (latStr.isEmpty() || lngStr.isEmpty()) {
                Toast.makeText(this, "Enter office latitude and longitude", Toast.LENGTH_SHORT).show();
                return;
            }
            if (radiusStr.isEmpty()) {
                Toast.makeText(this, "Enter allowed radius in meters", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // ── Network fencing validation ────────────────────────────────────────
        if (switchNetworkFencing.isChecked()) {
            if (etSSID.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Enter office WiFi name (SSID)", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // ── Build geofencing map ──────────────────────────────────────────────
        Map<String, Object> geoMap = new HashMap<>();
        geoMap.put("enabled", switchGeofencing.isChecked());
        if (switchGeofencing.isChecked()) {
            geoMap.put("latitude",     Double.parseDouble(etLatitude.getText().toString().trim()));
            geoMap.put("longitude",    Double.parseDouble(etLongitude.getText().toString().trim()));
            geoMap.put("radiusMeters", Integer.parseInt(etRadius.getText().toString().trim()));
        }

        // ── Build network fencing map ─────────────────────────────────────────
        Map<String, Object> netMap = new HashMap<>();
        netMap.put("enabled", switchNetworkFencing.isChecked());
        if (switchNetworkFencing.isChecked()) {
            netMap.put("allowedSSID", etSSID.getText().toString().trim());
        }

        // ── Write to Firebase ─────────────────────────────────────────────────
        rulesRef.child("geofencing").setValue(geoMap);
        rulesRef.child("networkFencing").setValue(netMap)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Rules saved successfully ✓", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save rules", Toast.LENGTH_SHORT).show());
    }
}