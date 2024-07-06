# If Git Diff
<!---freshmark shields
output = [
    link(shield('Gradle plugin', 'plugins.gradle.org', 'com.diffplug.if-git-diff', 'blue'), 'https://plugins.gradle.org/plugin/com.diffplug.if-git-diff'),
    link(shield('Maven central', 'mavencentral', 'available', 'blue'), 'https://search.maven.org/search?q=g:com.diffplug.spotless-changelog'),
    link(shield('Apache 2.0', 'license', 'apache-2.0', 'blue'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)'),
    '',
    link(shield('Changelog', 'changelog', versionLast, 'brightgreen'), 'CHANGELOG.md'),
    link(shield('Javadoc', 'javadoc', 'yes', 'brightgreen'), 'https://javadoc.jitpack.io/com/github/diffplug/spotless-changelog/spotless-changelog-agg/release~{{versionLast}}/javadoc/'),
    link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/diffplug/spotless-changelog'),
    link(image('CircleCI', 'https://circleci.com/gh/diffplug/spotless-changelog.svg?style=shield'), 'https://circleci.com/gh/diffplug/spotless-changelog')
    ].join('\n');
-->
[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-com.diffplug.if--git--diff-blue.svg)](https://plugins.gradle.org/plugin/com.diffplug.if-git-diff)
[![Maven central](https://img.shields.io/badge/mavencentral-available-blue.svg)](https://search.maven.org/search?q=g:com.diffplug.spotless-changelog)
[![Apache 2.0](https://img.shields.io/badge/license-apache--2.0-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

[![Changelog](https://img.shields.io/badge/changelog-3.1.1-brightgreen.svg)](CHANGELOG.md)
[![Javadoc](https://img.shields.io/badge/javadoc-yes-brightgreen.svg)](https://javadoc.jitpack.io/com/github/diffplug/spotless-changelog/spotless-changelog-agg/release~3.1.1/javadoc/)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/diffplug/spotless-changelog)
[![CircleCI](https://circleci.com/gh/diffplug/spotless-changelog.svg?style=shield)](https://circleci.com/gh/diffplug/spotless-changelog)
<!---freshmark /shields -->

This plugin can be applied in `settings.gradle` or `build.gradle`, and it lets you execute a block of code contingent on whether there are changes in the given folder relative to a given baseline git ref.

```gradle
plugins {
  id 'com.diffplug.if-git-diff'
}
ifGitDiff {
  baseline 'origin/main' // default value
  inFolder 'a', { include 'a' }
  inFolder 'b', { include 'b' }
}
```

## Limitations

This plugin does not work well with the configuration cache. Using the example above:

- run `gradlew test` on a clean checkout of `origin/main`, and you would see that `:test` ran but `:a:test` and `:b:test` did not; so far so good.
- now add a file `a/blah`
- now if you run `gradlew test`
  - without configuration-cache -> `:test` and `:a:test` -> good!
  - with configuration-cache -> only `:test` -> bad, cached configuration doesn't know that `a/blah` was added

A different approach which could work with configuration-cache is to mark tasks as up-to-date based on git status, see the [`GitDiffUpToDatePlugin`](https://github.com/thahnen/GitDiffUpToDatePlugin) for that.

## Roadmap

This plugin was built to solve [a fairly specific problem in the Spotless build](https://github.com/diffplug/spotless-changelog/issues/30). It is packaged with `spotless-changelog` because it's vaguely related, and it might make sense someday for `spotless-changelog` to assert "if files changed in X dir, then changelog Y must be updated".

## Reference

<!---freshmark version
output = prefixDelimiterReplace(input, "id 'com.diffplug.spotless-changelog' version '", "'", versionLast)
output = prefixDelimiterReplace(output, 'https://github.com/diffplug/spotless-changelog/blob/release/', '/spotless', versionLast)
output = prefixDelimiterReplace(output, 'https://javadoc.io/static/com.diffplug.spotless-changelog/spotless-changelog-plugin-gradle/', '/', versionLast)
-->

[Plugin DSL javadoc](https://javadoc.io/static/com.diffplug.spotless-changelog/spotless-changelog-plugin-gradle/3.1.1/com/diffplug/spotless/changelog/gradle/IfGitDiffExtension.html).  For requirements see [spotless-changelog](https://github.com/diffplug/spotless-changelog#requirements).

<!---freshmark /version -->

## Acknowledgments

- Git stuff by [jgit](https://www.eclipse.org/jgit/).
- Built by [gradle](https://gradle.org/).
- Maintained by [DiffPlug](https://www.diffplug.com/).
