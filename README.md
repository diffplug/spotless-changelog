# <img align="left" src="logo.png"> Spotless Changelog
***The changelog is cast, let the versions fall where they may.***
<!---freshmark shields
output = [
    link(shield('Gradle plugin', 'plugins.gradle.org', 'com.diffplug.spotless-changelog', 'blue'), 'https://plugins.gradle.org/plugin/com.diffplug.spotless-changelog'),
    link(shield('Maven central', 'mavencentral', 'available', 'blue'), 'https://search.maven.org/search?q=g:com.diffplug.spotless-changelog'),
    link(shield('Apache 2.0', 'license', 'apache-2.0', 'blue'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)'),
    '',
    link(shield('Changelog', 'changelog', versionLast, 'brightgreen'), 'CHANGELOG.md'),
    link(shield('Javadoc', 'javadoc', 'yes', 'brightgreen'), 'https://javadoc.jitpack.io/com/github/diffplug/spotless-changelog/spotless-changelog-agg/release~{{versionLast}}/javadoc/'),
    link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/diffplug/spotless-changelog'),
    link(image('JitCI', 'https://jitci.com/gh/diffplug/spotless-changelog/svg'), 'https://jitci.com/gh/diffplug/spotless-changelog')

    ].join('\n');
-->
[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-com.diffplug.spotless--changelog-blue.svg)](https://plugins.gradle.org/plugin/com.diffplug.spotless-changelog)
[![Maven central](https://img.shields.io/badge/mavencentral-available-blue.svg)](https://search.maven.org/search?q=g:com.diffplug.spotless-changelog)
[![Apache 2.0](https://img.shields.io/badge/license-apache--2.0-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

[![Changelog](https://img.shields.io/badge/changelog-0.1.1-brightgreen.svg)](CHANGELOG.md)
[![Javadoc](https://img.shields.io/badge/javadoc-yes-brightgreen.svg)](https://javadoc.jitpack.io/com/github/diffplug/spotless-changelog/spotless-changelog-agg/release~0.1.1/javadoc/)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/diffplug/spotless-changelog)
[![JitCI](https://jitci.com/gh/diffplug/spotless-changelog/svg)](https://jitci.com/gh/diffplug/spotless-changelog)
<!---freshmark /shields -->

Spotless Changelog checks that your changelog complies with the format of [keepachangelog](https://keepachangelog.com/), and uses the information from **only the changelog file** to compute the version of the next release. If you want, you can use the computed version to update the changelog file, then automatically commit, tag, and push when a release is published.

There are [many plugins](https://plugins.gradle.org/search?term=version) that compute your next version using a variety of configurable rules based on git tags, commit message standards, class file comparison, and other methods.  They tend to require a lot of documentation.

If your changelog doesn't already have information about breaking changes and new features, then you should fix that first, whether you end up adopting this plugin or not!  But once your changelog has that information, why not make things simple and use it as the source-of-truth?  If you want, there are also plugins which can generate your changelog automatically from your [git commits](https://plugins.gradle.org/search?term=git+changelog) or [github issues](https://plugins.gradle.org/search?term=github+changelog) (although we tend to think it's better to be a bit more human about it).

Currently Spotless Changelog only has a gradle plugin, but the logic lives in a separate library, and we welcome contributions for other build systems, just [as happened with the Spotless code formatter](https://github.com/diffplug/spotless/issues/102).

<!---freshmark version
output = prefixDelimiterReplace(input, "id 'com.diffplug.spotless-changelog' version '", "'", versionLast)
output = prefixDelimiterReplace(output, 'https://github.com/diffplug/spotless-changelog/blob/release/', '/spotless', versionLast)
output = prefixDelimiterReplace(output, 'https://javadoc.io/static/com.diffplug.spotless-changelog/spotless-changelog-plugin-gradle/', '/', versionLast)
-->

## Keep your changelog clean

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
  id 'com.diffplug.spotless-changelog' version '0.1.1'
}

spotlessChangelog { // only necessary if you need to change the defaults below
  changelogFile 'CHANGELOG.md'
  enforceCheck true
}
```

The `changelogCheck` task will now run every time `gradlew check` runs, and it will verify that the changelog versions and dates conform to the format.

It does not enforce the entire keepachangelog format, only the `## [x.y.z] yyyy-mm-dd` lines.  We're happy to take a PR to support a stricter format, but it will be optional.

### Legacy changelogs

If you have a big long legacy changelog that's not in the [keepachangelog](https://keepachangelog.com/) format, good job!  Changelogs are good!

You don't have to convert the whole thing.  Just stick `<!-- END CHANGELOG -->` on its own line into your changelog, and it will stop parsing as soon as this line is encountered.  The goal isn't perfection, just better.

## Compute the next version

By default, Spotless Changelog uses the names `breaking.added.fixed` for `x.y.z`. When computing the next version, Spotless Changelog always starts with the most recent version from your changelog.  From there, the only decision we have to make is which position to bump: `breaking`, `added`, or `fixed`.  By default, Spotless Changelog will bump `fixed`.  All you need to do is set the rules for escalating to an `added` or `breaking` bump:

```gradle
spotlessChangelog {  // defaults
  // breaking.added.fixed
  ifFoundBumpBreaking '**BREAKING**'
  ifFoundBumpAdded    '### Added'
}
```

If you're curious what the next version will be:

```gradle
cmd> gradlew changelogPrint
myproj 1.0.4 -> 1.1.0
```

You can use these versions in your buildscript as `spotlessChangelog.versionNext` and `spotlessChangelog.versionLast`:

```gradle
project.version = project.spotlessChangelog.versionNext
dependencies {
  implementation "group:artifact:${project(':dep-project-with-own-changelog').spotlessChangelog.versionLast}"
}
```

We also support other version schemas, like `2.0` instead of `2.0.0`, or `brand.major.minor.patch`, or `Ryyyy.SRx`, or any custom function you want.  See [ALTERNATE_VERSION_SCHEMAS.md](ALTERNATE_VERSION_SCHEMAS.md) for details.

### Alphas, betas, release-candidates, etc.

If you want, you can set `forceNextVersion '3.0.0.BETA7-RC1-FINAL'`.  It will still check that your changelog is formatted, but it will short-circuit the next version calculation.  This is also how to go from `0.x` to `1.0`.

## Update the changelog, commit, push

`gradlew changelogBump` will turn `[Unreleased]` into `[1.2.3] 2011-11-11` (or whatever) and add a new `[Unreleased]` section in your working copy file.  `gradlew changelogPush` will commit, tag, and push that change.

```gradle
spotlessChangelog {  // defaults
  tagPrefix 'release/'
  commitMessage 'Published release/{version}' // {version} will be replaced
  remote 'origin'
  branch 'master'
}
```

We recommend that you use `changelogPush` as your deploy task, and wire your tasks so that it will only happen if the publish was successful.  Here is a [battle-tested wiring plan](https://github.com/diffplug/blowdryer-diffplug/blob/master/src/main/resources/base/changelog.gradle) which can even [update your README.md with the latest version number](https://github.com/diffplug/blowdryer-diffplug/blob/2.0.0/src/main/resources/spotless/freshmark.gradle#L35-L61).  A simpler scheme is presented below:

### Multiple changelogs per project

That's fine too!  If you apply `com.diffplug.spotless-changelog` to multiple projects, just be sure to set the `tagPrefix` and `commitMessage` properties so that you have unique tags and commit messages for each project.  You also might want to use the snippet below in your root project to ensure that you only release one of the changelogs at a time.

```gradle
gradle.taskGraph.whenReady { taskGraph ->
  def changelogPushTasks = taskGraph.allTasks.stream()
    .filter { t -> t.name == 'changelogPush' }
    .map { t -> t.path }
    .toList()
  if (changelogPushTasks.size() > 1) {
    throw new IllegalArgumentException("Run changelogPush one at a time:\n" + changelogPushTasks.join('\n'))
  }
}
```

## Reference

### Plugin DSL ([javadoc](https://javadoc.io/static/com.diffplug.spotless-changelog/spotless-changelog-plugin-gradle/0.1.1/com/diffplug/spotless/changelog/gradle/ChangelogExtension.html))

```gradle
spotlessChangelog { // all defaults
  // keep changelog formatted
  changelogFile 'CHANGELOG.md'
  enforceCheck true
  // calculate next version (breaking.added.fixed)
  ifFoundBumpBreaking ['**BREAKING**']
  ifFoundBumpAdded    ['### Added']
  forceNextVersion null
  // see ALTERNATE_VERSION_SCHEMAS.md
  versionBumpFunction = new VersionBumpFunction.Semver()
  // tag and push
  tagPrefix 'release/'
  commitMessage 'Published release/{version}' // {version} will be replaced
  remote 'origin'
  branch 'master'
}
```

### Tasks ([code](https://github.com/diffplug/spotless-changelog/blob/release/0.1.1/spotless-changelog-plugin-gradle/src/main/java/com/diffplug/spotless/changelog/gradle/ChangelogPlugin.java))

- `changelogPrint` - prints the last published version and calculated next version
  - `myproj 1.0.4 -> 1.1.0`
- `changelogCheck` - throws an error if the changelog is not formatted according to your rules
  - if `enforceCheck true` (default) then `check.dependsOn changelogCheck`
- `changelogBump` - updates the changelog on disk with the next version and the current UTC date
  - applying `changelogBump` multiple times in a row is fine, an empty section under `[Unreleased]` is enough to know that it has already been applied.
- `changelogPush` - commits the changelog, tags, and pushes
  - `changelogPush` depends on `changelogBump` depends on `changelogCheck`
  - If `changelogPush` is in the task graph, then `changelogCheck` will do an extra check to make sure that the git push will succeed.  The `changelogBump` section above shows how you wire `changelogCheck` into your `jar` task so that your build will fail early if you haven't correctly setup the git credentials.

<!---freshmark /version -->

## Acknowledgments

- Huge thanks to [Olivier Lacan](https://github.com/olivierlacan) and [contributors](https://github.com/olivierlacan/keep-a-changelog/graphs/contributors) for [keepachangelog](https://keepachangelog.com/en/1.0.0/).
    - Changelog format is arbitrary, but a consistent format unlocks tooling. It's such an abstract thing, keepachangelog is an outstanding achievement of branding for the greater good!
- Thanks to [Colin Dean](https://github.com/colindean) for [keepachangelog-parser-java](https://github.com/colindean/keepachangelog-parser-java).  We ended up not using it, but because it existed, we [thought](https://twitter.com/pinboard/status/761656824202276864?lang=en) this plugin would be easy to build.
- Git stuff by [jgit](https://www.eclipse.org/jgit/).
- Built by [gradle](https://gradle.org/).
- Maintained by [DiffPlug](https://www.diffplug.com/).
