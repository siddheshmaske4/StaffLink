package com.example.stafflink;

import java.util.List;

public class DateAttendanceModel {
    private String date;
    private List<AttendanceModel> employees;

    public DateAttendanceModel(String date, List<AttendanceModel> employees) {
        this.date = date;
        this.employees = employees;
    }

    public String getDate() { return date; }
    public List<AttendanceModel> getEmployees() { return employees; }
}
