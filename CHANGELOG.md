# Changelog

All notable changes to Hall of Memories are documented here.

The format follows one top-level bullet per significant, user-facing change,
leading with a short bold summary — the `Release` workflow extracts exactly
those lead-ins into the GitHub Release body. See `CLAUDE.md`, "Changelog and
release process".

## [Unreleased]

- **Planning documents for the phased v1 build.** The functional
  specification, a seven-phase implementation plan, a measured PokéAPI and
  sprite reference, and the agent guide that governs how the code is written.
- **Project foundation (Phase 0).** The app now builds and installs: Gradle
  project setup with the pinned dependency catalogue, Hilt, Material 3 theme
  with light/dark/system switching, an in-app Italian/English language
  picker, a navigation drawer with the three v1 sections (hack library,
  saved Pokémon, settings), and the `Android CI` workflow.
