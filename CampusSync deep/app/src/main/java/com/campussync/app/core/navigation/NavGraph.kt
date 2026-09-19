package com.campussync.app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.campussync.app.feature.auth.LoginScreen
import com.campussync.app.feature.auth.AuthViewModel
import com.campussync.app.feature.dashboard.DashboardScreen
import com.campussync.app.feature.settings.SettingsScreen
import com.campussync.app.feature.support.SupportScreen
import com.campussync.app.feature.support.HelpCenterScreen
import com.campussync.app.feature.legal.*

import com.campussync.app.feature.assignments.AssignmentViewModel
import com.campussync.app.feature.attendance.AttendanceViewModel
import com.campussync.app.core.model.User
import com.campussync.app.feature.timetable.TimetableViewModel

// Removed old Routes object in favor of Screen sealed class

@Composable
fun CampusSyncNavGraph(navController: NavHostController = rememberNavController()) {
    val authViewModel = remember { AuthViewModel() }
    val attendanceViewModel = remember { AttendanceViewModel() }
    val timetableViewModel = remember { TimetableViewModel() }
    val assignmentViewModel: AssignmentViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val resourcesViewModel: com.campussync.app.feature.resources.ResourcesViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()

    val rawUser = authViewModel.currentUser.value
    var currentUser by remember { mutableStateOf(rawUser) }
    val safeUser = currentUser
    
    val context = androidx.compose.ui.platform.LocalContext.current

    val safePopBackStack: () -> Unit = {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(rawUser) {
        if (rawUser != null) {
            currentUser = rawUser
            
            // Route dynamically based on role
            val targetDashboard = when (rawUser.role.lowercase()) {
                "principal" -> Screen.PrincipalDashboard.route
                "admin", "tenant_admin" -> Screen.AdminDashboard.route
                "student" -> Screen.StudentDashboard.route
                else -> Screen.PrincipalDashboard.route // fallback
            }
            
            navController.navigate(targetDashboard) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            com.campussync.app.feature.splash.SplashScreen(
                onFinish = {
                    if (safeUser != null) {
                        val targetDashboard = when (safeUser.role.lowercase()) {
                            "principal" -> Screen.PrincipalDashboard.route
                            "admin", "tenant_admin" -> Screen.AdminDashboard.route
                            "student" -> Screen.StudentDashboard.route
                            else -> Screen.PrincipalDashboard.route
                        }
                        navController.navigate(targetDashboard) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToWaitingRoom = {
                    navController.navigate("waiting_room") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLoginSuccess = { role ->
                    val targetDashboard = when (role.lowercase()) {
                        "principal" -> Screen.PrincipalDashboard.route
                        "admin", "tenant_admin" -> Screen.AdminDashboard.route
                        "student" -> Screen.StudentDashboard.route
                        else -> Screen.PrincipalDashboard.route
                    }
                    navController.navigate(targetDashboard) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        

        composable("waiting_room") {
            com.campussync.app.feature.auth.WaitingRoomScreen(
                viewModel = authViewModel,
                onApproved = {
                    val user = authViewModel.currentUser.value
                    if (user != null) {
                        val targetDashboard = when (user.role.lowercase()) {
                            "principal" -> Screen.PrincipalDashboard.route
                            "admin", "tenant_admin" -> Screen.AdminDashboard.route
                            "student" -> Screen.StudentDashboard.route
                            else -> Screen.PrincipalDashboard.route
                        }
                        navController.navigate(targetDashboard) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        // --- PRINCIPAL DASHBOARD ---
        composable(Screen.PrincipalDashboard.route) {
            if (safeUser != null) {
                DashboardScreen(
                    user = safeUser,
                    classId = safeUser.classId,
                    attendanceViewModel = attendanceViewModel,
                    timetableViewModel = timetableViewModel,
                    assignmentViewModel = assignmentViewModel,
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToResources = { navController.navigate(Screen.StudentResources.route) },
                    onNavigateToFeeTypes = { navController.navigate(Screen.FeeManagement.route) },
                    onNavigateToAssignFee = { navController.navigate(Screen.AssignFee.route) },
                    onNavigateToStudentFees = { navController.navigate(Screen.StudentFees.route) },
                    onNavigateToApprovePayments = { navController.navigate(Screen.UtrApprovals.route) },
                    onNavigateToManageUsers = { navController.navigate(Screen.AdminStaff.route) },
                    onNavigateToVerifyInvites = { navController.navigate(Screen.AdminVerification.route) },
                    onNavigateToPrincipalOverview = { navController.navigate(Screen.PrincipalOverview.route) },
                    onNavigateToLecture = { classId, subjectName -> navController.navigate(Screen.LectureAttendance.createRoute(classId, subjectName)) },
                    onNavigateToDailySummary = { navController.navigate(Screen.DailyLectureSummary.route) }
                )
            }
        }

        // --- ADMIN DASHBOARD ---
        composable(Screen.AdminDashboard.route) {
            if (safeUser != null) {
                DashboardScreen(
                    user = safeUser,
                    classId = safeUser.classId,
                    attendanceViewModel = attendanceViewModel,
                    timetableViewModel = timetableViewModel,
                    assignmentViewModel = assignmentViewModel,
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToResources = { },
                    onNavigateToFeeTypes = { navController.navigate(Screen.FeeManagement.route) },
                    onNavigateToAssignFee = { navController.navigate(Screen.AssignFee.route) },
                    onNavigateToStudentFees = { },
                    onNavigateToApprovePayments = { navController.navigate(Screen.UtrApprovals.route) },
                    onNavigateToManageUsers = { navController.navigate(Screen.AdminStaff.route) },
                    onNavigateToVerifyInvites = { navController.navigate(Screen.AdminVerification.route) },
                    onNavigateToPrincipalOverview = { navController.navigate(Screen.PrincipalOverview.route) },
                    onNavigateToLecture = { classId, subjectName -> navController.navigate(Screen.LectureAttendance.createRoute(classId, subjectName)) },
                    onNavigateToDailySummary = { navController.navigate(Screen.DailyLectureSummary.route) }
                )
            } else {
                navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
            }
        }

        composable(Screen.DailyLectureSummary.route) {
            if (safeUser != null && (safeUser.role == "admin" || safeUser.role == "principal" || safeUser.role == "tenant_admin")) {
                com.campussync.app.feature.attendance.DailyLectureSummaryScreen(
                    collegeId = safeUser.collegeId,
                    onBack = safePopBackStack
                )
            }
        }

        // --- STUDENT DASHBOARD ---
        composable(Screen.StudentDashboard.route) {
            if (safeUser != null) {
                DashboardScreen(
                    user = safeUser,
                    classId = safeUser.classId,
                    attendanceViewModel = attendanceViewModel,
                    timetableViewModel = timetableViewModel,
                    assignmentViewModel = assignmentViewModel,
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToResources = { navController.navigate(Screen.StudentResources.route) },
                    onNavigateToFeeTypes = { },
                    onNavigateToAssignFee = { },
                    onNavigateToStudentFees = { navController.navigate(Screen.StudentFees.route) },
                    onNavigateToApprovePayments = { }
                )
            } else {
                navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
            }
        }

        // --- SHARED SCREENS ---
        composable(Screen.Settings.route) {
            SettingsScreen(
                currentUser = safeUser,
                onBack = safePopBackStack,
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }
        
        composable(Screen.Support.route) {
            SupportScreen(onBack = safePopBackStack)
        }
        
        composable(Screen.HelpCenter.route) {
            HelpCenterScreen(onBack = safePopBackStack)
        }
        
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBack = safePopBackStack)
        }
        
        composable(Screen.TermsOfService.route) {
            TermsOfServiceScreen(onBack = safePopBackStack)
        }
        
        composable(Screen.RefundPolicy.route) {
            RefundPolicyScreen(onBack = safePopBackStack)
        }
        
        composable(Screen.AcceptableUsePolicy.route) {
            AcceptableUsePolicyScreen(onBack = safePopBackStack)
        }
        
        composable(Screen.CommunityGuidelines.route) {
            CommunityGuidelinesScreen(onBack = safePopBackStack)
        }
        
        composable(Screen.StudentResources.route) {
            if (safeUser != null) {
                com.campussync.app.feature.resources.ResourcesScreen(
                    user = safeUser,
                    viewModel = resourcesViewModel,
                    onBack = safePopBackStack,
                    onNavigateToUpload = { navController.navigate(Screen.UploadResource.route) }
                )
            }
        }
        
        // Note: UploadResource wasn't strictly in my sealed class, I'll map it to an ad-hoc or add it to sealed class later.
        composable(Screen.UploadResource.route) {
            if (safeUser != null) {
                com.campussync.app.feature.resources.UploadResourceScreen(
                    user = safeUser,
                    viewModel = resourcesViewModel,
                    onBack = safePopBackStack
                )
            }
        }
        
        composable(Screen.FeeManagement.route) {
            // For now, mapping FeeManagement to the old FeeTypeScreen
            if (safeUser != null && (safeUser.role == "admin" || safeUser.role == "principal")) {
                com.campussync.app.feature.fees.FeeTypeScreen(
                    currentUser = safeUser,
                    onBack = safePopBackStack
                )
            }
        }
        
        // Re-adding Assign Fee explicitly until merged
        composable(Screen.AssignFee.route) {
            if (safeUser != null && (safeUser.role == "admin" || safeUser.role == "principal")) {
                com.campussync.app.feature.fees.AssignFeeScreen(
                    currentUser = safeUser,
                    onBack = safePopBackStack
                )
            }
        }
        
        composable(Screen.StudentFees.route) {
            if (safeUser != null && safeUser.role == "student") {
                com.campussync.app.feature.fees.StudentFeesScreen(currentUser = safeUser)
            }
        }
        
        composable(
            route = Screen.StudentFeeCheckout.route,
            arguments = listOf(navArgument("feeId") { type = NavType.StringType })
        ) {
            if (safeUser != null && safeUser.role == "student") {
                com.campussync.app.feature.fees.StudentFeeCheckoutScreen(
                    currentUser = safeUser,
                    onBack = safePopBackStack
                )
            }
        }
        
        composable(Screen.UtrApprovals.route) {
            if (safeUser != null && (safeUser.role == "admin" || safeUser.role == "principal")) {
                com.campussync.app.feature.fees.ApprovePaymentScreen(
                    currentUser = safeUser,
                    onBack = safePopBackStack
                )
            }
        }
        
        composable(Screen.AdminStaff.route) {
            if (safeUser != null && (safeUser.role == "admin" || safeUser.role == "principal")) {
                com.campussync.app.feature.admin.AdminStaffScreen(
                    currentUser = safeUser,
                    onBack = safePopBackStack
                )
            }
        }
        
        composable(Screen.AdminVerification.route) {
            if (safeUser != null && (safeUser.role == "admin" || safeUser.role == "principal")) {
                com.campussync.app.feature.admin.AdminVerificationScreen(
                    currentUser = safeUser,
                    onBack = safePopBackStack
                )
            }
        }
        
        composable(Screen.PrincipalOverview.route) {
            // 3-role system: Admin shares every Principal screen
            if (safeUser != null && (safeUser.role == "principal" || safeUser.role == "admin")) {
                com.campussync.app.feature.attendance.OverviewScreen(
                    collegeId = safeUser.collegeId,
                    onBack = safePopBackStack
                )
            }
        }
        
        composable(
            route = Screen.LectureAttendance.route,
            arguments = listOf(navArgument("subjectName") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
            if (safeUser != null && safeUser.role == "teacher") {
                com.campussync.app.feature.attendance.LectureAttendanceScreen(
                    collegeId = safeUser.collegeId,
                    classId = safeUser.classId,
                    teacherId = safeUser.userId,
                    subjectName = subjectName,
                    viewModel = attendanceViewModel,
                    onBack = safePopBackStack,
                    onOpenSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
        }
    }
}
