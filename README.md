# CodeScribe

A lightweight Android text editor for developers and technical writers, built for
**IS2205: Mobile Application Design and Development** (Mini-Project — *Modern
Mobile Text Editor with Incremental Version Control*).

CodeScribe supports Kotlin and Markdown syntax highlighting, local file
lifecycle management via the Storage Access Framework, automated crash
recovery, and a delta-based (non-duplicating) version control system backed
by Room and `java-diff-utils`.


## Features (mapped to the project spec)

| Spec requirement | Status | Where |
|---|---|---|
| Open / New / Recent / Save / Save As | ✅ | `FileRepository`, `HomeViewModel`, `EditorViewModel` |
| Word wrap, undo/redo, search & replace | ✅ | `EditorViewModel`, `UndoRedoStack`, `SearchReplaceBar` |
| Kotlin keyword highlighting (external keyword file) | ✅ | `KotlinHighlighter`, `assets/kotlin_keywords.txt` |
| Markdown highlighting + toggleable preview | ✅ | `MarkdownHighlighter`, `MarkdownPreviewPane` (Markwon) |
| 10s autosave crash recovery | ✅ | `AutosaveManager` |
| Read-only file lock | ✅ | `FileRepository.setReadOnly`, `EditorViewModel.toggleReadOnly` |
| Delta-based version control (no duplication) | ✅ | `VersionRepository`, `DiffEngine` (`java-diff-utils`) |
| Rollback + line-by-line diff view | ✅ | `VersionHistoryScreen`, `DiffScreen` |
| Room persistence for version history | ✅ | `AppDatabase`, `VersionEntity`/`VersionDao` |
| Code formatting (optional) | ⬜ Not implemented — optional per spec | — |
| Save As encoding picker | ⚠️ UTF-8 only, no picker — see report | `FileRepository` |

## Architecture

```
com.ucsc.codescribe
├─ ui/                 Compose screens, ViewModels, theming, navigation
│  ├─ home/            Recent files list, new/open file
│  ├─ editor/           Text editing, search & replace
│  ├─ versions/         Version history + diff viewer
│  ├─ components/       Shared dialogs, gutter, cards
│  └─ theme/             Colors, typography, Material3 theme
├─ editor/
│  ├─ highlight/         Kotlin & Markdown tokenizers (regex-based)
│  ├─ undo/              In-memory undo/redo stack
│  └─ autosave/          Periodic crash-recovery cache
├─ data/
│  ├─ repository/        File I/O (SAF) + version-control orchestration
│  ├─ diff/               java-diff-utils wrapper (patch + diff rows)
│  └─ db/                 Room database, entities, DAOs
└─ domain/model/          Plain data classes shared across layers
```

## Team & Individual Contributions

| Member | Responsibility | Key files |
|---|---|---|
| [MK.Hasna-24020397] | Delta-based version control: storage logic, patch generation/application, Room schema | `VersionRepository`, `DiffEngine`, `AppDatabase`, `TrackedFileEntity/Dao`, `VersionEntity/Dao` |
| [M.S.F.Haseefa-24020389] | Version history UI & diff comparison view; editor engine (undo/redo, search & replace, autosave, read-only lock) | `VersionHistoryScreen`, `DiffScreen`, `VersionsViewModel`, `FileRepository`, `OpenFileCoordinator`, `EditorViewModel`, `EditorScreen`, `UndoRedoStack`, `AutosaveManager`, `SearchReplaceBar` |
| [M.U.F.Ifadha-24020427] | Syntax highlighting engine, Markdown preview, app shell (navigation, theming, DI wiring) | `KotlinHighlighter`, `MarkdownHighlighter`, `SyntaxVisualTransformation`, `MarkdownPreviewPane`, `HomeScreen`, `HomeViewModel`, `Theme`/`Color`/`Type`, `CodeScribeNavHost`, `MainActivity`, `AppContainer`, `AppViewModelFactory` |

Each member developed and is individually responsible for their module on a
dedicated feature branch (`version-control`, `editor-core`, `highlighting-preview`),
merged into `main` via pull request. See the commit history and PRs for
per-person contribution detail.

## Tech Stack

Kotlin, Jetpack Compose, Room (SQLite), `java-diff-utils`, Markwon, Storage
Access Framework, Material 3. Native Android, min SDK 26.

