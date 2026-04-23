/**
 * semantic-release configuration for ISP 2026 Bookstore
 *
 * Commit convention → version bump:
 *   feat:  ...             → minor   (1.0.0 → 1.1.0)
 *   fix:  / perf: / refactor: → patch (1.0.0 → 1.0.1)
 *   feat!: ... / BREAKING CHANGE: → major (1.0.0 → 2.0.0)
 *   docs: / chore: / test: / ci: / build: / style: → no release
 *
 * Branches:
 *   main     → stable releases  (vX.Y.Z)
 *   develop  → beta prereleases (vX.Y.Z-beta.N, channel "beta")
 */

module.exports = {
  branches: [
    'main',
    {
      name: 'develop',
      prerelease: 'beta',
      channel: 'beta'
    }
  ],

  plugins: [
    [
      '@semantic-release/commit-analyzer',
      {
        preset: 'conventionalcommits',
        releaseRules: [
          // Promotion PRs (develop → main) are titled `chore(release): ...`
          // or `chore(promote): ...`. They arrive on main as a single
          // squash-merged chore commit, so the scope — not the type — is
          // what has to trigger the release.
          { type: 'chore', scope: 'sync', release: false },
          { type: 'chore', scope: 'release', release: 'minor' },
          { type: 'chore', scope: 'promote', release: 'minor' },
          { type: 'feat', release: 'minor' },
          { type: 'fix', release: 'patch' },
          { type: 'perf', release: 'patch' },
          { type: 'refactor', release: 'patch' },
          { type: 'revert', release: 'patch' },
          { type: 'docs', release: false },
          { type: 'style', release: false },
          { type: 'chore', release: false },
          { type: 'test', release: false },
          { type: 'build', release: false },
          { type: 'ci', release: false },
          { scope: 'no-release', release: false }
        ],
        parserOpts: {
          noteKeywords: ['BREAKING CHANGE', 'BREAKING CHANGES', 'BREAKING']
        }
      }
    ],

    [
      '@semantic-release/release-notes-generator',
      {
        preset: 'conventionalcommits',
        presetConfig: {
          types: [
            { type: 'feat', section: '✨ Features' },
            { type: 'fix', section: '🐛 Bug Fixes' },
            { type: 'perf', section: '⚡ Performance Improvements' },
            { type: 'refactor', section: '♻️ Code Refactoring' },
            { type: 'revert', section: '⏪ Reverts' },
            { type: 'docs', section: '📚 Documentation', hidden: false },
            { type: 'style', section: '💎 Styles', hidden: true },
            { type: 'chore', section: '🔧 Miscellaneous Chores', hidden: true },
            { type: 'test', section: '✅ Tests', hidden: true },
            { type: 'build', section: '🏗️ Build System', hidden: true },
            { type: 'ci', section: '👷 CI/CD', hidden: true }
          ]
        }
      }
    ],

    [
      '@semantic-release/changelog',
      {
        changelogFile: 'CHANGELOG.md',
        changelogTitle:
          '# Changelog\n\nAll notable changes to the ISP 2026 Bookstore will be documented in this file.\n\n' +
          'The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),\n' +
          'and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).'
      }
    ],

    // Bump <version> in pom.xml so the JAR built into the image carries the new version.
    [
      '@semantic-release/exec',
      {
        prepareCmd:
          'mvn versions:set -DnewVersion=${nextRelease.version} -DgenerateBackupPoms=false'
      }
    ],

    [
      '@semantic-release/github',
      {
        successComment: false,
        releasedLabels: ['released'],
        addReleases: 'bottom'
      }
    ],

    [
      '@semantic-release/git',
      {
        assets: ['CHANGELOG.md', 'pom.xml'],
        message:
          'chore(release): ${nextRelease.version} [skip ci]\n\n${nextRelease.notes}'
      }
    ]
  ]
};
