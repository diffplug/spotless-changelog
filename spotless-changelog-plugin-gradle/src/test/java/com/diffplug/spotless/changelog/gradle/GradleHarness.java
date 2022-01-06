/*
 * Copyright (C) 2019-2022 DiffPlug
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
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

public class GradleHarness extends ResourceHarness {
	private static final String V_GRADLE_OLDEST_SUPPORTED = "6.2";
	private static final String V_GRADLE_CONFIG_CACHE = "6.6";

	protected boolean isConfigCache() {
		String prop = System.getProperty("com.diffplug.config-cache-test");
		if (prop == null) {
			return false;
		} else if (prop.equals("true")) {
			return true;
		} else {
			throw new IllegalArgumentException("Unexpected com.diffplug.config-cache-test=" + prop);
		}
	}

	protected GradleRunner gradleRunner() throws IOException {
		String version;
		if (isConfigCache()) {
			setFile("gradle.properties").toContent("org.gradle.unsafe.configuration-cache=true");
			version = V_GRADLE_CONFIG_CACHE;
		} else {
			version = V_GRADLE_OLDEST_SUPPORTED;
		}
		return GradleRunner.create().withGradleVersion(version).withProjectDir(rootFolder()).withPluginClasspath();
	}

	private static final String configCacheGunk1 = "Configuration cache is an incubating feature.\n" +
			"Calculating task graph as no configuration cache is available for tasks: ";
	private static final String configCacheGunk2 = "Configuration cache is an incubating feature.\n" +
			"Reusing configuration cache.";

	private String getOutput(BuildResult result) {
		String output = result.getOutput().replace("\r\n", "\n");
		if (output.startsWith(configCacheGunk1)) {
			int firstNewlineAfter = output.indexOf('\n', configCacheGunk1.length() + 1);
			return output.substring(firstNewlineAfter).trim();
		} else if (output.startsWith(configCacheGunk2)) {
			int firstNewlineAfter = output.indexOf('\n', configCacheGunk2.length() + 1);
			return output.substring(firstNewlineAfter).trim();
		} else {
			return output.trim();
		}
	}

	protected AbstractStringAssert<?> assertOutput(String... args) throws IOException {
		String output = getOutput(gradleRunner().withArguments(args).build());
		return Assertions.assertThat(output);
	}

	protected AbstractStringAssert<?> assertFailOutput(String... args) throws IOException {
		String output = getOutput(gradleRunner().withArguments(args).buildAndFail());
		return Assertions.assertThat(output);
	}
}
