package com.cleanroommc.flare.common.sampler.async;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import com.cleanroommc.flare.common.sampler.async.jfr.JfrParsingException;
import com.cleanroommc.flare.common.sampler.async.jfr.JfrReader;
import com.google.common.collect.ImmutableList;
import one.profiler.AsyncProfiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a profiling job within async-profiler.
 *
 * <p>Only one job can be running at a time. This is guarded by
 * {@link #createNew(AsyncProfilerAccess, AsyncProfiler)}.</p>
 */
public class AsyncProfilerJob {

    /**
     * The currently active job.
     */
    private static final AtomicReference<AsyncProfilerJob> ACTIVE = new AtomicReference<>();

    /**
     * Creates a new {@link AsyncProfilerJob}.
     *
     * <p>Will throw an {@link IllegalStateException} if another job is already active.</p>
     *
     * @param access the profiler access object
     * @param profiler the profiler
     * @return the job
     */
    static AsyncProfilerJob createNew(AsyncProfilerAccess access, AsyncProfiler profiler) {
        synchronized (ACTIVE) {
            AsyncProfilerJob existing = ACTIVE.get();
            if (existing != null) {
                throw new IllegalStateException("Another profiler is already active: " + existing);
            }

            AsyncProfilerJob job = new AsyncProfilerJob(access, profiler);
            ACTIVE.set(job);
            return job;
        }
    }

    /** The async-profiler access object */
    private final AsyncProfilerAccess access;
    /** The async-profiler instance */
    private final AsyncProfiler profiler;

    // Set on init
    /** The Flare API */
    private FlareAPI flare;
    /** The sample collector */
    private SampleCollector<?> sampleCollector;
    /** The thread dumper */
    private ThreadDumper threadDumper;
    /** The profiling window */
    private int window;
    /** If the profiler should run in quiet mode */
    private boolean quiet;

    /** The file used by async-profiler to output data */
    private Path outputFile;

    private AsyncProfilerJob(AsyncProfilerAccess access, AsyncProfiler profiler) {
        this.access = access;
        this.profiler = profiler;
    }

    /**
     * Executes an async-profiler command.
     *
     * @param command the command
     * @return the output
     */
    private String execute(Collection<String> command) {
        try {
            return this.profiler.execute(String.join(",", command));
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst executing profiler command", e);
        }
    }

    /**
     * Checks to ensure that this job is still active.
     */
    private void checkActive() {
        if (ACTIVE.get() != this) {
            throw new IllegalStateException("Profiler job no longer active!");
        }
    }

    // Initialise the job
    public void init(FlareAPI flare, SampleCollector<?> collector, ThreadDumper threadDumper, int window, boolean quiet) {
        this.flare = flare;
        this.sampleCollector = collector;
        this.threadDumper = threadDumper;
        this.window = window;
        this.quiet = quiet;
    }

    /**
     * Starts the job.
     */
    public void start() {
        checkActive();

        try {
            // Create a new temporary output file
            try {
                this.outputFile = Files.createTempFile("flare-", "-profile-data.jfr.tmp");
            } catch (IOException e) {
                throw new RuntimeException("Unable to create temporary output file", e);
            }

            // Construct a command to send to async-profiler
            ImmutableList.Builder<String> command = ImmutableList.<String>builder()
                    .add("start")
                    .addAll(this.sampleCollector.initArguments(this.access))
                    .add("threads").add("jfr").add("file=" + this.outputFile.toString());

            if (this.quiet) {
                command.add("loglevel=NONE");
            }
            if (this.threadDumper instanceof ThreadDumper.Specific) {
                command.add("filter");
            }

            // Start the profiler
            String resp = execute(command.build()).trim();

            if (!resp.equalsIgnoreCase("profiling started")) {
                throw new RuntimeException("Unexpected response: " + resp);
            }

            // append threads to be profiled, if necessary
            if (this.threadDumper instanceof ThreadDumper.Specific) {
                ThreadDumper.Specific threadDumper = (ThreadDumper.Specific) this.threadDumper;
                for (Thread thread : threadDumper.threads()) {
                    this.profiler.addThread(thread);
                }
            }

        } catch (Exception e) {
            try {
                this.profiler.stop();
            } catch (Exception ignore) {
            } finally {
                close();
            }
            throw e;
        }
    }

    /**
     * Stops the job.
     */
    public void stop() {
        checkActive();
        try {
            this.profiler.stop();
        } catch (IllegalStateException e) {
            if (!e.getMessage().equals("Profiler is not active")) { // ignore
                throw e;
            }
        } finally {
            close();
        }
    }

    /**
     * Aggregates the collected data.
     */
    public void aggregate(AsyncDataAggregator dataAggregator) {
        // Read the jfr file produced by async-profiler
        try (JfrReader reader = new JfrReader(this.outputFile)) {
            readSegments(reader, this.sampleCollector, dataAggregator);
        } catch (Exception e) {
            boolean fileExists;
            try {
                fileExists = Files.exists(this.outputFile) && Files.size(this.outputFile) != 0;
            } catch (IOException ex) {
                fileExists = false;
            }

            if (fileExists) {
                throw new JfrParsingException("Error parsing JFR data from profiler output", e);
            } else {
                throw new JfrParsingException("Error parsing JFR data from profiler output - file " + this.outputFile + " does not exist!", e);
            }
        }

        deleteOutputFile();
    }

    public void deleteOutputFile() {
        try {
            Files.deleteIfExists(this.outputFile);
        } catch (IOException ignore) { }
    }

    private <E extends JfrReader.Event> void readSegments(JfrReader reader, SampleCollector<E> collector, AsyncDataAggregator dataAggregator) throws IOException {
        List<E> samples = reader.readAllEvents(collector.eventClass());
        for (E sample : samples) {
            String threadName = reader.threads.get(sample.tid);
            if (threadName == null) {
                continue;
            }
            if (!this.threadDumper.threadIncluded(sample.tid, threadName)) {
                continue;
            }
            long value = collector.measure(sample);

            // Parse the segment and give it to the data aggregator
            ProfileSegment segment = ProfileSegment.parseSegment(reader, sample, threadName, value);
            dataAggregator.insertData(segment, this.window);
        }
    }

    public int getWindow() {
        return this.window;
    }

    private void close() {
        ACTIVE.compareAndSet(this, null);
    }

}
