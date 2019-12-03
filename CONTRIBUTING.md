# Contributing to Blowdryer

Pull requests are welcome!  In order to get merged, you'll need to:

- [ ] have tests
- [ ] update the changelog

## Build instructions

It's a bog-standard gradle build.

- `gradlew eclipse` creates an Eclipse project file for you.
- `gradlew build` builds the jar and runs the tests

If you're getting style warnings, `gradlew spotlessApply` will apply anything necessary to fix formatting. For more info on the formatter, check out [spotless](https://github.com/diffplug/spotless).

## Test locally

To make changes to Blowdryer and test those changes on a local project, add the following to the top of your local project's `build.gradle` (the project you want to use Blowdryer on, not Blowdryer itself):

```groovy
buildscript {
  repositories {
    mavenLocal()
    jcenter()
    configurations.all {
      resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
    }
  }

  dependencies {
    classpath 'com.diffplug:blowdryer:+'
  }
}
```

To test your changes, run `gradlew publishToMavenLocal` on your Blowdryer project.  Now you can make changes and test them on your project.

## Use unreleased versions from JitPack

We publish tagged releases to mavenCentral, jcenter, and the gradle plugin portal.  But you can also grab any intermediate release using [JitPack](https://jitpack.io/#com.diffplug/blowdryer).

## License

By contributing your code, you agree to license your contribution under the terms of the APLv2: https://github.com/diffplug/blowdryer/blob/master/LICENSE

All files are released with the Apache 2.0 license as such:

```
Copyright 2019 DiffPlug

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
