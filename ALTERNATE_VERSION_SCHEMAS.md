# Alternate version schemas

Our thesis is:
- changelogs are more important than versions
- making the version a pure `f(changelog)` would be an improvement for a lot of projects
- haggling over the exact semantics is probably not worth it

But if you want to haggle, you are free to do so!  If you want `2.0` instead of `2.0.0`, or `brand.major.minor.patch`, or more complex logic for determining the bump, you can do all of those things.

All you have to do is implement the function below:

```java
public abstract class VersionBumpFunction implements Serializable {
  public abstract String nextVersion(String unreleasedChanges, String lastVersion);
}
```

Once you've done that, you can enable it in your buildscript like so.

```gradle
spotlessChangelog {
  next = new VersionBumpFunction.BrandPrefix()
}
```

TODO: link to code

## Available schemas

* `VersionBumpFunction.Semver (breaking.added.fixed)` - the highly-recommended default
* `VersionBumpFunction.BrandPrefix (brand.breaking.added.fixed)` - allows a leading `brand` digit, with no automatic way to bump it, but the rest bumps the same as semver

We don't want to host a debate about "which scheme is best".  If you make a new `VersionBumpFunction`, you can let it live in your buildscript, and we would love to link to it from the section above.  If you want to centralize your function across your builds, you can use [blowdryer](https://github.com/diffplug/blowdryer) for that.  If you think it should be a built-in, open up an issue or PR and we can talk :)
