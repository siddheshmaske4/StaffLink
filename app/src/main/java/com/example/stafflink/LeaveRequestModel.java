package com.example.stafflink;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * LeaveRequestModel — represents a leave request message.
 *
 * Firebase paths:
 *   Stafflink/admins/{adminKey}/inbox/{msgId}                          (admin view)
 *   companies/{companyCode}/employees/{empId}/messages/{msgId}         (employee history)
 */
@IgnoreExtraProperties
public class LeaveRequestModel {

    public String  messageId;     // set manually = snapshot.getKey()
    public String  title;
    public String  body;
    public String  leaveType;     // "Full Day" | "Half Day"
    public String  employeeEmail; // who submitted (shown in admin view)
    public long    createdAt;
    public boolean isRead;
    public String  type;          // "leave"
    public Sender  sender;

    public LeaveRequestModel() {}

    @IgnoreExtraProperties
    public static class Sender {
        public String id;
        public String role;

        public Sender() {}
    }
}