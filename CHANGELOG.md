# Changelog

## [0.5.1] - 2025-05-19

### Changed
- Updated Async Profiler to 4.0
- Added seconds to file name for heap dumps
- Moved `System::gc` call later, just before the heap dump is executed

### Fixed
- Async Profiler not working after update
- Cpu/Network modules not initializing earlier

## [0.5.0] - 2025-03-25

### Added
- Tick tracking, now works in samplers
- Tick monitoring
- Updated async-profiler to latest nightly (3.0-6761587)
- Allow entity icons to show up in reports

### Changed
- Moved GPU metadata to "extras" section
- Updated keybinds to reflect recent command changes
- Simplified chat message checking to prepend prefixes

### Fixed
- Console logging raw or malformed strings
- Various formatting issues with different translatable strings
- Dedicated server crashes
- Live viewer not working (note: experimental, works better than spark but not ideal)

## [0.4.0] - 2025-02-12

### Added
- GPU Information in reports (client-side only)
- Cleanroom detection

### Fixed
- Errors when creating sampler reports due to null defaults in ExportProps

### Removed
- Debug lines

## [0.3.0] - 2025-02-05

### Added
- Default sampler configurations

### Fixed
- Issues relating to dedicated server environments
- Typos on average/summary timings messages
- Configuration for stages profiling not working (Thanks CaliforniaDemise)
- Typo on location for async profiler libraries
- Activity logging

## [0.2.1] - 2024-09-05

### Fixed
- Crashes in Cleanroom environment due to path issues
- Ingame file paths not being correct

## [0.2.0] - 2024-09-05

### Added
- /flarec (client-side) commands
- Client-sided profiling

## [0.1.1] - 2024-09-03

### Fixed
- Crashes from not using srg names in reflection

### Changed
- Moved reflections to using mixins, since we depend on MixinBooter already

## [0.1.0] - 2024-09-02

### Added
- First release!