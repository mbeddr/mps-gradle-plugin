# Releases and snapshots

Each project keeps its version in its own `gradle.properties`:

| Project | Version file | Release tag example |
| --- | --- | --- |
| mps-gradle-plugin | `gradle.properties` | `mps-gradle-plugin-1.31.0` |
| git-based-versioning | `git-based-versioning/gradle.properties` | `git-based-versioning-1.1.1` |

To release a project:

1. Set its version to `MAJOR.MINOR.PATCH` without `-SNAPSHOT`, update its changelog, and commit the changes.
2. Push a tag named `<project>-<version>` pointing to that commit. For example:

   ```shell
   git tag mps-gradle-plugin-1.31.0
   git push origin mps-gradle-plugin-1.31.0
   ```

The release workflow checks that the tag matches the selected project's version, runs its build and checks, publishes
its Maven artifacts to itemis and GitHub Packages, and creates a GitHub Release. A release of one project does not
publish the other project. The main plugin depends on `git-based-versioning`; release that library first if its version
has changed. Version bumps after a release are manual, for example to `1.31.1-SNAPSHOT`.

Successful builds on `v1.x` and `master` publish projects whose versions end in `-SNAPSHOT` to
`https://artifacts.itemis.cloud/repository/maven-mps-snapshots/` only. Versions without `-SNAPSHOT` are skipped on branch
pushes. Pull requests do not publish artifacts. Release artifacts go to
`https://artifacts.itemis.cloud/repository/maven-mps-releases/` and
`https://maven.pkg.github.com/mbeddr/mps-gradle-plugin`.

Publishing uses the organization secrets `ARTIFACTS_ITEMIS_CLOUD_USER` and `ARTIFACTS_ITEMIS_CLOUD_PW`, which must be
available to this repository. GitHub Packages and GitHub Releases use the workflow's `GITHUB_TOKEN` with
`packages: write` and `contents: write` permissions. For local publishing, the Gradle properties
`artifacts.itemis.cloud.user`, `artifacts.itemis.cloud.pw`, `gpr.user`, and `gpr.token` are also supported.
