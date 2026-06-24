package com.example.stafflink;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * MessageModel — simple mail/announcement model.
 *
 * @IgnoreExtraProperties is important: old leftover "task" type messages
 * in Firebase have extra fields (deadline, priority, progress, etc.) from
 * the previous hybrid mail/task system. This annotation makes Firebase
 * silently ignore those extra fields instead of crashing.
 *
 * Firebase path:
 *   companies/{companyCode}/employees/{empId}/messages/{msgId}
 *   Stafflink/admins/{adminId}/messages/{msgId}  (admin's sent copy)
 */
@IgnoreExtraProperties
public class MessageModel {

    public String  messageId;  // set manually after fetch = snapshot.getKey()
    public String  title;
    public String  body;
    public long    createdAt;
    public boolean isRead;
    public String  type;       // "announcement"
    public Sender  sender;
    public String recipients; // comma-separated employee names

    /** Required empty constructor for Firebase */
    public MessageModel() {}

    public MessageModel(String title, String body, long createdAt, boolean isRead) {
        this.title     = title;
        this.body      = body;
        this.createdAt = createdAt;
        this.isRead    = isRead;
    }

    @IgnoreExtraProperties
    public static class Sender {
        public String id;
        public String role;
        public String name; // optional

        public Sender() {}

        public Sender(String id, String role, String name) {
            this.id   = id;
            this.role = role;
            this.name = name;
        }
    }
}