package de.itemis.mps.gradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException

@SuppressWarnings("unused")
class GitBasedVersioning {


    /**
     * Checks whenever git based versioning is available.
     * */
    static boolean isGitVersioningAvailable() {
        try {
            String output = getCommandOutput("git --version")
            return output.startsWith("git version 2")
        } catch (ignored) {
            return false
        }
    }

    static String getGitShortCommitHash() {
        return getCommandOutput('git rev-parse --short HEAD').substring(0,7)
    }

    static String getGitCommitHash() {
        return getCommandOutput('git rev-parse HEAD')
    }

    static int getGitCommitCount() {
        return getCommandOutput('git rev-list --count HEAD').toInteger()
    }

    /**
     * Gets the current Git branch either from TC env parameter (for CI builds) or from git rev-parse command (for commandline builds)
     * with slashes ('/') replaced by dashes ('-'). If the branch name cannot be determined, throws GradleException.
     * Never empty, never null.
     *
     * @return the current branch name with slashes ('/') replaced by dashes ('-')
     * @throws org.gradle.api.GradleException if the branch name cannot be determined
     */
    static String getGitBranch() throws GradleException {
        String gitBranch
        String gitBranchTC = System.getenv('teamcity_build_branch')
        if (gitBranchTC != null && !gitBranchTC.empty) {
            gitBranch = gitBranchTC
            println "Branch From TeamCity: " + gitBranch
        } else {
            gitBranch = getCommandOutput('git rev-parse --abbrev-ref HEAD')
            println "Branch From Git Commandline: " + gitBranch
        }

        if (gitBranch == null || gitBranch.empty) {
            throw new GradleException('Could not determine Git branch name')
        }
        return gitBranch.replace("/", "-")
    }

    private static String getCommandOutput(String command) {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            command = 'cmd /c ' + command
        }
        return command.execute().in.text.trim()
    }

    static String getVersion(String major, String minor) {
        getVersion(getGitBranch(), major, minor, getGitCommitCount())
    }

    static String getVersion(int major, int minor) {
        getVersion(getGitBranch(), major.toString(), minor.toString(), getGitCommitCount())
    }

    static String getVersion(String branch, String major, String minor) {
        getVersion(branch, major, minor, getGitCommitCount())
    }

    static String getVersionWithCount(String major, String minor, Integer count) {
        getVersion(getGitBranch(), major, minor, count)
    }

    static String getVersionWithBugfixAndCount(String major, String minor, String bugfix, Integer count) {
        getVersion(getGitBranch(), major, minor, bugfix, count)
    }

    static String getVersion(String branch, String major, String minor, String bugfix = "", Integer count) {
        def hash = getGitShortCommitHash()
        def countStr = count != null ? ".$count": ""
        def baseVersion = bugfix.isEmpty() ? "$major.$minor${countStr}.$hash" : "$major.$minor.$bugfix${countStr}.$hash"
        if (branch == 'master' || branch == 'main' || branch == 'HEAD' /*this happens in detached head situations*/) {
            return baseVersion
        }

        return "$branch.$baseVersion"
    }

    /**
     * Convenience method for creating version without maintenance branch prefix (i.e. if branch starts with 'maintenance' or 'mps')
     *
     * @param major
     * @param minor
     * @return
     */
    static String getVersionWithoutMaintenancePrefix(String major, String minor, Integer count = null) {
        def branch = getGitBranch()
        if (branch.startsWith("maintenance") || branch.startsWith("mps")) {
            getVersion("HEAD", major, minor, count != null? count: getGitCommitCount())
        } else {
            getVersion(getGitBranch(), major, minor, count != null? count: getGitCommitCount())
        }
    }
}
