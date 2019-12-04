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


import com.diffplug.common.base.Preconditions;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import pl.tlinkowski.annotation.basic.NullOr;

public class ParsedChangelog {
	private static final String VERSION_BEGIN = "\n## [";
	private static final String UNRELEASED = VERSION_BEGIN + "Unreleased]";
	private static final String DONT_PARSE_BELOW_HERE = "\n<!-- do not parse below here -->";

	private final PoolString contentUnix;
	private final boolean windowsNewlines;
	private final PoolString dontParse, beforeUnreleased;
	private final LinkedHashMap<VersionHeader, PoolString> versionsRaw = new LinkedHashMap<>();
	private final @NullOr PoolString unparseableAfterError;
	private final LinkedHashMap<Integer, String> parseErrors = new LinkedHashMap<>();

	public ParsedChangelog(String contentRaw) throws IOException {
		contentUnix = PoolString.of(contentRaw.replace("\r\n", "\n"));
		windowsNewlines = contentUnix.length() < contentRaw.length();

		PoolString toParse = contentUnix.until(DONT_PARSE_BELOW_HERE);
		dontParse = contentUnix.after(toParse);

		beforeUnreleased = toParse.until(UNRELEASED);
		toParse = toParse.after(beforeUnreleased);

		if (toParse.isEmpty()) {
			addError(-1, "Needs to have '## [Unreleased]'");
		}

		while (true) {
			PoolString versionHeader = toParse.until("\n");
			@NullOr
			VersionHeader header;
			if (versionsRaw.isEmpty()) {
				header = VersionHeader.unreleased(versionHeader);
			} else {
				header = VersionHeader.parse(versionHeader, this);
			}
			if (header == null) {
				unparseableAfterError = toParse;
				return;
			}
			PoolString versionContent = toParse.until(VERSION_BEGIN);
			versionsRaw.put(header, versionContent);
		}
	}

	public String toStringUnix() {
		PoolString total = beforeUnreleased;
		for (Map.Entry<VersionHeader, PoolString> entry : versionsRaw.entrySet()) {
			total = total.concat(entry.getKey().toStringUnix()).concat(entry.getValue());
		}
		if (unparseableAfterError != null) {
			total = total.concat(unparseableAfterError);
		}
		total = total.concat(dontParse);
		return total.toString();
	}

	private void addError(int lineNumber, String message) {
		parseErrors.put(lineNumber, message);
	}

	public boolean hasErrors() {
		return parseErrors.isEmpty();
	}

	/** Map from line number to the error message, in the order they were encountered. */
	public LinkedHashMap<Integer, String> errors() {
		return parseErrors;
	}

	@Override
	public String toString() {
		String unix = toStringUnix();
		return windowsNewlines ? unix.replace("\n", "\r\n") : unix;
	}

	static class VersionHeader {
		@NullOr
		PoolString version;
		@NullOr
		PoolString date;
		@NullOr
		PoolString misc;

		static VersionHeader unreleased(PoolString line) {
			Preconditions.checkArgument(line.startsWith(VERSION_BEGIN));
			VersionHeader header = new VersionHeader();
			header.misc = line.subSequence(VERSION_BEGIN.length(), line.length());
			return header;
		}

		static @NullOr VersionHeader parse(PoolString line, ParsedChangelog parser) {
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
				if (line.charAt(endDate + 1) != ' ') {
					parser.addError(line.baseLineNumberStart(), "If you want to put stuff after 'yyyy-mm-dd', you need to separate it with a space");
					return null;
				} else {
					misc = line.subSequence(endDate + 1, line.length());
				}
			}

			VersionHeader header = new VersionHeader();
			header.version = line.subSequence(VERSION_BEGIN.length(), versionEnd);
			header.date = line.subSequence(startDate, endDate);
			header.misc = misc;
			return header;
		}

		PoolString toStringUnix() {
			if (version == null) {
				return misc;
			} else if (misc == null) {
				return PoolString.concat("\n## [", version, "] - ", date);
			} else {
				return PoolString.concat("\n## [", version, "] - ", date, " ", misc);
			}
		}
	}
}
