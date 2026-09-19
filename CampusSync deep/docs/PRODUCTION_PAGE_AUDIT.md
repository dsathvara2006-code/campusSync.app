# Production Page Audit

| Category | Page or state | Status | Evidence | Applicability reason | Required action |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Legal** | Privacy Policy | BLOCKED_BY_MISSING_INFORMATION | Missing in project files. App collects email, name, attendance, fee data. | Collects and processes personal student/faculty data. | Create page structure and await business/legal information. |
| | Terms of Service | BLOCKED_BY_MISSING_INFORMATION | Missing in project files. App involves user accounts and payments. | Provides a public service and handles user-generated content/transactions. | Create page structure and await business/legal information. |
| | Cookie Policy | NOT_APPLICABLE | Native Android app. `package.json` and web config not found for frontend. | Native applications use SharedPreferences/DataStore, not cookies. | None. |
| | Cookie Preferences | NOT_APPLICABLE | See above. | No cookies used. | None. |
| | Refund Policy | BLOCKED_BY_MISSING_INFORMATION | `StudentFeeCheckout.kt` handles payments via UPI. | Processes zero-commission fee transactions. | Create page structure and await business/legal information. |
| | Cancellation Policy | NOT_APPLICABLE | `CampusSync_App_Details.txt` mentions only fee dues. | Fees are mandatory dues, not cancellable subscriptions. | None. |
| | Shipping Policy | NOT_APPLICABLE | No physical products found in codebase. | App is for college management (attendance, fees, resources). | None. |
| | Return / Exchange Policy| NOT_APPLICABLE | See above. | No physical products. | None. |
| | Disclaimer | NOT_APPLICABLE | No medical/legal/financial advice provided. | Standard educational management tool. | None. |
| | Accessibility Statement | APPLICABLE_MISSING | No statement found. | Public-facing app used by students with potential disabilities. | Create page structure and await compliance verification. |
| | Data Processing Agreement| NOT_APPLICABLE | B2B agreements are likely handled off-app between the provider and colleges. | College admins manage their own tenants. | None. |
| | Acceptable Use Policy | BLOCKED_BY_MISSING_INFORMATION | App features notice boards and resource sharing. | Users can upload generated content (PDFs, notices). | Create page structure and await business/legal information. |
| | Security Policy | BLOCKED_BY_MISSING_INFORMATION | Security rules exist (`firestore.rules`), but no public policy. | Handles sensitive student data and payments. | Create page structure and await business/legal information. |
| | Responsible Disclosure | BLOCKED_BY_MISSING_INFORMATION | No security reporting channel found. | Required for safe vulnerability reporting. | Create page structure and await business/legal information. |
| | Community Guidelines | APPLICABLE_MISSING | Missing. | App has notice boards where admins/teachers post. | Create page structure and await business/legal information. |
| **Customer lifecycle** | Login | EXISTS_AND_ADEQUATE | `Screen.Login.route` in `NavGraph.kt`. | User accounts are required for all functionality. | Retain. |
| | Register | NOT_APPLICABLE | `README.md` states new users must have a pending invite created by an admin. | Registration is invite-only. | None. |
| | Email Verification | NOT_APPLICABLE | Handled entirely by Google Auth (provider). | Using Google Sign-In exclusively. | None. |
| | Forgot Password | NOT_APPLICABLE | Handled entirely by Google Auth. | No custom password auth implemented. | None. |
| | Reset Password | NOT_APPLICABLE | Handled entirely by Google Auth. | No custom password auth implemented. | None. |
| | Onboarding | APPLICABLE_MISSING | Currently jumps directly to Dashboard after invite approval. | Needs role explanation or initial profile setup. | Implement simple onboarding flow after login/approval. |
| | Account Settings | EXISTS_NEEDS_IMPROVEMENT| `SettingsScreen.kt` exists but might need legal links and support options. | Users need to manage their preferences and view policies. | Update Settings screen to include links to legal/support pages. |
| | Billing | NOT_APPLICABLE | No subscription billing, only one-off fee payments. | Not a subscription SaaS. | None. |
| | Upgrade | NOT_APPLICABLE | See above. | No subscription tiers. | None. |
| | Downgrade | NOT_APPLICABLE | See above. | No subscription tiers. | None. |
| | Cancel Subscription | NOT_APPLICABLE | See above. | No subscription tiers. | None. |
| | Payment Success | EXISTS_NEEDS_IMPROVEMENT| Present in `StudentFeeCheckout.kt` logic. | Fee payments. | Ensure standalone visual success state exists. |
| | Payment Failed | EXISTS_NEEDS_IMPROVEMENT| Present in `StudentFeeCheckout.kt` logic. | Fee payments. | Ensure standalone visual error state with retry. |
| | Payment Pending | EXISTS_AND_ADEQUATE | Admin approvals handle pending UTR verification (`UtrApprovals.route`). | Payments are manually verified by admin. | Retain. |
| | Support | APPLICABLE_MISSING | Missing in codebase. | Users may need help with fees, invites, or app usage. | Implement Support page structure. |
| | Help Center | APPLICABLE_MISSING | Missing in codebase. | Students/Teachers need guidance on features. | Implement Help Center structure. |
| **UX states** | 404 (Unknown Route) | NOT_APPLICABLE | Native app uses strict `NavGraph.kt` routing. | Impossible to reach unknown URL. | None. |
| | 403 (Permission Denied)| APPLICABLE_MISSING | Firestore rules exist, but no explicit UI state for rejected access beyond waiting room. | Role-based access control. | Implement reusable Permission Denied component. |
| | 500 (Unexpected Error) | APPLICABLE_MISSING | Missing global error fallback screen. | App crashes on fatal errors. | Implement global error fallback state. |
| | Maintenance | APPLICABLE_MISSING | No maintenance mode flag found in Firestore or Remote Config. | System upgrades. | Implement Maintenance screen and config check. |
| | Offline | APPLICABLE_MISSING | Firestore has offline persistence enabled, but no UI indicator when network drops. | Mobile app usability. | Implement Offline indicator/state. |
| | Empty State | EXISTS_AND_ADEQUATE | `EmptyState` component exists in `Components.kt`. | Used for empty lists. | Retain. |
| | No Search Results | APPLICABLE_MISSING | Not explicitly defined as a separate component. | Filtering resources/notices. | Implement No Results component. |
| | Loading State | EXISTS_AND_ADEQUATE | `LoadingState` (Shimmer) exists in `Components.kt`. | Data fetching. | Retain. |
| | Error State | APPLICABLE_MISSING | Localized error component missing in `Components.kt`. | Recoverable fetch errors. | Implement localized Error component with retry. |
| | Success State | APPLICABLE_MISSING | No dedicated success animation/screen component. | Form submissions. | Implement reusable Success state component. |
| | Session Expired | APPLICABLE_MISSING | Google Auth handles tokens, but no explicit "Session Expired" UI redirect. | Token revocation or account deletion. | Implement Session Expired logic and screen. |

## Missing Factual Information (Blocking Legal/Business Pages)
The following information is required before legal and business pages can be finalized:
- Legal business/operator name
- Support and privacy contact details
- Registered or operating address
- Applicable jurisdiction
- Minimum user age
- Effective date
- Actual payment/refund rules for UPI fee payments
- Security-reporting address
