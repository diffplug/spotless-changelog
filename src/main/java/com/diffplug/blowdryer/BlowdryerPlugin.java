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
package com.diffplug.blowdryer;


import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class BlowdryerPlugin implements Plugin<Project> {
	static final String PLUGIN_ID = "com.diffplug.blowdryer";

	@Override
	public void apply(Project project) {
		if (project.getRootProject() != project) {
			throw new IllegalArgumentException("You must apply " + PLUGIN_ID + " only on the root project, not " + project.getPath());
		}
		Blowdryer.setResourcePluginNull(); // because of gradle daemon
		project.getExtensions().create(BlowdryerExtension.NAME, BlowdryerExtension.class, project);

		project.getTasks().register("blowdryerWipeEntireCache", task -> {
			task.doFirst(unused -> Blowdryer.wipeEntireCache());
		});
	}
}
