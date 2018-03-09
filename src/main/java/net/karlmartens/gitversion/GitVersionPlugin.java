package net.karlmartens.gitversion;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitVersionPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.allprojects(p -> {
            boolean isRelease = false;
            String[] v = gitVersion(p);
            if (v != null) {
                isRelease = v[2].equals("0");
                p.setVersion(v[0] + (isRelease ? "" : "." + v[2]) + v[1] + "-g" + v[3]);
            }

            p.getExtensions().getExtraProperties().set("isRelease", isRelease);
        });
    }

    private static String[] gitVersion(Project project) {
        String executable = gitExecutable();

        List<String> args = new ArrayList<>();
        args.add("describe");
        args.add("--tags");
        args.add("--long");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        project.exec(execSpec -> {
           execSpec.setExecutable(executable);
           execSpec.setArgs(args);
           execSpec.setStandardOutput(out);
        });


        Pattern pattern = Pattern.compile("^([0-9]+\\.[0-9]+\\.[0-9]+)(.*)-([0-9]+)-g([0-9a-f]{7})\\r?\\n?$");
        Matcher matcher = pattern.matcher(out.toString());
        if (matcher.matches()) {
            return new String[] { matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4) };
        }

        return null;
    }

    private static String gitExecutable() {
        String executable = "git";
        String gitHome = System.getenv("GIT_HOME");
        if (gitHome == null || gitHome.trim().isEmpty())
            return executable;

        Path path = Paths.get(gitHome, "bin", executable);
        return path.toString();
    }
}
