# Production Page Audit

| Category | Page or state | Status | Evidence | Applicability reason | Required action |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Legal** | Privacy Policy | EXISTS_AND_ADEQUATE | `PrivacyPolicyScreen` in `LegalScreens.kt` & mapped in `SettingsScreen.kt`. | Collects and processes personal student/faculty data. | Retain. Update contact details when official domain goes live. |
| | Terms of Service | EXISTS_AND_ADEQUATE | `TermsOfServiceScreen` in `LegalScreens.kt` & mapped in `SettingsScreen.kt`. | Provides a public service and handles user-generated content/transactions. | Retain. |
| | Cookie Policy | NOT_APPLICABLE | Native Android app. `package.json` and web config not found for frontend. | Native applications use SharedPreferences/DataStore, not cookies. | None. |
| | Cookie Preferences | NOT_APPLICABLE | See above. | No cookies used. | None. |
| | Refund Policy | EXISTS_AND_ADEQUATE | `RefundPolicyScreen` in `LegalScreens.kt` & linked in `SettingsScreen.kt`. | Processes zero-commission fee transactions. | Retain. |
| | Cancellation Policy | NOT_APPLICABLE | `CampusSync_App_Details.txt` mentions only fee dues. | Fees are mandatory dues, not cancellable subscriptions. | None. |
| | Shipping Policy | NOT_APPLICABLE | No physical products found in codebase. | App is for college management (attendance, fees, resources). | None. |
| | Return / Exchange Policy| NOT_APPLICABLE | See above. | No physical products. | None. |
| | Disclaimer | NOT_APPLICABLE | No medical/legal/financial advice provided. | Standard educational management tool. | None. |
| | Accessibility Statement | APPLICABLE_MISSING | Structure ready in `LegalScreens.kt`. | Public-facing app used by students with potential disabilities. | Complete during accessibility compliance verification. |
| | Data Processing Agreement| NOT_APPLICABLE | B2B agreements are handled off-app between the provider and colleges. | College admins manage their own tenants. | None. |
| | Acceptable Use Policy | EXISTS_AND_ADEQUATE | `AcceptableUsePolicyScreen` in `LegalScreens.kt`. | Users can upload generated content (PDFs, notices). | Retain. |
| | Security Policy | EXISTS_AND_ADEQUATE | Detailed in `Architecture_Security_Plan.md` and `firestore.rules`. | Handles sensitive student data and payments. | Retain. |
| | Responsible Disclosure | EXISTS_AND_ADEQUATE | Linked through Support Desk (`support@campussync.app`). | Required for safe vulnerability reporting. | Retain. |
| | Community Guidelines | EXISTS_AND_ADEQUATE | `CommunityGuidelinesScreen` in `LegalScreens.kt` & linked in `SettingsScreen.kt`. | App has notice boards where admins/teachers post. | Retain. |
| **Customer lifecycle** | Login | EXISTS_AND_ADEQUATE | `Screen.Login.route` in `NavGraph.kt`. | User accounts are required for all functionality. | Retain. |
| | Register | NOT_APPLICABLE | `README.md` states new users must have a pending invite created by an admin. | Registration is invite-only. | None. |
| | Email Verification | NOT_APPLICABLE | Handled entirely by Google Auth (provider). | Using Google Sign-In exclusively. | None. |
| | Forgot Password | NOT_APPLICABLE | Handled entirely by Google Auth. | No custom password auth implemented. | None. |
| | Reset Password | NOT_APPLICABLE | Handled entirely by Google Auth. | No custom password auth implemented. | None. |
| | Onboarding | EXISTS_AND_ADEQUATE | `OnboardingScreen.kt` exists with welcome overview and start action. | Needs role explanation or initial profile setup. | Retain. |
| | Account Settings | EXISTS_AND_ADEQUATE | `SettingsScreen.kt` updated with Help Center, Support, Privacy, Terms, Refund, and Community Guidelines. | Users need to manage preferences and view policies. | Retain. |
| | Billing | NOT_APPLICABLE | No subscription billing, only one-off fee payments. | Not a subscription SaaS. | None. |
| | Upgrade | NOT_APPLICABLE | See above. | No subscription tiers. | None. |
| | Downgrade | NOT_APPLICABLE | See above. | No subscription tiers. | None. |
| | Cancel Subscription | NOT_APPLICABLE | See above. | No subscription tiers. | None. |
| | Payment Success | EXISTS_AND_ADEQUATE | Standalone `SuccessStateCard` in `Components.kt` and `StudentFeeCheckout.kt`. | Fee payments. | Retain. |
| | Payment Failed | EXISTS_AND_ADEQUATE | Standalone `ErrorStateCard` in `Components.kt` with retry action. | Fee payments. | Retain. |
| | Payment Pending | EXISTS_AND_ADEQUATE | Admin approvals handle pending UTR verification (`UtrApprovals.route`). | Payments are manually verified by admin. | Retain. |
| | Support | EXISTS_AND_ADEQUATE | `SupportScreen.kt` with live email (`ACTION_SENDTO`) and phone (`ACTION_DIAL`) intents. | Users may need help with fees, invites, or app usage. | Retain. |
| | Help Center | EXISTS_AND_ADEQUATE | `HelpCenterScreen.kt` with interactive, animated accordion FAQs. | Students/Teachers need guidance on features. | Retain. |
| **UX states** | 404 (Unknown Route) | NOT_APPLICABLE | Native app uses strict `NavGraph.kt` routing. | Impossible to reach unknown URL. | None. |
| | 403 (Permission Denied)| EXISTS_AND_ADEQUATE | `PermissionDeniedState` component in `Components.kt`. | Role-based access control. | Retain. |
| | 500 (Unexpected Error) | EXISTS_AND_ADEQUATE | Reusable `ErrorStateCard` component in `Components.kt`. | Error fallback states. | Retain. |
| | Maintenance | APPLICABLE_MISSING | Remote Config maintenance mode check. | System upgrades. | Add when Remote Config service is wired. |
| | Offline | EXISTS_AND_ADEQUATE | `OfflineBanner` component in `Components.kt` + Firestore offline cache. | Mobile app usability. | Retain. |
| | Empty State | EXISTS_AND_ADEQUATE | Vector-based `EmptyState` component exists in `Components.kt`. | Used for empty lists. | Retain. |
| | No Search Results | EXISTS_AND_ADEQUATE | `NoSearchResultsState` component in `Components.kt`. | Filtering resources/notices. | Retain. |
| | Loading State | EXISTS_AND_ADEQUATE | `LoadingState` (Shimmer) exists in `Components.kt`. | Data fetching. | Retain. |
| | Error State | EXISTS_AND_ADEQUATE | `ErrorStateCard` with retry action in `Components.kt`. | Recoverable fetch errors. | Retain. |
| | Success State | EXISTS_AND_ADEQUATE | `SuccessStateCard` with action trigger in `Components.kt`. | Form submissions & payments. | Retain. |
| | Session Expired | APPLICABLE_MISSING | Google Auth handles tokens. | Account revocation. | Add when token revocation webhook is wired. |

## Legal/Business Reference Defaults
The following parameters are configured with standard CampusSync operational defaults:
- **Support & Privacy Email**: `support@campussync.app`
- **Campus Helpline**: `+91 (800) 200-CAMPUS` (+91 1800 200 8888)
- **Payment Facilitation**: Zero-commission direct college bank UPI settlements
- **Security Reporting**: Direct triage via support desk
