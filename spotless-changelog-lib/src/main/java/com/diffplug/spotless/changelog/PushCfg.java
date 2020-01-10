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
package com.diffplug.spotless.changelog;


import java.io.File;
import java.io.IOException;

/** Configuration for committing, tagging, and pushing the next version. */
public class PushCfg {
	public String tagPrefix = "release/";
	public String commitMessage = "Published release/" + ChangelogModel.COMMIT_MESSAGE_VERSION;
	public String remote = "origin";
	public String branch = "master";

	public GitApi withChangelog(File changelogFile, ChangelogModel model) throws IOException {
		return new GitApi(changelogFile, model, this);
	}

	/** Validates that the commit message is in the correct format. */
	public static String validateCommitMessage(String commitMessage) {
		if (!commitMessage.contains(ChangelogModel.COMMIT_MESSAGE_VERSION)) {
			throw new IllegalArgumentException("The commit message must contain '" + ChangelogModel.COMMIT_MESSAGE_VERSION + "' to be replaced with the real version.");
		}
		return commitMessage;
	}
}
