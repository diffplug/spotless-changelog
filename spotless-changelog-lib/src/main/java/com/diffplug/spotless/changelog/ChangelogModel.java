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
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import pl.tlinkowski.annotation.basic.NullOr;

public class ChangelogModel {
	public static final String DEFAULT_FILE = "CHANGELOG.md";
	public static final String COMMIT_MESSAGE_VERSION = "{version}";
	public static final String UNRELEASED = "## [Unreleased]";
	public static final String DONT_PARSE_BELOW_HERE = "<!-- dont parse below here -->";

	public static class NextVersionCfg implements Serializable {
		public List<String> types = Arrays.asList("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security");
		public List<String> typesBumpMinor = Arrays.asList("Added");
		public List<String> typesBumpMajor = Arrays.asList("Changed", "Removed");
		public List<String> ifFoundBumpMajor = Arrays.asList("**BREAKING**");
		public @NullOr String forceNextVersion = null;
	}

	// tag and push
	public static class PushCfg {
		public String tagPrefix = "release/";
		public String commitMessage = "Published release/" + COMMIT_MESSAGE_VERSION;
		public String remote = "origin";
		public String branch = "master";
	}

	public static String validateCommitMessage(String commitMessage) {
		if (!commitMessage.contains(COMMIT_MESSAGE_VERSION)) {
			throw new IllegalArgumentException("The commit message must contain '{version}' to be replaced with the real version.");
		}
		return commitMessage;
	}

	public static class VersionResult {
		String lastPublished;
		String next;
	}

	public static VersionResult calculate(File changelogFile, NextVersionCfg cfg) throws IOException {
		if (!(changelogFile.exists() && changelogFile.isFile())) {
			throw new IllegalArgumentException("Looked for changelog at '" + changelogFile.getAbsolutePath() + "', but it was not present.");
		}
		return null;
	}
}
