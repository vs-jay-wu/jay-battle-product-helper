# cs-docs-ci

Create or revise a CI documentation page following the ClassSwift docs design system.

## Usage

- **New document** — describe the source file and topic, e.g.:
  _"create a doc for trigger-build.yaml"_
- **Revision** — name the target file and describe the change, e.g.:
  _"revise build-android.html to add a Secrets section"_

## Steps

1. Read the `<style>` block from an existing page (e.g. `docs/ci/workflows/build-android.html`) to inventory all available classes and CSS variables before writing any HTML.
2. Determine intent from the user's prompt:
   - **New**: derive the filename in kebab-case (e.g. `trigger-build.html`), then create `docs/ci/workflows/<filename>.html` using the boilerplate below. Copy the full `<style>` block from an existing page into the new file.
   - **Revision**: read the target file at `docs/ci/workflows/<filename>.html`, apply the requested changes using existing classes. If a new UI pattern is genuinely needed, add the class to the `<style>` block of that HTML file first, then use it in the markup.
3. Confirm the output path and a one-line summary of what was created or changed.

## Hard rules

- Each HTML file is self-contained — copy the full `<style>` block from an existing page when creating a new one.
- Never use inline `style=""` attributes — always use a class defined in the `<style>` block.
- Never add a CSS class to an HTML file without first defining it in that file's `<style>` block.
- All documents live in `docs/ci/workflows/`.

## Standard HTML boilerplate

    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
      <title><!-- Title --> — ClassSwift</title>
      <link rel="preconnect" href="https://fonts.googleapis.com" />
      <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
      <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet" />
      <style>
        /* Copy the full <style> block from docs/ci/workflows/build-android.html */
      </style>
      <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
    </head>
    <body>
    <div class="page">

      <div class="header">
        <div class="header-eyebrow">
          <span class="header-eyebrow-dot"></span>
          <!-- source file path, e.g. .github/workflows/build-android.yaml -->
        </div>
        <h1><!-- Page title --></h1>
        <p class="header-sub"><!-- Subtitle --></p>
      </div>

      <!-- cards go here -->

    </div>
    <script>
      mermaid.initialize({ startOnLoad: false, theme: 'base', flowchart: { curve: 'basis', htmlLabels: true } });
      document.addEventListener('DOMContentLoaded', function () {
        mermaid.run({ querySelector: '.mermaid' }).then(function () {
          document.querySelectorAll('.env-pane:not(.active)').forEach(function (p) {
            p.style.display = 'none';
          });
        });
      });
      function showTab(env) {
        document.querySelectorAll('.env-tab-btn').forEach(function (btn) {
          btn.classList.toggle('active', btn.classList.contains(env));
        });
        document.querySelectorAll('.env-pane').forEach(function (pane) {
          pane.style.display = pane.id === 'tab-' + env ? 'block' : 'none';
        });
      }
    </script>
    </body>
    </html>

## Available components

Reference these classes from the embedded `<style>` block — do not reinvent them:

| Component | Classes |
|---|---|
| Page wrapper | `.page` |
| Header | `.header`, `.header-eyebrow`, `.header-eyebrow-dot`, `.header-sub`, `.header-env-row`, `.header-env-pill` + `.dot` inside, `.hep-stag`, `.hep-rc`, `.hep-prod` |
| Card | `.card`, `.card-title`, `.card-icon` |
| Body text | `.prose` |
| Callout / warning | `.callout`, `.callout-icon` |
| Badges | `.badge`, `.badge-stag`, `.badge-rc`, `.badge-prod`, `.badge-cond` |
| Table | `.table-wrap` + standard `table / th / td` |
| Env tabs | `.env-tabs-wrap`, `.env-tab-bar`, `.env-tab-btn` + `.active` (JS-toggled), `.tab-dot`, `.tab-chip`, `.env-pane` + `.active` (JS-toggled), `.env-pane-meta`, `.meta-kv`, `.meta-sep` + `.stag` / `.rc` / `.prod` modifiers |
| Mermaid diagram | `.mermaid-wrap`, `.mermaid` |
| Section label | `.subhead` |
| Command block | `.cmd-block`, `.cmd-comment` |
| Future work list | `.todo-list`, `.todo-ring` |
| Spacing utilities | `.mt-12`, `.mt-20`, `.mb-18` |

### CSS variables

    /* Colors */
    --stag, --stag-light, --stag-mid
    --rc,   --rc-light,   --rc-mid
    --prod, --prod-light, --prod-mid
    --cond, --cond-light, --cond-mid

    /* Neutrals */
    --bg, --card, --border, --border-light
    --text-primary, --text-secondary, --text-muted

    /* Elevation */
    --shadow-xs, --shadow-sm, --shadow-md

    /* Shape */
    --r, --r-sm
