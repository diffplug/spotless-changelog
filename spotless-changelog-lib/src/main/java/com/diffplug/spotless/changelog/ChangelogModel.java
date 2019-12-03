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
package com.diffplug.spotless.changelog;


import java.io.File;
import java.util.Arrays;
import java.util.List;
import pl.tlinkowski.annotation.basic.NullOr;

public class ChangelogModel {
	public static final String DEFAULT_FILE = "CHANGELOG.md";
	public static final String COMMIT_MESSAGE_VERSION = "{version}";

	// keep changelog formatted
	public File changelogFile = new File(DEFAULT_FILE);
	public List<String> types = Arrays.asList("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security");
	public boolean enforceCheck = true;
	// calculate next version
	public List<String> typesBumpMinor = Arrays.asList("Added");
	public List<String> typesBumpMajor = Arrays.asList("Changed", "Removed");
	public List<String> ifFoundBumpMajor = Arrays.asList("**BREAKING**");
	public @NullOr String forceNextVersion = null;
	// tag and push
	public String tagPrefix = "release/";
	public String commitMessage = "Published release/" + COMMIT_MESSAGE_VERSION;
	public String remote = "origin";
	public String branch = "master";

	public static String validateCommitMessage(String commitMessage) {
		if (!commitMessage.contains(COMMIT_MESSAGE_VERSION)) {
			throw new IllegalArgumentException("The commit message must contain '{version}' to be replaced with the real version.");
		}
		return commitMessage;
	}
}
