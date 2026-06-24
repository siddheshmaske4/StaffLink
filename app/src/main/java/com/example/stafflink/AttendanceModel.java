package com.example.stafflink;

public class AttendanceModel {
    private String employeeName;
    private String status;
    private String checkIn;
    private String checkOut;
    private int overtimeHours;
    private int fullDays;
    private int halfDays;
    private int paidLeaves;

    public AttendanceModel() {}

    public AttendanceModel(String employeeName, String status, String checkIn,
                           String checkOut, int overtimeHours, int fullDays,
                           int halfDays, int paidLeaves) {
        this.employeeName = employeeName;
        this.status = status;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.overtimeHours = overtimeHours;
        this.fullDays = fullDays;
        this.halfDays = halfDays;
        this.paidLeaves = paidLeaves;
    }

    public String getEmployeeName() { return employeeName; }
    public String getStatus() { return status; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public int getOvertimeHours() { return overtimeHours; }
    public int getFullDays() { return fullDays; }
    public int getHalfDays() { return halfDays; }
    public int getPaidLeaves() { return paidLeaves; }
}
