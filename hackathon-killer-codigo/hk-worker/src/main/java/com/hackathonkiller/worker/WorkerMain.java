package com.hackathonkiller.worker;

import com.hackathonkiller.worker.cli.WorkerCommand;
import picocli.CommandLine;

public class WorkerMain {

    public static void main(String[] args) {
        System.exit(new CommandLine(new WorkerCommand()).execute(args));
    }
}
