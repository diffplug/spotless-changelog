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


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Models the Changelog and its computed next version. */
public class ChangelogModel {
	public static final String DEFAULT_FILE = "CHANGELOG.md";
	public static final String COMMIT_MESSAGE_VERSION = "{version}";
	public static final String DONT_PARSE_BELOW_HERE = "<!-- dont parse below here -->";
	public static final String FIRST_VERSION = "0.1.0";

	/** Computes a ChangelogModel from the given changelogFile. */
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
			nextVersion = cfg.next.nextVersion(parsed.unreleasedChanges(), parsed.versionLast());
		}
		return new ChangelogModel(parsed, nextVersion);
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
