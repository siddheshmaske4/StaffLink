package com.example.stafflink;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class EmployeeModel {
    private String id;
    private String name;
    private String position;
    private String department;
    private long baseSalary;
    private String email;
    private String phone;
    private long finalSalary;
    private long deductions;
    private long overtime;
    private int halfDaysUsed;
    private int fullDaysUsed;
    private int paidLeavesUsed;
    private boolean isExpanded = false;

    public EmployeeModel() {} // Required for Firebase

    public EmployeeModel(String id, String name, String position, String department,
                         long baseSalary, String email, String phone,
                         long finalSalary, long deductions, long overtime,
                         int halfDaysUsed, int fullDaysUsed, int paidLeavesUsed) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.department = department;
        this.baseSalary = baseSalary;
        this.email = email;
        this.phone = phone;
        this.finalSalary = finalSalary;
        this.deductions = deductions;
        this.overtime = overtime;
        this.halfDaysUsed = halfDaysUsed;
        this.fullDaysUsed = fullDaysUsed;
        this.paidLeavesUsed = paidLeavesUsed;
    }

    // ✅ ADD THESE SETTERS
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPosition() { return position; }
    public String getDepartment() { return department; }
    public long getBaseSalary() { return baseSalary; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public long getFinalSalary() { return finalSalary; }
    public long getDeductions() { return deductions; }
    public long getOvertime() { return overtime; }
    public int getHalfDaysUsed() { return halfDaysUsed; }
    public int getFullDaysUsed() { return fullDaysUsed; }
    public int getPaidLeavesUsed() { return paidLeavesUsed; }

    // Expandable
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { this.isExpanded = expanded; }
}
