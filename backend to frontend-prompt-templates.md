# Backend-to-Frontend Prompt Templates for Copilot

This document provides standardized prompt templates to help Copilot generate accurate frontend code suggestions when backend changes occur. Each template includes placeholders for backend details and examples using actual code from the Grow Earn backend.

## Database Setup and Frontend Integration Guide

### Database Setup for Viewer Dashboard

**Required Tables and Sample Data:**

Run these SQL commands in your MySQL database to populate data for the viewer dashboard:

```sql
-- Create database
CREATE DATABASE IF NOT EXISTS grow_earn;
USE grow_earn;

-- Sample users
INSERT INTO users (email, password, role) VALUES
('creator@example.com', '$2a$10$dummyhashedpassword', 'CREATOR'),
('viewer@example.com', '$2a$10$dummyhashedpassword', 'USER');

-- Sample campaign
INSERT INTO campaigns (creator_id, platform, goal_type, channel_name, channel_link, content_type, video_link, subscriber_goal, views_goal, likes_goal, comments_goal, total_amount, status) VALUES
(1, 'YOUTUBE', 'SVLC', 'Tech Channel', 'https://youtube.com/@techchannel', 'VIDEO', 'https://youtube.com/watch?v=abc123', 100, 1000, 50, 20, 650.0, 'IN_PROGRESS');

-- Sample viewer tasks (available for claiming)
INSERT INTO viewer_tasks (campaign_id, viewer_id, creator_id, task_type, completed, status, target_link) VALUES
(1, NULL, 1, 'LIKE', FALSE, 'PENDING', 'https://youtube.com/watch?v=abc123'),
(1, NULL, 1, 'SUBSCRIBE', FALSE, 'PENDING', 'https://youtube.com/@techchannel');

-- Sample assigned task (claimed by viewer)
INSERT INTO viewer_tasks (campaign_id, viewer_id, creator_id, task_type, completed, status, target_link) VALUES
(1, 2, 1, 'COMMENT', FALSE, 'PENDING', 'https://youtube.com/watch?v=abc123');

-- Sample completed task
INSERT INTO viewer_tasks (campaign_id, viewer_id, creator_id, task_type, completed, status, target_link) VALUES
(1, 2, 1, 'VIEW', TRUE, 'COMPLETED', 'https://youtube.com/watch?v=abc123');
```

**Steps:**
1. Connect to MySQL: `mysql -u root -p`
2. Run the above SQL commands
3. Update `application.properties` with your DB credentials
4. Start the Spring Boot app: `mvn spring-boot:run`

### Frontend Integration for Viewer Dashboard

**API Endpoints:**
- `GET /api/viewer/dashboard` - Fetch dashboard data
- `POST /api/viewer/tasks/{taskId}/claim` - Claim available tasks

**Copilot Prompt for React Dashboard:**

```
Based on my Spring Boot backend, I added a new REST endpoint. Generate frontend code to integrate it.

Backend Details:
- Controller: ViewerDashboardController
- Endpoint: GET /api/viewer/dashboard
- Response: JSON with user info, stats, and tasks arrays
- Authentication: Bearer token required

Frontend Context:
- Framework: React with TypeScript
- Current auth: JWT in localStorage
- UI requirements: Dashboard with stats cards, task lists, claim buttons

Generate: Dashboard component, API service, task cards, stats display
```

**Implementation Steps:**
1. Create API service for dashboard calls
2. Create Dashboard component with stats and task sections
3. Add task claiming functionality
4. Handle loading states and errors
5. Test with real backend data

**********************************
Date: January 8, 2026, Time: 18:45
New Frontend Integration Prompt for Creator Dashboard:

Based on my Spring Boot backend, I added new REST endpoints. Generate frontend code to integrate them.

Backend Details:
- Controller: CampaignController
- Endpoints: 
  - GET /api/creator/campaign/in-progress - Fetch in-progress campaigns for creator
  - GET /api/creator/campaign/completed - Fetch completed campaigns for creator
- Response: JSON array of campaign objects with progress data
- Authentication: Bearer token required

Frontend Context:
- Framework: React with TypeScript
- Current auth: JWT in localStorage
- UI requirements: Creator dashboard with tabs for "In Progress" and "Completed Goals", displaying campaign cards with progress bars, stats, and links

Generate: CreatorDashboard component, API service for campaigns, campaign cards with progress visualization, tab navigation

**********************************

## Template Categories

### 1. New API Endpoint Template

**Template:**
```
I've added a new API endpoint to the backend. Please suggest the corresponding frontend code changes.

**Backend Details:**
- Endpoint: [HTTP_METHOD] [ENDPOINT_URL]
- Request Body: [REQUEST_BODY_SCHEMA]
- Response: [RESPONSE_SCHEMA]
- Authentication: [AUTH_REQUIREMENTS]
- Controller Code:
```
[BACKEND_CONTROLLER_CODE]
```

**Frontend Context:**
- Current frontend framework: [FRAMEWORK] (React, Vue, Angular, etc.)
- API client: [API_CLIENT] (fetch, axios, etc.)
- State management: [STATE_MANAGEMENT] (Redux, Context, etc.)
- Current auth handling: [AUTH_METHOD]

**Requirements:**
- [SPECIFIC_REQUIREMENTS]
- Handle [ERROR_CASES]
- Update [UI_COMPONENTS]
```

**Example (using AuthController signup endpoint):**
```
I've added a new API endpoint to the backend. Please suggest the corresponding frontend code changes.

**Backend Details:**
- Endpoint: POST /api/auth/signup
- Request Body: { email: string, password: string, role: string }
- Response: { token: string, role: string }
- Authentication: None required (public endpoint)
- Controller Code:
```java
@PostMapping("/signup")
public ResponseEntity<?> signup(@RequestBody Map<String, String> req) {
    String email = req.get("email");
    String password = req.get("password");
    Role role = Role.valueOf(req.get("role"));

    if (userRepo.findByEmailAndRole(email, role).isPresent()) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", "Account already exists"));
    }

    User user = new User(
            email,
            passwordEncoder.encode(password),
            role
    );

    userRepo.save(user);

    String token = jwtUtil.generateToken(user.getId(), role.name());

    return ResponseEntity.ok(
            Map.of("token", token, "role", role.name())
    );
}
```

**Frontend Context:**
- Current frontend framework: React
- API client: axios
- State management: Context API
- Current auth handling: JWT tokens stored in localStorage

**Requirements:**
- Create signup form component
- Handle validation errors
- Store JWT token on success
- Redirect to dashboard after signup
- Handle duplicate account error
```

### 2. Modified Entity/Model Template

**Template:**
```
I've modified an entity/model in the backend. Please suggest the corresponding frontend type/interface updates.

**Backend Changes:**
- Entity: [ENTITY_NAME]
- Added Fields: [NEW_FIELDS]
- Modified Fields: [CHANGED_FIELDS]
- Removed Fields: [REMOVED_FIELDS]
- Entity Code:
```
[BACKEND_ENTITY_CODE]
```

**Frontend Context:**
- TypeScript interfaces: [LOCATION]
- Current usage: [WHERE_USED]
- Validation: [VALIDATION_RULES]

**Requirements:**
- Update [INTERFACE_NAMES]
- Modify [FORM_COMPONENTS]
- Update [API_CALLS]
- Handle [BACKWARD_COMPATIBILITY]
```

**Example (using Campaign entity):**
```
I've modified an entity/model in the backend. Please suggest the corresponding frontend type/interface updates.

**Backend Changes:**
- Entity: Campaign
- Added Fields: videoDuration (string), totalAmount (double)
- Modified Fields: goalType now supports "SVLC" in addition to "S|V|L|C"
- Removed Fields: None
- Entity Code:
```java
@Entity
@Table(name = "campaigns")
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long creatorId;
    private String platform; // YOUTUBE
    private String goalType; // S | V | L | C | SVLC
    private String channelName;
    private String channelLink;
    private String contentType; // VIDEO | SHORT
    private String videoLink;
    private String videoDuration; // NEW
    private int subscriberGoal;
    private int viewsGoal;
    private int likesGoal;
    private int commentsGoal;
    private double totalAmount; // NEW
    private String status; // IN_PROGRESS | COMPLETED

    // getters and setters...
}
```

**Frontend Context:**
- TypeScript interfaces: src/types/campaign.ts
- Current usage: Campaign creation form, dashboard display
- Validation: Required fields for platform, goalType, channelName

**Requirements:**
- Update Campaign interface
- Modify campaign creation form to include videoDuration and totalAmount
- Update form validation
- Update display components to show new fields
```

### 3. Authentication/Security Changes Template

**Template:**
```
I've updated authentication/security in the backend. Please suggest corresponding frontend auth changes.

**Backend Changes:**
- Security Update: [SECURITY_CHANGE_TYPE]
- Affected Endpoints: [ENDPOINT_LIST]
- New Requirements: [AUTH_REQUIREMENTS]
- Security Code:
```
[BACKEND_SECURITY_CODE]
```

**Frontend Context:**
- Auth implementation: [AUTH_METHOD]
- Protected routes: [PROTECTED_ROUTES]
- Token handling: [TOKEN_STORAGE]

**Requirements:**
- Update [AUTH_COMPONENTS]
- Modify [API_INTERCEPTORS]
- Handle [NEW_ERROR_CODES]
- Update [LOGIN_FLOW]
```

**Example (using JWT authentication):**
```
I've updated authentication/security in the backend. Please suggest corresponding frontend auth changes.

**Backend Changes:**
- Security Update: Added JWT-based authentication
- Affected Endpoints: All /api/creator/* and /api/viewer/* endpoints
- New Requirements: Bearer token in Authorization header
- Security Code:
```java
@PostMapping("/create")
public Campaign createCampaign(
        @RequestBody Campaign campaign,
        HttpServletRequest request
) {
    String token = request.getHeader("Authorization").substring(7);
    Long creatorId = jwtUtil.extractUserId(token);
    // ... rest of method
}
```

**Frontend Context:**
- Auth implementation: JWT tokens
- Protected routes: /creator/*, /viewer/*
- Token handling: localStorage

**Requirements:**
- Add Authorization header to all API calls
- Create auth interceptor for axios
- Handle 401 responses with token refresh
- Update login component to store tokens
- Protect routes with auth guards
```

### 4. New Service Method Template

**Template:**
```
I've added a new service method in the backend. Please suggest frontend service/API client updates.

**Backend Changes:**
- Service: [SERVICE_NAME]
- New Method: [METHOD_NAME]
- Parameters: [METHOD_PARAMETERS]
- Return Type: [RETURN_TYPE]
- Service Code:
```
[BACKEND_SERVICE_CODE]
```

**Frontend Context:**
- API client location: [CLIENT_LOCATION]
- Current methods: [EXISTING_METHODS]
- Error handling: [ERROR_HANDLING]

**Requirements:**
- Add [METHOD_NAME] to API client
- Update [SERVICE_HOOKS]
- Handle [RETURN_DATA]
- Add [TEST_CASES]
```

### 5. Database Schema Changes Template

**Template:**
```
I've modified the database schema in the backend. Please suggest frontend data handling updates.

**Backend Changes:**
- Table/Entity: [TABLE_NAME]
- Schema Changes: [CHANGES_DESCRIPTION]
- Migration: [MIGRATION_DETAILS]
- Repository Code:
```
[BACKEND_REPOSITORY_CODE]
```

**Frontend Context:**
- Data fetching: [FETCH_METHODS]
- Caching: [CACHE_STRATEGY]
- Offline handling: [OFFLINE_SUPPORT]

**Requirements:**
- Update [DATA_MODELS]
- Modify [QUERY_HOOKS]
- Handle [SCHEMA_VERSIONING]
- Update [MOCK_DATA]
```

### 6. Configuration Changes Template

**Template:**
```
I've updated backend configuration. Please suggest corresponding frontend config changes.

**Backend Changes:**
- Config Type: [CONFIG_TYPE]
- New Settings: [NEW_CONFIG]
- Environment Variables: [ENV_VARS]
- Configuration Code:
```
[BACKEND_CONFIG_CODE]
```

**Frontend Context:**
- Config location: [CONFIG_LOCATION]
- Environment handling: [ENV_HANDLING]
- Build process: [BUILD_CONFIG]

**Requirements:**
- Update [CONFIG_FILES]
- Add [ENV_VARIABLES]
- Modify [BUILD_SCRIPTS]
- Update [DEPLOYMENT_CONFIG]
```

## Best Practices for Using These Templates

### 1. Be Specific About Backend Changes
- Include exact code snippets from your backend
- Specify request/response schemas clearly
- Mention authentication requirements
- Note any breaking changes

### 2. Provide Frontend Context
- Specify your frontend framework and version
- Mention state management solution
- Include current auth implementation
- Note existing patterns you want to follow

### 3. Define Clear Requirements
- List specific UI components to update
- Specify error handling needs
- Mention validation requirements
- Note any special business logic

### 4. Use Copilot Chat Effectively
- Start with the template, then ask follow-up questions
- Request code reviews of Copilot's suggestions
- Ask for alternative implementations
- Request tests for the suggested code

### 5. Iterate and Refine
- First request: Get the basic implementation
- Second request: Add error handling and edge cases
- Third request: Optimize performance and UX
- Fourth request: Add tests and documentation

## Workflow for Applying Suggestions

### Phase 1: Backend Change Analysis
1. Identify the type of backend change
2. Gather all relevant backend code snippets
3. Determine affected frontend areas
4. Fill in the appropriate template

### Phase 2: Copilot Consultation
1. Submit the filled template to Copilot Chat
2. Review the initial suggestions
3. Ask clarifying questions if needed
4. Request refinements based on your requirements

### Phase 3: Implementation
1. Create/update TypeScript interfaces
2. Implement API client methods
3. Update React components
4. Add form validation
5. Implement error handling

### Phase 4: Testing & Validation
1. Test the new functionality
2. Verify error scenarios
3. Check authentication flows
4. Validate data consistency

### Phase 5: Code Review & Refinement
1. Review Copilot's code for best practices
2. Ensure consistency with existing codebase
3. Add proper TypeScript types
4. Include comprehensive error handling

## Quick Reference

| Backend Change | Template # | Key Placeholders |
|----------------|------------|------------------|
| New Endpoint | 1 | HTTP_METHOD, ENDPOINT_URL, REQUEST_BODY_SCHEMA |
| Entity Changes | 2 | ENTITY_NAME, NEW_FIELDS, BACKEND_ENTITY_CODE |
| Auth Updates | 3 | SECURITY_CHANGE_TYPE, AUTH_REQUIREMENTS |
| Service Methods | 4 | SERVICE_NAME, METHOD_PARAMETERS |
| Schema Changes | 5 | TABLE_NAME, CHANGES_DESCRIPTION |
| Configuration | 6 | CONFIG_TYPE, NEW_CONFIG |

## Tips for Better Results

1. **Include Real Code**: Always paste actual backend code, not descriptions
2. **Specify Framework Details**: Mention React version, TypeScript usage, etc.
3. **Context is Key**: Tell Copilot about your existing patterns and conventions
4. **Iterate**: Start simple, then add complexity in follow-up requests
5. **Test Suggestions**: Always test Copilot's code before committing
6. **Review Types**: Pay special attention to TypeScript interface suggestions
7. **Error Handling**: Explicitly ask for comprehensive error handling
8. **Performance**: Request optimized implementations for data-heavy features

**********************************



The backend /api/viewer/dashboard endpoint now returns real data with the following fields:

userId
totalSubscriptions
videoViews
videoLikes
videoComments
moneyEarnings
Update your frontend dashboard component to:

Fetch data from /api/viewer/dashboard.
Display each of these fields in the UI.
Add loading and error handling for the API call.
If you need to display short views/likes/comments, let the backend team know how to identify those tasks

useEffect(() => {
  fetch('/api/viewer/dashboard', { headers: { Authorization: 'Bearer ' + token } })
    .then(res => res.json())
    .then(data => {
      setDashboardData(data);
    });
}, []);


************************************************
What to do next:

For the "Complete Task" button:
If you want to ensure every task has a valid targetLink, update your backend to always provide a valid URL or filter out tasks without a link.

For the Earnings page:
Update your frontend to fetch data from /api/viewer/earnings and display moneyEarnings for the logged-in user.

Sample React code for Earnings page:

import { useEffect, useState } from "react";

export default function Earnings() {
  const [earnings, setEarnings] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem("token");
    fetch("http://localhost:5000/api/viewer/earnings", {
      headers: { Authorization: "Bearer " + token }
    })
      .then(res => {
        if (!res.ok) throw new Error("Failed to fetch earnings");
        return res.json();
      })
      .then(data => setEarnings(data.moneyEarnings ?? 0))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h1>Earnings</h1>
      {loading && <p>Loading...</p>}
      {error && <p style={{color: "red"}}>{error}</p>}
      {earnings !== null && <p>Your earnings: ₹{earnings}</p>}
    </div>
  );
}
Summary:

Backend: Ensure every task has a valid targetLink or filter out those without.
Frontend: Update the Earnings page to fetch and display real data from /api/viewer/earnings.

************************************************************

Frontend Developer Prompt:

Ensure that every API request to /api/creator/campaign/* includes the correct JWT token in the Authorization header:

Authorization: Bearer <your_token>
Double-check that you are using the correct user account (with the CREATOR role) when accessing creator endpoints.
If you store the token in localStorage or cookies, make sure it is being read and sent with every request.
If you use Axios, set the default header:
axios.defaults.headers.common['Authorization'] = 'Bearer ' + token;
If you receive a 403 error, check if the token is expired, missing, or for the wrong user role.
If the backend was recently updated, try logging out and logging in again to refresh your token.

*********************************************

Frontend Developer Prompt:

- The backend now provides a valid target_link for each task using a dynamic join with the campaigns table. This ensures all links are always up-to-date and reliable.
- For the "Complete Task" button, use:

```js
onClick={() => {
  if (task.target_link && typeof task.target_link === "string" && task.target_link.startsWith("http")) {
    window.open(task.target_link, "_blank");
  } else {
    alert("No valid link for this task.");
  }
}}
```
- No extra mapping or logic is needed in the frontend. If a link is missing, the backend/data is at fault.
- For all /api/creator/campaign/* requests, always include the JWT token in the Authorization header:

```
Authorization: Bearer <your_token>
```
- If you use Axios, set the default header:

```js
axios.defaults.headers.common['Authorization'] = 'Bearer ' + token;
```
- If you receive a 403 error, check if the token is expired, missing, or for the wrong user role. Try logging out and logging in again to refresh your token.

This approach is scalable, maintainable, and ensures your frontend always works with the latest backend data.

***************************** 
Prompt for Backend Team
We are receiving 403 Forbidden errors from all /api/creator/campaigns endpoints and /api/creator/campaign/create even with a valid Authorization header. Please check:

Is the token validation logic correct and accepting valid tokens?
Is the user role in the token set to CREATOR?
Is the SecurityConfig mapping /api/creator/** to require only the CREATOR role?
Is the JWT filter setting the correct authorities (ROLE_CREATOR) in the authentication context?
Are there any CORS or filter issues that could block the request?
Please review the backend and ensure that:

The endpoints match the documented API.
Valid tokens with the CREATOR role are accepted.
The frontend can access these endpoints without 403 errors.
**************************************

ALTER TABLE campaigns MODIFY current_comments INT DEFAULT 0;
ALTER TABLE campaigns MODIFY current_likes INT DEFAULT 0;
ALTER TABLE campaigns MODIFY current_views INT DEFAULT 0;
ALTER TABLE campaigns MODIFY current_subscribers INT DEFAULT 0;
SHOW COLUMNS FROM campaigns;

*************************************************************


Frontend Prompt: Creator Dashboard Campaign Refresh & Tab Clickability
Objective:
Ensure that after a campaign is created, the creator dashboard updates to show the new campaign, and the "In Progress" and "Completed Goals" tabs are always clickable and fetch the correct data.

Requirements:

Refresh Campaign List After Creation

After a successful campaign creation (POST /api/creator/campaign/create), immediately re-fetch the campaign list for the "In Progress" tab using:
GET /api/creator/campaigns/list?status=IN_PROGRESS

Update the dashboard state/UI with the new campaign data.
Tab Clickability

The "In Progress" and "Completed Goals" tabs should always be clickable, regardless of whether there are campaigns in those states.
On tab click, fetch campaigns for the selected status:
"In Progress": GET /api/creator/campaigns/list?status=IN_PROGRESS
"Completed Goals": GET /api/creator/campaigns/list?status=COMPLETED
Display a message like "No campaigns found" if the list is empty, but do not disable the tabs.
UI/UX

Ensure the tab click handler updates the visible campaign list and highlights the active tab.
If using React, update the state after each fetch and re-render the campaign list.
Sample React Pseudocode:

// After successful campaign creation
await axios.post('/api/creator/campaign/create', campaignData);
fetchCampaigns('IN_PROGRESS'); // Refresh dashboard

// Tab click handler
function handleTabClick(status) {
  fetchCampaigns(status);
  setActiveTab(status);
}

async function fetchCampaigns(status) {
  const res = await axios.get([/api/creator/campaigns/list?status=${status}](http://_vscodecontentref_/0));
  setCampaigns(res.data);
}

Acceptance Criteria:

Creating a campaign immediately updates the dashboard.
"In Progress" and "Completed Goals" tabs are always clickable and fetch data.
UI shows campaigns or a "No campaigns found" message as appropriate.
Share this with your frontend team or use it to update your React code! If you want a more detailed code sample, let me know your frontend stack (React, Vue, etc.).Acceptance Criteria:

Creating a campaign immediately updates the dashboard.
"In Progress" and "Completed Goals" tabs are always clickable and fetch data.
UI shows campaigns or a "No campaigns found" message as appropriate.



*************************************************************
Frontend Prompt
Fetch /api/viewer/tasks?page=0&size=10 (or the desired page/size).
Render up to 10 cards per page.
Use the totalPages and currentPage fields to implement pagination controls.


Frontend Context:

Framework: React (or similar)
Auth: JWT in localStorage
UI: "My Tasks" page with cards for each task and pagination controls
Requirements:

Fetch tasks from /api/viewer/tasks?page=0&size=10 (update page as user navigates).
Display up to 10 task cards per page.
Show pagination controls (next/prev, page numbers) using totalPages and currentPage.
When the user changes the page, fetch the corresponding tasks.
If tasks is empty, show a "No tasks available" message.
Always include the JWT token in the Authorization header.
Sample React Pseudocode:

const [tasks, setTasks] = useState([]);
const [page, setPage] = useState(0);
const [totalPages, setTotalPages] = useState(1);

useEffect(() => {
  const token = localStorage.getItem('token');
  fetch(`/api/viewer/tasks?page=${page}&size=10`, {
    headers: { Authorization: 'Bearer ' + token }
  })
    .then(res => res.json())
    .then(data => {
      setTasks(data.tasks);
      setTotalPages(data.totalPages);
    });
}, [page]);

// Render tasks and pagination controls

Acceptance Criteria:

"My Tasks" page shows up to 10 tasks per page.
Pagination controls allow navigation between pages.
Tasks update as the user changes pages.
"No tasks available" message is shown if there are no tasks.
********************************************
Frontend Prompt:

After a successful campaign creation (POST /api/creator/campaign/create), immediately refresh the creator dashboard by fetching the updated list of campaigns.
Use the endpoint: GET /api/creator/campaigns/list?status=IN_PROGRESS
Always include the JWT token in the Authorization header.
Sample React Pseudocode:

// After successful campaign creation
await axios.post('/api/creator/campaign/create', campaignData, {
  headers: { Authorization: 'Bearer ' + token }
});
fetchCampaigns('IN_PROGRESS'); // Refresh dashboard

// Tab click handler
function handleTabClick(status) {
  fetchCampaigns(status);
  setActiveTab(status);
}

async function fetchCampaigns(status) {
  const res = await axios.get(`/api/creator/campaigns/list?status=${status}`, {
    headers: { Authorization: 'Bearer ' + token }
  });
  setCampaigns(res.data);
}

Acceptance Criteria:

Creating a campaign immediately updates the dashboard.
"In Progress" and "Completed Goals" tabs are always clickable and fetch data.
UI shows campaigns or a "No campaigns found" message as appropriate


***********************************************************

Frontend Prompt (for error handling):

If you receive a response with an "error" field, display the error message to the user (e.g., "Missing required fields: ...").
After successful campaign creation, refresh the dashboard by fetching the updated campaign list.
Example React error handling:

try {
  const res = await axios.post('/api/creator/campaign/create', campaignData, {
    headers: { Authorization: 'Bearer ' + token }
  });
  if (res.data.error) {
    alert(res.data.error);
  } else {
    fetchCampaigns('IN_PROGRESS'); // Refresh dashboard
  }
} catch (err) {
  alert('Failed to create campaign: ' + (err.response?.data?.error || err.message));
}
******************************************************

Frontend Prompt:

Update your frontend code to ensure the campaign creation request includes both totalAmount (a number > 0) and status (a non-empty string, e.g., "IN_PROGRESS").

Example payload:

{
  "platform": "YOUTUBE",
  "goalType": "SVLC",
  "channelName": "saikumar TECH",
  "channelLink": "https://www.youtube.com/@Tech_with_future",
  "commentsGoal": 2,
  "contentType": "VIDEO",
  "likesGoal": 2,
  "subscriberGoal": 2,
  "videoDuration": "<5min",
  "videoLink": "https://www.youtube.com/watch?v=V0cn7sePS4o",
  "viewsGoal": 2,
  "totalAmount": 100,           // <-- REQUIRED
  "status": "IN_PROGRESS"       // <-- REQUIRED
}
React Example:

const campaignData = {
  // ...other fields,
  totalAmount: Number(form.totalAmount), // ensure this is > 0
  status: "IN_PROGRESS" // or "COMPLETED" as needed
};

await axios.post('/api/creator/campaign/create', campaignData, {
  headers: { Authorization: 'Bearer ' + token }
});

Checklist for Frontend:

Add a field for totalAmount in your form and ensure it is sent in the request.
Set status to "IN_PROGRESS" or "COMPLETED" before sending the request.
Validate these fields before making the API call.


**************************************************



Frontend Prompt for JSON Response:

Your backend now runs on port 5173 and always returns JSON for /api/viewer/tasks.
Update your frontend code to fetch tasks like this:

const token = localStorage.getItem('token');
fetch('/api/viewer/tasks?page=0&size=10', {
  headers: { Authorization: 'Bearer ' + token }
})
  .then(res => res.json())
  .then(data => {
    setTasks(data.tasks);
    setTotalPages(data.totalPages);
  });

No need for a proxy or full URL.
Always include the JWT token in the Authorization header.
Handle the JSON response as shown above.


Frontend Prompt: API Integration with Backend on Port 5000

The backend API is running on http://localhost:5000.
All API requests (e.g., /api/viewer/tasks, /api/creator/campaign/create) must be sent to http://localhost:5000.
If your frontend runs on a different port (e.g., 5173), you must use the full backend URL or set up a proxy.
Option 1: Use Full URL in Fetch/Axios

const token = localStorage.getItem('token');
fetch('http://localhost:5000/api/viewer/tasks?page=0&size=10', {
  headers: { Authorization: 'Bearer ' + token }
})
  .then(res => res.json())
  .then(data => {
    setTasks(data.tasks);
    setTotalPages(data.totalPages);
  });
  Option 2: Vite Proxy Setup (Recommended)

In vite.config.js, add:

export default {
  server: {
    proxy: {
      '/api': 'http://localhost:5000'
    }
  }
}

Checklist:

Always include the JWT token in the Authorization header.
Handle JSON responses as shown above.
If you get HTML instead of JSON, check that the backend is running on port 5000 and your frontend is calling the correct URL.


**************************************************************





I've updated the backend to allow the creator to specify the number of tasks for each type when creating a campaign.

Backend Details:

Endpoint: POST /api/creator/campaign/create
Request Body:
subscriberTaskCount (number)
viewsTaskCount (number)
likesTaskCount (number)
commentsTaskCount (number)
(plus all existing campaign fields)
Response: Campaign object
Authentication: Bearer token required
Frontend Context:

Framework: React (TypeScript recommended)
API client: fetch or axios
Auth: JWT in localStorage
Requirements:

Update the campaign creation form to include fields for the number of tasks for each type (subscriber, view, like, comment).
Send these fields in the POST request body.
Validate that each task count is a positive integer.
After successful creation, refresh the creator dashboard.



************************************************



Here’s a prompt for your frontend team to fix the tab clickability issue:

Frontend Prompt: Make "In Progress" and "Completed Goals" Tabs Always Clickable

Ensure the "In Progress" and "Completed Goals" tabs are always clickable, even if there are no campaigns in those states.
On tab click, fetch campaigns for the selected status:
"In Progress": GET /api/creator/campaigns/list?status=IN_PROGRESS
"Completed Goals": GET /api/creator/campaigns/list?status=COMPLETED
If the campaign list is empty, display a message like "No campaigns found" but do not disable the tabs.
The tab click handler should update the visible campaign list and highlight the active tab.
Always include the JWT token in the Authorization header for API requests.
Sample React Pseudocode:

function handleTabClick(status) {
  fetchCampaigns(status);
  setActiveTab(status);
}

async function fetchCampaigns(status) {
  const res = await axios.get(`/api/creator/campaigns/list?status=${status}`, {
    headers: { Authorization: 'Bearer ' + token }
  });
  setCampaigns(res.data);
}


Acceptance Criteria:

Tabs are always clickable.
Campaigns are fetched and displayed for the selected tab.
If no campaigns, show a "No campaigns found" message.
UI updates correctly on tab change.



*******************************************************

If you are still seeing missing or empty fields in the frontend:

Check the CreatorStats entity and service:

Make sure the getters (getSubscribers, getVideoViews, etc.) return Integer (not int) and are never null after fetching/creating stats.
Ensure CreatorStatsService.getOrCreateStats always returns a fully initialized CreatorStats object.
Check your database:

If the stats are not being updated, the values will always be 0. Make sure your backend logic is updating CreatorStats for the creator.
Check the frontend:

Make sure the frontend is reading the correct fields from the response and not expecting a different structure.
**************************************************

- Make sure the Authorization header is set as: Bearer <creator_jwt_token>
- Decode the JWT you are sending and confirm the "role" is "CREATOR"
- If you have a login flow, ensure you are logging in as a creator and storing the correct token
- Double-check that the token sent in the request is not expired and is for a creator account
- If you are using localStorage/sessionStorage, verify the correct token is being read and sent
- If you are using fetch/axios, log the outgoing headers to confirm the Authorization header is correct

***********************************************

Frontend Prompt
Give this prompt to your frontend team:

Frontend Team Prompt: Multi-Step Campaign Creation Wizard (as per UI mockup)

Objective:
Implement a multi-step campaign creation wizard matching the attached UI (second image), with each step collecting only the relevant fields and using "Back" and "Proceed" navigation.

Steps:
1. Step 1: Select Goal
   - Show a dropdown or buttons for goal selection (S, V, L, C, or combinations).
   - On "Proceed", store the selected goal(s) and move to the next step.
2. Step 2: Fill Details
   - Show fields for:
     - Channel Name
     - Channel Link for Subscription
   - On "Proceed", validate these fields (locally or via /api/creator/campaign/validate-details).
3. Step 3: Content Type
   - Show a dropdown for Content Type (Video/Short).
   - On "Proceed", store the selection.
4. Step 4: Video Link
   - Show a field for "Video Link for Views/Likes/Comments".
   - On "Proceed", validate.
5. Step 5: Video Duration
   - Show a dropdown for video duration (e.g., "<= 5 Min").
   - On "Proceed", validate.
6. Step 6: Review & Submit
   - Show a summary of all entered data.
   - On "Proceed", call /api/creator/campaign/create with all collected fields.

Validation:
- At each step, validate the required fields before allowing the user to proceed.
- Use /api/creator/campaign/validate-details for backend validation if needed.

UX:
- Show "Back" and "Proceed" buttons at each step.
- Highlight the current step in the wizard header.
- Only show fields relevant to the current step.

Sample React State Structure:
```js
const [step, setStep] = useState(1);
const [form, setForm] = useState({
  goalType: "",
  channelName: "",
  channelLink: "",
  contentType: "",
  videoLink: "",
  videoDuration: ""
  // ...other fields as needed
});
```

Sample Step Navigation:
```js
function handleProceed() {
  // Validate current step fields
  // If valid, setStep(step + 1)
}
function handleBack() {
  setStep(step - 1);
}
```

Summary:
- Implement a multi-step wizard UI, not a single form.
- Each step collects only the fields shown in the second image.
- Use "Proceed" and "Back" to navigate.
- Validate at each step.
- On the final step, submit all data to the backend.

If you want a sample React component structure for this flow, let the backend team know!
*****************************************


Frontend Prompt for API Authentication (React + Axios Example):

Update all API requests to the backend (e.g., /api/creator/campaign/validate-details, /api/creator/campaign/create, etc.) to include the JWT token in the Authorization header.

Example using Axios:

import axios from "axios";

// Get the JWT token from localStorage, cookies, or your auth provider
const token = localStorage.getItem("jwtToken"); // or however you store it

// Example API call with Authorization header
axios.post(
  "http://localhost:5000/api/creator/campaign/validate-details",
  requestData,
  {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    // If using cookies for auth, also add:
    // withCredentials: true,
  }
)
.then(response => {
  // handle success
})
.catch(error => {
  // handle error
});


Make sure the token is valid and not expired.
The user must have the CREATOR role for these endpoints.
If you use cookies for authentication, ensure withCredentials: true is set and CORS is configured on the backend.
Summary:
All protected backend endpoints require a valid JWT token in the Authorization header. Without it, you will get a 403 Forbidden error.

Let me know if you need a version for fetch or another frontend framework!> import axios from "axios";

// Get the JWT token from localStorage, cookies, or your auth provider
const token = localStorage.getItem("jwtToken"); // or however you store it

// Example API call with Authorization header
axios.post(
  
"http://localhost:5000/api/creator/campaign/validate-details",
requestData,
{
headers: {
Authorization: Bearer ${token},
},
// If using cookies for auth, also add:
// withCredentials: true,
}
)
.then(response => {
// handle success
})
.catch(error => {
// handle error
});


- Make sure the token is valid and not expired.
- The user must have the CREATOR role for these endpoints.
- If you use cookies for authentication, ensure `withCredentials: true` is set and CORS is configured on the backend.

**Summary:**  
All protected backend endpoints require a valid JWT token in the Authorization header. Without it, you will get a 403 Forbidden error.



************************************************


Frontend Fix Required for Viewer Panel
Issue Summary
The Viewer Panel has issues with task completion and dashboard stats not updating properly.

Backend API Endpoints (Updated)
1. Get Active Tasks (Paginated)
GET /api/viewer/tasks?page=0&size=10
Authorization: Bearer <token>

Response:
{
  "content": [
    {
      "id": 4,
      "campaignId": 9,
      "taskType": "SUBSCRIBE",
      "targetLink": "https://www.youtube.com/@channel",
      "earning": 4.5,
      "status": "OPEN",
      "createdAt": "2026-02-02T12:58:11"
    }
  ],
  "totalElements": 4,
  "totalPages": 1,
  "number": 0,
  "size": 10
}

Complete a Task (Single Step - Grab + Submit Combined)

POST /api/viewer/tasks/complete
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{ "id": 4 }

Success Response:
{
  "status": "success",
  "message": "Task completed successfully",
  "earning": 4.5
}

Error Response:
{
  "status": "error", 
  "message": "Task not available or already completed"
}

3. Dashboard Stats


GET /api/viewer/tasks/dashboard
Authorization: Bearer <token>

Response:
{
  "completedTasksCount": 2,
  "totalEarnings": 9.0,
  "availableTasksCount": 4
}


Frontend Changes Needed
Active Tasks Page (viewerService.js or similar)

Call POST /api/viewer/tasks/complete with { id: taskId }
On success, show success message and refresh the tasks list
On error, show the error message from response
Complete Button Handler

const completeTask = async (taskId) => {
  try {
    const response = await axios.post('/api/viewer/tasks/complete', 
      { id: taskId },
      { headers: { Authorization: `Bearer ${token}` } }
    );
    
    if (response.data.status === 'success') {
      alert(`Task completed! You earned ₹${response.data.earning}`);
      // Refresh task list
      fetchTasks();
      // Refresh dashboard stats
      fetchDashboard();
    } else {
      alert(response.data.message || 'Failed to complete task');
    }
  } catch (error) {
    alert(error.response?.data?.message || 'Failed to complete task');
  }
};

Dashboard Page
Fetch from GET /api/viewer/tasks/dashboard
Display availableTasksCount for "Available Tasks"
Display completedTasksCount for "Completed Tasks"
Display totalEarnings for "Total Earnings"
Testing Steps
Login as viewer
Go to Active Tasks - should see tasks with "Complete" button
Click "Complete" - should show success and earn ₹4.5
Task should disappear from Active Tasks
Dashboard should update: Completed Tasks +1, Total Earnings +₹4.5, Available Tasks -1
****************************************


ere's a comprehensive prompt for the frontend developer to implement the corresponding UI changes:

Frontend Implementation Prompt for Grow-Earn Profile & Admin Features
Backend API Base URL: http://localhost:5000
1. Profile Page - WhatsApp Number Field
Endpoints:

GET /api/viewer/profile - Get viewer profile
PUT /api/viewer/profile - Update viewer profile
GET /api/creator/profile - Get creator profile
PUT /api/creator/profile - Update creator profile
Request Body for Update:

{
  "fullName": "John Doe",
  "whatsappNumber": "9876543210",
  "channelName": "@mychannel",
  "profilePicUrl": "https://example.com/pic.jpg"
}

Response includes both phoneNumber and whatsappNumber - use whatsappNumber for the form field.

2. Account Status Section (Add Below Password Change)
The profile response now includes an accountStatus object:

{
  "id": 28,
  "email": "viewer4@gmail.com",
  "fullName": "viewer4",
  "whatsappNumber": "1234567890",
  "channelName": "@Tech_with_future",
  "accountStatus": {
    "status": "ACTIVE",
    "statusLabel": "Active",
    "statusMessage": "Your account is in good standing.",
    "isActive": true,
    "isSuspended": false,
    "isBanned": false,
    "suspensionUntil": null,
    "suspensionExpired": false,
    "strikeCount": 0,
    "maxStrikes": 3,
    "totalViolations": 0,
    "recentViolations": []
  }
}

UI Components to Build:

// Account Status Section Component
<div className="account-status-section">
  <h3>Account Status</h3>
  
  {/* Status Cards Row */}
  <div className="status-cards">
    {/* Warnings Card */}
    <div className="status-card warning-card">
      <span className="icon">⚠️</span>
      <span className="label">Warnings</span>
      <span className="count">{warningsCount}</span>
    </div>
    
    {/* Strikes Card */}
    <div className="status-card strike-card">
      <span className="icon">🚫</span>
      <span className="label">Strikes</span>
      <span className="count">{strikeCount} / {maxStrikes}</span>
    </div>
  </div>
  
  {/* Status Banner (show only if suspended or banned) */}
  {accountStatus.isSuspended && (
    <div className="status-banner suspended">
      <span>⏸️ Account Suspended</span>
      <p>Your account is temporarily suspended until {formatDate(accountStatus.suspensionUntil)}</p>
    </div>
  )}
  
  {accountStatus.isBanned && (
    <div className="status-banner banned">
      <span>🚫 Account Banned</span>
      <p>Your account has been permanently banned due to multiple policy violations.</p>
    </div>
  )}
  
  {/* Recent Violations List (if any) */}
  {accountStatus.recentViolations.length > 0 && (
    <div className="violations-list">
      <h4>Recent Violations</h4>
      {accountStatus.recentViolations.map(v => (
        <div className="violation-item" key={v.createdAt}>
          <span className={`badge ${v.type === 'STRIKE' ? 'strike' : 'warning'}`}>
            {v.type === 'STRIKE' ? '🚫 Strike' : '⚠️ Warning'}
          </span>
          <span className="description">{v.description}</span>
          <span className="date">{formatDate(v.createdAt)}</span>
        </div>
      ))}
    </div>
  )}
</div>


Calculate Warnings Count:
const warningsCount = accountStatus.recentViolations.filter(v => v.type === 'WARNING').length;


3. Admin Violations Page - Remove Violation Feature
DELETE Endpoint:

DELETE /api/admin/violations/{violationId}

DELETE /api/admin/violations/{violationId}

Headers:

Authorization: Bearer <admin_jwt_token>


Success Response:

{
  "success": true,
  "message": "Violation removed successfully",
  "violationId": 5,
  "userId": 30,
  "violationType": "WARNING",
  "newStrikeCount": 0
}


Frontend Implementation:
const handleRemoveViolation = async (violationId) => {
  try {
    const response = await fetch(`http://localhost:5000/api/admin/violations/${violationId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (response.ok) {
      const data = await response.json();
      showToast('success', 'Violation removed successfully');
      // Refresh violations list
      fetchUserViolations(selectedUserId);
    } else {
      showToast('error', 'Failed to remove violation');
    }
  } catch (error) {
    showToast('error', 'Failed to remove violation');
  }
};


4. Admin - Clear All Violations for User
DELETE Endpoint:

DELETE /api/admin/violations/user/{userId}
Success Response:

{
  "success": true,
  "message": "All violations cleared for user",
  "userId": 30,
  "violationsRemoved": 3,
  "newStrikeCount": 0
}

5. Issue Warning/Strike APIs
Issue Warning (no strike added):

POST /api/admin/users/{userId}/violations/warning
{
  "violationType": "CONTENT_POLICY",
  "description": "Inappropriate content posted"
}

Issue Strike (auto-enforces: 1st=warning, 2nd=7-day suspension, 3rd=ban):

POST /api/admin/users/{userId}/violations/strike


{
  "violationType": "SPAM",
  "description": "Repeated spam activity"
}

Strike Response includes action taken:


{
  "success": true,
  "action": "SUSPENDED_7_DAYS",
  "userId": 30,
  "strikeCount": 2,
  "maxStrikes": 3,
  "message": "User suspended for 7 days after 2 strikes"
}


6. Styling Suggestions
/* Account Status Section */
.account-status-section {
  margin-top: 2rem;
  padding: 1.5rem;
  border-radius: 12px;
  background: #f8f9fa;
}

.status-cards {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.status-card {
  flex: 1;
  padding: 1rem;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.status-card.warning-card {
  background: linear-gradient(135deg, #fff3cd, #ffeeba);
  border: 1px solid #ffc107;
}

.status-card.strike-card {
  background: linear-gradient(135deg, #ffe6e6, #ffcccc);
  border: 1px solid #dc3545;
}

.status-banner.suspended {
  background: #fff3cd;
  border-left: 4px solid #ffc107;
  padding: 1rem;
  border-radius: 4px;
}

.status-banner.banned {
  background: #f8d7da;
  border-left: 4px solid #dc3545;
  padding: 1rem;
  border-radius: 4px;
}

.violation-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem;
  border-bottom: 1px solid #eee;
}

.badge.strike {
  background: #dc3545;
  color: white;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.badge.warning {
  background: #ffc107;
  color: #333;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}


Summary of Required Changes:
Page	Feature	API Endpoint
Viewer Profile	WhatsApp Number save/display	PUT /api/viewer/profile with whatsappNumber
Creator Profile	WhatsApp Number save/display	PUT /api/creator/profile with whatsappNumber
Both Profiles	Account Status section	Read from accountStatus object in profile response
Admin Violations	Remove violation button	DELETE /api/admin/violations/{violationId}
Admin Violations	Clear all violations	DELETE /api/admin/violations/user/{userId}
Admin Violations	Issue Warning	POST /api/admin/users/{userId}/violations/warning
Admin Violations	Issue Strike	POST /api/admin/users/{userId}/violations/strike



*********************************