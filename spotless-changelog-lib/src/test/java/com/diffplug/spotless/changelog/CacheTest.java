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
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CacheTest extends ResourceHarness {
	private File changelogFile;

	@Test
	public void testCache() throws IOException {
		changelogFile = write("CHANGELOG.md",
				"",
				"## [Unreleased]",
				"### Added",
				"",
				"## [1.2.3] - 2020-01-01",
				"");
		// responds to changes in forceNextVersion
		CfgNextVersion cfg = new CfgNextVersion();
		cfg.forceNextVersion = "5.6.7";
		assertNextLast(cfg, "5.6.7", "1.2.3");

		cfg.forceNextVersion = "8.9";
		ChangelogModel at89 = assertNextLast(cfg, "8.9", "1.2.3");

		// caching is happening
		ChangelogModel at89_2 = assertNextLast(cfg, "8.9", "1.2.3");
		Assertions.assertThat(at89).isNotSameAs(at89_2);
		Assertions.assertThat(at89.changelog()).isNotSameAs(at89_2.changelog());
		Assertions.assertThat(at89.versions()).isSameAs(at89_2.versions());

		// and for regular value caching
		cfg.forceNextVersion = null;
		ChangelogModel at130 = assertNextLast(cfg, "1.3.0", "1.2.3");

		// caching is happening
		ChangelogModel at130_2 = assertNextLast(cfg, "1.3.0", "1.2.3");
		Assertions.assertThat(at130).isNotSameAs(at130_2);
		Assertions.assertThat(at130.changelog()).isNotSameAs(at130_2.changelog());
		Assertions.assertThat(at130.versions()).isSameAs(at130_2.versions());

		// and it responds to bumps
		cfg.next.ifFoundBumpBreaking("### Added");
		ChangelogModel at200 = assertNextLast(cfg, "2.0.0", "1.2.3");

		// and gets cached
		ChangelogModel at200_2 = assertNextLast(cfg, "2.0.0", "1.2.3");
		Assertions.assertThat(at200).isNotSameAs(at200_2);
		Assertions.assertThat(at200.changelog()).isNotSameAs(at200_2.changelog());
		Assertions.assertThat(at200.versions()).isSameAs(at200_2.versions());
	}

	private ChangelogModel assertNextLast(CfgNextVersion cfg, String next, String last) throws IOException {
		ChangelogModel model = ChangelogModel.calculateUsingCache(changelogFile, cfg);
		Assertions.assertThat(model.versions().next()).isEqualTo(next);
		Assertions.assertThat(model.versions().last()).isEqualTo(last);
		return model;
	}
}
