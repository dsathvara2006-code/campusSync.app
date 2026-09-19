import base64

def get_mermaid_url(code):
    encoded = base64.b64encode(code.strip().encode('utf-8')).decode('utf-8')
    return f'https://mermaid.ink/img/{encoded}?theme=dark'

chart1 = '''graph TD
Splash[App Launch / Splash] --> AuthCheck{Logged In?}
AuthCheck -- No --> Login[Login Screen]
AuthCheck -- Yes --> RoleCheck{Check User Role}
Login --> GoogleAuth[Continue with Google]
GoogleAuth --> DBCheck{Exists in users collection?}
DBCheck -- Yes --> RoleCheck
DBCheck -- No --> RequestForm[Request Access Form + College Code]
RequestForm --> SubmitRequest[Send Request to Firestore]
SubmitRequest --> WaitingRoom[Waiting Room]
WaitingRoom -- Admin Approves --> RoleCheck
RoleCheck -- Principal --> PD[Principal Dashboard]
RoleCheck -- Admin --> AD[Admin Dashboard]
RoleCheck -- Student --> SD[Student Dashboard]
RoleCheck -- Teacher --> TD[Teacher Dashboard]'''

chart2 = '''graph TD
Dash[Admin / Principal Dashboard] --> F1(Verify Requests)
Dash --> F2(Manage Staff)
Dash --> F3(Fee Management)
Dash --> F4(Principal Overview)
F1 --> List[Fetch Pending Users for this College]
List --> Action1{Approve or Reject}
Action1 -- Approve --> StatusUpdate[Update Status to 'approved']
Action1 -- Reject --> Delete[Delete User Request]
F2 --> StaffList[View Teachers & Admins]
StaffList --> AddStaff[Assign Roles]
F3 --> Setup[Create Fee Types]
F3 --> Assign[Assign Fees to Classes]
F3 --> VerifyPayment[Approve Student UTR/Payments]
F4 --> Analytics[View College-wide Attendance Analytics]'''

chart3 = '''graph TD
Dash[Teacher Dashboard] --> F1(Timetable & Lectures)
Dash --> F2(Take Attendance)
Dash --> F3(Upload Resources)
Dash --> F4(Assignments)
F1 --> ViewClasses[View Assigned Subjects & Classes]
F2 --> SelectSubject[Select Subject]
SelectSubject --> StudentList[View List of Students in Class]
StudentList --> Mark[Toggle Present / Absent]
Mark --> Save[Save to Firestore]
F3 --> AddFile[Upload PDF / Document]
AddFile --> Tagging[Tag by Subject & Semester]
Tagging --> CloudStorage[Upload to Firebase Storage]
F4 --> CreateTask[Create Assignment & Due Date]'''

chart4 = '''graph TD
Dash[Student Dashboard] --> F1(View Attendance)
Dash --> F2(View Resources)
Dash --> F3(Pay Fees)
Dash --> F4(Assignments)
F1 --> Stats[See Personal Attendance %]
F2 --> Search[Search Notes by Subject]
Search --> Download[Download PDF]
F3 --> PendingFees[View Assigned Pending Fees]
PendingFees --> Pay[Upload UTR Number & Screenshot]
Pay --> WaitApproval[Wait for Admin to Verify Payment]
F4 --> ToDo[View Pending Assignments]'''

html_content = f'''<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>CampusSync Official Report</title>
    <style>
        body {{ font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f1115; color: #e1e1e1; margin: 0; padding: 40px; }}
        h1 {{ text-align: center; color: #6366F1; margin-bottom: 50px; font-size: 36px; }}
        .section {{ background: #1a1c23; padding: 40px; border-radius: 12px; margin-bottom: 50px; box-shadow: 0 10px 15px rgba(0,0,0,0.3); }}
        h2 {{ color: #4ade80; border-bottom: 2px solid #2d303b; padding-bottom: 15px; font-size: 24px; margin-top: 0; }}
        .img-container {{ text-align: center; margin: 30px 0; background: #22252e; padding: 20px; border-radius: 8px; }}
        img {{ max-width: 100%; border-radius: 8px; }}
        .report {{ background: #22252e; padding: 25px; border-left: 6px solid #6366F1; font-size: 16px; line-height: 1.8; border-radius: 8px; color: #a0aec0; }}
        .report strong {{ color: #e2e8f0; font-size: 18px; display: inline-block; margin-bottom: 10px; }}
    </style>
</head>
<body>
    <h1>CampusSync: Complete Architecture & Feature Report</h1>

    <div class="section">
        <h2>1. Master App Flow (Overall Architecture)</h2>
        <div class="img-container">
            <img src="{get_mermaid_url(chart1)}" alt="Master App Flow">
        </div>
        <div class="report">
            <strong>Report / Explanation:</strong><br>
            This flow demonstrates the bird's-eye view of CampusSync. It highlights the multi-tenancy entry point. When a new user logs in via Google, the system checks if they exist in the database. If not, they are directed to the <b>Request Access Form</b> where they must provide a unique <b>College Code</b>. They are then placed in a waiting room until their respective college Admin approves them. Once approved, they are routed to their role-specific dashboard.
        </div>
    </div>

    <div class="section">
        <h2>2. Principal & Admin Flow (Management & Verification)</h2>
        <div class="img-container">
            <img src="{get_mermaid_url(chart2)}" alt="Admin Flow">
        </div>
        <div class="report">
            <strong>Report / Explanation:</strong><br>
            The Admin/Principal workflow handles high-level management. The most critical aspect is <b>Verify Requests</b>, where the system queries only those pending users whose <code>collegeId</code> matches the Admin's. The Admin can approve (updating the user's status to 'approved' in Firestore) or reject (deleting the user from the database). Additionally, Admins can create fee structures, assign them to specific classes, and verify UTR numbers for payments made by students.
        </div>
    </div>

    <div class="section">
        <h2>3. Teacher Flow (Attendance & Academics)</h2>
        <div class="img-container">
            <img src="{get_mermaid_url(chart3)}" alt="Teacher Flow">
        </div>
        <div class="report">
            <strong>Report / Explanation:</strong><br>
            Teachers are responsible for daily academic operations. They can select their assigned subjects and classes to <b>Take Attendance</b> (instantly saved and synced to Firestore). They can also <b>Upload Resources</b> (like notes and PDFs), tagging them properly by subject and semester for students to easily locate them in the cloud. Finally, they can create assignments and track which students have completed them.
        </div>
    </div>

    <div class="section">
        <h2>4. Student Flow (Learning & Fees)</h2>
        <div class="img-container">
            <img src="{get_mermaid_url(chart4)}" alt="Student Flow">
        </div>
        <div class="report">
            <strong>Report / Explanation:</strong><br>
            The Student dashboard provides read-only access to academic data and read-write access to fee payments. Students can monitor their real-time attendance statistics updated by teachers. They can search the digital library to download PDF notes uploaded by their instructors. For administration, students can view their pending fees, upload UPI payment screenshots along with the UTR number, and wait for the Admin to verify and clear their dues.
        </div>
    </div>

</body>
</html>
'''

with open('CampusSync_Report_With_Photos.html', 'w', encoding='utf-8') as f:
    f.write(html_content)
