package com.vns.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;

public class ExternalExecuter {
    private List<String> args = new ArrayList();
    private Map<String, Object> subsMap = new HashMap();
    private String executable;
    private LogOutputStream logOutputStream;
    private LogOutputStream logErrorStream;
    private int exitCode = 0;
    private long waiting = 60000L;
    
    public ExternalExecuter() {
    }
    
    public ExternalExecuter setWaiting(long waiting) {
        this.waiting = waiting;
        return this;
    }
    
    public ExternalExecuter addArg(String arg) {
        args.add(arg);
        return this;
    }
    
    /**
     * CommandLine cmdLine = new CommandLine("AcroRd32.exe");
     * cmdLine.addArgument("${file}");
     * HashMap map = new HashMap();
     * map.put("file", new File("invoice.pdf"));
     * commandLine.setSubstitutionMap(map);
     *
     * @param key String
     * @param val Object
     * @return ExternalExecuter
     */
    public ExternalExecuter addSubstitution(String key, Object val) {
        if (val != null) {
            this.subsMap.put(key, val);
        }
        
        return this;
    }
    
    public ExternalExecuter setExecutable(String executable) {
        this.executable = executable;
        return this;
    }
    
    public ExternalExecuter setLogOutput(LogOutputStream logOutputStream) {
        this.logOutputStream = logOutputStream;
        return this;
    }
    
    public ExternalExecuter setErrorOutput(LogOutputStream logErrorStream) {
        this.logErrorStream = logErrorStream;
        return this;
    }
    
    public ExternalExecuter setExitCode(int exitCode) {
        this.exitCode = exitCode;
        return this;
    }
    
    public int execute() throws IOException {
        return this.createExecutor().execute(this.createCommandLine());
    }
    
    public ExecuteResultHandler asyncExecute(ExecuteResultHandler executeResultHandler) throws IOException {
        Object resultHandler = executeResultHandler == null ? new DefaultExecuteResultHandler() : executeResultHandler;
        this.createExecutor().execute(this.createCommandLine(), (ExecuteResultHandler) resultHandler);
        return (ExecuteResultHandler) resultHandler;
    }
    
    public ExecuteResultHandler asyncExecute() throws IOException {
        return this.asyncExecute(null);
    }
    
    private Executor createExecutor() throws IOException {
        ExecuteWatchdog watchdog = new ExecuteWatchdog(this.waiting);
        DefaultExecutor executor = new DefaultExecutor();
        if (this.logOutputStream != null && this.logErrorStream != null) {
            executor.setStreamHandler(new PumpStreamHandler(this.logOutputStream, this.logErrorStream));
        } else if (this.logOutputStream != null) {
            executor.setStreamHandler(new PumpStreamHandler(this.logOutputStream));
        } else if (this.logErrorStream != null) {
            executor.setStreamHandler(new PumpStreamHandler(null, this.logErrorStream));
        }
        executor.setExitValue(this.exitCode);
        executor.setWatchdog(watchdog);
        return executor;
    }
    
    private CommandLine createCommandLine() throws IOException {
        CommandLine cmdLine = new CommandLine(this.executable);
        
        for (String arg : args) {
            cmdLine.addArgument(arg);
        }
        
        if (!subsMap.isEmpty()) {
            cmdLine.setSubstitutionMap(subsMap);
        }
        
        return cmdLine;
    }
}