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
