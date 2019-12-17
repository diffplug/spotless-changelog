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
package com.diffplug.spotless.changelog;


import com.diffplug.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/** API for doing the commit, tag, and push operations. */
public class GitApi implements AutoCloseable {
	private final Repository repository;
	private final Git git;
	private final File changelogFile;
	private final ChangelogModel model;
	private final ChangelogModel.PushCfg cfg;

	GitApi(File changelogFile, ChangelogModel model, ChangelogModel.PushCfg cfg) throws IOException {
		this.changelogFile = changelogFile;
		this.model = model;
		this.cfg = cfg;
		repository = new FileRepositoryBuilder()
				.findGitDir(changelogFile)
				.build();
		repository.getWorkTree();
		git = new Git(repository);
	}

	/** Confirms that we can push a branch. */
	public void checkCanPush() throws GitAPIException, IOException {
		Ref ref = repository.getRefDatabase().exactRef(Constants.R_HEADS + cfg.branch);
		Objects.requireNonNull(ref, "Expected ref " + Constants.R_HEADS + cfg.branch);
		Ref remoteRef = repository.getRefDatabase().exactRef(Constants.R_REMOTES + cfg.remote + "/" + cfg.branch);
		Objects.requireNonNull(ref, "Expected ref " + Constants.R_REMOTES + cfg.remote + "/" + cfg.branch);
		if (!ref.getObjectId().equals(remoteRef.getObjectId())) {
			throw new IllegalStateException("Local branch " + cfg.branch + " is out of sync with " + cfg.remote + ", so we can't safely push it automatically.");
		}
		push(cfg.branch);
	}

	/** Asserts that there is no tag with the expected name. */
	public void assertNoTag() throws IOException {
		Ref ref = repository.getRefDatabase().exactRef(Constants.R_TAGS + tagName());
		if (ref != null) {
			throw new IllegalStateException("Already created the '" + tagName() + "' tag, so we can't create it automatically.");
		}
	}

	/** Adds and commits the changelog. */
	public void addAndCommit() throws GitAPIException {
		String commitMsg = cfg.commitMessage.replace(ChangelogModel.COMMIT_MESSAGE_VERSION, model.versionNext());
		String path = repository.getWorkTree().toPath().relativize(changelogFile.toPath()).toString();
		git.add()
				.addFilepattern(path)
				.call();
		git.commit()
				.setOnly(path)
				.setMessage(commitMsg)
				.call();
	}

	/** Tags and pushes the tag and the branch. */
	public void tagBranchPush() throws GitAPIException {
		Ref tagRef = git.tag().setName(tagName()).setAnnotated(false).call();
		push(tagRef);
		push(cfg.branch);
	}

	private String tagName() {
		return cfg.tagPrefix + model.versionNext();
	}

	@Override
	public void close() {
		repository.close();
	}

	private void push(String branch) throws GitAPIException {
		push(cmd -> cmd.add(branch));
	}

	private void push(Ref ref) throws GitAPIException {
		push(cmd -> cmd.add(ref));
	}

	private void push(Consumer<PushCommand> cmd) throws GitAPIException {
		PushCommand push = git.push().setCredentialsProvider(creds()).setRemote(cfg.remote);
		cmd.accept(push);
		PushResult result = Iterables.getOnlyElement(push.call());
		System.out.println("push: " + result.getMessages());
	}

	// same https://github.com/ajoberstar/grgit/blob/5766317fbe67ec39faa4632e2b80c2b056f5c124/grgit-core/src/main/groovy/org/ajoberstar/grgit/auth/AuthConfig.groovy
	private static CredentialsProvider creds() {
		String username = System.getenv(GRGIT_USERNAME_ENV_VAR);
		if (username != null) {
			String password = System.getenv(GRGIT_PASSWORD_ENV_VAR);
			if (password == null) {
				password = "";
			}
			return new UsernamePasswordCredentialsProvider(username, password);
		}
		username = System.getProperty(GRGIT_USERNAME_SYS_PROP);
		if (username != null) {
			String password = System.getenv(GRGIT_PASSWORD_SYS_PROP);
			if (password == null) {
				password = "";
			}
			return new UsernamePasswordCredentialsProvider(username, password);
		}
		username = System.getProperty(GH_TOKEN_ENV_VAR);
		if (username != null) {
			return new UsernamePasswordCredentialsProvider(username, "");
		}
		return null;
	}

	private static final String GH_TOKEN_ENV_VAR = "gh_token";
	private static final String GRGIT_USERNAME_ENV_VAR = "GRGIT_USER";
	private static final String GRGIT_PASSWORD_ENV_VAR = "GRGIT_PASS";
	private static final String GRGIT_USERNAME_SYS_PROP = "org.ajoberstar.grgit.auth.username";
	private static final String GRGIT_PASSWORD_SYS_PROP = "org.ajoberstar.grgit.auth.password";
}
