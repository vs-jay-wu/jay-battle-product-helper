# ragdoll-cat → Compose Multiplatform migration plan

Goal: bring **every** ragdoll-cat screen onto Compose Multiplatform (CMP) so it
can be driven by the Designer Shell (`:designer-bridge` → `runDesignerTarget`).
This is a long-running effort done in batches — one batch per session.

## Scope: SERVICE (MVB) path only

ragdoll-cat (ClassSwift) used to run standalone but now always runs as a **service
bound to MyViewBoard (MVB)**. The standalone path is legacy and **removable** — we
only migrate (and keep viable) the **service path**. Practically:

- Quiz screens: migrate the **`Mvb*` variants only** (e.g. `MvbMultipleChoiceStartWindow`);
  the non-`Mvb` duplicates (`MultipleChoiceStartWindow`, …) are standalone → out of scope.
- Shared screens with no `Mvb` twin (menus, class/org management, tools, prompts) are used
  in service mode → in scope.

(The Designer Shell renders these CMP screens on desktop regardless of MVB; "service viable"
means the screens we port are the ones the live MVB-bound app uses.)

## Scale (why batches)

- ~673 `.kt` in `:app`, ~60 screen windows total — **~35 in scope** after dropping standalone duplicates.
- Done so far: **Quiz Collection** (`:feature:quizcollection:ui`, 4 states) + **all of Batch 1** (9 screens, `:feature:servicescreens:ui`).

## Per-screen process (the recipe)

1. Port the screen's UI to a CMP module's `commonMain` — platform-neutral Compose, **no Android deps**; pull sample state from `:fixtures` (add fixtures as needed). Real assets via `compose.resources` (no grey placeholders — see real-assets rule).
2. Tag meaningful elements with `Modifier.designNode("<id>")` (from `:core:ui`) so the shell can select per-instance.
3. Ensure the screen's module has Compose **source information** on (raw `-P …sourceInformation=true` flag — KMP+Android modules need it, the DSL is ignored).
4. Register it as a `DesignerPage` in `:designer-shell` (`ComposeTargetHost.kt`'s `runDesignerTarget` page list).
5. Verify in the shell: renders, design-mode selection + source location, structure tree, hot-reload.

## Inventory & batch priority

Ordered by value × tractability. Each batch = one session.

### ✅ Done
- Quiz Collection (`QuizCollectionScreen`, 4 states).

### Batch 1 — simple menus & prompts (static, fast wins) — ✅ DONE
All in `:feature:servicescreens:ui`, real assets via `compose.resources` (17 icons + 2
illustrations copied), `designNode` tags per element, registered as `svc_*` DesignerPages:
- ✅ `UnderMaintenanceWindow` / `UpcomingMaintenanceWindow` → shared `MaintenanceCard` (413dp card)
- ✅ `UpcomingMaintenanceCornerPromptWindow` → 360dp left-rounded corner toast
- ✅ `ComingSoonPromptWindow` / `UpgradePromptWindow` → black toolbar prompt + `SOON` pill (+ `ic_premium`)
- ✅ `SettingMenuWindow` / `ToolsMenuWindow` / `QuizMenuWindow` / `ClassManagementMenuWindow`
  → shared `SubordinateMenu` + `MenuItemRow` (F5F5F5 surface, 24dp black-tinted icon rows)

### Batch 2 — class & org management — ✅ DONE
All in `:feature:servicescreens:ui`, real assets via `compose.resources`, `designNode` tags,
shared `CSButton` (ClassSwiftLoadingButton stand-in) + `QrMatrix` (generated QR, not a placeholder):
- ✅ `SelectOrgWindow` → `SelectOrgScreen` (350dp card, org list + Sign Out/Select)
- ✅ `SelectOrgAndSelectClassWindow` → `SelectOrgAndClassScreen` (org dropdown + plan badge + class list)
- ✅ `MyClassWindow` → `MyClassScreen` (680×393 two-panel: class list + detail/actions)
- ✅ `StudentManagementWindow` → `StudentManagementScreen` (core: class info + QR + 5-col seat grid;
  _deferred: Groups tab, more-menu, edit-mode overlay_)
- ✅ `JoinClassWindow` → `JoinClassScreen` (primary join state: URL + code tiles + QR + attendance grid;
  _deferred: expanded-QR / empty / disconnected states, leave/remove dialogs & tooltips_)

### Batch 3 — classroom tools (interactive, self-contained) — ✅ DONE
All in `:feature:servicescreens:ui`, real assets via `compose.resources`, `designNode` tags,
shared `ToolCard` (348×336 tool-window chrome):
- ✅ `BuzzerWindow` → `BuzzerScreen` (title, time, big blue Start circle)
- ✅ `RandomDrawWindow` → `RandomDrawScreen` (title + blue dice circle, real `ic_dice`)
- ✅ `TimerToolWindow` → `TimerToolScreen` (MM:SS digits, Timer/Stopwatch radios, Start)
- ✅ `MvbSpinnerWindow` → `SpinnerScreen` (MVB header + Canvas spinner wheel; `ic_mvb_spinner`
  @color refs inlined to hex). _(standalone `SpinnerWindow` → out of scope per MVB-service rule.)_
- ✅ `SettingsWindow` → `SettingsScreen` (language spinner, translation switch, tutorial;
  _debug-tool section is debug-build only → out of scope_)
- ✅ `ToolbarWindow` → `ToolbarScreen` (expanded state: 9 feature icons + Leave/Start/End actions)

### Batch 4 — quiz start (Mvb / service variants only; share a base layout) — ✅ DONE
All 8 variants share the shell (`window_mvb_*_start.xml`) + `panel_mvb_quizzing.xml`; ported as
one parameterized `MvbQuizStartScreen(type)` in `:feature:servicescreens:ui` rendering the
**quizzing (start) state**: 853×480 shell, header, question section (type icon+label, stopwatch,
screenshot frame, option chips), End-and-review, student responses grid.
- ✅ MultipleChoice (A–D) · ✅ TrueFalse · ✅ ShortAnswer · ✅ Poll (A–D) · ✅ Audio
  · ✅ SketchResponse · ✅ TextShortAnswer · ✅ TextTrueFalse → 8 `svc_quiz_*` pages.
- `@color`-referencing vector icons inlined to hex via a reusable resolver.
- _Deferred: disclose & result modes (mid-lesson, not the start window). Standalone non-Mvb
  duplicates remain out of scope._

### Batch 5 — quiz edit (Mvb / service variants only) — ✅ DONE
Each `window_mvb_*_edit.xml` is structurally the same editor (header, image upload, `MvbOptionPanel`,
Cancel/Start action bar); ported as one parameterized `MvbQuizEditScreen(type)` in
`:feature:servicescreens:ui`: 541×626 card, question-image upload frame, answer-option boxes
(+ Add for MC/Poll, correct marker), Answer-types/options settings dropdowns, Cancel / Start-question.
- ✅ MultipleChoice · ✅ TrueFalse · ✅ ShortAnswer · ✅ Poll · ✅ Audio · ✅ SketchResponse
  → 6 `svc_edit_*` pages. _(standalone duplicates out of scope.)_

### Batch 6 — results, leaderboard & misc
`BatchQuizResultWindow`, `LeaderboardWindow`, `PushRespondWindow`, `CropImageWindow` /
`MvbCropImageWindow`, `TutorialWindow` / `InAppTutorialWindow`, `CSSystemDialogWindow`, `ToastWindow`.

> Each window also has a `WindowModel` (business logic) and possibly viewholders/adapters
> (list items). Porting = the UI + its sample state; the WindowModel logic stays where it
> belongs or is adapted into a platform-neutral state holder.

## Progress log

| Batch | Screen | Module | Status |
|-------|--------|--------|--------|
| —     | Quiz Collection | `:feature:quizcollection:ui` | ✅ done (4 states) |
| 1     | Under / Upcoming Maintenance, Corner Prompt, Coming Soon, Upgrade, Setting/Tools/Quiz/Class menus (9) | `:feature:servicescreens:ui` | ✅ done (real assets, `designNode` tags, 9 `svc_*` pages) |
| 2     | SelectOrg, SelectOrg&Class, MyClass, StudentManagement (core), JoinClass (primary state) (5) | `:feature:servicescreens:ui` | ✅ done (5 `svc_*` pages; 2 with deferred sub-states noted above) |
| 3     | Buzzer, RandomDraw, Timer, Spinner, Settings, Toolbar (6) | `:feature:servicescreens:ui` | ✅ done (6 `svc_*` pages; Settings debug-tool & Toolbar collapsed state out of scope) |
| 4     | 8 Mvb quiz-start variants (MC/TF/SA/Poll/Audio/Sketch/TextSA/TextTF) | `:feature:servicescreens:ui` | ✅ done (1 shared `MvbQuizStartScreen(type)`, 8 `svc_quiz_*` pages; disclose/result modes deferred) |
| 5     | 6 Mvb quiz-edit variants (MC/TF/SA/Poll/Audio/Sketch) | `:feature:servicescreens:ui` | ✅ done (1 shared `MvbQuizEditScreen(type)`, 6 `svc_edit_*` pages) |
