# Noodling

NeoForge mod that rasterises pasta shapes into blocks. `InnerSpaceIterable` (package `us.drullk.noodling.shape`) is the core
primitive: an oriented cuboid with its own inner coordinate space, iterated as a 3D generalisation of Bresenham/DDA.

## Versions

- Minecraft 26.3
- NeoForge 26.3.0.23-beta (ModDevGradle 2.0.147)
- Java 25 toolchain
- JUnit 5.13.4 (`src/test`, run with `./gradlew test`; never packed into the mod jar)

## Datagen

- Providers live in the `datagen` source set (`src/datagen`), which is never packed into the mod jar; only the
  `data` run uses it as its classpath.
- `./gradlew runData` writes into `src/generated/resources` (included in the jar); commit the generated JSON.
- `assets/noodling/lang/en_us.json` is generated, so add translations in `PastaLanguageProvider`, not by hand.

## AI attribution procedure

The repository owner cites AI-generated work, so every session Claude works in must follow these steps.

1. Transcripts live at `llm/transcripts/transcript_{start date, UTC}_{session uuid}.md` and are generated only when the owner asks, never on Claude's own initiative.
2. Generate one with `python3 llm/tools/claude_transcript_to_md.py` from the repo root; it picks the most recent session for this project and names the file itself.
3. A session may span several commits, so re-run the same command whenever the owner asks for an update and commit the changed transcript alongside the code; Git tracks its history.
4. Every class Claude creates or modifies gets a class-level Javadoc line `<p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript-....md}.` using the repo-relative path (a modified class that already cites an earlier transcript keeps that line and gains one for the new session).
5. Find the path to cite before the transcript exists with `python3 llm/tools/claude_transcript_to_md.py --print-path`.
6. The desktop app sometimes resumes a conversation under a new session uuid; the tool detects this, renames the earlier transcript with `git mv`, and rewrites references in the project's text files, so check its output and stage the renamed file.
7. Do not add the owner's email, token usage, request IDs or Claude's reasoning to anything committed; the tool already strips them from transcripts.

## Transcript tool usage

- `python3 llm/tools/claude_transcript_to_md.py --list` lists the sessions recorded for this project with their uuid, start and end time, and opening prompt.
- `python3 llm/tools/claude_transcript_to_md.py` exports the latest session to `llm/transcripts/`.
- `--session <uuid prefix>` selects a specific session; `--file <path.jsonl>` exports a transcript file directly.
- `--print-path` prints the repo-relative output path without writing anything.
- `--include-results` embeds truncated tool output; `--max-chars N` changes the truncation limit (0 = unlimited); `-o <file>` or `-o -` overrides the destination.
- Sessions are read from `~/.claude/projects/<project path with slashes replaced by dashes>/*.jsonl`; the desktop app's `local_…` session handle is not a file name.
