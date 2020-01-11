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
package com.diffplug.spotless.changelog.gradle;


import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.spotless.changelog.ChangelogModel;
import com.diffplug.spotless.changelog.GitCfg;
import com.diffplug.spotless.changelog.Misc;
import com.diffplug.spotless.changelog.NextVersionCfg;
import com.diffplug.spotless.changelog.NextVersionFunction;
import com.diffplug.spotless.changelog.ParsedChangelog;
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
	GitCfg pushCfg;
	boolean enforceCheck;

	public ChangelogExtension(Project project) {
		this.project = Objects.requireNonNull(project);
		this.changelogFile = project.file(ChangelogModel.DEFAULT_FILE);
		this.nextVersionCfg = new NextVersionCfg();
		this.pushCfg = new GitCfg();
		changelogFile(ChangelogModel.DEFAULT_FILE);
	}

	private volatile ChangelogModel model;

	/**
	 * Parses the changelog and calculates the next version.  Once this
	 * has been done, the user can't change the configuration at all.
	 * Use {@link #assertNotCalculatedYet()} on every mutation to check for this. 
	 */
	ChangelogModel model() {
		if (model == null) {
			synchronized (this) {
				if (model == null) {
					try {
						model = ChangelogModel.calculateUsingCache(changelogFile, nextVersionCfg);
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
		Preconditions.checkState(model == null, "You can't change the next version calculation after calling `versionNext`, `versionLast`, or `parsedChangelog`.");
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
	public ParsedChangelog getParsedChangelog() {
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
	 * Sets a custom version bump function.  The default value is {@link com.diffplug.spotless.changelog.NextVersionFunction.Semver}.
	 * 
	 * The value that you pass in will be copied (using serialization) to ensure that it is not modified
	 * after calling {@link #getVersionLast() versionLast} or {@link #getVersionNext() versionNext}. So you
	 * can't change it after you have passed it, except by calling `ifFoundBumpXXX`, which will delegate
	 * to those methods on your function.
	 */
	public void setNextVersionFunction(NextVersionFunction next) throws ClassNotFoundException, IOException {
		assertNotCalculatedYet();
		nextVersionCfg.function = Misc.copy(next);
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

	// tag and push
	/** Default value is `release/` */
	public void tagPrefix(String tagPrefix) {
		pushCfg.tagPrefix = tagPrefix;
	}

	/** Default value is `Published release/{{version}}` - the {{version}} will be replaced. */
	public void commitMessage(String commitMessage) {
		pushCfg.commitMessage = GitCfg.validateCommitMessage(commitMessage);
	}

	/** Default value is 'origin' */
	public void remote(String remote) {
		pushCfg.remote = remote;
	}

	/** Default value is 'master' */
	public void branch(String branch) {
		pushCfg.branch = branch;
	}
}
