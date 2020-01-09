# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Now possible to set the version bump function.
- **BREAKING** api changes to support
  - changed `ifFoundBumpMinor` to `next.ifFoundBumpAdded` to enable the improvement above
  - changed `ifFoundBumpMajor` to `next.ifFoundBumpBreaking` to enable the improvement above
### Fixed
- We now respect the `gh_pages` env variable as a way to do GitHub auth, as we intended to all along.

## [0.1.1] - 2020-01-02
### Fixed
- Fixed the "push status" messages so that they are useful instead of `push: org.eclipse.jgit.transport.PushResult@48be852b`.

## [0.1.0] - 2019-12-21
First release.
