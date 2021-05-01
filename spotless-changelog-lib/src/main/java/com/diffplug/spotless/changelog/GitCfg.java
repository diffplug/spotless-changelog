/*
 * Copyright (C) 2019-2021 DiffPlug
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
public class GitCfg {
	public static final String COMMIT_MESSAGE_VERSION = "{{version}}";
	public static final String TAG_MESSAGE_CHANGES = "{{changes}}";
	public static final String NONE__USE_LIGHTWEIGHT_TAG = null;

	/** Prefix used for release tags, default is `release/`. */
	public String tagPrefix = "release/";
	/** Message used for release commits, default is `Published release/{{version}}`. */
	public String commitMessage = "Published release/" + COMMIT_MESSAGE_VERSION;
	private String tagMessage = NONE__USE_LIGHTWEIGHT_TAG;
	public String remote = "origin";
	public String branch = "main";
	public String sshStrictHostKeyChecking = "yes";

	/** Returns an api configured with this config. */
	public GitActions withChangelog(File changelogFile, ChangelogAndNext model) throws IOException {
		return new GitActions(changelogFile, model, this);
	}

	/** Validates that the commit message is in the correct format. */
	public static String validateCommitMessage(String commitMessage) {
		if (!commitMessage.contains(COMMIT_MESSAGE_VERSION)) {
			throw new IllegalArgumentException("The commit message must contain " + COMMIT_MESSAGE_VERSION + " to be replaced with the real version.");
		}
		return commitMessage;
	}

	/** Sets the essage to annotate release tag: if null (the default), then a lightweight tag is created`. */
	public void tagMessage(String tagMessage) {
		this.tagMessage = tagMessage;
	}

	/** Gets the essage to annotate release tag: if null (the default), then a lightweight tag is created`. */
	public String tagMessage() {
		return tagMessage;
	}

	public boolean useAnnotatedTag() {
		return tagMessage != NONE__USE_LIGHTWEIGHT_TAG;
	}
}
