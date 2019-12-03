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


import com.diffplug.common.base.Errors;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import org.gradle.api.Project;

public class BlowdryerExtension {
	static final String NAME = "blowdryer";

	private final Project project;

	public BlowdryerExtension(Project project) {
		this.project = Objects.requireNonNull(project);
	}

	private String repoSubfolder = "src/main/resources";

	/**
	 * Default value is `src/main/resources`.  If you change, you must change as the *first* call.
	 * 
	 * The nice thing about the default `src/main/resources` is that if you ever want to, you could
	 * copy the blowdryer code into your blowdryer repo, and deploy your own plugin that pulls resources
	 * from the local jar rather than from github.  Keeping the default lets you switch to that approach
	 * in the future without moving your scripts.
	 */
	public void repoSubfolder(String repoSubfolder) {
		Blowdryer.assertPluginNotSet("You have to call `repoSubfolder` first.");
		this.repoSubfolder = assertNoLeadingOrTrailingSlash(repoSubfolder);
	}

	public enum GitAnchorType {
		TAG, COMMIT, TREE
	}

	/** Sets the source where we will grab these scripts. */
	public void github(String repoOrg, GitAnchorType anchorType, String anchor) {
		assertNoLeadingOrTrailingSlash(repoOrg);
		assertNoLeadingOrTrailingSlash(anchor);
		String root = "https://raw.githubusercontent.com/" + repoOrg + "/" + anchor + "/" + repoSubfolder + "/";
		Blowdryer.setResourcePlugin(resource -> root + resource);
	}

	/** Sets the source to be the given local folder, usually for developing changes before they are pushed to git. */
	public void devLocal(Object devPath) {
		File projectRoot = Errors.rethrow().get(() -> project.file(devPath).getCanonicalFile());
		File resourceRoot = new File(projectRoot, repoSubfolder);
		Blowdryer.setResourcePlugin(new Blowdryer.DevPlugin(resourceRoot));
	}

	private static String assertNoLeadingOrTrailingSlash(String input) {
		Objects.requireNonNull(input);
		if (input.isEmpty()) {
			return input;
		}
		if (input.charAt(0) == '/') {
			throw new IllegalArgumentException("Remove the leading slash");
		}
		if (input.charAt(input.length() - 1) == '/') {
			throw new IllegalArgumentException("Remove the trailing slash");
		}
		return input;
	}

	public void applyFrom(String... scripts) {
		applyFrom(Arrays.asList(scripts));
	}

	public void applyFrom(Collection<String> scripts) {
		for (String script : scripts) {
			project.apply(cfg -> {
				cfg.from(Blowdryer.file(script));
			});
		}
	}
}
