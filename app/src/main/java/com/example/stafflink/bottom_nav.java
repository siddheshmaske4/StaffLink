package com.example.stafflink;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class bottom_nav extends AppCompatActivity {

    private BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_nav);

        nav = findViewById(R.id.bottomNavigation);

        loadFragment(new DashboardFragment());

        nav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;

            if (item.getItemId() == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (item.getItemId() == R.id.nav_payroll) {
                fragment = new PayrollFragment();
            } else if (item.getItemId() == R.id.nav_mail) {
                fragment = new MailFragment();
            } else if (item.getItemId() == R.id.nav_profile) {
                fragment = new EmployeeDetailActivity();
            } else if (item.getItemId() == R.id.nav_tasks) {
                fragment = new TaskFragment();
            }

            return loadFragment(fragment);
        });
    }


    @Override
    public void onBackPressed() {

        if (nav.getSelectedItemId() != R.id.nav_dashboard) {

            nav.setSelectedItemId(R.id.nav_dashboard);

        } else {

            super.onBackPressed();

        }
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}