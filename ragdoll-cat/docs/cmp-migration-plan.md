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

### Batch 2 — class & org management
`MyClassWindow`, `JoinClassWindow`, `SelectOrgWindow`, `SelectOrgAndSelectClassWindow`,
`StudentManagementWindow`.

### Batch 3 — classroom tools (interactive, self-contained)
`SpinnerWindow` / `MvbSpinnerWindow`, `BuzzerWindow`, `RandomDrawWindow`,
`TimerToolWindow`, `SettingsWindow`, `ToolbarWindow`.

### Batch 4 — quiz start (Mvb / service variants only; share a base layout)
`MvbMultipleChoiceStartWindow`, `MvbTrueFalseStartWindow`, `MvbShortAnswerStartWindow`,
`MvbPollQuizStartWindow`, `MvbAudioQuizStartWindow`, `MvbSketchResponseStartWindow`,
`MvbTextShortAnswerStartWindow`, `MvbTextTrueFalseStartWindow`.
_(standalone duplicates `MultipleChoiceStartWindow` etc. → out of scope, removable.)_

### Batch 5 — quiz edit (Mvb / service variants only)
`MvbMultipleChoiceEditWindow`, `MvbTrueFalseEditWindow`, `MvbShortAnswerEditWindow`,
`MvbPollQuizEditWindow`, `MvbAudioQuizEditWindow`, `MvbSketchResponseEditWindow`.
_(standalone duplicates → out of scope.)_

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
