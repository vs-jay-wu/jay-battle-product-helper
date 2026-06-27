# AGENTS — Global Project Conventions

This project follows strict and consistent development conventions.
Subordinate folders MAY define their own `AGENTS.override.md`,
but must not break the rules declared here unless explicitly allowed.

## Project Tech Stack Summary
This document outlines the core technologies, libraries, and architectural patterns used in the project.
It provides a clear and concise overview for onboarding, collaboration, and future maintenance.

### 1. UI Layer
- **Primary Framework:** Android Views (XML)
- **Animation:** Lottie

### 2. Networking
- **HTTP Client:** OkHttp
- **API Communication:** Retrofit
- **Real-time Communication:** Socket.IO

### 3. Data Layer
- **Local Database:** Room
- **Key-Value Storage:** DataStore

### 4. Dependency Injection
- **DI Framework:** Koin

### 5. Serialization
- **JSON Serialization:** Moshi

### 6. Architecture
- **Architecture Pattern:** MVVM (with custom floating-window support through a dedicated layer: IWindow/IWindowModel + WindowContainer + CSWindowManager).

### 7. Floating-Window MVVM Framework (com.viewsonic.classswift.windowframework.core)
- **IWindow**: Defines the window view contract/lifecycle.
- **IWindowModel**: Lightweight ViewModel-style contract with only `onCleared()`.
- **WindowContainer**: Wraps an IWindow with WindowManager params, config (tag/size/location), drag/touch handling, sub-window following, and z-order hoisting with flicker-free bitmap swap.
- **CSWindowManager**: Creates windows by location/gravity, tracks z-order/hidden state, manages main-sub window pairs, brings windows to top safely, and notifies listeners.

## Naming Conventions

### Class names
- Use PascalCase for class names.
- Treat acronyms as regular words in PascalCase (e.g., `Ui`, `Api`, `Id`), so use `QuizSharedUiInfo` instead of `QuizSharedUIInfo`.
- When combining acronyms with regular words, follow standard PascalCase throughout (e.g., `MyClassWindow`).

### Custom view class names
- Format: `CS{Function}{Component}` (e.g., `CSTextView`, `CSLoginButton`).

### XML `android:id`
- Single-word platform widgets: full name in lowercase (e.g., `Button -> @+id/button_xxx`).
- Multi-word platform widgets: lowercase shorthand prefix (e.g., `LinearLayout -> @+id/ll_xxx`).
- Custom view classes: use the custom view class acronym as the prefix (e.g., `ClassSwiftTextView -> @+id/cstv_{usage}`).
- Custom widgets: use the acronym `csw` as the prefix (e.g., `CSBatchQuizListWidget -> android:id="@+id/csw_batch_quiz_list"`).

### Layout resources
- Activities: `activity_xxx`
- Fragments: `fragment_xxx`
- Windows: `window_xxx`
- Dialogs: `dialog_xxx`
- Widgets: `widget_xxx`
- Custom views: use the class acronym prefix (e.g., `ClassSwiftTextView -> cstv_xxx`, `ClassSwiftLoginButton -> cslb_xxx`).

### Density
- Assets live in `xxhdpi` only.

### Color resources
- Store canonical color values in `pure_colors.xml`; other resources reference these entries.
- Opaque colors: `color_{HEX}` (e.g., `<color name="color_85E1EA">#85E1EA</color>`).
- Black, white, and gray use named entries; grays include luminance (e.g., `gray_l18` for 18%).
- With alpha: prefix with `a` + percentage (e.g., `color_a50_85E1EA` for 50% opacity, `white_a90` for 90% white).

### Drawable resources
- Icons: prefix with `ic_`.
- Backgrounds: prefix with `bg_`. Reusable backgrounds follow `bg_{fillColor}_radius{radius}_line_{borderColor}_border{thickness}`; omit `line_..._border...` if no border.
- Example (no border): `bg_neutral0_radius800` — fill `neutral0`, radius `800`.
- Example (with border): `bg_neutral0_radius400_line_neutral450_border100` — fill `neutral0`, radius `400`, border color `neutral450`, border thickness `100`.

### Extension functions
- Place all extension files in `com.viewsonic.classswift.utils.extension`.
- File names start with the type they extend (e.g., `ContextExtension.kt` for `Context`).

### API response data classes
- Suffix class names with `Response`.
- Prefer non-null properties whenever the API guarantees the field.
- Example:

```
// API response
{
    "code_challenge": "Z2gDNNdo5GpRcqSTNE7bUYPR_8h8Xjg0iDqqjeMev9I",
    "code_challenge_method": "S256"
}

// CodeChallengeResponse.kt
@JsonClass(generateAdapter = true)
data class CodeChallengeResponse(
    @Json(name = "code_challenge")
    val codeChallenge: String, // use lowerCamelCase for project variables
    @Json(name = "code_challenge_method")
    val codeChallengeMethod: String
)
```

## Git Rules (Rebase Only)
- To sync with `develop`, use rebase only. Do not create merge commits.
- Forbidden:
  - `git merge origin/develop`
  - `git pull origin develop`
- Required flow:
  1. `git fetch origin`
  2. `git rebase origin/develop`
  3. `git push --force-with-lease`
- Recommended local config:
  1. `git config --global pull.rebase true`
  2. `git config --global rebase.autoStash true`
  3. `git config --global pull.ff only`
