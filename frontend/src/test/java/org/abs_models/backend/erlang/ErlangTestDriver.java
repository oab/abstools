/**

 * Copyright (c) 2009-2011, The HATS Consortium. All rights reserved.
 * This file is licensed under the terms of the Modified BSD License.
 */
package org.abs_models.backend.erlang;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Optional;

import com.google.common.io.Files;

import org.abs_models.ABSTest;
import org.abs_models.backend.BackendTestDriver;
import org.abs_models.backend.common.InternalBackendException;
import org.abs_models.backend.common.SemanticTests;
import org.abs_models.frontend.ast.AddAddExp;
import org.abs_models.frontend.ast.ExpressionStmt;
import org.abs_models.frontend.ast.FnApp;
import org.abs_models.frontend.ast.List;
import org.abs_models.frontend.ast.MainBlock;
import org.abs_models.frontend.ast.Model;
import org.abs_models.frontend.ast.StringLiteral;
import org.abs_models.frontend.ast.VarUse;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Assume;

public class ErlangTestDriver extends ABSTest implements BackendTestDriver {

    @Override
    public String toString() {
        return "Erlang";
    }

    // Terminate tests after 2 minutes to deal with deadlocks
    public static long test_timeout = 120000;

    public static boolean checkErlang() {
        /* TODO: Should be checked earlier instead, before configuring the JUnit suites. */
        String doAbs = System.getProperty("abs.junit.erlang");
        Assume.assumeFalse("Erlang tests disabled via -Dabs.junit.erlang", "0".equals(doAbs));
        if (SemanticTests.checkProg("erl")) {
            try {
                Assume.assumeTrue("Need Erlang R" + Integer.toString(ErlangBackend.minErlangVersion) + " or later.",
                                  ErlangBackend.getErlangVersion() >= ErlangBackend.minErlangVersion);
            } catch (IOException e) {
                return false;
            } catch (InterruptedException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void assertEvalEquals(String absCode, boolean value) throws Exception {
        if (value)
            assertEvalTrue(absCode);
        else
            assertEvalFails(absCode);
    }

    @Override
    public void assertEvalFails(String absCode) throws Exception {
        Assert.assertEquals(null, runAndCheck(absCode));
    }

    @Override
    public void assertEvalTrue(String absCode) throws Exception {
        Assert.assertEquals("True", runAndCheck(absCode));
    }

    /**
     * Parses, complies, runs given code and returns value of testresult.
     *
     * @param absCode
     * @return either the Result, or null if an execution error occurred
     */
    private String runAndCheck(String absCode) throws Exception {
        File f = null;
        try {
            f = java.nio.file.Files.createTempDirectory("erlangtest").toFile();
            f.deleteOnExit();
            Model model = assertParse(absCode, Config.TYPE_CHECK, /* XXX:CI Config.WITH_LOC_INF, */ Config.WITHOUT_MODULE_NAME);
            String mainModule = genCode(model, f, true);
            return runAndCheck(f, mainModule);
        } finally {
            try {
                FileUtils.deleteDirectory(f);
            } catch (IOException e) {
                // Ignore Ex, File should be deleted anyway
            }
        }
    }

    public void generateAndCompile(Model model) throws Exception {
        File f = null;
        try {
            f = Files.createTempDir();
            f.deleteOnExit();
            genCode(model, f, false);
        } finally {
            try {
                FileUtils.deleteDirectory(f);
            } catch (IOException e) {
                // Ignore Ex, File should be deleted anyway
            }
        }
    }

    /**
     * Generates Erlang code in target directory, adding a last statement that
     * prints the value of the `testresult' variable.
     *
     * @return a Module Name containing a Main Block
     * @throws InternalBackendException
     *
     */
    public String genCode(Model model, File targetDir, boolean appendResultprinter) throws IOException, InterruptedException, InternalBackendException {
        if (model.hasErrors()) {
            Assert.fail(model.getErrors().getFirstError().getHelpMessage());
        }
        if (model.hasTypeErrors()) {
            Assert.fail(model.getTypeErrors().getFirstError().getHelpMessage());
        }
        MainBlock mb = model.getMainBlock();
        if (mb != null && appendResultprinter) {
            // We search for this output in the `run' method below
            mb.addStmt(new ExpressionStmt(
                new List<>(),
                new FnApp("ABS.StdLib.println",
                    new List<>(new AddAddExp(new StringLiteral("RES="),
                        new FnApp("ABS.StdLib.toString",
                            new List<>(new VarUse("testresult"))))))));
        }
        new ErlangBackend().compile(model, targetDir,
// use the following argument for silent compiler:
                              EnumSet.noneOf(ErlangBackend.CompileOptions.class)
// use the following argument for verbose compiler output:
                              // EnumSet.of(ErlangBackend.CompileOptions.VERBOSE)
                              );
        if (mb == null)
            return null;
        else
            return mb.getModuleDecl().getName();

    }

    /**
     * Input should be read from a Process object using a BufferedReader:
     * "Implementation note: It is a good idea for the returned input stream to be buffered."
     * (https://docs.oracle.com/javase/8/docs/api/java/lang/Process.html#getInputStream--)
     *
     * However, using readLine() on the Reader may block forever if the process
     * does not terminate or terminates while reading from its output.
     *
     * read() guarantees, that it does not block, when only attempting to read
     * as many bytes as available() returns.
     * (https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html#available--)
     * "read or skip of this many bytes will not block, but may read or skip fewer bytes"
     *
     * This class implements readLine() in a non blocking way using read() and
     * available().
     *
     * NOTE: Since InputStream.available() counts bytes, but BufferedReader
     * reads chars (16bit), this solution may not be compatible with unicode
     * output.
     **/
    class ProcessReader implements AutoCloseable {
        private final Process p;
        private final InputStream is;
        private final InputStreamReader isr;
        private final BufferedReader br;

        private final StringBuilder lineAcc = new StringBuilder();
        private final LinkedList<String> lineBuffer = new LinkedList<>();

        private boolean streamEnded = false;

        private boolean encounteredCarriageReturn = false;

        public ProcessReader(final Process p) {
            this.p = p;
            this.is = p.getInputStream();
            this.isr = new InputStreamReader(is);
            this.br = new BufferedReader(isr);
        }

        /**
         * Internal helper function which saves characters read so far as one
         * line in the internal line buffer.
         *
         * It also clears the string builder for reuse.
         */
        private void saveLineToBuffer() {
            final String line = lineAcc.toString();
            lineAcc.delete(0, lineAcc.length()); // reuse string builder for next line

            lineBuffer.add(line);
        }

        /**
         * Attempts to read as many bytes as possible from the BufferedReader
         * without risking to block
         */
        private void read() throws IOException {
            int estimatedAvailableBytes = is.available();

            if (estimatedAvailableBytes >= 0) {
                // only attempt to read EOF if process is dead, otherwise read()
                // may block
                if (estimatedAvailableBytes == 0 && !p.isAlive()) {
                    ++estimatedAvailableBytes;
                }

                final char[] buffer = new char[estimatedAvailableBytes];
                final int readBytes = br.read(buffer, 0, estimatedAvailableBytes);

                if (readBytes == -1) { //EOF reached
                    streamEnded = true;
                    if (lineAcc.length() > 0) {
                        saveLineToBuffer();
                    }
                }

                for (int i = 0; i < readBytes; ++i) {
                    final char nextChar = buffer[i];

                    if (nextChar == '\r') {
                        encounteredCarriageReturn = true;
                    }

                    // Return a line, if current character ends a line
                    if (
                           (nextChar == '\n' && !encounteredCarriageReturn) // line end has already been handled, if \r has been encountered before
                        || nextChar == '\r'
                    ) {
                        saveLineToBuffer();
                    }

                    else {
                        lineAcc.append(nextChar); // no full line encountered yet
                    }

                    if (nextChar != '\r') {
                        encounteredCarriageReturn = false;
                    }
                }
            }
        }

        /**
         * Returns a line read from the output of the process if a full line
         * is available yet, otherwise collects more output and returns
         * Optional.empty().
         * 
         * It follows the line definition of BufferedReader.readLine()
         * (ends with '\n', '\r' or "\r\n"). A line end is also reached,
         * if the end of the stream has been reached.
         */
        public Optional<String> readLineNonBlocking() throws IOException {
            final Optional<String> result;

            if (!streamEnded) {
                read();
            }

            if (lineBuffer.isEmpty()) {
                return Optional.empty();
            }

            else {
                return Optional.of(lineBuffer.remove());
            }
        }

        public boolean hasStreamEnded() {
            return streamEnded && lineBuffer.isEmpty();
        }

        @Override
        public void close() throws IOException {
            this.br.close();
            this.isr.close();
            this.is.close();
        }
    }

    /**
     * Executes a model and returns its output as a list of strings, one per
     * line.
     *
     * To detect faults, we have a Timeout process which will kill the runtime
     * system after a timeout.  We always return the accumulated output, no
     * matter how the process was terminated.
     *
     * @param workDir a temporary directory containing a compiled model.
     * @param mainModule the module whose main block should be executed.
     * @param arguments additional arguments to pass to the model
     * @return the lines output by the process until finished or killed by timeout
     */
    public java.util.List<String> runCompiledModel(File workDir, String mainModule, String... arguments) throws Exception {
        ArrayList<String> val = new ArrayList<String>();
        ArrayList<String> process_args = new ArrayList<String>();
        String runFileName = ErlangBackend.isWindows() ? "run.bat" : "run";
        File runFile = new File(workDir, runFileName);
        process_args.add(runFile.getAbsolutePath());
        process_args.add(mainModule);
        process_args.addAll(Arrays.asList(arguments));
        ProcessBuilder pb = new ProcessBuilder(process_args);
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        final TimeoutThread tt = new TimeoutThread(p, test_timeout);
        final Thread t = new Thread(tt);

        try ( // try-with-resources statement, which will ensure, that the declared resources are closed
            ProcessReader r = new ProcessReader(p);
        ) {
            t.start();
            // Search for result
            while (!tt.hasBeenAborted()) {
                final Optional<String> maybeLine = r.readLineNonBlocking();
                
                if (maybeLine.isPresent()) {
                    final String line = maybeLine.get();
                    val.add(line);
                }

                if (r.hasStreamEnded()) {
                    break;
                }

                // Wait 0.1 second before trying again, if the process is still
                // alive and may produce further output.
                else if (p.isAlive()) {
                    Thread.sleep(100);
                }
            }
            if (tt.hasBeenAborted()) {
                throw new java.util.concurrent.TimeoutException("Test did not complete within timeout.");
            }
        }

        p.waitFor();
        t.interrupt();
        return val;
    }

    /**
     * Executes a model and returns the value of the `testresult' abs
     * variable.
     *
     * To detect faults, we have a Timeout process which will kill the
     * runtime system after 10 seconds
     *
     * @param workDir a temporary directory containing a compiled model.
     * @param mainModule the module whose main block should be executed.
     * @return the remainder of an output line starting with "RES=" or null if none found.
     */
    private String runAndCheck(File workDir, String mainModule) throws Exception {
        java.util.List<String> output = runCompiledModel(workDir, mainModule);
        if (output == null) {
            return null;
        } else {
            for (String line : output) {
                if (line.startsWith("RES=")) { // see `genCode' above
                    String val = line.split("=")[1];
                    return val;
                }
            }
            return null;
        }
    }

    @Override
    public void assertEvalTrue(Model model) throws Exception {
        File f = null;
        try {
            f = Files.createTempDir();
            f.deleteOnExit();
            String mainModule = genCode(model, f, true);
            assertEquals("True",runAndCheck(f, mainModule));
        } finally {
            try {
                FileUtils.deleteDirectory(f);
            } catch (IOException e) {
                // Ignore Ex, File should be deleted anyway
            }
        }
    }

    @Override
    public void assertEvalTrueWithTestfiles(Model m, File... f) throws Exception {
        File dir = null;
        try {
            dir = Files.createTempDir();
            dir.deleteOnExit();
            // implementation-specific location for static files; the erlang
            // backend expects e.g. index.html, database files there
            File auxdir = new File(dir, "absmodel/_build/default/lib/absmodel/priv");
            String mainModule = genCode(m, dir, true);
            for (File auxFile : f) {
                File rf = new File(resolveFileName(auxFile.toString()));
                FileUtils.copyFileToDirectory(rf, auxdir);
            }
            assertEquals("True",runAndCheck(dir, mainModule));
        } finally {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                // Ignore Ex, File should be deleted anyway
            }
        }
    }

    @Override
    public BackendName getBackendName() {
        return BackendName.ERLANG;
    }

    @Override
    public boolean supportsCustomSchedulers() { return true; }

    @Override
    public boolean supportsTimedAbs() { return true; }

    @Override
    public boolean supportsExceptions() { return true; }

    @Override
    public boolean supportsDowncasting() { return true; }

    @Override
    public boolean supportsSQLite() { return true; }
}

class TimeoutThread implements Runnable {

    private final Process p;
    private final long timeout;
    private boolean aborted = false;

    public TimeoutThread(Process p, long timeout) {
        super();
        this.p = p;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(timeout);

            if (p.isAlive()) { // If the test is still running by now, terminate it
                p.destroyForcibly();
                this.aborted = true; // record, that the test has not stopped by itself.
            }
        } catch (InterruptedException e) {
        } 
    }

    public boolean hasBeenAborted() {
        return this.aborted;
    }
}
