package com.cleanroommc.flare.common.command.sub.memory.sub;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.content.BytebinClient;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.common.component.memory.heap.dump.HeapDumpSummary;
import com.cleanroommc.flare.proto.FlareHeapProtos.HeapData;
import com.cleanroommc.flare.util.LangKeys;
import com.cleanroommc.flare.util.ProtoUtil;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HeapSummaryCommand extends FlareSubCommand {

    public HeapSummaryCommand(FlareAPI flare) {
        super(flare);
    }

    @Override
    public String getName() {
        return "summary";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("s");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare heap summary [--save-locally], [--run-gc-first]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (hasArg(args, "run-gc-first")) {
            System.gc();
        }
        sendMessage(sender, LangKeys.HEAP_SUMMARY_WAIT);
        HeapDumpSummary heapDump;
        try {
            heapDump = HeapDumpSummary.createNew();
        } catch (Exception e) {
            sendMessage(sender, LangKeys.INSPECTING_HEAP_UNEXPECTED_EXCEPTION);
            e.printStackTrace();
            return;
        }
        HeapData heapData = ProtoUtil.getHeapDataProto(this.flare, sender, heapDump);
        boolean saveLocally = hasArg(args, "save-locally");
        if (!saveLocally) {
            try {
                String key = this.flare.bytebinClient().postContent(BytebinClient.FLARE_HEAP_MEDIA_TYPE, heapData);
                String url = this.flare.viewerUrl() + key;
                sendMessage(sender, LangKeys.HEAP_SUMMARY_REPORT,
                        text -> text.getStyle().setClickEvent(new ClickEvent(Action.OPEN_URL, url)), url);
            } catch (Throwable t) {
                sendMessage(sender, LangKeys.CANNOT_UPLOAD_SAVE_TO_DISK_INSTEAD);
                this.flare.logger().fatal(t);
                saveLocally = true;
            }
        }
        if (saveLocally) {
            try {
                Path heapSummaryDir = this.flare.saveDirectory().resolve("heap").resolve("summary");
                Files.createDirectories(heapSummaryDir);
                Path heapSummaryFile = heapSummaryDir.resolve(new SimpleDateFormat("yyyy-MM-dd-hh_mm'.sparkheap'").format(new Date()));
                Files.write(heapSummaryFile, heapData.toByteArray());
                sendMessage(sender, LangKeys.HEAP_SUMMARY_REPORT, text -> text.getStyle().setClickEvent(
                        new ClickEvent(Action.OPEN_FILE, heapSummaryFile.toFile().getAbsolutePath())),
                        heapSummaryFile.getFileName());
                sendMessage(sender, LangKeys.HEAP_SUMMARY_REPORT_USAGE_HINT, this.flare.viewerUrl());
            } catch (IOException e) {
                sendMessage(sender, LangKeys.CANNOT_SAVE_TO_DISK);
                e.printStackTrace();
            }
        }
    }

}
