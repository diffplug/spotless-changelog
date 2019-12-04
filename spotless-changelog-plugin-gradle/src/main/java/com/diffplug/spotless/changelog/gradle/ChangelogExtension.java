/*
 * Copyright 2019 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.changelog.gradle;


import com.diffplug.spotless.changelog.ChangelogModel;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.gradle.api.Project;

/** Plugin DSL. */
public class ChangelogExtension {
	static final String NAME = "spotlessChangelog";

	private final Project project;

	File changelogFile;
	ChangelogModel.NextVersionCfg nextVersionCfg;
	ChangelogModel.PushCfg pushCfg;
	boolean enforceCheck;

	public ChangelogExtension(Project project) {
		this.project = Objects.requireNonNull(project);
		this.changelogFile = project.file(ChangelogModel.DEFAULT_FILE);
		this.nextVersionCfg = new ChangelogModel.NextVersionCfg();
		this.pushCfg = new ChangelogModel.PushCfg();
		changelogFile(ChangelogModel.DEFAULT_FILE);
	}

	// keep changelog formatted
	public void changelogFile(Object file) {
		changelogFile = project.file(file);
	}

	public void types(String... types) {
		types(Arrays.asList(types));
	}

	public void types(List<String> types) {
		nextVersionCfg.types = types;
	}

	public void enforceCheck(boolean enforceCheck) {
		this.enforceCheck = enforceCheck;
	}

	// calculate next version
	public void typesBumpMinor(String... types) {
		typesBumpMinor(Arrays.asList(types));
	}

	public void typesBumpMinor(List<String> types) {
		nextVersionCfg.typesBumpMinor = types;
	}

	public void typesBumpMajor(String... types) {
		typesBumpMajor(Arrays.asList(types));
	}

	public void typesBumpMajor(List<String> types) {
		nextVersionCfg.typesBumpMajor = types;
	}

	public void ifFoundBumpMajor(String... types) {
		ifFoundBumpMajor(Arrays.asList(types));
	}

	public void ifFoundBumpMajor(List<String> types) {
		nextVersionCfg.ifFoundBumpMajor = types;
	}

	public void forceNextVersion(String forceNextVersion) {
		nextVersionCfg.forceNextVersion = forceNextVersion;
	}

	// tag and push
	public void tagPrefix(String tagPrefix) {
		pushCfg.tagPrefix = tagPrefix;
	}

	public void commitMessage(String commitMessage) {
		pushCfg.commitMessage = ChangelogModel.validateCommitMessage(commitMessage);
	}

	public void remote(String remote) {
		pushCfg.remote = remote;
	}

	public void branch(String branch) {
		pushCfg.branch = branch;
	}
}
