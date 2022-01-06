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


import com.diffplug.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;

public abstract class IfGitDiffExtension<T> {
	static final String NAME = "ifGitDiff";

	public static class ForProject extends IfGitDiffExtension<Project> {
		public ForProject(Project owner) {
			super(owner);
		}

		@Override
		protected File file(Object fileArg) {
			return owner.file(fileArg);
		}
	}

	public static class ForSettings extends IfGitDiffExtension<Settings> {
		public ForSettings(Settings owner) {
			super(owner);
		}

		@Override
		protected File file(Object fileArg) {
			if (fileArg instanceof File) {
				return (File) fileArg;
			} else if (fileArg instanceof String) {
				return new File(owner.getRootDir(), (String) fileArg);
			} else {
				throw new IllegalArgumentException("We only support String or File, this was " + fileArg.getClass());
			}
		}
	}

	final T owner;

	IfGitDiffExtension(T owner) {
		this.owner = owner;
	}

	private String baseline = "origin/main";

	public void setBaseline(String baseline) {
		this.baseline = baseline;
	}

	public String getBaseline() {
		return baseline;
	}

	protected abstract File file(Object fileArg);

	private TreeFilter filterTo(Repository repo, File child) {
		String rootAbs = repo.getWorkTree().getAbsolutePath();
		String childAbs = child.getAbsolutePath();
		if (rootAbs.equals(childAbs)) {
			return TreeFilter.ALL;
		} else if (childAbs.startsWith(rootAbs)) {
			String filter = childAbs.substring(rootAbs.length()).replace('\\', '/');
			Preconditions.checkState(filter.charAt(0) == '/');
			return PathFilter.create(filter.substring(1));
		} else {
			throw new GradleException(childAbs + " is not contained within the git repo " + rootAbs);
		}
	}

	public void inFolder(Object folder, Action<T> onChanged) {
		try (Repository repo = new FileRepositoryBuilder()
				.findGitDir(file(""))
				.build()) {
			ObjectId baselineSha = repo.resolve(baseline);
			if (baselineSha == null) {
				throw new GradleException("Unable to resolve " + baseline);
			}

			CanonicalTreeParser baselineTree = new CanonicalTreeParser();
			try (ObjectReader reader = repo.newObjectReader()) {
				RevWalk walk = new RevWalk(reader);
				baselineTree.reset(reader, walk.parseCommit(baselineSha).getTree());
			}
			Git git = new Git(repo);
			List<DiffEntry> changes = git.diff()
					.setOldTree(baselineTree)
					.setShowNameAndStatusOnly(true)
					.setPathFilter(filterTo(repo, file(folder)))
					.call();
			if (!changes.isEmpty()) {
				onChanged.execute(owner);
			}
		} catch (IOException e) {
			throw new GradleException("Unable to find git repository", e);
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		}
	}
}
