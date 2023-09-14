<h1 style="text-align:center">
    <img alt="Flare Banner" src="src/main/resources/assets/flare/banner.png">
</h1>

<h3 style="text-align:center">
    Derived from spark; flare is a performance profiler and debugger for 1.12.2 Forge & Cleanroom environments.
</h3>

## What does flare do?

flare is made up these separate components:

* **CPU Profiler**: Diagnose performance issues.
* **Memory Inspection**: Diagnose memory issues.
* **Server Health Reporting**: Keep track of overall server health.

### :zap: CPU Profiler

flare's profiler can be used to diagnose performance issues: "lag", low tick rate, high CPU usage, etc.

It is:

* **Lightweight** - can be ran in production with minimal impact.
* **Easy to use** - no configuration or setup necessary, just install the plugin/mod.
* **Quick to Produce Results** - running for just ~30 seconds is enough to produce useful insights into problematic areas for performance.
* **Customisable** - can be tuned to target specific threads, sample at a specific interval, record only "laggy" periods, etc
* **Highly Readable** - simple tree structure lends itself to easy analysis and interpretation. The viewer can also apply deobfuscation mappings.

It works by sampling statistical data about the systems activity, and constructing a call graph based on this data. The call graph is then displayed in an online viewer for further analysis by the user.

There are two different profiler engines:
* Native `AsyncGetCallTrace` + `perf_events` - uses [async-profiler](https://github.com/jvm-profiling-tools/async-profiler) (*only available on Linux x86_64 systems*)
* Built-in Java `ThreadMXBean` - an improved version of the popular [WarmRoast profiler](https://github.com/sk89q/WarmRoast) by sk89q.

### :sparkles: Memory Inspection

flare includes a number of tools which are useful for diagnosing memory issues with a server.

* **Heap Summary** - take & analyse a basic snapshot of the servers memory
    * A simple view of the JVM's heap, see memory usage and instance counts for each class
    * Not intended to be a full replacement of proper memory analysis tools. (see next item)
* **Heap Dump** - take a full (HPROF) snapshot of the servers memory
    * Dumps (& optionally compresses) a full snapshot of JVM's heap.
    * This snapshot can then be inspected using conventional analysis tools.
* **GC Monitoring** - monitor garbage collection activity on the server
    * Allows the user to relate GC activity to game server hangs, and easily see how long they are taking & how much memory is being free'd.
    * Observe frequency/duration of young/old generation garbage collections to inform which GC tuning flags to use

### :sparkles: Server Health Reporting

flare can report a number of metrics summarising the servers overall health.

These metrics include:

* **TPS** - ticks per second, to a more accurate degree indicated by the /tps command
* **Tick Durations** - how long each tick is taking (min, max and average)
* **CPU Usage** - how much of the CPU is being used by the process, and by the overall system
* **Memory Usage** - how much memory is being used by the process
* **Disk Usage** - how much disk space is free/being used by the system

As well as providing tick rate averages, flare can also **monitor individual ticks** - sending a report whenever a single tick's duration exceeds a certain threshold. This can be used to identify trends and the nature of performance issues, relative to other system or game events.

For a comparison between flare, WarmRoast, Minecraft timings and other profiles, see this [page](https://spark.lucko.me/docs/misc/spark-vs-others) in the original spark docs.

## How do I use flare?

flare uses Minecraft's command system for the end-user to use flare to its full potential.



## License

flare is free & open source. It is released under the terms of the GNU GPLv3 license. Please see [`LICENSE`](LICENSE) for more information.

The flare API submodule is released under the terms of the more permissive MIT license. Please see [`flare-api/LICENSE`](src/main/java/com/cleanroommc/flare/api/LICENSE) for more information.

flare is a fork of [spark](https://github.com/lucko/spark), which was also [licensed using the GPLv3](https://github.com/lucko/spark/blob/master/LICENSE.txt).