package com.hackathonkiller.worker.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Ejecuta subprocesos de forma controlada (directorio fijo, timeout, salida capturada). */
public class CommandRunner {

    private static final int MAX_OUTPUT_BYTES = 64 * 1024;

    public record Result(int exitCode, String output) {
        public boolean ok() {
            return exitCode == 0;
        }
    }

    private final Duration timeout;

    public CommandRunner(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("El timeout debe ser mayor que cero");
        }
        this.timeout = timeout;
    }

    public Result run(Path workDir, List<String> command) {
        if (command == null || command.isEmpty() || command.get(0).isBlank()) {
            return new Result(127, "El comando está vacío");
        }
        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectErrorStream(true);
        Process process = null;
        try {
            process = pb.start();
            Process runningProcess = process;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            AtomicReference<IOException> readFailure = new AtomicReference<>();
            AtomicBoolean outputTruncated = new AtomicBoolean();
            Thread outputReader = new Thread(() -> {
                try (InputStream stream = runningProcess.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = stream.read(buffer)) != -1) {
                        synchronized (output) {
                            int retained = Math.min(count, MAX_OUTPUT_BYTES - output.size());
                            if (retained > 0) {
                                output.write(buffer, 0, retained);
                            }
                            if (retained < count) {
                                outputTruncated.set(true);
                            }
                        }
                    }
                } catch (IOException e) {
                    readFailure.set(e);
                }
            }, "command-output-reader");
            outputReader.setDaemon(true);
            outputReader.start();

            boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor();
            }
            outputReader.join(2000);
            String captured;
            synchronized (output) {
                captured = output.toString(StandardCharsets.UTF_8);
            }
            if (outputTruncated.get()) {
                captured = append(captured, "Salida truncada a " + MAX_OUTPUT_BYTES + " bytes");
            }
            if (!completed) {
                return new Result(124, append(captured, "Timeout tras " + timeout));
            }
            IOException failure = readFailure.get();
            if (failure != null) {
                return new Result(1, append(captured, "No se pudo leer la salida: " + failure.getMessage()));
            }
            return new Result(process.exitValue(), captured);
        } catch (IOException e) {
            return new Result(127, "No se pudo iniciar el comando: " + e.getMessage());
        } catch (InterruptedException e) {
            if (process != null) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            return new Result(130, "Interrumpido");
        }
    }

    private static String append(String output, String message) {
        return output.isBlank() ? message : output + System.lineSeparator() + message;
    }
}
