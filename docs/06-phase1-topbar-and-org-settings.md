# EduLearn Platform — Phase 1: Topbar Controls & Organisation Settings

## What This Document Covers

Changes made in Phase 1 Sprint 1:
1. Admin activity controls moved to **top-right corner** of the dashboard
2. **Notification panel** with unread badges
3. **User dropdown menu** with profile, org settings, and logout
4. New **Organisation Settings** page (admin-only)
5. New **Organisation Settings** DB table and API endpoints

---

## 1. Topbar Right-Side Controls

### Layout (left → right)

```
[Search 🔍]  [Notifications 🔔]  |  [Avatar · Admin User · Administrator ▾]
```

### Search button
- Icon button (magnifying glass)
- Placeholder for future full-text search modal
- Backend endpoint (future): `GET /api/v1/search?q=term`

### Notifications bell
- Shows a red dot badge when unread notifications exist
- Clicking opens a dropdown panel (320px wide)
- Panel has "Mark all read" button → clears all dots, hides red badge
- Unread items have blue dot + light blue background
- Read items have hollow grey dot

**Notification types to support (backend):**
| Event | Message template |
|-------|-----------------|
| Teacher submits content | `{teacher} submitted {item} for approval` |
| Teacher account created | `New teacher {name} account created` |
| Content approved | `{item} approved by admin` |
| Content rejected | `{item} rejected — reason attached` |

**Future API endpoint:**
```
GET  /api/v1/notifications?page=0&size=20
PUT  /api/v1/notifications/{id}/read
PUT  /api/v1/notifications/read-all
```

### User pill / dropdown menu

Clicking the user pill (top right) opens a dropdown with:

| Item | Action |
|------|--------|
| My Profile | Opens profile edit page (future module) |
| Organisation Settings | Navigates to `/admin/org-settings.html` |
| Preferences | Opens preferences panel (future module) |
| ─── | Divider |
| Sign out | Clears `sessionStorage`, redirects to `/login.html` |

**Behaviour rules:**
- Clicking outside the dropdown closes it
- Chevron rotates 180° when open
- Sign out is styled in red with red hover background
- Only one panel (dropdown or notifications) can be open at a time

---

## 2. Organisation Settings Page

**Route:** `/admin/org-settings.html` (or rendered as a page within `admin-dashboard.html`)
**Access:** Admin only — show `"Admin only"` chip badge in page header
**Nav location:** Left sidebar under **System** section + accessible from user dropdown

### Page sections

#### 2.1 Branding
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| Organisation logo | File upload | No | PNG/JPG/SVG, max 2MB, 200×200px recommended |
| Organisation name | Text | Yes | Full legal trading name |
| Display / short name | Text | No | Used in UI headers |

Logo upload behaviour:
- Click box or "Choose file" button → file picker
- On select → preview shown immediately in the upload box
- Stored as file path in DB, served from `/static/uploads/org/`

#### 2.2 Legal Details
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| Legal entity name | Text | Yes | e.g. EduLearn Institute Ltd |
| Registration number | Text | Yes | Company/charity reg number |
| Registration date | Date | No | |
| Registered country | Select | Yes | Dropdown of countries |
| Tax / VAT number | Text | No | e.g. GB123456789 |
| Organisation type | Select | Yes | Secondary School, Primary School, College, University, Private Tutoring Centre, Other |

#### 2.3 Registered Address
| Field | Type | Required |
|-------|------|----------|
| Address line 1 | Text | Yes |
| Address line 2 | Text | No |
| City | Text | Yes |
| County / State | Text | No |
| Postcode | Text | Yes |
| Country | Select | Yes |

#### 2.4 Points of Contact
Two contacts supported: **Primary** (required) and **Secondary** (optional).

| Field | Type | Required (Primary) |
|-------|------|-------------------|
| Full name | Text | Yes |
| Job title | Text | No |
| Email | Email | Yes |
| Phone | Tel | No |

#### 2.5 Academic Information
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| Academic year start | Date | No | |
| Academic year end | Date | No | |
| Curriculum framework | Select | No | UK National Curriculum, IB, Cambridge, CBSE, Custom |
| Accreditation number | Text | No | Ofsted ref etc. |
| Website | URL | No | |
| About the organisation | Textarea | No | Mission statement, max 1000 chars |

### Save behaviour
- Each section has its own **Save** and **Discard** button
- Saving a section calls its own API endpoint (see below)
- Discarding resets that section's fields to last saved values
- Toast notification on success/error

---

## 3. Database — Organisation Settings Table

```sql
CREATE TABLE organisation_settings (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Branding
    org_name            VARCHAR(200) NOT NULL,
    display_name        VARCHAR(100),
    logo_path           VARCHAR(500),

    -- Legal
    legal_entity_name   VARCHAR(200),
    registration_number VARCHAR(100),
    registration_date   DATE,
    registered_country  VARCHAR(100),
    vat_number          VARCHAR(100),
    org_type            VARCHAR(50)  CHECK (org_type IN (
                            'SECONDARY_SCHOOL','PRIMARY_SCHOOL','COLLEGE',
                            'UNIVERSITY','TUTORING_CENTRE','OTHER'
                        )),

    -- Address
    address_line1       VARCHAR(200),
    address_line2       VARCHAR(200),
    city                VARCHAR(100),
    county              VARCHAR(100),
    postcode            VARCHAR(20),
    country             VARCHAR(100),

    -- Primary contact
    poc1_name           VARCHAR(120),
    poc1_title          VARCHAR(100),
    poc1_email          VARCHAR(200),
    poc1_phone          VARCHAR(30),

    -- Secondary contact
    poc2_name           VARCHAR(120),
    poc2_title          VARCHAR(100),
    poc2_email          VARCHAR(200),
    poc2_phone          VARCHAR(30),

    -- Academic info
    academic_year_start DATE,
    academic_year_end   DATE,
    curriculum          VARCHAR(100),
    accreditation_no    VARCHAR(100),
    website             VARCHAR(300),
    about_text          TEXT,

    -- Audit
    updated_by          UUID         REFERENCES users(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Only one row ever (singleton pattern)
-- Enforce via application logic or unique constraint on a fixed key:
INSERT INTO organisation_settings (org_name) VALUES ('My Organisation')
ON CONFLICT DO NOTHING;

CREATE TRIGGER trg_org_upd
    BEFORE UPDATE ON organisation_settings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

---

## 4. API Endpoints — Organisation Settings

Base: `http://localhost:8080/api/v1`
Access: **Admin only** for all write operations. GET is readable by all authenticated users.

| Method | Path | Description |
|--------|------|-------------|
| GET    | `/org/settings` | Get full org settings |
| PUT    | `/org/settings/branding` | Update branding section |
| PUT    | `/org/settings/legal` | Update legal details |
| PUT    | `/org/settings/address` | Update address |
| PUT    | `/org/settings/contacts` | Update points of contact |
| PUT    | `/org/settings/academic` | Update academic info |
| POST   | `/org/settings/logo` | Upload logo (multipart/form-data) |

### Sample payloads

**GET /org/settings response:**
```json
{
  "status": "success",
  "data": {
    "orgName": "EduLearn Institute",
    "displayName": "EduLearn",
    "logoUrl": "/static/uploads/org/logo.png",
    "legalEntityName": "EduLearn Institute Ltd",
    "registrationNumber": "12345678",
    "registrationDate": "2020-01-15",
    "orgType": "SECONDARY_SCHOOL",
    "address": {
      "line1": "123 School Lane",
      "city": "London",
      "postcode": "EC1A 1BB",
      "country": "United Kingdom"
    },
    "primaryContact": {
      "name": "Dr. Jane Smith",
      "title": "Principal",
      "email": "head@edulearn.edu",
      "phone": "+44 20 7946 0958"
    },
    "academicYearStart": "2025-09-01",
    "academicYearEnd": "2026-07-15",
    "curriculum": "UK National Curriculum"
  }
}
```

**PUT /org/settings/legal request:**
```json
{
  "legalEntityName": "EduLearn Institute Ltd",
  "registrationNumber": "12345678",
  "registrationDate": "2020-01-15",
  "registeredCountry": "United Kingdom",
  "vatNumber": "GB123456789",
  "orgType": "SECONDARY_SCHOOL"
}
```

**POST /org/settings/logo request:**
```
Content-Type: multipart/form-data
Body: file=<binary image data>
```
**Response:**
```json
{ "status": "success", "data": { "logoUrl": "/static/uploads/org/logo.png" } }
```

---

## 5. Spring Boot Implementation Notes

### New classes required

```
com.edulearn
├── controller/
│   └── OrgSettingsController.java
├── service/
│   └── OrgSettingsService.java
├── repository/
│   └── OrgSettingsRepository.java
├── entity/
│   └── OrgSettings.java
└── dto/
    ├── request/
    │   ├── OrgBrandingRequest.java
    │   ├── OrgLegalRequest.java
    │   ├── OrgAddressRequest.java
    │   ├── OrgContactsRequest.java
    │   └── OrgAcademicRequest.java
    └── response/
        └── OrgSettingsResponse.java
```

### OrgSettings.java (singleton entity)
```java
@Entity
@Table(name = "organisation_settings")
public class OrgSettings {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String orgName;
    private String displayName;
    private String logoPath;
    private String legalEntityName;
    private String registrationNumber;
    private LocalDate registrationDate;
    private String registeredCountry;
    private String vatNumber;

    @Enumerated(EnumType.STRING)
    private OrgType orgType;

    // Address fields
    private String addressLine1, addressLine2, city, county, postcode, country;

    // Primary contact
    private String poc1Name, poc1Title, poc1Email, poc1Phone;

    // Secondary contact
    private String poc2Name, poc2Title, poc2Email, poc2Phone;

    // Academic
    private LocalDate academicYearStart, academicYearEnd;
    private String curriculum, accreditationNo, website;
    @Column(columnDefinition = "TEXT")
    private String aboutText;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreationTimestamp private OffsetDateTime createdAt;
    @UpdateTimestamp  private OffsetDateTime updatedAt;
}
```

### File upload config (application.properties additions)
```properties
# File upload
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=2MB
spring.servlet.multipart.max-request-size=2MB
app.upload.dir=src/main/resources/static/uploads/org
```

---

## 6. Notification System (Backend — future sprint)

### DB table
```sql
CREATE TABLE notifications (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message     TEXT        NOT NULL,
    is_read     BOOLEAN     NOT NULL DEFAULT FALSE,
    link        VARCHAR(300),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notif_user ON notifications(user_id) WHERE is_read = FALSE;
```

### Trigger points (create a notification when...)
| Event | Recipient |
|-------|-----------|
| Teacher submits content | All admins |
| Admin approves content | Submitting teacher |
| Admin rejects content | Submitting teacher |
| New teacher account created | That teacher |

---

## 7. Files Changed / Added in This Session

| File | Change |
|------|--------|
| `html/admin-dashboard.html` | Topbar user pill, dropdown menu, notifications panel, Organisation Settings page added |
| `sql/02-database-schema.sql` | Add `organisation_settings` table + `notifications` table |
| `docs/06-phase1-topbar-and-org-settings.md` | **This file — new** |
| `CLAUDE.md` | Add org settings classes and endpoints to build order |

---

## 8. UI Behaviour Rules Summary

| Rule | Detail |
|------|--------|
| Only one panel open at a time | Opening notifications closes user menu and vice versa |
| Click outside to close | Both panels close on outside click via `document` listener |
| Logout | Clears `sessionStorage`, redirects to `/login.html` |
| Logo preview | Instant preview on file select, no upload until Save is clicked |
| Org settings access | Admin only — hide from teacher nav entirely |
| Each section saves independently | No single giant save — reduces risk of data loss |
