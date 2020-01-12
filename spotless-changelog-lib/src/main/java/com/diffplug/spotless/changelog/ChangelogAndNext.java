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
 * {@link #calculate(File, NextVersionCfg)} will
 * return a `ChangelogAndNext` which contains the parsed ({@link #changelog() changelog()} and {@link #versions() versions()} (which
 * in turn has {@link Versions#last() last()} and {@link Versions#next() next()}.
 * 
 * You can speed this calculation up using {@link #calculateUsingCache(File, NextVersionCfg)}.
 */
public class ChangelogAndNext {
	public static final String DEFAULT_FILE = "CHANGELOG.md";
	public static final String FIRST_VERSION = "0.1.0";
	private static final String DASH_SNAPSHOT = "-SNAPSHOT";

	/** Computes a ChangelogModel from the given changelogFile. */
	public static ChangelogAndNext calculate(File changelogFile, NextVersionCfg cfg) throws IOException {
		assertChangelogFileExists(changelogFile);
		String content = new String(Files.readAllBytes(changelogFile.toPath()), StandardCharsets.UTF_8);
		return calculate(content, cfg);
	}

	private static void assertChangelogFileExists(File changelogFile) {
		if (!(changelogFile.exists() && changelogFile.isFile())) {
			throw new IllegalArgumentException("Looked for changelog at '" + changelogFile.getAbsolutePath() + "', but it was not present.");
		}
	}

	static ChangelogAndNext calculate(String content, NextVersionCfg cfg) {
		Changelog changelog = new Changelog(content);

		String nextVersion;
		if (cfg.forceNextVersion != null) {
			nextVersion = cfg.forceNextVersion;
		} else if (changelog.versionLast() == null) {
			nextVersion = FIRST_VERSION;
		} else if (changelog.noUnreleasedChanges()) {
			// we bumped, but don't have any new changes, so the next version is still "this" version
			nextVersion = changelog.versionLast();
		} else {
			nextVersion = cfg.function.nextVersion(changelog);
		}
		if (cfg.appendSnapshot) {
			if (nextVersion.endsWith(DASH_SNAPSHOT)) {
				throw new RuntimeException("Can't append -SNAPSHOT to " + nextVersion + " because it's already there!");
			} else {
				nextVersion = nextVersion + DASH_SNAPSHOT;
			}
		}
		return new ChangelogAndNext(() -> changelog, new Versions(nextVersion, changelog));
	}

	/** Internally lazy to facilitate easy caching of the versions, without having to cache the whole changelog. */
	private final Supplier<Changelog> changelog;
	private final Versions versions;

	private ChangelogAndNext(Supplier<Changelog> changelog, Versions versions) {
		this.changelog = changelog;
		this.versions = versions;
	}

	public Changelog changelog() {
		return changelog.get();
	}

	public Versions versions() {
		return versions;
	}

	/** The next and previously published versions. */
	public static class Versions implements Serializable {
		private final String next, last;

		private Versions(String next, Changelog changelog) {
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
		NextVersionCfg cfgNextVersion;
	}

	/**
	 * Exact same behavior as {@link #calculate(File, NextVersionCfg) calculate()}, but it uses an in-memory
	 * per-changelogfile cache to optimize performance and delay parsing the changelog if possible.  Only
	 * downside is that you will leak a teensy-teensy bit of memory since there's no way to clear the cache.
	 * 
	 * It doesn't cache the parsed changelog, only the versions, since those are usually all the user needs.
	 * The changelog is parsed lazily when it is asked for. 
	 */
	public static ChangelogAndNext calculateUsingCache(File changelogFile, NextVersionCfg cfg) throws IOException {
		assertChangelogFileExists(changelogFile);

		Input input = new Input();
		input.changelogFile = FileSignature.sign(changelogFile);
		input.cfgNextVersion = cfg;
		Serialized<Input> inputActual = Serialized.fromValue(input);

		Versions cachedVersions = cacheRead(inputActual);
		if (cachedVersions == null) {
			ChangelogAndNext model = calculate(changelogFile, cfg);
			cacheStore(inputActual, model.versions());
			return model;
		} else {
			return new ChangelogAndNext(Suppliers.memoize(Errors.rethrow().wrap(() -> {
				return new Changelog(new String(Files.readAllBytes(changelogFile.toPath()), StandardCharsets.UTF_8));
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
