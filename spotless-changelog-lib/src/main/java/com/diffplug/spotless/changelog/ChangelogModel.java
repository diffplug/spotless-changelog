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


import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Suppliers;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Supplier;
import pl.tlinkowski.annotation.basic.NullOr;

/** Models the Changelog and its computed next version. */
public class ChangelogModel {
	public static final String DEFAULT_FILE = "CHANGELOG.md";
	public static final String COMMIT_MESSAGE_VERSION = "{{version}}";
	public static final String DONT_PARSE_BELOW_HERE = "<!-- dont parse below here -->";
	public static final String FIRST_VERSION = "0.1.0";

	/** Computes a ChangelogModel from the given changelogFile. */
	public static ChangelogModel calculate(File changelogFile, CfgNextVersion cfg) throws IOException {
		assertChangelogFileExists(changelogFile);
		String content = new String(Files.readAllBytes(changelogFile.toPath()), StandardCharsets.UTF_8);
		return calculate(content, cfg);
	}

	private static void assertChangelogFileExists(File changelogFile) {
		if (!(changelogFile.exists() && changelogFile.isFile())) {
			throw new IllegalArgumentException("Looked for changelog at '" + changelogFile.getAbsolutePath() + "', but it was not present.");
		}
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
		return new ChangelogModel(() -> changelog, new Versions(nextVersion, changelog));
	}

	/** Internally lazy to facilitate easy caching of the versions, without having to cache the whole changelog. */
	private final Supplier<ParsedChangelog> changelog;
	private final Versions versions;

	private ChangelogModel(Supplier<ParsedChangelog> changelog, Versions versions) {
		this.changelog = changelog;
		this.versions = versions;
	}

	public ParsedChangelog changelog() {
		return changelog.get();
	}

	public Versions versions() {
		return versions;
	}

	/** The next and previously published versions. */
	public static class Versions implements Serializable {
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
	}

	/** Computes a ChangelogModel from the given changelogFile. */
	public static ChangelogModel calculateCacheable(File changelogFile, CfgNextVersion cfg, File cacheInput, File cacheOutput) throws IOException, ClassNotFoundException {
		assertChangelogFileExists(changelogFile);

		Input input = new Input();
		input.changelogFile = FileSignature.signAsList(changelogFile);
		input.cfgNextVersion = cfg;

		Serialized<Input> inputActual = Serialized.fromValue(input);
		Serialized<Input> inputCached = Serialized.fromFile(cacheInput, Input.class);
		Serialized<Versions> outputCached = Serialized.fromFile(cacheOutput, Versions.class);

		if (inputActual.equals(inputCached) && outputCached.value() != null) {
			// if the cache is successful, then we can use the cached versions, and in the unlikely event that they want the changelog, we'll parse that on demand 
			return new ChangelogModel(Suppliers.memoize(Errors.rethrow().wrap(() -> {
				return new ParsedChangelog(new String(Files.readAllBytes(changelogFile.toPath()), StandardCharsets.UTF_8));
			})), outputCached.value());
		} else {
			ChangelogModel model = calculate(changelogFile, cfg);
			inputActual.writeTo(cacheInput);
			Serialized<Versions> outputActual = Serialized.fromValue(model.versions());
			if (!outputActual.equals(outputCached)) {
				outputActual.writeTo(cacheOutput);
			}
			return model;
		}
	}

	/** The input to the next-version calculation. */
	static class Input implements Serializable {
		FileSignature changelogFile;
		@NullOr
		CfgNextVersion cfgNextVersion;
	}
}
