# EduLearn Platform — Amendment: Calendar Drag-and-Drop & Recurring Schedules

## What This Document Adds

Extends `docs/14-amendment-module8-calendar.md` with two major capabilities:
1. **Drag-and-drop rescheduling** — drag any event to a new day; only the date changes
2. **Recurring / range schedule booking** — weekly, fortnightly, monthly repeat patterns and date-range bulk booking

---

## Part 1 — Drag-and-Drop Rescheduling

### How it works

1. Teacher grabs any event pill on the calendar (cursor changes to `grab`)
2. A floating ghost label follows the cursor showing the event title
3. Days highlight in blue as the event is dragged over them
4. Dropping on a new day reschedules the event — only the date changes; title, time, class and duration remain identical
5. A status bar at the bottom confirms the move with old and new dates

### Series-aware rescheduling

When a recurring event (marked `↻`) is dragged, a dialog appears asking:

```
"Year 11A English" is part of a recurring series.
Moving it 7 days forward — what would you like to update?

○  This event only (03/06/2025)
○  This and all following events in the series
○  All events in this series
```

The date offset is applied consistently across all selected events. The dialog only affects **date** — all other fields unchanged.

### Role rules

| Role | Can drag | Scope |
|------|----------|-------|
| Admin | Any event | Move any event including other teachers' |
| Teacher | Own events only | Cannot move admin-created or other teachers' entries |

### Drag behaviour rules

- Dropping on the same day as the original = no change (silent, status bar notifies)
- Cannot drag exam events to a date where that exam schedule has already been attempted by students (`status = ACTIVE` or past)
- Cross-month drag is supported (navigate to target month first, then drop)
- Keyboard alternative: edit modal → change date field manually

---

## Part 2 — Recurring & Range Schedule Booking

### Three booking modes (modal selector)

The quick-add modal gains a booking type selector at the top:

```
[ Single ]  [ Date range ]  [ Recurring ]
```

---

### Mode A — Single (existing behaviour, unchanged)

One event on one date. No change to current spec.

---

### Mode B — Date Range

Books a session on **every calendar day** between a from-date and to-date (inclusive). Suitable for holidays, study periods, or a block of consecutive daily lectures.

**Fields:**
| Field | Description |
|-------|-------------|
| From date | First day of the range |
| To date | Last day of the range |
| Class | Optional |
| Start time | Applies to every day |
| Duration | Minutes per session |

**Preview:** Live count shown below the dates — "This will create 14 daily sessions from 07/06 to 20/06."

**On save:** All created events share the same `series_id` so they can be edited or deleted together.

---

### Mode C — Recurring

Repeats on a chosen frequency within a date window. Three sub-types:

#### Weekly
- User picks day(s) of the week (Mon/Tue/Wed/Thu/Fri/Sat/Sun toggle pills)
- Repeat every 7 days on the chosen days within the date window

#### Fortnightly
- Same day-of-week selector
- Repeat every 14 days

#### Monthly (same date)
- Repeats on the same calendar date each month (e.g. every 15th)
- Useful for monthly review sessions or payment deadlines

**Preview panel** (live, updates as dates/frequency change):
```
12 sessions will be created:
▸ Tue 3 Jun
▸ Tue 10 Jun
▸ Tue 17 Jun
▸ Tue 24 Jun
▸ Tue 1 Jul
… and 7 more
```

**Common fields for recurring:**
| Field | Description |
|-------|-------------|
| Start date | First possible occurrence |
| End date | Last possible occurrence |
| Frequency | Weekly / Fortnightly / Monthly |
| Days of week | Shown for Weekly and Fortnightly |
| Class | Optional |
| Start time | Same for all occurrences |
| Duration | Minutes per session |

---

## DB Changes

### series_id on exam_schedules

Already exists as a concept in the frontend — now formalise in DB:

```sql
-- Add series tracking to exam_schedules
ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS series_id UUID;

-- Add recurrence metadata (stored for display and edit purposes)
ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS recurrence_type VARCHAR(20)
        CHECK (recurrence_type IN ('NONE','DAILY','WEEKLY','FORTNIGHTLY','MONTHLY'));

ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS recurrence_days VARCHAR(20);
    -- Comma-separated day numbers: "1,2,3,4,5" = Mon-Fri

ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS series_start DATE;

ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS series_end DATE;

-- Index for series queries
CREATE INDEX IF NOT EXISTS idx_sched_series_id
    ON exam_schedules(series_id)
    WHERE series_id IS NOT NULL AND is_deleted = FALSE;
```

---

## New API Endpoints

Add to `ScheduleController`:

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/api/v1/schedules/range` | Admin/Teacher | Create daily sessions across a date range |
| POST | `/api/v1/schedules/recurring` | Admin/Teacher | Create a recurring series |
| GET  | `/api/v1/schedules/series/{seriesId}` | Admin/Teacher | Get all events in a series |
| PUT  | `/api/v1/schedules/{id}/move` | Admin/Teacher | Move one event (drag-drop result) |
| PUT  | `/api/v1/schedules/series/{seriesId}/move` | Admin/Teacher | Move all in series by offset |
| DELETE | `/api/v1/schedules/series/{seriesId}` | Admin | Delete entire series |

### POST /schedules/range

```json
{
  "scheduleType": "CLASS",
  "title": "Year 10A Science lecture",
  "classId": "uuid",
  "fromDate": "2025-06-07",
  "toDate": "2025-06-20",
  "startTime": "10:00",
  "durationMinutes": 60
}
```

Response: `{ "seriesId": "uuid", "created": 14 }`

### POST /schedules/recurring

```json
{
  "scheduleType": "CLASS",
  "title": "Year 11A English",
  "classId": "uuid",
  "recurrenceType": "WEEKLY",
  "recurrenceDays": "1,2",
  "seriesStart": "2025-06-02",
  "seriesEnd": "2025-07-18",
  "startTime": "09:00",
  "durationMinutes": 60
}
```

Response: `{ "seriesId": "uuid", "created": 12 }`

### PUT /schedules/{id}/move (drag-drop)

```json
{
  "newDate": "2025-06-14",
  "applyTo": "THIS | THIS_AND_FORWARD | ALL"
}
```

`applyTo` controls series behaviour:
- `THIS` — detaches this occurrence from the series and moves only it
- `THIS_AND_FORWARD` — shifts this and all later occurrences by the same date offset
- `ALL` — shifts every occurrence in the series by the same date offset

---

## Spring Boot Changes

### New / modified classes

```
entity/      ExamSchedule.java      ADD seriesId, recurrenceType, recurrenceDays,
                                        seriesStart, seriesEnd fields
enums/       RecurrenceType.java    NEW — NONE, DAILY, WEEKLY, FORTNIGHTLY, MONTHLY
enums/       SeriesMoveScope.java   NEW — THIS, THIS_AND_FORWARD, ALL
dto/request/ RangeScheduleRequest.java     NEW
             RecurringScheduleRequest.java NEW
             MoveSheduleRequest.java        NEW
dto/response/SeriesCreatedResponse.java    NEW
service/     ScheduleService.java   ADD createRange(), createRecurring(),
                                        moveSchedule(), moveSeries(), deleteSeries()
controller/  ScheduleController.java ADD 6 new endpoints (see table above)
```

### ScheduleService — date generation logic

```java
// Weekly / fortnightly — generate all matching dates in the window
public List<LocalDate> generateDates(RecurringScheduleRequest req) {
    List<LocalDate> dates = new ArrayList<>();
    LocalDate d = req.getSeriesStart();
    Set<DayOfWeek> targetDays = parseDays(req.getRecurrenceDays());
    int stepDays = req.getRecurrenceType() == RecurrenceType.FORTNIGHTLY ? 14 : 7;

    while (!d.isAfter(req.getSeriesEnd())) {
        if (req.getRecurrenceType() == RecurrenceType.MONTHLY) {
            dates.add(d);
            d = d.plusMonths(1);
        } else {
            if (targetDays.contains(d.getDayOfWeek())) dates.add(d);
            d = d.plusDays(req.getRecurrenceType() == RecurrenceType.FORTNIGHTLY
                ? stepDays : 1);
        }
    }
    return dates;
}

// Move with series scope
@Transactional
public void moveSchedule(UUID id, LocalDate newDate, SeriesMoveScope scope) {
    ExamSchedule ev = scheduleRepo.findById(id).orElseThrow();
    long offsetDays = ChronoUnit.DAYS.between(ev.getStartAt().toLocalDate(), newDate);

    if (scope == SeriesMoveScope.THIS || ev.getSeriesId() == null) {
        ev.setSeriesId(null); // detach from series
        ev.setStartAt(ev.getStartAt().plusDays(offsetDays));
        ev.setEndAt(ev.getEndAt().plusDays(offsetDays));
    } else {
        List<ExamSchedule> series = scope == SeriesMoveScope.ALL
            ? scheduleRepo.findBySeriesId(ev.getSeriesId())
            : scheduleRepo.findBySeriesIdAndStartAtAfterEqual(ev.getSeriesId(), ev.getStartAt());
        series.forEach(s -> {
            s.setStartAt(s.getStartAt().plusDays(offsetDays));
            s.setEndAt(s.getEndAt().plusDays(offsetDays));
        });
        scheduleRepo.saveAll(series);
    }
    scheduleRepo.save(ev);
}
```

---

## Frontend — JS Implementation

### Drag-and-drop (HTML5 native, no library needed)

```javascript
// On each event pill
pill.draggable = true;
pill.ondragstart = (e) => {
  dragId = eventId;
  e.dataTransfer.setData('text/plain', eventId);
  e.dataTransfer.effectAllowed = 'move';
  showDragGhost(eventTitle);
};
pill.ondragend = () => { hideDragGhost(); clearDragHighlights(); };

// On each day cell
cell.ondragover = (e) => { e.preventDefault(); cell.classList.add('drag-over'); };
cell.ondragleave = () => cell.classList.remove('drag-over');
cell.ondrop = async (e) => {
  e.preventDefault();
  cell.classList.remove('drag-over');
  const id = parseInt(e.dataTransfer.getData('text/plain'));
  const newDate = cell.dataset.date;
  const ev = findEvent(id);
  if (ev.seriesId) {
    showSeriesDialog(ev, newDate); // ask THIS / FORWARD / ALL
  } else {
    await apiFetch(`/schedules/${id}/move`, {
      method: 'PUT',
      body: JSON.stringify({ newDate, applyTo: 'THIS' })
    });
    reloadCalendar();
  }
};
```

### Date generation (client-side preview only — server generates authoritatively)

```javascript
function buildPreviewDates(start, end, freq, selectedDays) {
  const dates = [];
  let d = new Date(start + 'T12:00');
  const e = new Date(end + 'T12:00');
  const step = freq === 'fortnightly' ? 14 : 1;
  while (d <= e) {
    const dow = d.getDay() === 0 ? 7 : d.getDay(); // Mon=1 … Sun=7
    if (freq === 'monthly') {
      dates.push(formatDate(d)); d.setMonth(d.getMonth() + 1);
    } else {
      if (selectedDays.has(dow)) dates.push(formatDate(d));
      d.setDate(d.getDate() + (freq === 'fortnightly' && selectedDays.has(dow) ? 14 : 1));
    }
  }
  return dates;
}
```

---

## Role-Based Restrictions

| Feature | Admin | Teacher |
|---------|-------|---------|
| Drag any event | Yes | Own events only |
| Drag recurring — scope options | All 3 | All 3 (own series only) |
| Create date range | Yes | Yes |
| Create weekly recurring | Yes | Yes |
| Create fortnightly recurring | Yes | Yes |
| Create monthly recurring | Yes | Yes |
| Delete entire series | Yes | Own series only |
| See other teachers' events on calendar | Yes (all) | Yes (view only, cannot drag/edit) |

---

## Files Added / Changed

| File | Change |
|------|--------|
| `docs/15-amendment-drag-drop-recurring.md` | **This file — new** |
| `sql/11-drag-drop-recurring-schema.sql` | Adds series_id, recurrence_type, recurrence_days, series_start, series_end to exam_schedules |
| `CLAUDE.md` | Updated endpoints, new enums, new DTOs, new service methods, build steps |
