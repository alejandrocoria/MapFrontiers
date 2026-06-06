**Beta 5:**
* Added: Collections can now be shown on maps with their own labels, banners, and visibility settings.
* Changed: Significant performance optimizations were made to the server, client, and network.

**Beta 4:**
* Added: Frontier collections, including grouped sections and per-collection actions in the frontier list.
* Added: HUD now supports a fourth slot and includes Collection by default (Name, Collection, Owner, Banner).
* Added: Temporary personal frontiers can now be created directly from the GUI for session-only use.
* Added: Frontier info paste options now support copying Path style between Path frontiers.
* Changed: Frontier creation from the frontier list now stays in the list unless shape editing is selected.
* Changed: Renamed frontier mode to shape across the GUI and client config for clearer terminology.
* Changed: Replaced the frontier list Type filter with a Shape filter (All, Vertex, Chunk, Path).
* Changed: Improved keyboard navigation across many GUI screens and widgets, including tabs, scroll lists, group settings, the color palette, and the color picker.
* Fixed: Changing frontier info paste options no longer sends unnecessary frontier updates to the server.
* Fixed: OP permission changes from the server only applying after the player disconnected and reconnected.

**Beta 3:**
* Added: More colors to the color palette.
* Changed: Screens can now be closed with the configured inventory key, and settings can also be closed with the configured MapFrontiers settings key.
* Changed: Screens and dialogs now scale and fit their contents more consistently at small window sizes.
* Fixed: Filter reset not saving changes.
* Fixed: Personal frontiers from a previous singleplayer world could be carried over into a newly opened world.

**Beta 2:**
* API - Added: Temporary personal frontiers that exist only for the current session and are not saved, synchronized, or shareable.
* Added: New Path frontier mode for open line-shaped frontiers, including editable points, marker styles, creation presets, and API support.
* Added: Save and cancel buttons to dialogs.
* Changed: Incomplete Vertex frontiers now have a distinct temporary look.
* Changed: Frontier names now support up to 48 characters per line, and the frontier info and list screens were updated to display longer names more cleanly.
* Changed: Frontier labels and banners now use improved shape-aware placement, keeping them better inside the frontier.
* Changed: Selected frontiers now remain highlighted on the fullscreen map even when hidden by visibility settings.
* Changed: Frontier lists now use mode icons to show each frontier mode and shape count more clearly.
* Fixed: Strange behavior of the maximum limit of int text box.

**Beta 1:**
* Added: API available soon.
* Changed: Reorganized the client config file into grouped sections and removed the dependency on ForgeConfigApiPort, so it no longer needs to be installed just for this mod.
* Changed: Existing client configs are now migrated automatically to the new layout, with backup/recovery when the client config file is invalid or unreadable.
* Change: Reduced the number of times frontier changes are written to the file.
* Change: The copy button puts the frontier ID on the clipboard.
* Change: Improved several translations across existing languages. If you notice anything that sounds off, feedback and corrections from native speakers are very welcome.
* Fixed: HUD continues to display with F1.
* Fixed: owned personal frontiers disappear when reconnecting after the server no longer has the mod.
* Fixed: MapFrontiers now fails gracefully when JourneyMap does not initialize its plugin. In-world features are disabled, but the mod no longer breaks.

**To see previous changes, go to the changelog for version 2.7.0-beta.21**
 
**Includes:**
* MapFrontiers API: ${api_version}

**Requirements:**
* ${loadername}: ${loaderversion}
* JourneyMap: ${journeymap_doc_version}
${extra_doc_requirements}
