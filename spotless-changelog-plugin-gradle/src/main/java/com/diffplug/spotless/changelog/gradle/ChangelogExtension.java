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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.gradle.api.Project;

/** Plugin DSL. */
public class ChangelogExtension {
	static final String NAME = "spotlessChangelog";
	private final Project project;
	private final ChangelogModel model;

	public ChangelogExtension(Project project) {
		this.project = Objects.requireNonNull(project);
		model = new ChangelogModel();
		changelogFile(ChangelogModel.DEFAULT_FILE);
	}

	// keep changelog formatted
	public void changelogFile(Object file) {
		model.changelogFile = project.file(ChangelogModel.DEFAULT_FILE);
	}

	public void types(String... types) {
		types(Arrays.asList(types));
	}

	public void types(List<String> types) {
		model.types = types;
	}

	public void enforceCheck(boolean enforceCheck) {
		model.enforceCheck = enforceCheck;
	}

	// calculate next version
	public void typesBumpMinor(String... types) {
		typesBumpMinor(Arrays.asList(types));
	}

	public void typesBumpMinor(List<String> types) {
		model.typesBumpMinor = types;
	}

	public void typesBumpMajor(String... types) {
		typesBumpMajor(Arrays.asList(types));
	}

	public void typesBumpMajor(List<String> types) {
		model.typesBumpMajor = types;
	}

	public void ifFoundBumpMajor(String... types) {
		ifFoundBumpMajor(Arrays.asList(types));
	}

	public void ifFoundBumpMajor(List<String> types) {
		model.ifFoundBumpMajor = types;
	}

	public void forceNextVersion(String forceNextVersion) {
		model.forceNextVersion = forceNextVersion;
	}

	// tag and push
	public void tagPrefix(String tagPrefix) {
		model.tagPrefix = tagPrefix;
	}

	public void commitMessage(String commitMessage) {
		model.commitMessage = ChangelogModel.validateCommitMessage(commitMessage);
	}

	public void remote(String remote) {
		model.remote = remote;
	}

	public void branch(String branch) {
		model.branch = branch;
	}
}
