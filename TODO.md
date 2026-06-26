# TODO

## Maven Central publishing / Maven Central 发布

Current status: GitHub Packages is the active publishing target. Maven Central
publishing is intentionally deferred.

当前状态：GitHub Packages 是当前发布目标。Maven Central 发布暂时延后。

Before starting Maven Central publishing:

正式开始 Maven Central 发布前，需要补齐：

- verify Sonatype Central Portal namespace ownership for `io.github.oayilix`;
- generate and store Central Portal publishing credentials;
- generate and store in-memory PGP signing secrets for CI;
- choose the Gradle publishing path, preferably a Central Portal-compatible plugin;
- add a Maven Central-specific release workflow, separate from GitHub Packages;
- add version/tag guards that reject `SNAPSHOT` releases for Maven Central;
- validate signed artifacts, sources JARs, javadoc JARs, POM metadata, and checksums;
- verify a post-release consumer can resolve artifacts from Maven Central;
- optionally add API/binary compatibility validation before the first central release.
