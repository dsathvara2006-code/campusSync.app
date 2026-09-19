# CampusSync Architecture & Flowcharts

Below are detailed flowcharts mapping out the entire logic, architecture, and user journeys of the CampusSync app. 

## 1. Full App Navigation Flow
This chart shows the complete screen-to-screen navigation of the app, starting from the Splash screen to the various role-based dashboards.

```mermaid
graph TD
    Splash[Splash Screen] --> AuthCheck{Is User Logged In & Approved?}
    
    AuthCheck -- No --> Login[Login Screen]
    AuthCheck -- Yes --> RoleCheck{Check User Role}
    
    Login --> GoogleAuth[Google Sign-In]
    GoogleAuth --> DBCheck{User Exists in DB?}
    
    DBCheck -- Yes --> RoleCheck
    DBCheck -- No --> RequestForm[Request Access Form]
    RequestForm --> SubmitRequest[Submit College Code & Details]
    SubmitRequest --> WaitingRoom[Waiting Room]
    WaitingRoom -- Admin Approves --> RoleCheck
    
    RoleCheck -- Principal --> PD[Principal Dashboard]
    RoleCheck -- Admin --> AD[Admin Dashboard]
    RoleCheck -- Student --> SD[Student Dashboard]
    RoleCheck -- Teacher --> TD[Teacher Dashboard]
    
    PD --> Settings[Settings / Profile]
    PD --> P_Resources[Resources]
    PD --> P_Fees[Fee Management]
    PD --> P_Admin[Manage Staff & Verify Users]
    PD --> P_Attendance[Principal Overview]
    
    AD --> Settings
    AD --> A_Fees[Fee Management]
    AD --> A_Admin[Manage Staff & Verify Users]
    
    SD --> Settings
    SD --> S_Resources[Student Resources]
    SD --> S_Fees[Pay Fees]
```

---

## 2. Authentication & Multi-Tenancy Flow
This flowchart details the backend process of how a new user requests access to a specific college, and how the app handles multi-tenancy.

```mermaid
sequenceDiagram
    participant User as New User
    participant UI as Login Screen
    participant Auth as Firebase Auth
    participant DB as Firestore (users)
    
    User->>UI: Fills Form (Role, Class, College Code)
    User->>UI: Clicks "Continue with Google"
    UI->>Auth: Initiates Google Sign-in
    Auth-->>UI: Returns ID Token
    UI->>DB: Check if UID exists
    DB-->>UI: User Not Found
    UI->>DB: batch.set() Create User (Status: Pending)
    UI->>DB: Save College Code (collegeId)
    UI->>User: Navigates to Waiting Room
    
    loop Real-time Listener
        UI->>DB: Listen to status changes on UID
        DB-->>UI: Status: pending
    end
```

---

## 3. Admin Verification & Approval Flow
This chart shows how an Admin logs in and approves users specifically for their own college.

```mermaid
graph TD
    Admin[Admin / Principal] --> AdminDash[Admin Dashboard]
    AdminDash --> VerifyScreen[Verify Requests Screen]
    
    VerifyScreen --> FetchDB[Fetch from Firestore]
    FetchDB --> Query[Query: status == 'pending' AND <br> collegeId == Admin.collegeId]
    Query --> ShowList[Show List of Pending Users]
    
    ShowList --> Card[User Card: Name, Class, Roll No]
    
    Card --> Action{Admin Action}
    
    Action -- Click Reject --> DeleteUser[Delete User from DB]
    DeleteUser --> Refresh[Refresh List]
    
    Action -- Click Approve --> UpdateStatus[Update status = 'approved' <br> in users & colleges/roles collection]
    UpdateStatus --> TriggerClient[Real-time listener on Student app <br> detects 'approved']
    TriggerClient --> Dash[Student Auto-Navigates to Dashboard]
    UpdateStatus --> Refresh
```

---

## 4. Role-Based Feature Access
A breakdown of which features are accessible by which user roles based on the current architecture.

```mermaid
graph LR
    subgraph Users
        PR[Principal]
        AD[Admin]
        ST[Student]
        TC[Teacher]
    end

    subgraph Features
        F1[Verify New Users]
        F2[Manage Staff]
        F3[Fee Setup & Approvals]
        F4[Pay Fees]
        F5[Upload Resources]
        F6[View Resources]
        F7[Take Attendance]
        F8[View College Overview]
    end

    PR --> F1
    PR --> F2
    PR --> F3
    PR --> F6
    PR --> F8

    AD --> F1
    AD --> F2
    AD --> F3

    ST --> F4
    ST --> F6
    
    TC --> F5
    TC --> F6
    TC --> F7
```
