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


import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.TaskProvider;

public class ChangelogPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		// make sure there is a check task
		project.getPlugins().apply(BasePlugin.class);

		ChangelogExtension extension = project.getExtensions().create(ChangelogExtension.NAME, ChangelogExtension.class, project);
		TaskProvider<CheckTask> check = project.getTasks().register(CheckTask.NAME, CheckTask.class, extension.nextVersionCfg);
		project.afterEvaluate(unused -> {
			if (extension.enforceCheck) {
				project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(t -> t.dependsOn(check));
			}
		});
	}

	public static abstract class ChangelogTask extends DefaultTask {
		protected final ChangelogExtension cfg;

		protected ChangelogTask(ChangelogExtension cfg) {
			this.cfg = cfg;
		}
	}

	public static abstract class CheckTask extends ChangelogTask {
		public static final String NAME = "changelogCheck";

		@Inject
		public CheckTask(ChangelogExtension cfg) {
			super(cfg);
		}
	}
}
