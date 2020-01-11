# Alternate version schemas

Our thesis is:
- changelogs are more important than versions
- making the version a pure `f(changelog)` would be an improvement for a lot of projects
- haggling over the exact semantics is probably not worth it

But if you want to haggle, you are free to do so!  If you want `2.0` instead of `2.0.0`, or `brand.major.minor.patch`, or more complex logic for determining the bump, you can do those things easily.

All you have to do is implement the function below:

```java
public abstract class NextVersionFunction implements Serializable {
  /** version = f(changelog) */
  public String nextVersion(ParsedChangelog changelog) {
    return nextVersion(changelog.unreleasedChanges(), changelog.versionLast());
  }

  /**
   * Given a string containing all the unreleased changes and the last published
   * version, this function computes the next version number.
   */
  protected abstract String nextVersion(String unreleasedChanges, String lastVersion);
}
```

Once you've done that, you can enable it in your buildscript like so.

```gradle
import com.diffplug.spotless.changelog.NextVersionFunction.SemverCondense__X_Y_0__to__X_Y
spotlessChangelog {
  versionSchema SemverCondense__X_Y_0__to__X_Y.class
}
```

## Available schemas

### Built-in
* [`Semver`](https://github.com/diffplug/spotless-changelog/blob/8cc570f9ea2049ecc3d4f997b4bc609148f7fa74/spotless-changelog-lib/src/main/java/com/diffplug/spotless/changelog/NextVersionFunction.java#L80-L118) - the highly-recommended default `breaking.added.fixed`
* [`SemverBrandPrefix`](https://github.com/diffplug/spotless-changelog/blob/8cc570f9ea2049ecc3d4f997b4bc609148f7fa74/spotless-changelog-lib/src/main/java/com/diffplug/spotless/changelog/NextVersionFunction.java#L120-L135) - allows a leading `brand` digit (`brand.breaking.added.fixed`), with no automatic way to bump it (use `forceNextVersion`), but the rest bumps the same as semver
* [`SemverCondense__X_Y_0__to__X_Y`](https://github.com/diffplug/spotless-changelog/blob/8cc570f9ea2049ecc3d4f997b4bc609148f7fa74/spotless-changelog-lib/src/main/java/com/diffplug/spotless/changelog/NextVersionFunction.java#L137-L152) - condenses `1.0.0` to `1.0` and `1.2.0` to `1.2`, but leaves `1.2.3` unchanged.

### In user buildscripts

* None so far

### Contributing a schema

We don't care which scheme is "best".  If you make a new `NextVersionFunction`, you can let it live in your buildscript, and we would love to link to it from the section above.  If you want to centralize your function across your builds, you can use [blowdryer](https://github.com/diffplug/blowdryer) for that.  If you think it should be a built-in, open up an issue or PR.  As long as it's not too project-specific, we'll probably merge it in.
