package com.example.stafflink;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * TaskModel — completely separate from MessageModel/mail system.
 *
 * Firebase path:
 *   companies/{companyCode}/employees/{empId}/tasks/{taskId}
 */
@IgnoreExtraProperties
public class TaskModel {

    // ─── Status constants ─────────────────────────────────────────────────────
    public static final String STATUS_PENDING     = "pending";
    public static final String STATUS_IN_PROGRESS = "in_progress";
    public static final String STATUS_COMPLETED   = "completed";
    public static final String STATUS_APPROVED    = "approved";
    public static final String STATUS_REJECTED    = "rejected";

    // ─── Priority constants ───────────────────────────────────────────────────
    public static final String PRIORITY_HIGH   = "high";
    public static final String PRIORITY_MEDIUM = "medium";
    public static final String PRIORITY_LOW    = "low";

    // ─── Fields ───────────────────────────────────────────────────────────────
    public String  taskId;           // set manually after fetch = snapshot.getKey()
    public String  title;
    public String  description;
    public String  assignedTo;       // empId
    public String  assignedToName;   // employee display name
    public String  assignedBy;       // adminId
    public String  assignedByName;   // admin display name
    public String  deadline;         // "yyyy-MM-dd"
    public String  priority;         // "high" | "medium" | "low"
    public String  status;           // "pending" | "in_progress" | "completed" | "approved" | "rejected"
    public int     progress;         // 0–100
    public boolean isRead;           // employee opened it
    public boolean completionRequested;
    public boolean completionApproved;
    public long    createdAt;
    public long    updatedAt;
    public String workNotes; // employee's work description — proof of work

    // ─── Required empty constructor for Firebase ──────────────────────────────
    public TaskModel() {}

    // ─── Constructor for creating a new task ─────────────────────────────────
    public TaskModel(String title, String description,
                     String assignedTo, String assignedToName,
                     String assignedBy, String assignedByName,
                     String deadline, String priority) {
        this.title              = title;
        this.description        = description;
        this.assignedTo         = assignedTo;
        this.assignedToName     = assignedToName;
        this.assignedBy         = assignedBy;
        this.assignedByName     = assignedByName;
        this.deadline           = deadline;
        this.priority           = priority;
        this.status             = STATUS_PENDING;
        this.progress           = 0;
        this.isRead             = false;
        this.completionRequested = false;
        this.completionApproved  = false;
        this.createdAt          = System.currentTimeMillis();
        this.updatedAt          = System.currentTimeMillis();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public boolean isPendingApproval() {
        return completionRequested && !completionApproved;
    }

    public String getStatusDisplay() {
        if (status == null) return "Pending";
        switch (status) {
            case STATUS_PENDING:     return "Pending";
            case STATUS_IN_PROGRESS: return "In Progress";
            case STATUS_COMPLETED:   return "Completed";
            case STATUS_APPROVED:    return "Approved ✓";
            case STATUS_REJECTED:    return "Rejected";
            default:                 return status;
        }
    }

    public String getPriorityDisplay() {
        if (priority == null) return "Medium";
        switch (priority) {
            case PRIORITY_HIGH:   return "🔴 High";
            case PRIORITY_MEDIUM: return "🟡 Medium";
            case PRIORITY_LOW:    return "🟢 Low";
            default:              return priority;
        }
    }

    public int getStatusColor() {
        if (status == null) return 0xFFFF8C00;
        switch (status) {
            case STATUS_PENDING:     return 0xFFFF8C00; // orange
            case STATUS_IN_PROGRESS: return 0xFF1565C0; // blue
            case STATUS_COMPLETED:   return 0xFF2E7D32; // green
            case STATUS_APPROVED:    return 0xFF1B5E20; // dark green
            case STATUS_REJECTED:    return 0xFFB71C1C; // red
            default:                 return 0xFF888888;
        }
    }
}