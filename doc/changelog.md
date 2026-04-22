**Beta 3:**
* Added: More colors to the color palette.
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
