/*
 * Copyright (C) 2019-2021 DiffPlug
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


import com.diffplug.common.base.Preconditions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import pl.tlinkowski.annotation.basic.NullOr;

/**
 * Partitions a string into its changelog headers and the release notes under each header.
 * 
 * Capable of round-tripping - if you change the strings inside ParsedChangelog, you can write out
 * a new changelog with new content, while preserving all formatting and non-changelog content in
 * the string. 
 */
public class Changelog {
	private static final String VERSION_BEGIN = "\n## [";
	private static final String UNRELEASED = VERSION_BEGIN + "Unreleased]";
	private static final String DONT_PARSE_BELOW_HERE = "\n<!-- END CHANGELOG -->";
	private final boolean windowsNewlines;
	private final PoolString dontParse, beforeUnreleased;
	private final List<VersionEntry> versionsRaw;
	private final @NullOr PoolString unparseableAfterError;
	private final LinkedHashMap<Integer, String> parseErrors = new LinkedHashMap<>();

	/** Takes a changelog string as its argument. */
	public Changelog(String contentRaw) {
		versionsRaw = new ArrayList<>();
		PoolString contentUnix = PoolString.of(contentRaw.replace("\r\n", "\n"));
		windowsNewlines = contentUnix.length() < contentRaw.length();

		PoolString toParse = contentUnix.until(DONT_PARSE_BELOW_HERE);
		dontParse = contentUnix.after(toParse);

		beforeUnreleased = toParse.until(UNRELEASED);
		toParse = toParse.after(beforeUnreleased);

		if (toParse.isEmpty()) {
			unparseableAfterError = null;
			if (!beforeUnreleased.endsWith(UNRELEASED)) {
				int almostHadIt = contentRaw.indexOf("## [Unreleased]");
				if (almostHadIt >= 0) {
					int lineNumber = PoolString.of(contentRaw, almostHadIt, almostHadIt + 1).baseLineNumberStart();
					addError(lineNumber, "Needs a newline directly before '## [Unreleased]'");
				} else {
					addError(-1, "Needs to have '## [Unreleased]'");
				}
			}
			return;
		}

		while (true) {
			int lastSlash = toParse.subSequence(1, toParse.length()).indexOf('\n');
			PoolString versionHeader = lastSlash == -1 ? toParse : toParse.subSequence(0, lastSlash + 1);
			VersionEntry version = VersionEntry.parse(versionHeader, this);
			if (version == null) {
				unparseableAfterError = toParse;
				return;
			}
			toParse = toParse.after(versionHeader);
			version.changes = toParse.until(VERSION_BEGIN);
			versionsRaw.add(version);
			toParse = toParse.after(version.changes);
			if (toParse.isEmpty()) {
				unparseableAfterError = null;
				return;
			}
		}
	}

	/** Copy-constructor. */
	private Changelog(boolean windowsNewlines,
			PoolString dontParse, PoolString beforeUnreleased,
			List<VersionEntry> versionsRaw,
			@NullOr PoolString unparseableAfterError) {
		this.windowsNewlines = windowsNewlines;
		this.dontParse = dontParse;
		this.beforeUnreleased = beforeUnreleased;
		this.versionsRaw = versionsRaw;
		this.unparseableAfterError = unparseableAfterError;
	}

	/** Returns the full content of this changelog as a string unix-newlines. */
	public String toStringUnix() {
		PoolString total = beforeUnreleased;
		for (VersionEntry entry : versionsRaw) {
			total = total.concat(entry.toStringUnix());
		}
		if (unparseableAfterError != null) {
			total = total.concat(unparseableAfterError);
		}
		total = total.concat(dontParse);
		return total.toString();
	}

	/** Returns the full content of this changelog as a string, with the same newlines as the input string. */
	@Override
	public String toString() {
		String unix = toStringUnix();
		return windowsNewlines ? unix.replace("\n", "\r\n") : unix;
	}

	/** Returns the most recently published version, if any. */
	public @NullOr String versionLast() {
		if (versionsRaw.size() <= 1) {
			return null;
		} else {
			return versionsRaw.get(1).version.toString();
		}
	}

	/** Returns the string describing unreleased changes - starts with a newline, and has unix newlines. */
	public String unreleasedChanges() {
		if (versionsRaw.isEmpty()) {
			return "";
		}
		return versionsRaw.get(0).changes.toString();
	}

	/** Returns true if there are no unreleased changes. */
	public boolean noUnreleasedChanges() {
		return unreleasedChanges().replace("\n", "").trim().isEmpty();
	}

	private void addError(int lineNumber, String message) {
		parseErrors.put(lineNumber, message);
	}

	/** Map from line number to the error message, in the order they were encountered. */
	public LinkedHashMap<Integer, String> errors() {
		return parseErrors;
	}

	/** Contains everything about a single entry in the changelog list. */
	public static class VersionEntry {
		/** Null signifies unreleased. */
		private @NullOr PoolString version;

		/** Null signifies unreleased. */
		private @NullOr PoolString date;

		/** Null signifies nothing after the date. For unreleased, it is always non-null. */
		private @NullOr PoolString headerMisc;

		/** The changes for this version. Guaranteed to be non-null once parsed. */
		private @NullOr PoolString changes;

		private VersionEntry() {}

		/** Creates a VersionHeader of the given version and date. */
		public static VersionEntry versionDate(String version, String date) {
			VersionEntry header = new VersionEntry();
			header.version = PoolString.of(version);
			header.date = PoolString.of(date);
			return header;
		}

		/** If unreleased, you can't ask for version or date. */
		public boolean isUnreleased() {
			return version == null;
		}

		/** The version (make sure it's not {@link #isUnreleased()}. */
		public CharSequence version() {
			Preconditions.checkArgument(!isUnreleased());
			return version;
		}

		/** The date (make sure it's not {@link #isUnreleased()}. */
		public CharSequence date() {
			Preconditions.checkArgument(!isUnreleased());
			return date;
		}

		/** Anything that appears after the `[Unreleased]` tag. */
		public @NullOr CharSequence headerMisc() {
			if (headerMisc == null) {
				return null;
			} else {
				if (isUnreleased()) {
					// unreleased behaves a little different
					return headerMisc.length() <= 1 ? null : headerMisc.subSequence(1, headerMisc.length());
				} else {
					return headerMisc;
				}
			}
		}

		/** Sets the changes to be used in this entry (must be start with a newline, or be empty). Unix or windows newlines are fine. */
		public VersionEntry setHeaderMisc(@NullOr String headerMisc) {
			if (headerMisc == null) {
				this.headerMisc = null;
				return this;
			}
			Preconditions.checkArgument(headerMisc.indexOf('\n') == -1);
			this.headerMisc = PoolString.of(isUnreleased() ? " " + headerMisc : headerMisc);
			return this;
		}

		/** The changes in this version, guaranteed to either be empty or to start with a newline. */
		public CharSequence changes() {
			return Objects.requireNonNull(changes);
		}

		/** Sets the changes to be used in this entry (must be start with a newline, or be empty). Unix or windows newlines are fine. */
		public VersionEntry setChanges(CharSequence changesRaw) {
			CharSequence changesUnix = asUnix(changesRaw);
			Preconditions.checkArgument(changesUnix.length() == 0 || changesUnix.charAt(0) == '\n');
			if (changesUnix instanceof PoolString) {
				this.changes = (PoolString) changesUnix;
			} else if (!this.changes.sameAs(changesUnix)) {
				this.changes = PoolString.of(changesUnix.toString());
			}
			return this;
		}

		private static CharSequence asUnix(CharSequence change) {
			for (int i = 0; i < change.length(); ++i) {
				if (change.charAt(i) == '\r') {
					return change.toString().replace("\r\n", "\n");
				}
			}
			return change;
		}

		private static @NullOr VersionEntry parse(PoolString line, Changelog parser) {
			VersionEntry header = new VersionEntry();
			if (parser.versionsRaw.isEmpty()) {
				Preconditions.checkArgument(line.startsWith(UNRELEASED));
				header.headerMisc = line.subSequence(UNRELEASED.length(), line.length());
				return header;
			}
			Preconditions.checkArgument(line.startsWith(VERSION_BEGIN));
			int versionEnd = line.indexOf("] - ");
			if (versionEnd == -1) {
				parser.addError(line.baseLineNumberStart(), "'] - ' is missing from the expected '## [x.y.z] - yyyy-mm-dd'");
				return null;
			}

			int startDate = versionEnd + "] - ".length();
			int endDate = startDate + "yyyy-mm-dd".length();
			if (endDate > line.length()) {
				parser.addError(line.baseLineNumberStart(), "'yyyy-mm-dd' is missing from the expected '## [x.y.z] - yyyy-mm-dd'");
				return null;
			}

			@NullOr
			PoolString misc;
			if (endDate == line.length()) {
				misc = null;
			} else {
				// endDate > line.length()
				if (line.charAt(endDate) != ' ') {
					parser.addError(line.baseLineNumberStart(), "If you want to put stuff after 'yyyy-mm-dd', you need to separate it with a space");
					return null;
				} else {
					misc = line.subSequence(endDate + 1, line.length());
				}
			}

			header.version = line.subSequence(VERSION_BEGIN.length(), versionEnd);
			header.date = line.subSequence(startDate, endDate);
			header.headerMisc = misc;
			return header;
		}

		PoolString toStringUnix() {
			if (version == null) {
				// {{beforeUnreleased includes '## [Unreleased]'}}{{misc}}
				return PoolString.concat(Changelog.UNRELEASED, headerMisc, changes);
			} else if (headerMisc == null) {
				return PoolString.concat("\n## [", version, "] - ", date, changes);
			} else {
				return PoolString.concat("\n## [", version, "] - ", date, " ", headerMisc, changes);
			}
		}
	}

	/** Returns a ParsedChangelog where the version list has been mutated by the given mutator. */
	private Changelog withMutatedVersions(Consumer<List<VersionEntry>> mutator) {
		List<VersionEntry> copy = new ArrayList<>(versionsRaw);
		mutator.accept(copy);
		return new Changelog(windowsNewlines,
				dontParse, beforeUnreleased,
				copy,
				unparseableAfterError);
	}

	/** Returns a new changelog where the [Unreleased] section has been released with the given version and date. */
	public Changelog releaseUnreleased(String version, String date) {
		return withMutatedVersions(list -> {
			VersionEntry unreleased = list.get(0);
			Preconditions.checkArgument(unreleased.isUnreleased());

			VersionEntry entry = VersionEntry.versionDate(version, date);
			entry.setChanges(unreleased.changes());
			unreleased.setChanges("\n");
			list.add(1, entry);
		});
	}
}
