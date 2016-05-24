// Copyright � 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package steve.test.grass;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class ProcessRunner {

    private final ProcessBuilder builder;

    private Process process;
    private Thread shutdownHook;
    private final MulticastPipe outputMulticast = new MulticastPipe();
    private Writer input;

    public ProcessRunner(File workingDir, String... command) {
        builder = new ProcessBuilder(command);
        builder.directory(workingDir);
        builder.redirectErrorStream(true);
        
    }
    
    public void addEnvironmentVariable(String varName, String value) {
    	builder.environment().put(varName, value);
    }
    
    public ProcessBuilder getProcessBuilder() {
    	return builder;
    }

    public OutputReader subscribeToOutput() {
        return new OutputReader(outputMulticast.subscribe());
    }

    public void start() throws IOException {
        process = builder.start();
        shutdownHook = new Thread(new DestroyProcessRunner(process));

        InputStreamReader output = new InputStreamReader(new BufferedInputStream(process.getInputStream()));
        Thread t = new Thread(new ReaderToWriterCopier(output, outputMulticast));
        t.setDaemon(true);
        t.start();

        input = new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream()));
    }

    public void destroyOnShutdown() {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public void destroy() {
        process.destroy();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }

    public boolean isAlive() {
        if (process == null) {
            return false;
        }
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    public void writeInput(String s) throws IOException {
        input.write(s);
        input.flush();
    }


    private static class DestroyProcessRunner implements Runnable {
        private final Process process;

        public DestroyProcessRunner(Process process) {
            this.process = process;
        }

        public void run() {
            process.destroy();
        }
    }
}