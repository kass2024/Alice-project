package com.alice.smartteacher.model;

public final class Enums {
    private Enums() {}

    public enum Role { ADMIN, HEAD_OF_STUDIES, TEACHER }

    public enum AttendanceStatus { PRESENT, ABSENT, LATE, ON_LEAVE }

    public enum MarkMethod { MANUAL, BULK, FACE, OVERRIDE }

    public enum DocCategory {
        LESSON_PLAN, SCHEME_OF_WORK, CLASS_REGISTER, REPORT,
        SYLLABUS, WEEKS_BREAKDOWN, CURRICULUM, CHRONOGRAM
    }

    public enum DocStatus { DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, RETURNED }

    public enum Track { REB, TVET, GENERAL }

    public enum ActivityStatus { SCHEDULED, ON_SCHEDULE, DELAYED, MISSED, COMPLETED }

    public enum Priority { LOW, NORMAL, HIGH }
}
