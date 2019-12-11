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
	public void changelogErrors() throws IOException {
		writeSpotlessChangelog();
		write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"",
				"## [1.0.0]");
		Assertions.assertThat(gradleRunner().withArguments("changelogCheck").buildAndFail().getOutput())
				.contains("CHANGELOG.md:5: '] - ' is missing from the expected '## [x.y.z] - yyyy-mm-dd");
	}
}
