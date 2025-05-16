package com.cleanroommc.flare.common.command.sub.memory.sub;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.common.component.memory.heap.dump.HeapDump;
import com.cleanroommc.flare.util.FormatUtil;
import com.cleanroommc.flare.util.LangKeys;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.event.ClickEvent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

public class HeapDumpCommand extends FlareSubCommand {

    public HeapDumpCommand(FlareAPI flare) {
        super(flare);
    }

    @Override
    public String getName() {
        return "dump";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare heap dump [--run-gc-first], [--include-non-live], [--compress]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (hasArg(args, "run-gc-first")) {
            System.gc();
        }
        Path heapDumpDir = this.flare.saveDirectory().resolve("heap").resolve("dump");
        String fileName = new SimpleDateFormat("yyyy-MM-dd-hh_mm_ss").format(new Date()) + (HeapDump.isOpenJ9() ? ".phd" : ".hprof");
        Path heapDumpFile = heapDumpDir.resolve(fileName);
        sendMessage(sender, LangKeys.HEAP_DUMP_WAIT);
        try {
            Files.createDirectories(heapDumpDir);
            HeapDump.dumpHeap(heapDumpFile, !hasArg(args, "include-non-live"));
            sendMessage(sender, LangKeys.HEAP_DUMP_REPORT, text ->
                text.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, heapDumpFile.toFile().getAbsolutePath())),
                    heapDumpFile.getFileName());
        } catch (Exception e) {
            sendMessage(sender, LangKeys.INSPECTING_HEAP_UNEXPECTED_EXCEPTION);
            e.printStackTrace();
            return;
        }

        if (hasArg(args, "compress")) {
            sendMessage(sender, LangKeys.COMPRESS_FILE_START, "heap dump");
            try {
                long size = Files.size(heapDumpFile);
                AtomicLong lastReport = new AtomicLong(System.currentTimeMillis());
                this.flare.runAsync(() -> {
                    Path heapDumpCompressedFile = heapDumpDir.resolve(fileName + ".gz");
                    try (InputStream is = Files.newInputStream(heapDumpFile)) {
                        try (GZIPOutputStream os = new GZIPOutputStream(Files.newOutputStream(heapDumpCompressedFile), 1024 * 64)) {
                            byte[] buf = new byte[1024 * 64];
                            AtomicLong total = new AtomicLong();
                            long iterations = 0;
                            while (true) {
                                int r = is.read(buf);
                                if (r == -1) {
                                    break;
                                }
                                os.write(buf, 0, r);
                                total.addAndGet(r);
                                // Report progress every 5MB
                                if (iterations++ % ((1024 / 64) * 5) == 0) {
                                    long millis = System.currentTimeMillis();
                                    long timeSinceLastReport = millis - lastReport.get();
                                    if (timeSinceLastReport > TimeUnit.SECONDS.toMillis(5)) {
                                        lastReport.set(millis);
                                        this.flare.syncWithServer(() -> sendMessage(sender, LangKeys.COMPRESS_FILE_PROGRESS,
                                                FormatUtil.formatBytes(total.get()),
                                                FormatUtil.formatBytes(size),
                                                FormatUtil.percent(total.get(), size)));
                                    }
                                }
                            }
                        }
                        long heapDumpCompressedSize = Files.size(heapDumpCompressedFile);
                        this.flare.syncWithServer(() -> sendMessage(sender, LangKeys.COMPRESS_FILE_REPORT, text ->
                            text.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, heapDumpCompressedFile.toAbsolutePath().toString())),
                                FormatUtil.formatBytes(size),
                                FormatUtil.formatBytes(heapDumpCompressedSize),
                                FormatUtil.percent(heapDumpCompressedSize, size),
                                heapDumpCompressedFile.getFileName()));
                    } catch (IOException e) {
                        sendMessage(sender, LangKeys.CANNOT_COMPRESS_FILE);
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                sendMessage(sender, LangKeys.CANNOT_COMPRESS_FILE);
                e.printStackTrace();
            }
        }

    }

}
