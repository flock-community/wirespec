task<Exec>("npmInstall") {
    commandLine("npm", "install")
}

task<Exec>("npmRunBuild") {
    commandLine("npm", "run", "build")
}