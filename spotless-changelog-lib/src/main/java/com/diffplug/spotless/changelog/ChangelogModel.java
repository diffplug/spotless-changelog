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
	public static final String COMMIT_MESSAGE_VERSION = "{{version}}";
	public static final String DONT_PARSE_BELOW_HERE = "<!-- dont parse below here -->";
	public static final String FIRST_VERSION = "0.1.0";

	/** Computes a ChangelogModel from the given changelogFile. */
	public static ChangelogModel calculate(File changelogFile, CfgNextVersion cfg) throws IOException {
		if (!(changelogFile.exists() && changelogFile.isFile())) {
			throw new IllegalArgumentException("Looked for changelog at '" + changelogFile.getAbsolutePath() + "', but it was not present.");
		}
		String content = new String(Files.readAllBytes(changelogFile.toPath()), StandardCharsets.UTF_8);
		return calculate(content, cfg);
	}

	static ChangelogModel calculate(String content, CfgNextVersion cfg) {
		ParsedChangelog changelog = new ParsedChangelog(content);

		String nextVersion;
		if (cfg.forceNextVersion != null) {
			nextVersion = cfg.forceNextVersion;
		} else if (changelog.versionLast() == null) {
			nextVersion = FIRST_VERSION;
		} else if (changelog.noUnreleasedChanges()) {
			// we bumped, but don't have any new changes, so the next version is still "this" version
			nextVersion = changelog.versionLast();
		} else {
			nextVersion = cfg.next.nextVersion(changelog.unreleasedChanges(), changelog.versionLast());
		}
		return new ChangelogModel(changelog, nextVersion);
	}

	private final ParsedChangelog changelog;
	private final Versions versions;

	private ChangelogModel(ParsedChangelog changelog, String nextVersion) {
		this.changelog = changelog;
		this.versions = new Versions(nextVersion, changelog);
	}

	public ParsedChangelog changelog() {
		return changelog;
	}

	public Versions versions() {
		return versions;
	}

	/** The next and previously published versions. */
	public static class Versions implements java.io.Serializable {
		private final String next, last;

		private Versions(String next, ParsedChangelog changelog) {
			this.next = next;
			this.last = changelog.versionLast();
		}

		public String next() {
			return next;
		}

		public String last() {
			return last;
		}

		@Override
		public boolean equals(Object other) {
			if (other == this) {
				return true;
			} else if (other instanceof Versions) {
				Versions o = (Versions) other;
				return next.equals(o.next) && last.equals(o.last);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return 31 * next.hashCode() + last.hashCode();
		}
	}
}
