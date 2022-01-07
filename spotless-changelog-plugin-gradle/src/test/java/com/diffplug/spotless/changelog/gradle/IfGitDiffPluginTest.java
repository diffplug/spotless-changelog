/*
 * Copyright (C) 2022 DiffPlug
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.Git;
import org.gradle.testkit.runner.BuildTask;
import org.junit.Assume;
import org.junit.Test;

public class IfGitDiffPluginTest extends GradleHarness {
	@Test
	public void ifGitDiff() throws Exception {
		Assume.assumeFalse(isConfigCache());
		Git git = Git.init().setDirectory(rootFolder()).setInitialBranch("main").call();
		setFile("build.gradle").toContent("tasks.register('test')");
		setFile("a/build.gradle").toContent("tasks.register('test')");
		setFile("b/build.gradle").toContent("tasks.register('test')");
		setFile("settings.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.if-git-diff'",
				"}",
				"ifGitDiff {",
				"  baseline 'main'",
				"  inFolder 'a', { include 'a' }",
				"  inFolder 'b', { include 'b' }",
				"}");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Initial").call();

		assertRan(":test");

		setFile("blah").toContent("");
		assertRan(":test");

		setFile("a/blah").toContent("");
		assertRan(":test", ":a:test");

		setFile("b/blah").toContent("");
		assertRan(":test", ":a:test", ":b:test");
	}

	private void assertRan(String... expectedPaths) throws IOException {
		List<BuildTask> tasks = gradleRunner().withArguments("test").forwardOutput().build().getTasks();
		Set<String> actualPaths = tasks.stream().map(BuildTask::getPath).collect(Collectors.toSet());
		Assertions.assertThat(actualPaths).containsExactlyInAnyOrder(expectedPaths);
	}
}
