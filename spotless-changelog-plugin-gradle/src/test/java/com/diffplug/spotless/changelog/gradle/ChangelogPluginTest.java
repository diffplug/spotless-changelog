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
package com.diffplug.spotless.changelog.gradle;


import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ChangelogPluginTest extends GradleHarness {
	private void writeSpotlessChangelog(String... lines) throws IOException {
		write("build.gradle",
				"plugins {",
				"  id 'com.diffplug.spotless-changelog'",
				"}",
				"",
				"spotlessChangelog {",
				Arrays.stream(lines).collect(Collectors.joining("\n")),
				"}");
	}

	@Test
	public void missingChangelog() throws IOException {
		writeSpotlessChangelog();
		Assertions.assertThat(gradleRunner().withArguments("changelogCheck").buildAndFail().getOutput())
				.contains("Looked for changelog at '", "', but it was not present.");
	}

	@Test
	public void changelogCheck() throws IOException {
		writeSpotlessChangelog();
		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"",
				"## [1.0.0]");
		Assertions.assertThat(gradleRunner().withArguments("changelogCheck").buildAndFail().getOutput())
				.contains("CHANGELOG.md:5: '] - ' is missing from the expected '## [x.y.z] - yyyy-mm-dd");

		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"",
				"## [1.0.0] - 2020-10-10");
		gradleRunner().withArguments("changelogCheck").build();
	}

	@Test
	public void changelogPrint() throws IOException {
		writeSpotlessChangelog();
		write("settings.gradle", "rootProject.name='undertest'");
		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"",
				"## [1.0.0] - 2020-10-10");
		Assertions.assertThat(gradleRunner().withArguments("changelogPrint").build().getOutput().replace("\r\n", "\n"))
				.startsWith("\n> Task :changelogPrint\nundertest 1.0.0 (no unreleased changes)");
		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"Some minor change",
				"",
				"## [1.0.0] - 2020-10-10");
		Assertions.assertThat(gradleRunner().withArguments("changelogPrint").build().getOutput().replace("\r\n", "\n"))
				.startsWith("\n> Task :changelogPrint\nundertest 1.0.0 -> 1.0.1");
		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"### Added",
				"",
				"## [1.0.0] - 2020-10-10");
		Assertions.assertThat(gradleRunner().withArguments("changelogPrint").build().getOutput().replace("\r\n", "\n"))
				.startsWith("\n> Task :changelogPrint\nundertest 1.0.0 -> 1.1.0");
		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"**BREAKING**",
				"",
				"## [1.0.0] - 2020-10-10");
		Assertions.assertThat(gradleRunner().withArguments("changelogPrint").build().getOutput().replace("\r\n", "\n"))
				.startsWith("\n> Task :changelogPrint\nundertest 1.0.0 -> 2.0.0");
	}
}
