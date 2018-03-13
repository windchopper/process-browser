package com.github.windchopper.tools.process.browser;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ProcessHandleRepresentative {

    private final long pid;
    private final long parendPid;
    private final String name;
    private final String command;

    public ProcessHandleRepresentative(ProcessHandle processHandle) {
        pid = processHandle.pid();
        parendPid = processHandle.parent()
            .map(ProcessHandle::pid)
            .orElse(-1L);
        name = processHandle.info().command()
            .map(Paths::get)
            .map(Path::getFileName)
            .map(Object::toString)
            .orElse("?");
        command = processHandle.info().command()
            .orElse("?");
    }

    public long pid() {
        return pid;
    }

    public long parendPid() {
        return parendPid;
    }

    public String name() {
        return name;
    }

    public String command() {
        return command;
    }

    public void destroyForcibly() {
        ProcessHandle.of(pid)
            .ifPresent(ProcessHandle::destroyForcibly);
    }

}
