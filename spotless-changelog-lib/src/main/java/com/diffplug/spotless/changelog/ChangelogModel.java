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
import com.diffplug.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import pl.tlinkowski.annotation.basic.NullOr;

/**
 * {@link #calculate(File, CfgNextVersion)} will
 * return a `ChangelogModel` which contains the parsed changelog ({@link #changelog() changelog()} and {@link #versions() versions()} (which
 * in turn has {@link Versions#last() last()} and {@link Versions#next() next()}.
 * 
 * You can speed this calculation up using {@link #calculateUsingCache(File, CfgNextVersion)}.
 */
public class ChangelogModel {
	public static final String DEFAULT_FILE = "CHANGELOG.md";
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

	/** The input to the next-version calculation. */
	static class Input implements Serializable {
		FileSignature changelogFile;
		@NullOr
		CfgNextVersion cfgNextVersion;
	}

	/** Computes a ChangelogModel from the given changelogFile. */
	public static ChangelogModel calculateUsingCache(File changelogFile, CfgNextVersion cfg) throws IOException {
		assertChangelogFileExists(changelogFile);

		Input input = new Input();
		input.changelogFile = FileSignature.sign(changelogFile);
		input.cfgNextVersion = cfg;
		Serialized<Input> inputActual = Serialized.fromValue(input);

		Versions cachedVersions = cacheRead(inputActual);
		if (cachedVersions == null) {
			ChangelogModel model = calculate(changelogFile, cfg);
			cacheStore(inputActual, model.versions());
			return model;
		} else {
			return new ChangelogModel(Suppliers.memoize(Errors.rethrow().wrap(() -> {
				return new ParsedChangelog(new String(Files.readAllBytes(changelogFile.toPath()), StandardCharsets.UTF_8));
			})), cachedVersions);
		}
	}

	private static Versions cacheRead(Serialized<Input> inputActual) {
		synchronized (cache) {
			Entry<Serialized<Input>, Versions> cached = cache.get(inputActual.value().changelogFile.canonicalPath());
			if (cached != null && cached.getKey().equals(inputActual)) {
				return cached.getValue();
			} else {
				return null;
			}
		}
	}

	private static void cacheStore(Serialized<Input> inputActual, Versions versions) {
		synchronized (cache) {
			cache.put(inputActual.value().changelogFile.canonicalPath(), Maps.immutableEntry(inputActual, versions));
		}
	}

	static final Map<String, Entry<Serialized<Input>, Versions>> cache = new HashMap<>();
}
