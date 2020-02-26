# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Fixed
- Bug in `PoolString.concat(String)`.

## [1.1.0] - 2020-01-13
### Added
- Support for a `-SNAPSHOT` mode ([#4](https://github.com/diffplug/spotless-changelog/pull/4)).
### Fixed
- Minor documentation and test improvements.

## [1.0.0] - 2020-01-12
![Peeling off the wrapper](raw-peel.jpg)
*[Gear Live](http://www.gearlive.com/gallery/image_full/142268).  Gear Live Inc.  [CC Public Domain](https://creativecommons.org/share-your-work/public-domain/).*

### Changed
- **BREAKING** Renamed `SemverCondense__X_Y_0__to__X_Y` to `SemverCondense_XY0_to_XY`.
### Fixed
- The project URL was accidentally set to [blowdryer](https://github.com/diffplug/blowdryer).

## [0.3.0] - 2020-01-11
### Added
- Groups and descriptions for the tasks, so that they show up in `./gradlew tasks`.

## [0.2.1] - 2020-01-11
### Fixed
- The git status messages that get printed to console.

## [0.2.0] - 2020-01-11
### Added
- Version calculation is cacheable in-memory
- Third-party version calculation is now possible
- **BREAKING** misc to make the above work
### Fixed
- We now respect the `gh_pages` env variable as a way to do GitHub auth, as we intended to all along.

## [0.1.1] - 2020-01-02
### Fixed
- Fixed the "push status" messages so that they are useful instead of `push: org.eclipse.jgit.transport.PushResult@48be852b`.

## [0.1.0] - 2019-12-21
First release.
