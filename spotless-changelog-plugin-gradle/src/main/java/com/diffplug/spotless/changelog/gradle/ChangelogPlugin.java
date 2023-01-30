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

import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.globals.Time;
import com.diffplug.spotless.changelog.Changelog;
import com.diffplug.spotless.changelog.ChangelogAndNext;
import com.diffplug.spotless.changelog.GitActions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import javax.inject.Inject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;

/** @see ChangelogExtension */
public class ChangelogPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		// make sure there is a check task
		project.getPlugins().apply(BasePlugin.class);

		ChangelogExtension extension = project.getExtensions().create(ChangelogExtension.NAME, ChangelogExtension.class, project);
		project.getTasks().register(PrintTask.NAME, PrintTask.class, extension);
		String sshStrictHostKeyChecking = (String) project.getRootProject().findProperty("sshStrictHostKeyChecking");
		if (sshStrictHostKeyChecking != null) {
			extension.data.gitCfg.sshStrictHostKeyChecking = sshStrictHostKeyChecking;
		}

		Provider<TaskOrderingService> taskOrdering = project.getGradle().getSharedServices().registerIfAbsent("ChangelogService", TaskOrderingService.class, unused -> {});

		TaskProvider<PushWillRunTask> pushWillRun = project.getTasks().register(PushWillRunTask.NAME, PushWillRunTask.class, task -> {
			task.getTaskOrderingService().set(taskOrdering);
		});

		TaskProvider<CheckTask> check = project.getTasks().register(CheckTask.NAME, CheckTask.class, extension);
		check.configure(t -> {
			t.getTaskOrderingService().set(taskOrdering);
			t.mustRunAfter(pushWillRun);
		});
		TaskProvider<BumpTask> bump = project.getTasks().register(BumpTask.NAME, BumpTask.class, extension);
		bump.configure(t -> t.dependsOn(check));
		TaskProvider<PushTask> push = project.getTasks().register(PushTask.NAME, PushTask.class, extension);
		push.configure(t -> {
			t.dependsOn(bump);
			t.dependsOn(pushWillRun);
		});

		project.afterEvaluate(unused -> {
			if (extension.data.enforceCheck) {
				project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(t -> t.dependsOn(check));
			}
		});
	}

	/**
	 * +---------------------------+                                        +-------+
	 * | pushWillRun               |<--dependsOn----------------------------| push  |
	 * |                           |                                        +---T---+
	 * | marks that push will run  |                                        dependsOn
	 * |  in TaskOrderingService   |                 +-------+              +---V---+
	 * | as a taskGraph workaround |<--mustRunAfter--| check |<--dependsOn--| bump  |
	 * +---------------------------+                 +-------+              +-------+
	 */
	public static abstract class TaskOrderingService implements BuildService<BuildServiceParameters.None> {
		private final Set<String> pushWillRun = Collections.synchronizedSet(new HashSet<>());

		private String projectFor(Task task) {
			String path = task.getPath();
			int lastColon = path.lastIndexOf(':');
			return lastColon == -1 ? path : path.substring(0, lastColon);
		}

		void notifyPushWillRun(PushWillRunTask task) {
			pushWillRun.add(projectFor(task));
		}

		boolean pushWillRun(CheckTask task) {
			return pushWillRun.contains(projectFor(task));
		}
	}

	public static abstract class PushWillRunTask extends DefaultTask {
		public static final String NAME = "changelogInternalPushWillRun";

		@Internal
		abstract Property<TaskOrderingService> getTaskOrderingService();

		@TaskAction
		public void pushWillRun() {
			getTaskOrderingService().get().notifyPushWillRun(this);
		}
	}

	private static abstract class ChangelogTask extends DefaultTask {
		protected final ChangelogExtension.Data data;

		protected ChangelogTask(ChangelogExtension extension) {
			this.data = extension.data;
			setGroup("changelog");
		}

		protected void assertNotSnapshot() {
			if (data.getVersionNext().endsWith(ChangelogAndNext.DASH_SNAPSHOT)) {
				if (data.nextVersionCfg.appendSnapshot) {
					throw new GradleException("You must add `-Prelease=true` to remove the -SNAPSHOT from " + data.getVersionNext());
				} else {
					throw new GradleException("It doesn't make sense to put a -SNAPSHOT version into the changelog, nor to make a git tag " + data.gitCfg.tagPrefix + data.getVersionNext());
				}
			}
		}
	}

	/** `changelogCheck` - throws an error if the changelog is not formatted according to your rules */
	public static abstract class CheckTask extends ChangelogTask {
		public static final String NAME = "changelogCheck";

		@Internal
		abstract Property<TaskOrderingService> getTaskOrderingService();

		@Inject
		public CheckTask(ChangelogExtension extension) {
			super(extension);
			setDescription("checks that the changelog is formatted according to your rules");
		}

		@TaskAction
		public void check() throws IOException, GitAPIException {
			boolean pushWillRun = getTaskOrderingService().get().pushWillRun(this);
			if (pushWillRun) {
				// if we're going to push later, let's first make sure that will work before we bump and publish
				GitActions git = data.gitCfg.withChangelog(data.changelogFile, data.model());
				git.checkWcClean();
				git.assertNoTag();
				git.checkCanPush();
			}

			LinkedHashMap<Integer, String> errors = data.model().changelog().errors();
			if (errors.isEmpty()) {
				return;
			}

			String path = data.projectRoot.toPath().relativize(data.changelogFile.toPath()).toString();
			String allErrors = StringPrinter.buildString(printer -> errors.forEach((idx, error) -> {
				if (idx == -1) {
					printer.println(path + ": " + error);
				} else {
					printer.println(path + ":" + idx + ": " + error);
				}
			}));
			throw new GradleException(allErrors);
		}
	}

	/** `changelogPrint` - prints the last published version and calculated next version, e.g. `myproj 1.0.4 -> 1.1.0` */
	public static abstract class PrintTask extends ChangelogTask {
		public static final String NAME = "changelogPrint";

		@Inject
		public PrintTask(ChangelogExtension extension) {
			super(extension);
			setDescription("prints the last published version and the calculated next version, e.g. `myproj 1.0.4 -> 1.1.0`");
		}

		@TaskAction
		public void print() {
			if (data.getVersionNext().equals(data.getVersionLast())) {
				System.out.println(data.projectName + " " + data.getVersionLast() + " (no unreleased changes)");
			} else {
				System.out.println(data.projectName + " " + data.getVersionLast() + " -> " + data.getVersionNext());
			}
		}
	}

	/** `changelogBump` - updates the changelog on disk with the next version and the current UTC date */
	public static abstract class BumpTask extends ChangelogTask {
		public static final String NAME = "changelogBump";

		@Inject
		public BumpTask(ChangelogExtension extension) {
			super(extension);
			setDescription("updates the changelog on disk with the next version and the current UTC date");
		}

		@TaskAction
		public void bump() throws IOException {
			assertNotSnapshot();
			if (data.getVersionNext().equals(data.getVersionLast())) {
				// if there are no unreleased changes, then the changelog on disk has already been bumped
				return;
			}
			// time to bump!
			ChangelogAndNext model = data.model();
			LocalDate localDate = LocalDate.now(Time.clockUtc());
			Changelog bumped = model.changelog().releaseUnreleased(model.versions().next(), localDate.toString());
			Files.write(data.changelogFile.toPath(), bumped.toString().getBytes(StandardCharsets.UTF_8));
		}
	}

	/** `changelogPush` - commits the changelog, tags, and pushes */
	public static abstract class PushTask extends ChangelogTask {
		public static final String NAME = "changelogPush";

		@Inject
		public PushTask(ChangelogExtension extension) {
			super(extension);
			setDescription("commits the bumped changelog, tags it, and pushes (recommend wiring to your publish task)");
		}

		@TaskAction
		public void push() throws IOException, GitAPIException {
			assertNotSnapshot();
			GitActions git = data.gitCfg.withChangelog(data.changelogFile, data.model());
			git.addAndCommit();
			git.tagBranchPush();
		}
	}
}
