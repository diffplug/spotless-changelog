/*
 * Copyright (C) 2019-2023 DiffPlug
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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class ChangelogPluginPushTest extends GradleHarness {

	@Rule
	public DeleteOnSuccessTemporaryFolder temporaryFolder = new DeleteOnSuccessTemporaryFolder(new File("build"));
	@Rule
	public KeepTempFolderOnFailure keepTempFolderOnFailure = new KeepTempFolderOnFailure(temporaryFolder);

	@Test
	public void verifyAnnotatedTagMessage() throws IOException, GitAPIException {
		final String testCaseFolder = "1.0.3-annotatedTag";
		File origin = initUpstreamRepo(testCaseFolder);

		final File working = temporaryFolder.newFolder("working");
		Git git = Git.cloneRepository().setURI(origin.getAbsolutePath())
				.setDirectory(working).call();

		gradleRunner().withProjectDir(working)/*.withDebug(true)*/
				.withArguments("changelogPush").build();

		assertEquals("Version is 1.0.3, here are the changes:"
				+ "\n\n### Fixed\n"
				+ "- this should be in tag message\n",
				annotatedTagMessage(git, "release/1.0.3"));
	}

	private String annotatedTagMessage(Git localGit, final String tagName) throws IOException {
		try (RevWalk walk = new RevWalk(localGit.getRepository())) {
			return walk.parseTag(
					localGit.getRepository().findRef(tagName).getObjectId())
					.getFullMessage();
		}
	}

	private File initUpstreamRepo(String testCaseFolder) throws IOException, GitAPIException {
		File origin = temporaryFolder.newFolder("origin");
		Git git = Git.init().setDirectory(origin).setInitialBranch("master").call();
		copyResourceFile(origin, "settings.gradle");
		copyResourceFile(origin, testCaseFolder, "build.gradle");
		copyResourceFile(origin, testCaseFolder, ".gitignore");
		copyResourceFile(origin, testCaseFolder, "CHANGELOG.md");
		git.add().addFilepattern(".").call();
		git.commit()
				.setMessage("Commit all changes including additions")
				.call();
		return origin;
	}

	private void copyResourceFile(File working, String testCaseFolder, String fileName) throws IOException {
		Files.copy(Paths.get("src/test/resources", testCaseFolder, fileName), working.toPath().resolve(fileName));
	}

	private void copyResourceFile(File working, String fileName) throws IOException {
		Files.copy(Paths.get("src/test/resources", fileName), working.toPath().resolve(fileName));
	}

	static class KeepTempFolderOnFailure extends TestWatcher {
		private final DeleteOnSuccessTemporaryFolder folder;

		KeepTempFolderOnFailure(DeleteOnSuccessTemporaryFolder folder) {
			this.folder = folder;
		}

		@Override
		protected void failed(Throwable e, Description description) {
			folder.disableDeletion();
		}
	}

	static class DeleteOnSuccessTemporaryFolder extends TemporaryFolder {

		boolean canDelete = true;

		public DeleteOnSuccessTemporaryFolder(File file) {
			super(file);
		}

		@Override
		protected void after() {
			if (canDelete)
				super.after();
		}

		public void disableDeletion() {
			canDelete = false;
		}
	}
}
