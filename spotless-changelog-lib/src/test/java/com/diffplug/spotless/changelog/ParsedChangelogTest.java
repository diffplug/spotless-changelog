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
import java.util.function.Consumer;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import pl.tlinkowski.annotation.basic.NullOr;

public class ParsedChangelogTest {
	@Test
	public void empty() {
		Consumer<String> test = str -> {
			test(str)
					.errors("{-1=Needs to have '## [Unreleased]'}")
					.last(null);
		};
		test.accept("");
		test.accept("\n");
		test.accept("\n\n");
		test.accept("\n\n\n");
	}

	@Test
	public void unreleased() throws IOException {
		test("## [Unreleased]")
				.errors("{1=Needs a newline directly before '## [Unreleased]'}")
				.last(null);
		test("\n\n  ## [Unreleased]")
				.errors("{3=Needs a newline directly before '## [Unreleased]'}")
				.last(null);

		Function<String, ChangelogAssertions> test = str -> test(str).errors("{}").last(null);
		test.apply("\n## [Unreleased]").unreleasedChanges("");
		test.apply("First line\n## [Unreleased]").unreleasedChanges("");
		test.apply("First line\n## [Unreleased]\nLast line").unreleasedChanges("\nLast line");
		test.apply("First line\n## [Unreleased] with stuff after\nLast line").unreleasedChanges("\nLast line");
		test.apply("First line\n## [Unreleased] with stuff after\nLast line\n").unreleasedChanges("\nLast line\n");
	}

	@Test
	public void releasedOne() throws IOException {
		test("\n## [Unreleased]\n## [").last(null)
				.errors("{3='] - ' is missing from the expected '## [x.y.z] - yyyy-mm-dd'}");
		test("\n## [Unreleased]\n## [x.y.z").last(null)
				.errors("{3='] - ' is missing from the expected '## [x.y.z] - yyyy-mm-dd'}");
		test("\n## [Unreleased]\n## [x.y.z] -").last(null)
				.errors("{3='] - ' is missing from the expected '## [x.y.z] - yyyy-mm-dd'}");
		test("\n## [Unreleased]\n## [x.y.z] - ").last(null)
				.errors("{3='yyyy-mm-dd' is missing from the expected '## [x.y.z] - yyyy-mm-dd'}");
		test("\n## [Unreleased]\n## [x.y.z] - 1234a23").last(null)
				.errors("{3='yyyy-mm-dd' is missing from the expected '## [x.y.z] - yyyy-mm-dd'}");
		test("\n## [Unreleased]\n## [x.y.z] - 1234a56b78").last("x.y.z")
				.errors("{}");
		test("\n## [Unreleased]\n## [x.y.z] - 1234a56b78a").last(null)
				.errors("{3=If you want to put stuff after 'yyyy-mm-dd', you need to separate it with a space}");
		test("\n## [Unreleased]\n## [x.y.z] - 1234a56b78 moreStuff").last("x.y.z").errors("{}").unreleasedChanges("");
		test("\n## [Unreleased]\nOnething\n## [x.y.z] - 1234a56b78 moreStuff").last("x.y.z").errors("{}").unreleasedChanges("\nOnething");
	}

	static class ChangelogAssertions {
		ParsedChangelog unix, win;

		ChangelogAssertions(String contentUnix) {
			Preconditions.checkArgument(contentUnix.indexOf("\r\n") == -1);
			this.unix = new ParsedChangelog(contentUnix);
			Assertions.assertThat(unix.toString()).isEqualTo(contentUnix);

			String contentWin = contentUnix.replace("\n", "\r\n");
			this.win = new ParsedChangelog(contentWin);
			Assertions.assertThat(win.toString()).isEqualTo(contentWin);
		}

		ChangelogAssertions errors(String errors) {
			Assertions.assertThat(unix.errors().toString()).isEqualTo(errors);
			Assertions.assertThat(win.errors().toString()).isEqualTo(errors);
			return this;
		}

		ChangelogAssertions last(@NullOr String version) {
			Assertions.assertThat(unix.versionLast()).isEqualTo(version);
			Assertions.assertThat(win.versionLast()).isEqualTo(version);
			return this;
		}

		ChangelogAssertions unreleasedChanges(String unreleased) {
			Assertions.assertThat(unix.unreleasedChanges()).isEqualTo(unreleased);
			Assertions.assertThat(win.unreleasedChanges()).isEqualTo(unreleased);
			return this;
		}
	}

	private ChangelogAssertions test(String content) {
		return new ChangelogAssertions(content);
	}
}
