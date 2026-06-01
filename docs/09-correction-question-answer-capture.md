# EduLearn Platform — Correction: Question Type Answer Capture

## What This Document Fixes

During review, 5 gaps were found across Phase 1 (Question Builder UI) and
Phase 3 (Exam Attempt UI + auto-marking logic). This document supersedes any
conflicting guidance in `docs/05-springboot-implementation.md` and
`docs/08-phase3-assessment-engine.md`.

---

## Gap Summary

| # | Where | Gap |
|---|-------|-----|
| 1 | Phase 1 — Question Builder UI | After selecting a question type, NO type-specific fields appear. All types incorrectly show only a generic "Answer guide" textarea. |
| 2 | Phase 1 — QuestionRequest DTO | MCQ options collected but no UI built for entering option text or marking which is correct. |
| 3 | Phase 3 — Student Exam Attempt UI | Exam screen shows no type-specific answer inputs (radio/checkbox/textarea/code/image). |
| 4 | Phase 3 — IMAGE_BASED attempt | Image not displayed to student during attempt. |
| 5 | Phase 3 — MCQ_MULTIPLE marking | Partial marking strategy not defined. |

---

## Fix 1 — Question Builder: Type-Specific Fields

### Correct behaviour
When a teacher or admin selects a question type, the form below the type selector
must **immediately update** to show the correct input fields for that type.

### Field specification per type

#### MCQ_SINGLE
```
Question text:  [___________________________________]
Question type:  ( MCQ Single )  selected

Options:
  Option 1:  [___________________]  ○ Correct
  Option 2:  [___________________]  ○ Correct
  Option 3:  [___________________]  ○ Correct
  Option 4:  [___________________]  ○ Correct
  [+ Add option]                    (max 6 options)

  Rule: exactly ONE radio can be selected as correct.

Marks:  [ 1 ]
Answer explanation (shown after attempt):  [______________]
```

#### MCQ_MULTIPLE
```
Question text:  [___________________________________]
Question type:  ( MCQ Multiple )  selected

Options:
  Option 1:  [___________________]  ☐ Correct
  Option 2:  [___________________]  ☑ Correct
  Option 3:  [___________________]  ☑ Correct
  Option 4:  [___________________]  ☐ Correct
  [+ Add option]

  Rule: ONE or MORE checkboxes can be selected as correct.
  Partial marking:  ○ Full marks only  ● Award per correct option

Marks:  [ 4 ]
Answer explanation:  [______________]
```

#### TRUE_FALSE
```
Question text:  [___________________________________]
Question type:  ( True / False )  selected

Correct answer:
  ● True   ○ False

Marks:  [ 1 ]
Answer explanation:  [______________]
```

#### SHORT_ANSWER
```
Question text:  [___________________________________]
Question type:  ( Short Answer )  selected

Model answer (shown to marker):
  [___________________________________]
  (Used as reference during manual marking — not shown to student)

Marks:  [ 2 ]
Answer explanation:  [______________]
```

#### ESSAY
```
Question text:  [___________________________________]
Question type:  ( Essay )  selected

Marking scheme:
  [_____________________________________________]
  [_____________________________________________]
  e.g. "Award 2 marks for identification of technique,
        2 for explanation, 2 for effect on reader"

Word limit:  [____]  (0 = no limit)
Marks:  [ 6 ]
```

#### CODE
```
Question text:  [___________________________________]
Question type:  ( Code )  selected

Language:  [ Python ▾ ]  (Python / Java / JavaScript / SQL / HTML / CSS / Other)

Starter code (shown to student in editor):
  ┌──────────────────────────────────────┐
  │ def add(a, b):                       │
  │     pass                             │
  └──────────────────────────────────────┘

Expected output (used in auto-check — optional):
  [ 5 ]

Marks:  [ 3 ]
Answer guide / marking notes:  [______________]
```

#### IMAGE_BASED
```
Question type:  ( Image Based )  selected

Question image:
  [ Upload image ]  (PNG/JPG, max 5MB)
  [ Preview shown after upload ]

Alt text (accessibility):  [___________________]

Question text:  [___________________________________]
  (The question about the image — shown below the image)

Answer type:
  ○ Written response (textarea)
  ○ MCQ options (shows option builder same as MCQ_SINGLE)

Marks:  [ 2 ]
Answer guide:  [______________]
```

---

## Fix 2 — question_options Table: DB Already Correct, UI Missing

The `question_options` table in `sql/02-database-schema.sql` is correctly structured:
```sql
CREATE TABLE question_options (
    id            UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id   UUID     NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_text   TEXT     NOT NULL,
    is_correct    BOOLEAN  NOT NULL DEFAULT FALSE,
    display_order SMALLINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

What was missing was the UI to populate it.

### Additional column needed — MCQ_MULTIPLE partial marking

```sql
ALTER TABLE question_options
    ADD COLUMN IF NOT EXISTS partial_marks SMALLINT DEFAULT NULL;
-- NULL = use the all-or-nothing approach
-- A value = marks awarded if THIS specific option is correctly chosen/not chosen
```

### Validation rules (enforce in service layer)

```java
// MCQ_SINGLE
long correctCount = options.stream().filter(OptionRequest::isCorrect).count();
if (correctCount != 1)
    throw new ValidationException("MCQ_SINGLE must have exactly 1 correct option");

// MCQ_MULTIPLE
long correctCount = options.stream().filter(OptionRequest::isCorrect).count();
if (correctCount < 1)
    throw new ValidationException("MCQ_MULTIPLE must have at least 1 correct option");

// MCQ — minimum options
if (options.size() < 2)
    throw new ValidationException("MCQ questions must have at least 2 options");
```

---

## Fix 3 — Student Exam Attempt: Type-Specific Answer Inputs

The exam attempt screen (`student/exam.html`) must render a different answer
input for each question type. The question type is returned in the attempt response.

### Answer input per type

#### MCQ_SINGLE — Radio buttons
```html
<div class="answer-options">
  <label class="opt-row">
    <input type="radio" name="q_{{questionId}}" value="{{optionId}}"/>
    <span>Option text here</span>
  </label>
  <!-- repeat for each option -->
</div>
```
- Options displayed in `display_order` order
- If `shuffle_options = true` on the exam, shuffle before display
- One selection auto-deselects others

#### MCQ_MULTIPLE — Checkboxes
```html
<div class="answer-options">
  <label class="opt-row">
    <input type="checkbox" name="q_{{questionId}}" value="{{optionId}}"/>
    <span>Option text here</span>
  </label>
  <!-- repeat for each option -->
</div>
<p class="opt-hint">Select all that apply</p>
```

#### TRUE_FALSE — Two large buttons
```html
<div class="tf-buttons">
  <button class="tf-btn" onclick="selectTF(true)">✓ True</button>
  <button class="tf-btn" onclick="selectTF(false)">✗ False</button>
</div>
```

#### SHORT_ANSWER — Single textarea (max 500 chars)
```html
<textarea class="answer-text short" maxlength="500"
  placeholder="Write your answer here (max 500 characters)..."
  oninput="updateWordCount(this)"></textarea>
<div class="char-count">0 / 500</div>
```

#### ESSAY — Large textarea with word count
```html
<textarea class="answer-text essay"
  placeholder="Write your essay response here..."
  oninput="updateWordCount(this)"></textarea>
<div class="word-count">0 words
  <span id="word-limit"><!-- shown if word_limit set --></span>
</div>
```

#### CODE — Code textarea with language label
```html
<div class="code-editor-wrap">
  <div class="code-lang-badge">Python</div>
  <textarea class="answer-code" spellcheck="false"
    placeholder="Write your code here...">
<!-- pre-filled with starter_code if set -->
  </textarea>
</div>
```
> Note: A full code editor (e.g. CodeMirror) can be added as a future enhancement.
> For Phase 3 use a styled `<textarea>` with monospace font.

#### IMAGE_BASED — Image displayed above answer input
```html
<div class="image-question">
  <img src="{{imageUrl}}" alt="{{imageAltText}}" class="q-image"/>
  <!-- Then answer input based on image_answer_type -->
  <!-- If written: -->
  <textarea class="answer-text" placeholder="Write your answer about the image..."></textarea>
  <!-- If MCQ: render radio buttons same as MCQ_SINGLE -->
</div>
```

---

## Fix 4 — Auto-Marking: Corrected Logic

### MCQ_MULTIPLE — Partial Marking Decision

**Decision: support both modes**, controlled by `partial_marking` flag on the exam question.

```java
case MCQ_MULTIPLE -> {
    Set<UUID> correctOptionIds = getCorrectOptionIds(q);
    Set<UUID> selectedIds = new HashSet<>(
        answer.getSelectedOptionIds() != null ? answer.getSelectedOptionIds() : List.of()
    );

    if (examQuestion.isPartialMarking()) {
        // Award 1 mark per correctly selected option, deduct 1 per incorrectly selected
        // Floor at 0 — never go negative
        long correctSelections = selectedIds.stream()
            .filter(correctOptionIds::contains).count();
        long incorrectSelections = selectedIds.stream()
            .filter(id -> !correctOptionIds.contains(id)).count();
        int awarded = (int) Math.max(0, correctSelections - incorrectSelections);
        // Scale to question marks proportionally
        int scaled = (int) Math.round((double) awarded / correctOptionIds.size() * questionMarks);
        answer.setMarksAwarded(scaled);
        answer.setIsCorrect(correctOptionIds.equals(selectedIds));
    } else {
        // Full marks only if exact match
        boolean correct = correctOptionIds.equals(selectedIds);
        answer.setIsCorrect(correct);
        answer.setMarksAwarded(correct ? questionMarks : 0);
    }
}
```

### Add partial_marking flag to exam_questions

```sql
ALTER TABLE exam_questions
    ADD COLUMN IF NOT EXISTS partial_marking BOOLEAN NOT NULL DEFAULT FALSE;
```

### IMAGE_BASED auto-marking
If the image question uses MCQ options → auto-mark as MCQ_SINGLE.
If it uses written response → flag for manual marking (same as ESSAY).

```java
case IMAGE_BASED -> {
    if (q.getOptions() != null && !q.getOptions().isEmpty()) {
        // treat as MCQ_SINGLE
        boolean correct = answer.getSelectedOptionIds() != null
            && answer.getSelectedOptionIds().size() == 1
            && isCorrectOption(answer.getSelectedOptionIds().get(0), q);
        answer.setIsCorrect(correct);
        answer.setMarksAwarded(correct ? questionMarks : 0);
    } else {
        // written answer — manual marking required
        answer.setIsCorrect(null);
        answer.setMarksAwarded(null);
    }
}
```

---

## Fix 5 — Updated QuestionRequest DTO

```java
public record QuestionRequest(

    // Always required
    @NotBlank String questionText,
    @NotNull  QuestionType questionType,
    @Min(1)   Short marks,
    String    answerExplanation,   // shown to student after attempt

    // MCQ_SINGLE / MCQ_MULTIPLE
    List<OptionRequest> options,

    // TRUE_FALSE
    Boolean correctBoolean,

    // SHORT_ANSWER
    String modelAnswer,

    // ESSAY
    String markingScheme,
    Integer wordLimit,             // null = no limit

    // CODE
    CodeLanguage codeLang,
    String starterCode,
    String expectedOutput,

    // IMAGE_BASED
    String imageUrl,
    String imageAltText,
    ImageAnswerType imageAnswerType // WRITTEN or MCQ

) {}

public record OptionRequest(
    @NotBlank String optionText,
    boolean isCorrect,
    int displayOrder,
    Short partialMarks            // null = use all-or-nothing
) {}

public enum ImageAnswerType { WRITTEN, MCQ }
```

Add `ImageAnswerType` to `enums/` package.

---

## Updated DB — Full Corrections Summary

Run this as `sql/05-question-answer-corrections.sql`:

```sql
-- ── Fix: question_options — partial marks support ──────────────
ALTER TABLE question_options
    ADD COLUMN IF NOT EXISTS partial_marks SMALLINT DEFAULT NULL;

-- ── Fix: questions — additional fields ────────────────────────
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS answer_explanation TEXT;      -- shown after attempt
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS marking_scheme     TEXT;      -- essay criteria
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS word_limit         INTEGER;   -- essay word cap
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS image_answer_type  VARCHAR(10)
        CHECK (image_answer_type IN ('WRITTEN','MCQ'));   -- image-based subtype

-- ── Fix: exam_questions — partial marking flag ─────────────────
ALTER TABLE exam_questions
    ADD COLUMN IF NOT EXISTS partial_marking BOOLEAN NOT NULL DEFAULT FALSE;
```

---

## HTML — Dynamic Question Form (JS Pattern)

In both `admin-dashboard.html` and `teacher-dashboard.html`, the `showDetail()`
function must render type-specific fields when `n.type === 'question'`.

Replace the current static question form block with this JS pattern:

```javascript
function renderQuestionTypeFields(n) {
    const qt = n.qtype || 'mcq';
    switch(qt) {

        case 'mcq':
        case 'mcq_multiple':
            const isMultiple = qt === 'mcq_multiple';
            const inputType  = isMultiple ? 'checkbox' : 'radio';
            const options    = n.options || [{text:'',correct:false},{text:'',correct:false}];
            return `
              <div class="divider-lbl">Answer options
                <span style="font-size:10px;color:var(--small);font-weight:400;margin-left:6px">
                  ${isMultiple ? 'Check all correct answers' : 'Select ONE correct answer'}
                </span>
              </div>
              <div id="optionsList">
                ${options.map((o,i) => `
                  <div class="opt-row" data-idx="${i}">
                    <span class="opt-handle">⠿</span>
                    <input class="form-input opt-text" placeholder="Option ${i+1}"
                           value="${o.text||''}" style="flex:1"/>
                    <label class="opt-correct-lbl">
                      <input type="${inputType}" name="correct_${n.id}"
                             ${o.correct?'checked':''}
                             onchange="setCorrect(${n.id},${i},this)"/>
                      Correct
                    </label>
                    <button class="t-action del" onclick="removeOption(${i})">
                      <svg viewBox="0 0 24 24" width="12" height="12">
                        <line x1="18" y1="6" x2="6" y2="18"/>
                        <line x1="6" y1="6" x2="18" y2="18"/>
                      </svg>
                    </button>
                  </div>`).join('')}
              </div>
              <button class="btn-secondary" style="margin-top:8px;font-size:12px"
                      onclick="addOption(${n.id})">+ Add option</button>
              ${isMultiple ? `
              <div class="form-group" style="margin-top:12px">
                <label class="form-label">Partial marking</label>
                <select class="form-select" style="width:auto">
                  <option value="none">Full marks only (exact match)</option>
                  <option value="partial">Award marks per correct option</option>
                </select>
              </div>` : ''}`;

        case 'truefalse':
            return `
              <div class="divider-lbl">Correct answer</div>
              <div style="display:flex;gap:10px">
                <label class="tf-choice ${n.correctBoolean===true?'sel':''}">
                  <input type="radio" name="tf_${n.id}" value="true"
                         ${n.correctBoolean===true?'checked':''}
                         onchange="setTF(${n.id},true)"/>
                  ✓ True
                </label>
                <label class="tf-choice ${n.correctBoolean===false?'sel':''}">
                  <input type="radio" name="tf_${n.id}" value="false"
                         ${n.correctBoolean===false?'checked':''}
                         onchange="setTF(${n.id},false)"/>
                  ✗ False
                </label>
              </div>`;

        case 'short':
            return `
              <div class="form-group">
                <label class="form-label">Model answer
                  <span style="font-weight:400;color:var(--small)"> — shown to marker only</span>
                </label>
                <textarea class="form-textarea" placeholder="Expected answer...">${n.modelAnswer||''}</textarea>
              </div>`;

        case 'essay':
            return `
              <div class="form-group">
                <label class="form-label">Marking scheme / criteria</label>
                <textarea class="form-textarea" style="min-height:90px"
                  placeholder="e.g. Award 2 marks for technique, 2 for explanation, 2 for effect..."
                  >${n.markingScheme||''}</textarea>
              </div>
              <div class="form-group">
                <label class="form-label">Word limit (0 = no limit)</label>
                <input class="form-input" type="number" value="${n.wordLimit||0}" style="width:100px"/>
              </div>`;

        case 'code':
            return `
              <div class="form-group">
                <label class="form-label">Programming language</label>
                <select class="form-select" style="width:auto">
                  ${['Python','Java','JavaScript','SQL','HTML','CSS','Other']
                    .map(l=>`<option ${n.codeLang===l?'selected':''}>${l}</option>`).join('')}
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Starter code (shown to student)</label>
                <textarea class="form-textarea" style="font-family:monospace;font-size:12px;min-height:100px"
                  placeholder="def add(a, b):\n    pass">${n.starterCode||''}</textarea>
              </div>
              <div class="form-group">
                <label class="form-label">Expected output (optional — for auto-check)</label>
                <input class="form-input" style="font-family:monospace"
                       placeholder="e.g. 5" value="${n.expectedOutput||''}"/>
              </div>`;

        case 'image':
            return `
              <div class="form-group">
                <label class="form-label">Question image</label>
                <div class="logo-box" style="width:100%;height:120px" onclick="document.getElementById('qImg_${n.id}').click()">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <rect x="3" y="3" width="18" height="18" rx="2"/>
                    <circle cx="8.5" cy="8.5" r="1.5"/>
                    <polyline points="21 15 16 10 5 21"/>
                  </svg>
                  <span>Click to upload image</span>
                  <input type="file" id="qImg_${n.id}" accept="image/*" style="display:none"
                         onchange="previewQImage(this,'${n.id}')"/>
                </div>
                ${n.imageUrl?`<img src="${n.imageUrl}" style="max-width:100%;border-radius:8px;margin-top:8px"/>` : ''}
              </div>
              <div class="form-group">
                <label class="form-label">Alt text (accessibility)</label>
                <input class="form-input" placeholder="Describe the image..." value="${n.imageAltText||''}"/>
              </div>
              <div class="form-group">
                <label class="form-label">Answer type</label>
                <select class="form-select" style="width:auto" onchange="updateImageAnswerType(${n.id},this.value)">
                  <option value="WRITTEN" ${(n.imageAnswerType||'WRITTEN')==='WRITTEN'?'selected':''}>Written response</option>
                  <option value="MCQ"     ${n.imageAnswerType==='MCQ'?'selected':''}>MCQ options</option>
                </select>
              </div>
              ${n.imageAnswerType==='MCQ' ? renderQuestionTypeFields({...n, qtype:'mcq'}) : ''}`;

        default:
            return '';
    }
}
```

### Common fields rendered after type-specific block (always shown):
```javascript
// Always appended after type-specific fields:
`<div class="form-group" style="margin-top:4px">
  <label class="form-label">Marks</label>
  <input class="form-input" type="number" min="1" value="${n.marks||1}" style="width:80px"/>
</div>
<div class="form-group">
  <label class="form-label">Answer explanation
    <span style="font-weight:400;color:var(--small)"> — shown to student after attempt</span>
  </label>
  <textarea class="form-textarea" placeholder="Explain the correct answer...">${n.answerExplanation||''}</textarea>
</div>`
```

---

## Student Exam — Dynamic Answer Renderer (JS Pattern)

In `student/exam.html`, replace the static question display with:

```javascript
function renderAnswerInput(question, savedAnswer) {
    switch(question.questionType) {

        case 'MCQ_SINGLE':
            return `<div class="answer-options">
              ${question.options.map(o => `
                <label class="opt-row ${savedAnswer?.selectedOptionIds?.includes(o.id)?'selected':''}">
                  <input type="radio" name="q${question.id}" value="${o.id}"
                    ${savedAnswer?.selectedOptionIds?.includes(o.id)?'checked':''}
                    onchange="saveAnswer('${question.id}','mcq_single','${o.id}')"/>
                  <span>${o.optionText}</span>
                </label>`).join('')}
            </div>`;

        case 'MCQ_MULTIPLE':
            return `<p class="opt-hint">Select all that apply</p>
            <div class="answer-options">
              ${question.options.map(o => `
                <label class="opt-row ${savedAnswer?.selectedOptionIds?.includes(o.id)?'selected':''}">
                  <input type="checkbox" name="q${question.id}" value="${o.id}"
                    ${savedAnswer?.selectedOptionIds?.includes(o.id)?'checked':''}
                    onchange="saveAnswer('${question.id}','mcq_multiple')"/>
                  <span>${o.optionText}</span>
                </label>`).join('')}
            </div>`;

        case 'TRUE_FALSE':
            return `<div class="tf-buttons">
              <button class="tf-btn ${savedAnswer?.booleanAnswer===true?'sel':''}"
                onclick="saveAnswer('${question.id}','truefalse',true)">✓ True</button>
              <button class="tf-btn ${savedAnswer?.booleanAnswer===false?'sel':''}"
                onclick="saveAnswer('${question.id}','truefalse',false)">✗ False</button>
            </div>`;

        case 'SHORT_ANSWER':
            return `<textarea class="answer-text short" maxlength="500"
              placeholder="Write your answer (max 500 characters)..."
              oninput="saveAnswer('${question.id}','text',this.value);updateCharCount(this)"
              >${savedAnswer?.textAnswer||''}</textarea>
            <div class="char-count">${(savedAnswer?.textAnswer||'').length} / 500</div>`;

        case 'ESSAY':
            return `<textarea class="answer-text essay"
              placeholder="Write your essay response here..."
              oninput="saveAnswer('${question.id}','text',this.value);updateWordCount(this)"
              >${savedAnswer?.textAnswer||''}</textarea>
            <div class="word-count" id="wc_${question.id}">
              ${countWords(savedAnswer?.textAnswer||'')} words
              ${question.wordLimit?` / ${question.wordLimit} word limit`:''}
            </div>`;

        case 'CODE':
            return `<div class="code-editor-wrap">
              <div class="code-lang-badge">${question.codeLang||'Code'}</div>
              <textarea class="answer-code" spellcheck="false"
                oninput="saveAnswer('${question.id}','text',this.value)"
                >${savedAnswer?.textAnswer||question.starterCode||''}</textarea>
            </div>`;

        case 'IMAGE_BASED':
            return `<div class="image-question">
              <img src="${question.imageUrl}" alt="${question.imageAltText||'Question image'}"
                   class="q-image" style="max-width:100%;border-radius:8px;margin-bottom:16px"/>
              ${question.imageAnswerType === 'MCQ'
                ? renderAnswerInput({...question, questionType:'MCQ_SINGLE'}, savedAnswer)
                : `<textarea class="answer-text"
                     placeholder="Write your answer about the image..."
                     oninput="saveAnswer('${question.id}','text',this.value)"
                     >${savedAnswer?.textAnswer||''}</textarea>`
              }
            </div>`;

        default:
            return '<p>Unknown question type</p>';
    }
}
```

---

## Files Added / Changed

| File | Change |
|------|--------|
| `docs/09-correction-question-answer-capture.md` | **This file — new** |
| `sql/05-question-answer-corrections.sql` | Adds `answer_explanation`, `marking_scheme`, `word_limit`, `image_answer_type` to questions; `partial_marks` to question_options; `partial_marking` to exam_questions |
| `html/admin-dashboard.html` | Replace static question form with `renderQuestionTypeFields()` |
| `html/teacher-dashboard.html` | Same as admin |
| `student/exam.html` (Phase 3) | Replace static question display with `renderAnswerInput()` |
| `CLAUDE.md` | Add ImageAnswerType enum, corrected DTOs, build notes |
