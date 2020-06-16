/*
 * Copyright (C) 2019-2020 DiffPlug
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
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import pl.tlinkowski.annotation.basic.NullOr;

public class ChangelogTest {
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

	@Test
	public void afterRelease() throws IOException {
		test("\n## [Unreleased]\n## [x.y.z] - 1234a56b78").afterRelease(
				"\n## [Unreleased]\n\n## [1.0.0] - 2020-12-30\n## [x.y.z] - 1234a56b78");
		test("\n## [Unreleased] moreStuff\n## [x.y.z] - 1234a56b78").afterRelease(
				"\n## [Unreleased] moreStuff\n\n## [1.0.0] - 2020-12-30\n## [x.y.z] - 1234a56b78");
		test("\n## [Unreleased]\n-CONTENT\n## [x.y.z] - 1234a56b78").afterRelease(
				"\n## [Unreleased]\n\n## [1.0.0] - 2020-12-30\n-CONTENT\n## [x.y.z] - 1234a56b78");
		test("\n## [Unreleased] moreStuff\n-CONTENT\n## [x.y.z] - 1234a56b78").afterRelease(
				"\n## [Unreleased] moreStuff\n\n## [1.0.0] - 2020-12-30\n-CONTENT\n## [x.y.z] - 1234a56b78");
		test("\n## [Unreleased]\n-CONTENT\n").afterRelease(
				"\n## [Unreleased]\n\n## [1.0.0] - 2020-12-30\n-CONTENT\n");
		test("\n## [Unreleased] moreStuff\n-CONTENT\n").afterRelease(
				"\n## [Unreleased] moreStuff\n\n## [1.0.0] - 2020-12-30\n-CONTENT\n");
		test("\n## [Unreleased]").afterRelease(
				"\n## [Unreleased]\n\n## [1.0.0] - 2020-12-30");
		test("\n## [Unreleased] moreStuff\n-CONTENT\n").afterRelease(
				"\n## [Unreleased] moreStuff\n\n## [1.0.0] - 2020-12-30\n-CONTENT\n");
	}

	static class ChangelogAssertions {
		Changelog unix, win;

		ChangelogAssertions(String contentUnix) {
			Preconditions.checkArgument(contentUnix.indexOf("\r\n") == -1);
			this.unix = new Changelog(contentUnix);
			Assertions.assertThat(unix.toString()).isEqualTo(contentUnix);

			String contentWin = contentUnix.replace("\n", "\r\n");
			this.win = new Changelog(contentWin);
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

		ChangelogAssertions afterRelease(String afterRelease) {
			errors("{}");
			String VERSION = "1.0.0";
			String DATE = "2020-12-30";
			String unreleased = unix.releaseUnreleased(VERSION, DATE).toString();
			Assertions.assertThat(unreleased).isEqualTo(afterRelease);
			Assertions.assertThat(win.releaseUnreleased(VERSION, DATE).toString()).isEqualTo(unreleased.replace("\n", "\r\n"));
			return this;
		}
	}

	private ChangelogAssertions test(String content) {
		return new ChangelogAssertions(content);
	}
}
