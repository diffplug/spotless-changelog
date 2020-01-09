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
import org.osgi.framework.Version;

/** Function which defines the logic for implementing a version schema. */
public abstract class VersionBumpFunction implements Serializable {
	public abstract String nextVersion(String unreleasedChanges, String lastVersion);

	public abstract void ifFoundBumpAdded(List<String> toFind);

	public abstract void ifFoundBumpBreaking(List<String> toFind);

	public final void ifFoundBumpBreaking(String... toFind) {
		ifFoundBumpBreaking(Arrays.asList(toFind));
	}

	public final void ifFoundBumpAdded(String... toFind) {
		ifFoundBumpAdded(Arrays.asList(toFind));
	}

	/**
	 * Base class for {@link VersionBumpFunction} for the unusual case that 
	 * {@link #ifFoundBumpAdded(List)} and {@link #ifFoundBumpBreaking(List)} are not supported.
	 */
	public static abstract class NonSemver extends VersionBumpFunction {
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
	public static class Semver extends VersionBumpFunction {
		protected List<String> ifFoundBumpBreaking = Arrays.asList("**BREAKING**");
		protected List<String> ifFoundBumpAdded = Arrays.asList("### Added");

		@Override
		public void ifFoundBumpBreaking(List<String> toFind) {
			ifFoundBumpBreaking = toFind;
		}

		@Override
		public void ifFoundBumpAdded(List<String> toFind) {
			ifFoundBumpAdded = toFind;
		}

		@Override
		public String nextVersion(String unreleasedChanges, String lastVersion) {
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
	public static class BrandPrefix extends Semver {
		protected int brand = -1;

		@Override
		public String nextVersion(String unreleasedChanges, String lastVersion) {
			int brandDot = lastVersion.indexOf('.');
			brand = Integer.parseInt(lastVersion.substring(0, brandDot));
			String result = super.nextVersion(unreleasedChanges, lastVersion.substring(brandDot + 1));
			return brand + "." + result;
		}
	}
}