# Spotless Changelog

Spotless Changelog checks that your changelog complies with the format of [keepachangelog](https://keepachangelog.com/), and uses the information from **only the changelog file** to compute the version of the next release. If you want, you can use the computed version to update the changelog file, then commit, tag, and push whenever a release is published.

There are [many plugins](https://plugins.gradle.org/search?term=version) that compute your next version using a variety of configurable rules based on git tags, commit message standards, class file comparison, and other methods.  They tend to require a lot of documentation.

If your changelog doesn't already have information about breaking changes and new features, then you should fix that first, whether you end up adopting this plugin or not!  But once your changelog has that information, why not make things simple and use it as the source-of-truth?  If you want, there are also plugins which can generate your changelog automatically from your [git commits](https://plugins.gradle.org/search?term=git+changelog) or [github issues](https://plugins.gradle.org/search?term=github+changelog) (although we tend to think that's overkill).

Currently Spotless Changelog only has a gradle plugin, but the logic lives in a separate lib, and we welcome contributions for other build systems, just [as happened with the Spotless code formatter](https://github.com/diffplug/spotless/issues/102).

## How it works

You have to start out with a changelog in the [keepachangelog](https://keepachangelog.com/) format. Here is a concise example:

```
## [Unreleased]
### Added
- A great feature

## [1.0.0] - 2019-12-02
Initial release.
```

In your `build.gradle`, you do this:

```gradle
plugins {
  id 'com.diffplug.spotless-changelog' version 'TODO'
}

spotlessChangelog { // only necessary if you need to change the defaults below
  changelogFile 'CHANGELOG.md'
  types ['Added', 'Changed', 'Deprecated', 'Removed', 'Fixed', 'Security']
  enforceCheck true
}
```

The `changelogCheck` task will now run every time `gradlew check` runs, and it will verify that the changelog conforms to the changelog format, and that every change type is one of the allowed types.

### Computing the next version

When computing the next version, Spotless Changelog always starts with the most recent version from your changelog.  From there, the only decision Spotless Changelog has to make is which position to bump: `major`, `minor`, or `patch`.  By default, Spotless Changelog will bump `patch`.  All you need to do is set the rules for escalating to a `minor` or `major` bump:

```gradle
spotlessChangelog {  // defaults
  typesBumpMinor ['Added']
  typesBumpMajor ['Changed', 'Removed']
  ifFoundBumpMajor '**BREAKING**'
}
```

The defaults above are what is required for [semver](https://semver.org/).  If you want more control over when you bump the major version, you could do this:

```gradle
spotlessChangelog { // but semver is good!  use it!  don't mistake your version for a brand!
  typesBumpMajor []
  ifFoundBumpMajor '**BREAKING**'
}
```

If you're curious what the next version will be:

```gradle
cmd> gradlew changelogPrint
myproj 1.0.4 -> 1.1.0
```

#### First release, betas, release-candidates, etc.

If you want, you can set `forceNextVersion '3.0.0.BETA7-RC1-FINAL'`.  It will still check that your changelog is formatted, but it will short-circuit everything else.


### Update the changelog, commit, push

`gradlew changelogBump` will turn `[Unreleased]` into `[1.2.3] 2011-11-11` (or whatever) and add a new `[Unreleased]` section in your working copy file.  `gradlew changelogPush` will commit, tag, and push that change.

```gradle
spotlessChangelog {  // defaults
  tagPrefix 'release/'
  commitMessage 'Published release/{version}' // {version} will be replaced 
  remote 'origin'
  branch 'master'
}
```


If `release` is the task that publishes your library, then `tasks.release.finalizedBy tasks.changelogPush` will tag and push after a successful publish.

## Reference

### Plugin DSL

```gradle
spotlessChangelog { // all defaults
  // keep changelog formatted
  changelogFile 'CHANGELOG.md'
  types ['Added', 'Changed', 'Deprecated', 'Removed', 'Fixed', 'Security']
  enforceCheck true
  // calculate next version
  typesBumpMinor ['Added']
  typesBumpMajor ['Changed', 'Removed']
  ifFoundBumpMajor '**BREAKING**'
  forceNextVersion null
  // tag and push
  tagPrefix 'release/'
  commitMessage 'Published release/{version}' // {version} will be replaced 
  remote 'origin'
  branch 'master'
}
```

### Tasks

- `changelogCheck` - throws an error if the changelog is not formatted according to [keepachangelog](https://keepachangelog.com/)
  - if `enforceCheck true` (default) then `check.dependsOn changelogCheck`
- `changelogBump` - updates the changelog on disk with the next version and the current date
  - applying `changelogBump` multiple times in a row is fine, an empty section under `[Unreleased]` is enough to know that it has already been applied.
- `changelogPush` - commits the changelog, tags, and pushes
  - `changelogPush.dependsOn changelogBump`

## Acknowledgments

- Huge thanks to Olivier Lacan and [contributors](https://github.com/olivierlacan/keep-a-changelog/graphs/contributors) for [keepachangelog](https://keepachangelog.com/en/1.0.0/).
    - Changelog format is arbitrary, but a consistent format unlocks tooling. It's such an abstract thing, keepachangelog is an outstanding achievement of branding for the greater good!
- Huge thanks to [Colin Dean](https://github.com/colindean) for [keepachangelog-parser-java](https://github.com/colindean/keepachangelog-parser-java).  That library was much harder to write than Spotless Changelog!
- Built by [gradle](https://gradle.org/).
- Maintained by [DiffPlug](https://www.diffplug.com/).