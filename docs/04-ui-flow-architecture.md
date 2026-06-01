# EduLearn Platform — UI Flow & Screen Architecture

Base URL: `http://localhost:8080`

---

## 1. Entry Point & Routing

```
http://localhost:8080/
        │
        ▼
  login.html  (split screen: Admin | Teacher tab)
        │
   JWT stored in sessionStorage
        │
  ┌─────┴──────┐
  │            │
admin/       teacher/
dashboard    dashboard
.html        .html
```

Static files served from Spring Boot `src/main/resources/static/`.

---

## 2. Screen Inventory

| File | Role | Description |
|------|------|-------------|
| `login.html` | Public | Split login — toggle Admin / Teacher |
| `admin/dashboard.html` | Admin | Stats, pending approvals, quick actions |
| `admin/courses.html` | Admin | Tree sidebar + detail form panel |
| `admin/submissions.html` | Admin | Pending approval queue |
| `admin/users.html` | Admin | Teacher & student management |
| `teacher/dashboard.html` | Teacher | Stats, my submissions status |
| `teacher/courses.html` | Teacher | Same tree UI — submit for approval flow |
| `teacher/submissions.html` | Teacher | My submissions tracker |

---

## 3. Login Screen Flow

```
┌─────────────────────────────────────────────────┐
│  Left panel (blue/green)    │  Right panel (form) │
│  Role branding + features   │  [Admin] [Teacher]  │
│                             │  Email ____________ │
│                             │  Password _________ │
│                             │  [Sign in]          │
│                             │  [SSO]              │
└─────────────────────────────────────────────────┘
```

- Toggle pill switches left panel colour and form context.
- On success → redirect to role-specific dashboard.
- JWT stored in `sessionStorage` (cleared on tab close).

---

## 4. Admin Dashboard Layout

```
┌──────────┬──────────────────────────────────────┐
│  NAV     │  TOPBAR  (breadcrumb + search + bell) │
│          ├──────────────────────────────────────┤
│ Dashboard│                                       │
│ Courses  │   [Stats cards × 4]                  │
│ Approvals│                                       │
│ Teachers │   Pending approvals list              │
│ Students │                                       │
│ Reports  │   Quick action cards                  │
│ Settings │                                       │
│          │                                       │
│ [avatar] │                                       │
└──────────┴──────────────────────────────────────┘
```

---

## 5. Course Management (Tree + Detail Panel)

```
┌──────────┬─────────────────────┬────────────────────────┐
│  NAV     │  TREE SIDEBAR       │  DETAIL PANEL          │
│          │                     │                        │
│          │  ▶ Year 10          │  [Form fields for      │
│          │    ▼ English        │   selected node]       │
│          │      ▶ Topic A      │                        │
│          │      ▶ Topic B ●    │  [Save] [Approve]      │
│          │    ▶ Maths          │  [Add child] [Delete]  │
│          │                     │                        │
│          │  [+ Add Category]   │                        │
└──────────┴─────────────────────┴────────────────────────┘
  ● = pending badge
```

**Tree interaction rules:**
- Click `›` chevron → expand/collapse children.
- Click node label → load detail form in right panel.
- Hover → show `+` (add child) and `🗑` (delete) action icons.
- Admin-owned nodes shown with lock icon for teachers.

---

## 6. Teacher Course Screen Differences

| Behaviour | Admin | Teacher |
|-----------|-------|---------|
| Save button label | Save | Submit for approval |
| New node default status | APPROVED | DRAFT |
| Can edit admin-owned nodes | Yes | No (read-only banner) |
| Can delete any node | Yes | Own nodes only |
| Approve button visible | Yes | No |
| Re-edit pending item | Yes | Yes (re-submits) |

---

## 7. Approval Status Colours

| Status | Indicator |
|--------|-----------|
| DRAFT | Grey badge |
| PENDING | Amber badge |
| APPROVED | Green badge (hidden for admin-owned to reduce noise) |
| REJECTED | Red badge |

---

## 8. Navigation Token (JWT) Lifecycle

```
Login → POST /api/v1/auth/login
     ← { token, role, name }
     → sessionStorage.setItem('edu_token', token)

Every API call → Authorization: Bearer <token>

Logout / tab close → sessionStorage.clear()
                   → redirect to login.html
```

---

## 9. Frontend File Structure

```
src/main/resources/static/
├── login.html
├── css/
│   └── main.css
├── js/
│   ├── auth.js          # login / logout / token helpers
│   ├── api.js           # fetch wrapper with Authorization header
│   └── tree.js          # reusable tree component
├── admin/
│   ├── dashboard.html
│   ├── courses.html
│   └── submissions.html
└── teacher/
    ├── dashboard.html
    ├── courses.html
    └── submissions.html
```
