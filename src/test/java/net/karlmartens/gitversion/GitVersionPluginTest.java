package net.karlmartens.gitversion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import net.karlmartens.gitversion.GitVersionPlugin.VersionInfo;
import net.karlmartens.gitversion.GitVersionPlugin.BranchInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

public class GitVersionPluginTest {

    @ParameterizedTest(name = "{0} = {3}")
    @MethodSource("versionData")
    public void testVersionString(String name, VersionInfo versionInfo, BranchInfo branchInfo, String expected) {
        String actual = GitVersionPlugin.versionString(versionInfo, branchInfo);
        assertEquals(expected, actual);
    }

    public static Stream<Arguments> versionData() {
        return Stream.of(
                Arguments.of("Release", new VersionInfo("0.5.0", "0", "", "7650b9"), new BranchInfo("master"), "0.5.0-g7650b9"),
                Arguments.of("Snapshot 1", new VersionInfo("0.5.0", "1", "-alpha", "7650b9"), new BranchInfo("master"), "0.5.0.1-alpha-g7650b9"),
                Arguments.of("Snapshot 130", new VersionInfo("0.5.0", "130", "-alpha", "7650b9"), new BranchInfo("master"), "0.5.0.130-alpha-g7650b9"),
                Arguments.of("Feature Branch", new VersionInfo("0.5.0", "130", "-alpha", "7650b9"), new BranchInfo("feature-auth"), "0.5.0.130-alpha-fauth-g7650b9")
        );
    }

    @ParameterizedTest(name = "S: \"{0}\"")
    @MethodSource("tagData")
    public void testVersionInfo(String tag, Optional<VersionInfo> expected) {
        Optional<VersionInfo> actual = VersionInfo.fromString(tag);
        assertEquals(expected, actual);
    }

    public static Stream<Arguments> tagData() {
        return Stream.of(
                Arguments.of("", Optional.empty()),
                Arguments.of("fatal: No names found, cannot describe anything.", Optional.empty()),
                Arguments.of("1.5.4-alpha-305-g0b65efa", Optional.of(new VersionInfo("1.5.4", "305", "-alpha", "0b65efa")))
        );
    }

    @ParameterizedTest(name = "OUT: \"{0}\"")
    @MethodSource("branchData")
    public void testBranchInfo(String out, Optional<BranchInfo> expected) {
        Optional<BranchInfo> actual = BranchInfo.fromString(out);
        assertEquals(expected, actual);
    }

    public static Stream<Arguments>  branchData() {
        return Stream.of(
                Arguments.of("", Optional.empty()),
                Arguments.of("  0.1.x\n" +
                        "  WIP\n" +
                        "* feature-auth\n" +
                        "  import-xunit\n" +
                        "  master\n", Optional.of(new BranchInfo("feature-auth"))),
                Arguments.of("  0.1.x\n" +
                        "  WIP\n" +
                        "  feature-auth\n" +
                        "  import-xunit\n" +
                        "  master\n", Optional.empty())
        );
    }
}
