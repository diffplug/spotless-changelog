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
import com.diffplug.spotless.changelog.CfgNextVersion;
import com.diffplug.spotless.changelog.CfgPush;
import com.diffplug.spotless.changelog.ChangelogModel;
import com.diffplug.spotless.changelog.VersionBumpFunction;
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
	CfgNextVersion nextVersionCfg;
	CfgPush pushCfg;
	boolean enforceCheck;

	public ChangelogExtension(Project project) {
		this.project = Objects.requireNonNull(project);
		this.changelogFile = project.file(ChangelogModel.DEFAULT_FILE);
		this.nextVersionCfg = new CfgNextVersion();
		this.pushCfg = new CfgPush();
		changelogFile(ChangelogModel.DEFAULT_FILE);
	}

	private volatile ChangelogModel model;

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

	private synchronized void assertNotCalculatedYet() {
		Preconditions.checkState(model == null, "You can't change the config after calling versionNext or versionLast");
	}

	/** Reads the last-published version - you can't change the configuration after calling this method. */
	public String getVersionLast() {
		return model().changelog().versionLast();
	}

	/** Calculates the next-to-publish version - you can't change the configuration after calling this method. */
	public String getVersionNext() {
		return model().versions().next();
	}

	/** Sets the changelog file using {@link Project#file(Object)}. */
	public void changelogFile(Object file) {
		assertNotCalculatedYet();
		changelogFile = project.file(file);
	}

	/** Determines whether `changelogCheck` will be a dependency of `check`. */
	public void enforceCheck(boolean enforceCheck) {
		this.enforceCheck = enforceCheck;
	}

	/** Sets a custom version bump function.  The default value is {@link com.diffplug.spotless.changelog.VersionBumpFunction.Semver}. */
	public void setVersionBumpFunction(VersionBumpFunction next) {
		assertNotCalculatedYet();
		nextVersionCfg.next = next;
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
		nextVersionCfg.next.ifFoundBumpAdded(toFind);
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
		nextVersionCfg.next.ifFoundBumpBreaking(toFind);
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
	public void tagPrefix(String tagPrefix) {
		pushCfg.tagPrefix = tagPrefix;
	}

	public void commitMessage(String commitMessage) {
		pushCfg.commitMessage = CfgPush.validateCommitMessage(commitMessage);
	}

	public void remote(String remote) {
		pushCfg.remote = remote;
	}

	public void branch(String branch) {
		pushCfg.branch = branch;
	}
}
