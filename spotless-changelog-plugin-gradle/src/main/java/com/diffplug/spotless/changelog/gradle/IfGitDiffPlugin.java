/*
 * Copyright (C) 2019-2022 DiffPlug
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


import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.ExtensionAware;

/** @see IfGitDiffExtension */
public class IfGitDiffPlugin implements Plugin<ExtensionAware> {
	@Override
	public void apply(ExtensionAware projectOrSettings) {
		if (projectOrSettings instanceof Project) {
			Project project = (Project) projectOrSettings;
			projectOrSettings.getExtensions().create(IfGitDiffExtension.NAME, IfGitDiffExtension.ForProject.class, project);
		} else if (projectOrSettings instanceof Settings) {
			Settings settings = (Settings) projectOrSettings;
			projectOrSettings.getExtensions().create(IfGitDiffExtension.NAME, IfGitDiffExtension.ForSettings.class, settings);
		} else {
			throw new IllegalArgumentException("We support build.gradle and settings.gradle, this was " + projectOrSettings.getClass());
		}
	}
}
