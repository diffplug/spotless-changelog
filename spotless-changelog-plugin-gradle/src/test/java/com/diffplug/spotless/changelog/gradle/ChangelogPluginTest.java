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
package com.diffplug.spotless.changelog.gradle;


import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Test;

public class ChangelogPluginTest extends GradleHarness {
	private static final String DATE_NOW = "2019-01-30";

	private void writeSpotlessChangelog(String... lines) throws IOException {
		write("build.gradle",
				"plugins {",
				"  id 'com.diffplug.spotless-changelog'",
				"}",
				"try {",
				"  com.diffplug.common.globals.TimeDev.install().setUTC(java.time.LocalDate.parse('" + DATE_NOW + "'))",
				"} catch (Exception e) {",
				"  // this will fail the second time, which is fine",
				"}",
				"",
				"spotlessChangelog {",
				Arrays.stream(lines).collect(Collectors.joining("\n")),
				"}");
	}

	@Test
	public void tasks() throws IOException {
		writeSpotlessChangelog();
		assertOutput("tasks").contains("Changelog tasks\n" +
				"---------------\n" +
				"changelogBump - updates the changelog on disk with the next version and the current UTC date\n" +
				"changelogCheck - checks that the changelog is formatted according to your rules\n" +
				"changelogPrint - prints the last published version and the calculated next version, e.g. `myproj 1.0.4 -> 1.1.0`\n" +
				"changelogPush - commits the bumped changelog, tags it, and pushes (recommend wiring to your publish task)");
	}

	@Test
	public void missingChangelog() throws IOException {
		writeSpotlessChangelog();
		assertFailOutput("changelogCheck")
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
		assertFailOutput("changelogCheck")
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
		assertOutput("changelogPrint")
				.startsWith("\n> Task :changelogPrint\nundertest 1.0.0 (no unreleased changes)");
		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"Some minor change",
				"",
				"## [1.0.0] - 2020-10-10");
		assertOutput("changelogPrint")
				.startsWith("\n> Task :changelogPrint\nundertest 1.0.0 -> 1.0.1");
		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"### Added",
				"",
				"## [1.0.0] - 2020-10-10");
		assertOutput("changelogPrint")
				.startsWith("\n> Task :changelogPrint\nundertest 1.0.0 -> 1.1.0");
		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"**BREAKING**",
				"",
				"## [1.0.0] - 2020-10-10");
		assertOutput("changelogPrint")
				.startsWith("\n> Task :changelogPrint\nundertest 1.0.0 -> 2.0.0");
	}

	@Test
	public void changelogBump() throws IOException {
		writeSpotlessChangelog();
		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"",
				"## [1.0.0] - 2020-10-10");
		String noUnreleasedChanges = read("CHANGELOG.md");
		gradleRunner().withArguments("changelogBump").build();
		assertFile("CHANGELOG.md").hasContent(noUnreleasedChanges);

		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"### Added",
				"- Some change",
				"",
				"## [1.0.0] - 2020-10-10");
		gradleRunner().withArguments("changelogBump").build();
		assertFile("CHANGELOG.md").hasContent("\n" +
				"## [Unreleased]\n" +
				"\n" +
				"## [1.1.0] - 2019-01-30\n" +
				"### Added\n" +
				"- Some change\n" +
				"\n" +
				"## [1.0.0] - 2020-10-10");
	}

	@Test
	public void changelogBumpCustomNextVersionFunction() throws IOException {
		writeSpotlessChangelog("versionSchema(com.diffplug.spotless.changelog.NextVersionFunction.SemverBrandPrefix)");
		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"",
				"## [52.1.0.0] - 2020-10-10");
		String noUnreleasedChanges = read("CHANGELOG.md");
		gradleRunner().withArguments("changelogBump").build();
		assertFile("CHANGELOG.md").hasContent(noUnreleasedChanges);

		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"### Added",
				"- Some change",
				"",
				"## [52.1.0.0] - 2020-10-10");
		gradleRunner().withArguments("changelogBump").build();
		assertFile("CHANGELOG.md").hasContent("\n" +
				"## [Unreleased]\n" +
				"\n" +
				"## [52.1.1.0] - 2019-01-30\n" +
				"### Added\n" +
				"- Some change\n" +
				"\n" +
				"## [52.1.0.0] - 2020-10-10");
	}
}
