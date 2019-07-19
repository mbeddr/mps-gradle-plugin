package de.itemis.mps.gradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException

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

    static String getVersionWithCount(String major, String minor, int count) {
        getVersion(getGitBranch(), major, minor, count)
    }

    static String getVersion(String branch, String major, String minor, int count) {
        def hash = getGitShortCommitHash()
        def baseVersion = "$major.$minor.$count.$hash"
        if (branch == 'master' || branch == 'HEAD' /*this happens in detached head situations*/) {
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
    static String getVersionWithoutMaintenancePrefix(String major, String minor) {
        def branch = getGitBranch()
        if (branch.startsWith("maintenance") || branch.startsWith("mps")) {
            getVersion("HEAD", major, minor)
        } else {
            getVersion(major, minor)
        }
    }
}
