package com.campussync.app.core.navigation

sealed class Screen(val route: String) {
    // Auth Graph
    object Splash : Screen("splash")
    object Login : Screen("login")

    // Principal Graph
    object PrincipalDashboard : Screen("principal_dashboard")
    object ManageFaculty : Screen("manage_faculty")
    object ManageClasses : Screen("manage_classes")
    object PrincipalOverview : Screen("principal_overview")

    // Admin Graph
    object AdminDashboard : Screen("admin_dashboard")
    object DailyLectureSummary : Screen("daily_lecture_summary")
    object FeeManagement : Screen("fee_management")
    object UtrApprovals : Screen("utr_approvals")
    object AdminStaff : Screen("admin_staff")
    object AdminVerification : Screen("admin_verification")

    // Student Graph
    object StudentDashboard : Screen("student_dashboard")
    object StudentAttendance : Screen("student_attendance")
    object StudentFees : Screen("student_fees")
    object StudentFeeCheckout : Screen("student_fee_checkout/{feeId}") {
        fun createRoute(feeId: String) = "student_fee_checkout/$feeId"
    }
    object StudentResources : Screen("student_resources")

    // Common
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object UploadResource : Screen("upload_resource")
    object AssignFee : Screen("assign_fee")
    
    // Legal & Support
    object Support : Screen("support")
    object HelpCenter : Screen("help_center")
    object PrivacyPolicy : Screen("privacy_policy")
    object TermsOfService : Screen("terms_of_service")
    object RefundPolicy : Screen("refund_policy")
    object AcceptableUsePolicy : Screen("acceptable_use_policy")
    object CommunityGuidelines : Screen("community_guidelines")
    
    object LectureAttendance : Screen("lecture_attendance/{classId}/{subjectName}") {
        fun createRoute(classId: String, subjectName: String) = "lecture_attendance/$classId/$subjectName"
    }
}
