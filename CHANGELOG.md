# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Fixed
- Fix a bug when loading spotless-changelog from configuration cache.

## [2.3.0] - 2021-11-10
### Added
- Full support for Gradle Configuration Cache ([#27](https://github.com/diffplug/spotless-changelog/pull/27)).
### Changed
- Minimum required Gradle bumped from `5.2` to `6.2` because we need `BuildService` and `ProviderFactory.gradleProperty`.

## [2.2.0] - 2021-05-11
### Added
- `tagMessage` allows to create annotated tags when publishing new versions. ([#22](https://github.com/diffplug/spotless-changelog/pull/22))

## [2.1.2] - 2021-04-10
### Fixed
- Starting in `2.1.0`, we stopped sending credentials to `git:` repositories, this is now fixed.

## [2.1.1] - 2021-04-10
### Fixed
- Added `org.eclipse.jgit.ssh.jsch` dependency, which is required for JGit >= `5.8`.

## [2.1.0] - 2021-03-30
### Added
- Support for remote url `ssh://` ([#19](https://github.com/diffplug/spotless-changelog/issues/19))

## [2.0.1] - 2021-03-13
### Fixed
- When the remote branch was missing (as in a bare checkout), the user got an NPE rather than a nice error message. ([#16](https://github.com/diffplug/spotless-changelog/issues/16))

## [2.0.0] - 2020-06-16
### Changed
- **BREAKING** Default branch is now assumed to be `main` rather than `master`. ([#13](https://github.com/diffplug/spotless-changelog/pull/13))
### Fixed
- Better error message for cases where the `spotlessChangelog` block is too low. ([#6](https://github.com/diffplug/spotless-changelog/issues/6))
- Bug in `PoolString.concat(String)` ([1f6da65](https://github.com/diffplug/spotless-changelog/commit/1f6da65b51c5ee7af847dc0e427fe685fbd3d43c)).
- No longer accepts git failures silently (they were always printed, but did not properly kill the build). ([#11](https://github.com/diffplug/spotless-changelog/issues/11))

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
