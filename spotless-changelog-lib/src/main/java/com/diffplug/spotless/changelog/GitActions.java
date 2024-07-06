/*
 * Copyright (C) 2019-2024 DiffPlug
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

import com.diffplug.common.collect.Iterables;
import com.jcraft.jsch.Session;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;

/** API for doing the commit, tag, and push operations.  See {@link GitCfg#withChangelog(File, ChangelogAndNext)}. */
public class GitActions implements AutoCloseable {
	private final Repository repository;
	private final Git git;
	private final File changelogFile;
	private final ChangelogAndNext model;
	private final GitCfg cfg;

	GitActions(File changelogFile, ChangelogAndNext model, GitCfg cfg) throws IOException {
		this.changelogFile = changelogFile;
		this.model = model;
		this.cfg = cfg;
		repository = new FileRepositoryBuilder()
				.findGitDir(changelogFile)
				.build();
		git = new Git(repository);
	}

	/** Confirms that we can update the target branch on the target remote. */
	public void checkCanPush() throws GitAPIException, IOException {
		try {
			Ref ref = repository.getRefDatabase().exactRef(Constants.R_HEADS + cfg.branch);
			Objects.requireNonNull(ref, "Expected ref " + Constants.R_HEADS + cfg.branch);
			Ref remoteRef = repository.getRefDatabase().exactRef(Constants.R_REMOTES + cfg.remote + "/" + cfg.branch);
			Objects.requireNonNull(remoteRef, "Expected ref " + Constants.R_REMOTES + cfg.remote + "/" + cfg.branch);
			if (!ref.getObjectId().equals(remoteRef.getObjectId())) {
				throw new IllegalStateException("Local branch " + cfg.branch + " is out of sync with " + cfg.remote + ", so we can't safely push it automatically.");
			}
			push(cfg.branch, RemoteRefUpdate.Status.UP_TO_DATE);
		} catch (GitAPIException e) {
			throw new IllegalArgumentException("You can set user/pass with any of these environment variables: " + envVars() + ", or try -PsshStrictHostKeyChecking=no on ssh remotes", e);
		}
	}

	/** Throw an exception if the working copy is not clean. */
	public void checkWcClean() throws GitAPIException {
		var status = new Git(repository).status().call();
		if (!status.isClean()) {
			StringBuilder builder = new StringBuilder("The working copy is not clean, make a commit first. Uncommitted changes:\n");
			status.getUntracked().forEach(str -> builder.append("  ").append(str).append('\n'));
			status.getUncommittedChanges().forEach(str -> builder.append("  ").append(str).append('\n'));
			throw new IllegalStateException(builder.toString());
		}
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
		String path = repository.getWorkTree().toPath().relativize(changelogFile.toPath()).toString();
		git.add()
				.addFilepattern(path)
				.call();
		git.commit()
				.setMessage(formatCommitMessage(cfg.commitMessage))
				.call();
	}

	/** Tags and pushes the tag and the branch.  */
	public void tagBranchPush() throws GitAPIException {
		TagCommand tagCommand = git.tag().setName(tagName());
		if (cfg.tagMessage != null) {
			tagCommand.setAnnotated(true).setMessage(formatTagMessage(cfg.tagMessage));
		}
		push(tagCommand.call(), RemoteRefUpdate.Status.OK);
		push(cfg.branch, RemoteRefUpdate.Status.OK);
	}

	public void runAfterPush() {
		if (cfg.runAfterPush == null) {
			return;
		}
		try (var runner = new ProcessRunner()) {
			var result = runner.shell(formatTagMessage(cfg.runAfterPush));
			System.out.write(result.stdOut());
			System.out.flush();
			System.err.write(result.stdErr());
			System.err.flush();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("runAfterPush failed: " + formatTagMessage(cfg.runAfterPush), e);
		}
	}

	private String formatCommitMessage(final String commitMessage) {
		return commitMessage.replace(GitCfg.COMMIT_MESSAGE_VERSION, model.versions().next());
	}

	private String formatTagMessage(final String tagMessage) {
		return formatCommitMessage(tagMessage)
				.replace(GitCfg.TAG_MESSAGE_CHANGES, model.changelog().unreleasedChanges())
				.replace(GitCfg.COMMIT_MESSAGE_VERSION, model.versions().next());
	}

	private String tagName() {
		return cfg.tagPrefix + model.versions().next();
	}

	@Override
	public void close() {
		repository.close();
	}

	private void push(String branch, RemoteRefUpdate.Status expected) throws GitAPIException {
		push(cmd -> cmd.add(branch), expected);
	}

	private void push(Ref ref, RemoteRefUpdate.Status expected) throws GitAPIException {
		push(cmd -> cmd.add(ref), expected);
	}

	private void push(Consumer<PushCommand> cmd, RemoteRefUpdate.Status expected) throws GitAPIException {
		String remoteUrl = git.getRepository().getConfig().getString(ConfigConstants.CONFIG_REMOTE_SECTION, cfg.remote, ConfigConstants.CONFIG_KEY_URL);

		PushCommand push = git.push().setRemote(cfg.remote);
		if (remoteUrl.startsWith("ssh://")) {
			push = push.setTransportConfigCallback(transport -> {
				SshTransport sshTransport = (SshTransport) transport;
				sshTransport.setSshSessionFactory(new JschConfigSessionFactory() {
					@Override
					protected void configure(OpenSshConfig.Host host, Session session) {
						session.setConfig("StrictHostKeyChecking", cfg.sshStrictHostKeyChecking);
					}
				});
			});
		} else {
			push = push.setCredentialsProvider(creds());
		}

		cmd.accept(push);

		RefSpec spec = Iterables.getOnlyElement(push.getRefSpecs());
		System.out.println("push " + spec.getSource() + " to " + cfg.remote + " " + remoteUrl);

		PushResult result = Iterables.getOnlyElement(push.call());
		RemoteRefUpdate update = Iterables.getOnlyElement(result.getRemoteUpdates());
		System.out.println("  " + update.getStatus() + " "
				+ (update.getExpectedOldObjectId() != null ? update.getExpectedOldObjectId().name()
						: "(null)")
				+ "..."
				+ (update.getNewObjectId() != null ? update.getNewObjectId().name() : "(null)")
				+ (update.isFastForward() ? " fastForward" : "")
				+ (update.getMessage() != null ? update.getMessage() : ""));
		Optional<RemoteRefUpdate.Status> failure = result.getRemoteUpdates().stream()
				.map(RemoteRefUpdate::getStatus)
				.filter(r -> !expected.equals(r))
				.findAny();
		if (failure.isPresent()) {
			throw new IllegalStateException("Error! Expected " + expected + ", got " + failure.get() + ".");
		}
	}

	// similar to https://github.com/ajoberstar/grgit/blob/5766317fbe67ec39faa4632e2b80c2b056f5c124/grgit-core/src/main/groovy/org/ajoberstar/grgit/auth/AuthConfig.groovy
	private static CredentialsProvider creds() {
		String username = System.getenv(GRGIT_USERNAME_ENV_VAR);
		if (username != null) {
			String password = System.getenv(GRGIT_PASSWORD_ENV_VAR);
			if (password == null) {
				password = "";
			}
			return new UsernamePasswordCredentialsProvider(username, password);
		}
		username = System.getenv(GH_TOKEN_ENV_VAR);
		if (username != null) {
			return new UsernamePasswordCredentialsProvider(username, "");
		}
		return null;
	}

	private static final String GRGIT_USERNAME_ENV_VAR = "GRGIT_USER";
	private static final String GRGIT_PASSWORD_ENV_VAR = "GRGIT_PASS";
	private static final String GH_TOKEN_ENV_VAR = "gh_token";

	private static List<String> envVars() {
		return Arrays.asList(GRGIT_USERNAME_ENV_VAR, GRGIT_PASSWORD_ENV_VAR, GH_TOKEN_ENV_VAR);
	}
}
