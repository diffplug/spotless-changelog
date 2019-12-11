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
package com.diffplug.blowdryer;


import com.diffplug.common.base.Preconditions;
import com.diffplug.spotless.changelog.ParsedChangelog;
import java.io.IOException;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import pl.tlinkowski.annotation.basic.NullOr;

public class ParsedChangelogTest {
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

		ChangelogAssertions mostRecent(@NullOr String version) {
			Assertions.assertThat(unix.versionMostRecent()).isEqualTo(version);
			Assertions.assertThat(win.versionMostRecent()).isEqualTo(version);
			return this;
		}
	}

	private ChangelogAssertions test(String content) {
		return new ChangelogAssertions(content);
	}

	@Test
	public void empty() {
		Consumer<String> test = str -> {
			test(str)
					.errors("{-1=Needs to have '## [Unreleased]'}")
					.mostRecent(null);
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
				.mostRecent(null);
		test("\n\n  ## [Unreleased]")
				.errors("{3=Needs a newline directly before '## [Unreleased]'}")
				.mostRecent(null);

		Consumer<String> test = str -> {
			//			test(str)
			//				.errors("{-1, "Neees }")
			//				.mostRecent(null);
		};
		//		test.accept("## [Unreleased]");
	}
}
