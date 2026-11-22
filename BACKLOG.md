# Backlog

## 1. Stop Zalgo from reading nearby text

```text
Make a focused privacy fix in the Lettering Android app. Follow AGENTS.md.

In base/src/main/java/io/github/diepet/lettering/styles/Zalgo.kt, Zalgo is marked as sequence-aware even though its encode method only uses the text being styled. MyInputMethodService.encodeCharacter uses that flag to decide whether to ask the active editor for up to five characters before the cursor.

Mark Zalgo as a style that needs no surrounding text. Preserve its decorative output and keep RandomCaps sequence-aware because that style uses preceding text to choose the next letter's case.

Add or update a regression test that verifies Zalgo is not sequence-aware and still adds combining marks when called without surrounding text. Check for the marks without asserting an exact output, since Zalgo is randomized. Run the relevant test if the environment permits and report the result.
```

## 2. Use plain typing in password fields

```text
Make a focused privacy fix in the Lettering Android app. Follow AGENTS.md.

In base/src/main/java/io/github/diepet/lettering/keyboard/MyInputMethodService.kt, use the active editor's EditorInfo.inputType to identify password fields. Match both the input class and variation. Cover text passwords, visible text passwords, web passwords, and numeric passwords such as PINs.

For these fields, commit the character the user intended after normal key and Shift handling. Bypass all decorative transformations, reads of nearby text for styling, and cursor movement added by reverse styles. Use normal deletion direction while typing in password fields. Apply this behavior to on-screen keys and physical keys handled by Lettering. Keep the user's style selection so it works again in regular fields. Refresh the password state whenever input starts or restarts, including when switching between fields.

Add or update tests for the password input types, plain typing with a style selected, and switching back to a regular field. Verify that the password typing path does not request text before the cursor. Run the relevant tests if the environment permits and report the result.
```

## 3. Delete highlighted text without reading it

```text
Make a focused privacy fix in the Lettering Android app. Follow AGENTS.md.

In base/src/main/java/io/github/diepet/lettering/keyboard/MyInputMethodService.kt, change handleDeleteKeyPress so Backspace can delete highlighted text without calling getSelectedText or fetching text around the cursor to infer the selection. Use selection start and end positions from Android or an editor-handled deletion action that respects the live selection.

Preserve the current behavior. A highlighted range is deleted on Backspace. With a collapsed selection, Backspace deletes before the cursor in normal typing and after the cursor when reverse typing is active. If you track positions, initialize them for each input session, refresh them when Android reports a selection change, and account for unknown or delayed positions. Prefer an editor-handled fallback over guessing where the selection is.

Add or update tests for highlighted and collapsed selections, both deletion directions, and field switches. Check rapid typing and repeated Backspace, when selection updates can arrive late. Verify that Backspace does not request the selected text or surrounding text. Run the relevant tests if the environment permits and report the result.
```

## 4. Choose which text styles appear on the keyboard

```text
Let Lettering users choose which text styles appear in the keyboard's style picker. Follow AGENTS.md.

Start with Settings → Configure text styles. StyleConfigFragment, StyleConfigAdapter, and AvailableStyles already provide per-style switches and local storage. Use those controls and saved preferences. Show every available style in Settings, including disabled styles, so users can re-enable any style. Keep the order the user configured.

When the user returns to the keyboard, show only enabled styles and make sure the highlighted style matches the style applied to typing. Keep an active style selected by its ID if it is still enabled. If the active style was disabled or no styles are enabled, use plain typing. Re-enabling a style should make it available in the picker again.

Add or update tests for disabling and re-enabling a style, restoring the saved choices after an app restart, disabling the active style, and disabling all styles. Run the relevant tests if the environment permits and report the result.
```
