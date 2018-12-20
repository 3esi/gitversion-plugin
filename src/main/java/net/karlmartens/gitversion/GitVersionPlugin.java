package net.karlmartens.gitversion;

import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NonNullApi
public class GitVersionPlugin implements Plugin<Project> {

    private static final BranchInfo DEFAULT_BRANCH = new BranchInfo("");


    @Override
    public void apply(Project project) {
        project.allprojects(p -> {
            final Optional<VersionInfo> versionInfo = gitVersion(p);

            Optional<String> version = versionInfo.map(v -> {
                final BranchInfo branchInfo = gitBranch(p).orElse(DEFAULT_BRANCH);
                return versionString(v, branchInfo);
            });
            version.ifPresent(p::setVersion);

            boolean isRelease = versionInfo.map(v -> v.isRelease).orElse(false);
            p.getExtensions().getExtraProperties().set("isRelease", isRelease);
        });
    }

    private static Optional<BranchInfo> gitBranch(Project project) {
        String out = exec(project, gitExecutable(), "branch");
        return BranchInfo.fromString(out);
    }

    static String versionString(VersionInfo versionInfo, BranchInfo branchInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(versionInfo.version);

        if (!versionInfo.isRelease)
            sb.append(".").append(versionInfo.build);

        sb.append(versionInfo.qualifier);

        if (branchInfo.isFeature)
            sb.append("-f").append(branchInfo.shortName());

        sb.append("-g").append(versionInfo.commit);

        return sb.toString();
    }

    private static Optional<VersionInfo> gitVersion(Project project) {
        String out = exec(project, gitExecutable(), "describe", "--tags", "--long");
        return VersionInfo.fromString(out);
    }

    private static String exec(Project project, String executable, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        project.exec(execSpec -> {
            execSpec.setExecutable(executable);
            execSpec.setArgs(Arrays.asList(args));
            execSpec.setStandardOutput(out);
        });

        return out.toString();
    }

    private static String gitExecutable() {
        String executable = "git";
        String gitHome = System.getenv("GIT_HOME");
        if (gitHome == null || gitHome.trim().isEmpty())
            return executable;

        Path path = Paths.get(gitHome, "bin", executable);
        return path.toString();
    }

    static class VersionInfo {

        private final String version;
        private final String build;
        private final String qualifier;
        private final String commit;
        private final boolean isRelease;

        VersionInfo(String version, String build, String qualifier, String commit) {
            this.version = version;
            this.build = build;
            this.qualifier = qualifier;
            this.commit = commit;
            this.isRelease = "0".equals(build);
        }

        public static Optional<VersionInfo> fromString(String out) {
            Pattern pattern = Pattern.compile("^([0-9]+\\.[0-9]+\\.[0-9]+)(.*)-([0-9]+)-g([0-9a-f]{7})\\r?\\n?$");
            Matcher matcher = pattern.matcher(out);
            if (matcher.matches()) {
                VersionInfo info = new VersionInfo(matcher.group(1), matcher.group(3), matcher.group(2), matcher.group(4));
                return Optional.of(info);
            }

            return Optional.empty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VersionInfo that = (VersionInfo) o;
            return isRelease == that.isRelease &&
                    Objects.equals(version, that.version) &&
                    Objects.equals(build, that.build) &&
                    Objects.equals(qualifier, that.qualifier) &&
                    Objects.equals(commit, that.commit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(version, build, qualifier, commit, isRelease);
        }

        @Override
        public String toString() {
            return "VersionInfo{" +
                    "version='" + version + '\'' +
                    ", build='" + build + '\'' +
                    ", qualifier='" + qualifier + '\'' +
                    ", commit='" + commit + '\'' +
                    ", isRelease=" + isRelease +
                    '}';
        }
    }

    static class BranchInfo {

        private final String name;
        private final boolean isFeature;

        BranchInfo(String name) {
            this.name = name;
            this.isFeature = name.startsWith("feature-");
        }

        static Optional<BranchInfo> fromString(String out) {
            BufferedReader reader = new BufferedReader(new StringReader(out));
            return reader.lines()
                    .filter(s -> s.startsWith("*"))
                    .findFirst()
                    .map(s -> s.substring(1).trim())
                    .map(BranchInfo::new);
        };

        private String shortName() {
            if (!isFeature)
                return name;

            return name.substring(8);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BranchInfo that = (BranchInfo) o;
            return isFeature == that.isFeature &&
                    Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, isFeature);
        }

        @Override
        public String toString() {
            return "BranchInfo{" +
                    "name='" + name + '\'' +
                    ", isFeature=" + isFeature +
                    '}';
        }
    }
}
