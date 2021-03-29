/*
 * Copyright (C) 2019-2021 DiffPlug
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
package com.diffplug.spotless.changelog.gradle;


import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.spotless.changelog.Changelog;
import com.diffplug.spotless.changelog.ChangelogAndNext;
import com.diffplug.spotless.changelog.GitCfg;
import com.diffplug.spotless.changelog.NextVersionCfg;
import com.diffplug.spotless.changelog.NextVersionFunction;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.gradle.api.Project;

/** Plugin DSL. */
public class ChangelogExtension {
	static final String NAME = "spotlessChangelog";

	private final Project project;

	File changelogFile;
	NextVersionCfg nextVersionCfg;
	GitCfg gitCfg;
	boolean enforceCheck;

	public ChangelogExtension(Project project) {
		this.project = Objects.requireNonNull(project);
		this.changelogFile = project.file(ChangelogAndNext.DEFAULT_FILE);
		this.nextVersionCfg = new NextVersionCfg();
		this.gitCfg = new GitCfg();
		changelogFile(ChangelogAndNext.DEFAULT_FILE);
	}

	private volatile ChangelogAndNext model;

	/**
	 * Parses the changelog and calculates the next version.  Once this
	 * has been done, the user can't change the configuration at all.
	 * Use {@link #assertNotCalculatedYet()} on every mutation to check for this.
	 */
	ChangelogAndNext model() {
		if (model == null) {
			synchronized (this) {
				if (model == null) {
					try {
						model = ChangelogAndNext.calculateUsingCache(changelogFile, nextVersionCfg);
					} catch (IOException e) {
						throw Errors.asRuntime(e);
					}
				}
			}
		}
		return model;
	}

	/** Ensures that we haven't locked the next version calculation already. */
	private synchronized void assertNotCalculatedYet() {
		Preconditions.checkState(model == null,
				"You have to configure the `spotlessChangelog` block before calling `versionNext`, `versionLast`, or `parsedChangelog`.\n" +
						"Try moving `spotlessChangelog` higher in your buildscript, and make sure you don't change it after calling `versionNext`, `versionLast`, or `parsedChangelog`.");
	}

	/** Reads the last-published version - you can't change the configuration after calling this method. */
	public String getVersionLast() {
		return model().changelog().versionLast();
	}

	/** Calculates the next-to-publish version - you can't change the configuration after calling this method. */
	public String getVersionNext() {
		return model().versions().next();
	}

	/**
	 * Returns the fully-parsed changelog.  You probably won't need this method unless you're
	 * building on top of this plugin, perhaps to make a new plugin that creates GitHub releases
	 * for old tags or something like that.  If you do anything interesting with it, send us a PR so we can
	 * link back to you from here.
	 */
	public Changelog getParsedChangelog() {
		return model().changelog();
	}

	/** Sets the changelog file using {@link Project#file(Object)}. */
	public void changelogFile(Object file) {
		assertNotCalculatedYet();
		changelogFile = project.file(file);
	}

	/** Determines whether `changelogCheck` will be a dependency of `check`.  Default is true. */
	public void enforceCheck(boolean enforceCheck) {
		this.enforceCheck = enforceCheck;
	}

	/**
	 * Sets a custom {@link NextVersionFunction} by calling the public no-arg constructor of the given class.
	 * Default value is {@link com.diffplug.spotless.changelog.NextVersionFunction.Semver Semver}.
	 * See [ALTERNATE_VERSION_SCHEMAS.md](https://github.com/diffplug/spotless-changelog/blob/main/ALTERNATE_VERSION_SCHEMAS.md) for more info.
	 */
	public void versionSchema(Class<? extends NextVersionFunction> functionClass) throws InstantiationException, IllegalAccessException {
		assertNotCalculatedYet();
		nextVersionCfg.function = functionClass.newInstance();
	}

	/**
	 * If any of these strings are found in the `## [Unreleased]` section, then the
	 * next version will bump the `added` place in `breaking.added.fixed` (unless
	 * overruled by `ifFoundBumpBreaking`).
	 *
	 * Default value is `['### Added']`
	 */
	public void ifFoundBumpAdded(List<String> toFind) {
		assertNotCalculatedYet();
		nextVersionCfg.function.ifFoundBumpAdded(toFind);
	}

	/** @see #ifFoundBumpAdded(List) */
	public void ifFoundBumpAdded(String... toFind) {
		ifFoundBumpAdded(Arrays.asList(toFind));
	}

	/**
	 * If any of these strings are found in the `## [Unreleased]` section, then the
	 * next version will bump the `breaking` place in `breaking.added.fixed`.
	 *
	 * Default value is `['**BREAKING**']`.
	 */
	public void ifFoundBumpBreaking(List<String> toFind) {
		assertNotCalculatedYet();
		nextVersionCfg.function.ifFoundBumpBreaking(toFind);
	}

	/** @see #ifFoundBumpBreaking(List) */
	public void ifFoundBumpBreaking(String... toFind) {
		ifFoundBumpBreaking(Arrays.asList(toFind));
	}

	/** Short-circuits the next-version calculation and just uses this string. */
	public void forceNextVersion(String forceNextVersion) {
		assertNotCalculatedYet();
		nextVersionCfg.forceNextVersion = forceNextVersion;
	}

	/**
	 * If you set this to true, then the calculated version will always have `-SNAPSHOT`
	 * appended to the end, unless you add `-Prelease=true` to the gradle command line.
	 * Essentially, it asks like a gun safety where all versions are nerfed to `-SNAPSHOT`,
	 * until you allow a release by adding `-Prelease`.
	 *
	 * Enabling this mode should look like this in your buildscript: `appendDashSnapshotUnless_dashPrelease=true`
	 */
	public void setAppendDashSnapshotUnless_dashPrelease(boolean appendSnapshot) {
		if (appendSnapshot && !"true".equals(project.getRootProject().findProperty("release"))) {
			nextVersionCfg.appendSnapshot = true;
		}
	}

	/**
	 * If you set this to true, then the ssh host key checking over ssh:// remotes will be disabled.
	 * By default strict host key checking is enabled. Make sure that there is an entry
	 * in know_hosts file for given ssh remote.
	 * You can also add `-PsshDisableStrictHostKeyChecking` to the gradle command.
	 * In your buildscript you can disable checking with `sshDisableStrictHostKeyChecking = true`
	 */
	public void setSshDisableStrictHostKeyChecking(boolean disableStrictHostKeyChecking) {
		gitCfg.disableStrictHostKeyChecking = disableStrictHostKeyChecking;
	}

	// tag and push
	/** Default value is `release/` */
	public void tagPrefix(String tagPrefix) {
		gitCfg.tagPrefix = tagPrefix;
	}

	/** Default value is `Published release/{{version}}` - the {{version}} will be replaced. */
	public void commitMessage(String commitMessage) {
		gitCfg.commitMessage = GitCfg.validateCommitMessage(commitMessage);
	}

	/** Default value is 'origin' */
	public void remote(String remote) {
		gitCfg.remote = remote;
	}

	/** Default value is 'main' */
	public void branch(String branch) {
		gitCfg.branch = branch;
	}
}
