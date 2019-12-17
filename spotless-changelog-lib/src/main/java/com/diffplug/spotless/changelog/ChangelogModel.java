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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.osgi.framework.Version;
import pl.tlinkowski.annotation.basic.NullOr;

public class ChangelogModel {
	public static final String DEFAULT_FILE = "CHANGELOG.md";
	public static final String COMMIT_MESSAGE_VERSION = "{version}";
	public static final String DONT_PARSE_BELOW_HERE = "<!-- dont parse below here -->";
	public static final String FIRST_VERSION = "0.1.0";

	public static class NextVersionCfg implements Serializable {
		public List<String> ifFoundBumpMinor = Arrays.asList("### Added");
		public List<String> ifFoundBumpMajor = Arrays.asList("**BREAKING**");
		public @NullOr String forceNextVersion = null;
	}

	public static class PushCfg {
		public String tagPrefix = "release/";
		public String commitMessage = "Published release/" + COMMIT_MESSAGE_VERSION;
		public String remote = "origin";
		public String branch = "master";

		public GitApi withChangelog(File changelogFile, ChangelogModel model) throws IOException {
			return new GitApi(changelogFile, model, this);
		}
	}

	public static String validateCommitMessage(String commitMessage) {
		if (!commitMessage.contains(COMMIT_MESSAGE_VERSION)) {
			throw new IllegalArgumentException("The commit message must contain '" + COMMIT_MESSAGE_VERSION + "' to be replaced with the real version.");
		}
		return commitMessage;
	}

	public static ChangelogModel calculate(File changelogFile, NextVersionCfg cfg) throws IOException {
		if (!(changelogFile.exists() && changelogFile.isFile())) {
			throw new IllegalArgumentException("Looked for changelog at '" + changelogFile.getAbsolutePath() + "', but it was not present.");
		}
		String content = new String(Files.readAllBytes(changelogFile.toPath()), StandardCharsets.UTF_8);
		return calculate(content, cfg);
	}

	static ChangelogModel calculate(String content, NextVersionCfg cfg) {
		ParsedChangelog parsed = new ParsedChangelog(content);

		String nextVersion;
		if (cfg.forceNextVersion != null) {
			nextVersion = cfg.forceNextVersion;
		} else if (parsed.versionLast() == null) {
			nextVersion = FIRST_VERSION;
		} else if (parsed.noUnreleasedChanges()) {
			// we bumped, but don't have any new changes, so the next version is still "this" version
			nextVersion = parsed.versionLast();
		} else {
			nextVersion = nextVersion(parsed.unreleasedChanges(), Version.parseVersion(parsed.versionLast()), cfg).toString();
		}
		return new ChangelogModel(parsed, nextVersion);
	}

	private static Version nextVersion(String unreleasedChanges, Version last, NextVersionCfg cfg) {
		if (last.getMajor() == 0) {
			boolean bumpMinor = cfg.ifFoundBumpMajor.stream().anyMatch(unreleasedChanges::contains) ||
					cfg.ifFoundBumpMinor.stream().anyMatch(unreleasedChanges::contains);
			if (bumpMinor) {
				return new Version(0, last.getMinor() + 1, 0);
			} else {
				return new Version(0, last.getMinor(), last.getMicro() + 1);
			}
		} else {
			boolean bumpMajor = cfg.ifFoundBumpMajor.stream().anyMatch(unreleasedChanges::contains);
			if (bumpMajor) {
				return new Version(last.getMajor() + 1, 0, 0);
			}
			boolean bumpMinor = cfg.ifFoundBumpMinor.stream().anyMatch(unreleasedChanges::contains);
			if (bumpMinor) {
				return new Version(last.getMajor(), last.getMinor() + 1, 0);
			}
			return new Version(last.getMajor(), last.getMinor(), last.getMicro() + 1);
		}
	}

	private final ParsedChangelog parsed;
	private final String nextVersion;

	private ChangelogModel(ParsedChangelog parsed, String nextVersion) {
		this.parsed = parsed;
		this.nextVersion = nextVersion;
	}

	public String versionNext() {
		return nextVersion;
	}

	public ParsedChangelog parsed() {
		return parsed;
	}
}
