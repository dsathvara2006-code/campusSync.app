package com.campussync.app.core.model

data class FeeSettings(
    val collegeId: String = "",
    val upiId: String = "",
    val collegeName: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

data class FeeType(
    val id: String = "",
    val collegeId: String = "",
    val title: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class FeeDue(
    val id: String = "",
    val collegeId: String = "",
    val studentId: String = "", // Assigned to specific student
    val classId: String = "", // Or assigned to an entire class
    val feeTypeId: String = "", // Links to FeeType
    val title: String = "",
    val amount: Double = 0.0,
    val dueDate: String = "", // YYYY-MM-DD
    val status: String = "pending", // "pending", "submitted", "approved", "rejected"
    val adminNote: String = "", // Reason for rejection
    val assignedAt: Long = System.currentTimeMillis()
)

data class FeePayment(
    val id: String = "",
    val dueId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val collegeId: String = "",
    val feeTitle: String = "",
    val amount: Double = 0.0,
    val utr: String = "", // Must be unique across college
    val proofUrl: String = "",
    val status: String = "submitted", // "submitted", "approved", "rejected"
    val adminNote: String = "", // Reason for rejection
    val timestamp: Long = System.currentTimeMillis()
)

data class PaymentOrder(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val collegeId: String = "",
    val dueIds: List<String> = emptyList(), // Array of bundled fee dues
    val expectedTotal: Double = 0.0,
    val feeSummaryTitle: String = "", // e.g., "Tuition + Bus"
    val utr: String = "",
    val proofUrl: String = "",
    val status: String = "processing", // "processing", "approved", "rejected"
    val adminNote: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
