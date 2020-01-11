/*
 * Copyright 2020 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.changelog;


import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.osgi.framework.Version;

/**
 * Function which defines the logic for implementing a version schema.
 * If you want to make a custom function, you can either override
 * {@link Semver}, or you can override {@link NonSemver}.  The advantage
 * of {@link Semver} is that you can use the `ifFoundBumpXXX` methods.
 * See {@link SemverBrandPrefix} or {@link SemverCondense__X_Y_0__to__X_Y} for a simple example.
 * 
 * If you have any methods for configuring your function besides those, you
 * will have to set it up *before* passing it to the gradle extension,
 * because it will be copied-by-value (via serialization) to ensure that
 * it is not mutated after the version has been computed.
 */
public abstract class NextVersionFunction implements Serializable {
	private NextVersionFunction() {}

	/** version = f(changelog) */
	public String nextVersion(Changelog changelog) {
		return nextVersion(changelog.unreleasedChanges(), changelog.versionLast());
	}

	/**
	 * Given a string containing all the unreleased changes and the last published
	 * version, this function computes the next version number.
	 */
	protected abstract String nextVersion(String unreleasedChanges, String lastVersion);

	/** Optional API, used for subclasses of {@link Semver}, throws runtime error for subclasses of {@link NonSemver}. */
	public abstract void ifFoundBumpAdded(List<String> toFind);

	/** Optional API, used for subclasses of {@link Semver}, throws runtime error for subclasses of {@link NonSemver}. */
	public abstract void ifFoundBumpBreaking(List<String> toFind);

	/** Optional API, used for subclasses of {@link Semver}, throws runtime error for subclasses of {@link NonSemver}. */
	public final void ifFoundBumpBreaking(String... toFind) {
		ifFoundBumpBreaking(Arrays.asList(toFind));
	}

	/** Optional API, used for subclasses of {@link Semver}, throws runtime error for subclasses of {@link NonSemver}. */
	public final void ifFoundBumpAdded(String... toFind) {
		ifFoundBumpAdded(Arrays.asList(toFind));
	}

	/**
	 * Base class for {@link NextVersionFunction} for the unusual case that 
	 * {@link #ifFoundBumpAdded(List)} and {@link #ifFoundBumpBreaking(List)} are not supported.
	 */
	public static abstract class NonSemver extends NextVersionFunction {
		@Override
		@Deprecated
		public void ifFoundBumpBreaking(List<String> toFind) {
			throw new IllegalArgumentException(getClass() + " does not support `breaking.added.fixed`.");
		}

		@Override
		@Deprecated
		public void ifFoundBumpAdded(List<String> toFind) {
			throw new IllegalArgumentException(getClass() + " does not support `breaking.added.fixed`.");
		}
	}

	/** Standard semver behavior. */
	public static class Semver extends NextVersionFunction {
		protected List<String> ifFoundBumpBreaking = Arrays.asList("**BREAKING**");
		protected List<String> ifFoundBumpAdded = Arrays.asList("### Added");

		@Override
		public void ifFoundBumpBreaking(List<String> toFind) {
			ifFoundBumpBreaking = Objects.requireNonNull(toFind);
		}

		@Override
		public void ifFoundBumpAdded(List<String> toFind) {
			ifFoundBumpAdded = Objects.requireNonNull(toFind);
		}

		@Override
		protected String nextVersion(String unreleasedChanges, String lastVersion) {
			Version last = Version.parseVersion(lastVersion);
			if (last.getMajor() == 0) {
				boolean bumpMinor = ifFoundBumpBreaking.stream().anyMatch(unreleasedChanges::contains) ||
						ifFoundBumpAdded.stream().anyMatch(unreleasedChanges::contains);
				if (bumpMinor) {
					return "0." + (last.getMinor() + 1) + ".0";
				} else {
					return "0." + last.getMinor() + "." + (last.getMicro() + 1);
				}
			} else {
				boolean bumpMajor = ifFoundBumpBreaking.stream().anyMatch(unreleasedChanges::contains);
				if (bumpMajor) {
					return (last.getMajor() + 1) + ".0.0";
				}
				boolean bumpMinor = ifFoundBumpAdded.stream().anyMatch(unreleasedChanges::contains);
				if (bumpMinor) {
					return last.getMajor() + "." + (last.getMinor() + 1) + ".0";
				}
				return last.getMajor() + "." + last.getMinor() + "." + (last.getMicro() + 1);
			}
		}
	}

	/**
	 * Modifies {@link Semver} to suport `brand.breaking.added.fixed`.  Does not have an
	 * automatic method of bumping the `brand` number, it is assumed that those will be handled
	 * using `forceNextVersion`, and then this can maintain semver from there.
	 */
	public static class SemverBrandPrefix extends Semver {
		protected int brand = -1;

		@Override
		protected String nextVersion(String unreleasedChanges, String lastVersion) {
			int brandDot = lastVersion.indexOf('.');
			brand = Integer.parseInt(lastVersion.substring(0, brandDot));
			String result = super.nextVersion(unreleasedChanges, lastVersion.substring(brandDot + 1));
			return brand + "." + result;
		}
	}

	/**
	 * Modifies {@link Semver} to turn `2.1.0` into `2.1`.
	 */
	public static class SemverCondense__X_Y_0__to__X_Y extends Semver {
		protected int brand = -1;

		@Override
		protected String nextVersion(String unreleasedChanges, String lastVersion) {
			String semver = super.nextVersion(unreleasedChanges, lastVersion);
			if (semver.endsWith(".0")) {
				return semver.substring(0, semver.length() - 2);
			} else {
				return semver;
			}
		}
	}
}
