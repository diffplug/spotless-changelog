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


import com.diffplug.spotless.changelog.ChangelogModel.NextVersionCfg;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class NextVersionTest {
	private AbstractStringAssert<?> test(String changelog) {
		return test(new NextVersionCfg(), changelog);
	}

	private AbstractStringAssert<?> test(NextVersionCfg cfg, String changelog) {
		ChangelogModel result = ChangelogModel.calculate(changelog, cfg);
		return Assertions.assertThat(result.versionNext());
	}

	@Test
	public void initialRelease() {
		test("").isEqualTo("0.1.0");
		test("\n## [Unreleased]").isEqualTo("0.1.0");
		test("\n## [Unreleased]\n### Added\n").isEqualTo("0.1.0");
		test("\n## [Unreleased]\n**BREAKING**\n").isEqualTo("0.1.0");
		test("\n## [Unreleased]\n### Added\n**BREAKING**\n").isEqualTo("0.1.0");
		test("\n## [Unreleased]\nSome change\n### Added\n").isEqualTo("0.1.0");
		test("\n## [Unreleased]\nSome change\n**BREAKING**\n").isEqualTo("0.1.0");
		test("\n## [Unreleased]\nSome change\n### Added\n**BREAKING**\n").isEqualTo("0.1.0");
	}

	@Test
	public void ohDotVersions() {
		test("\n## [Unreleased]\nSome change\n## [0.2.0] - 2020-10-10").isEqualTo("0.2.1");
		test("\n## [Unreleased]\nSome change\n## [0.2.5] - 2020-10-10").isEqualTo("0.2.6");
		test("\n## [Unreleased]\nSome change\n### Added\n## [0.2.0] - 2020-10-10\n").isEqualTo("0.3.0");
		test("\n## [Unreleased]\nSome change\n### Added\n## [0.2.5] - 2020-10-10\n").isEqualTo("0.3.0");
		test("\n## [Unreleased]\nSome change\n**BREAKING**\n## [0.2.0] - 2020-10-10\n").isEqualTo("0.3.0");
		test("\n## [Unreleased]\nSome change\n**BREAKING**\n## [0.2.5] - 2020-10-10\n").isEqualTo("0.3.0");
		test("\n## [Unreleased]\nSome change\n### Added\n**BREAKING**\n## [0.2.0] - 2020-10-10\n").isEqualTo("0.3.0");
		test("\n## [Unreleased]\nSome change\n### Added\n**BREAKING**\n## [0.2.5] - 2020-10-10\n").isEqualTo("0.3.0");
	}

	@Test
	public void standardVersions() {
		test("\n## [Unreleased]\nSome change\n## [1.2.0] - 2020-10-10\n").isEqualTo("1.2.1");
		test("\n## [Unreleased]\nSome change\n## [1.2.5] - 2020-10-10\n").isEqualTo("1.2.6");
		test("\n## [Unreleased]\nSome change\n### Added\n## [1.2.0] - 2020-10-10\n").isEqualTo("1.3.0");
		test("\n## [Unreleased]\nSome change\n### Added\n## [1.2.5] - 2020-10-10\n").isEqualTo("1.3.0");
		test("\n## [Unreleased]\nSome change\n**BREAKING**\n## [1.2.0] - 2020-10-10\n").isEqualTo("2.0.0");
		test("\n## [Unreleased]\nSome change\n**BREAKING**\n## [1.2.5] - 2020-10-10\n").isEqualTo("2.0.0");
		test("\n## [Unreleased]\nSome change\n### Added\n**BREAKING**\n## [1.2.0] - 2020-10-10\n").isEqualTo("2.0.0");
		test("\n## [Unreleased]\nSome change\n### Added\n**BREAKING**\n## [1.2.5] - 2020-10-10\n").isEqualTo("2.0.0");
	}

	@Test
	public void forceVersions() {
		NextVersionCfg cfg = new NextVersionCfg();
		cfg.forceNextVersion = "shoopty";
		test(cfg, "\n## [Unreleased]\nSome change\n## [1.2.0] - 2020-10-10\n").isEqualTo("shoopty");
		test(cfg, "\n## [Unreleased]\nSome change\n## [1.2.5] - 2020-10-10\n").isEqualTo("shoopty");
		test(cfg, "\n## [Unreleased]\nSome change\n### Added\n## [1.2.0] - 2020-10-10\n").isEqualTo("shoopty");
		test(cfg, "\n## [Unreleased]\nSome change\n### Added\n## [1.2.5] - 2020-10-10\n").isEqualTo("shoopty");
		test(cfg, "\n## [Unreleased]\nSome change\n**BREAKING**\n## [1.2.0] - 2020-10-10\n").isEqualTo("shoopty");
		test(cfg, "\n## [Unreleased]\nSome change\n**BREAKING**\n## [1.2.5] - 2020-10-10\n").isEqualTo("shoopty");
		test(cfg, "\n## [Unreleased]\nSome change\n### Added\n**BREAKING**\n## [1.2.0] - 2020-10-10\n").isEqualTo("shoopty");
		test(cfg, "\n## [Unreleased]\nSome change\n### Added\n**BREAKING**\n## [1.2.5] - 2020-10-10\n").isEqualTo("shoopty");
	}
}
