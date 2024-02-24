package com.cleanroommc.flare.common.command.sub.sampler.sub;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.content.BytebinClient;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand.CommandSender;
import com.cleanroommc.flare.common.sampler.AbstractSampler;
import com.cleanroommc.flare.common.sampler.ExportProps;
import com.cleanroommc.flare.proto.FlareSamplerProtos.SamplerData;
import com.cleanroommc.flare.util.LangKeys;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class SamplerUtil {

    static String upload(FlareAPI flare, ExportProps exportProps, Sampler sampler, boolean live, boolean stop) throws IOException {
        SamplerData samplerData = getSamplerProto(flare, exportProps, sampler, stop);
        String key;
        if (live) {
            key = flare.bytebinClient().postContent(BytebinClient.FLARE_SAMPLER_MEDIA_TYPE, "live", samplerData);
        } else {
            key = flare.bytebinClient().postContent(BytebinClient.FLARE_SAMPLER_MEDIA_TYPE, samplerData);
        }
        return flare.viewerUrl() + key;
    }

    static void upload(FlareAPI flare, ExportProps exportProps, CommandSender sender, Sampler sampler, boolean stop) {
        if (!exportProps.saveToFile()) {
            try {
                final String url = upload(flare, exportProps, sampler, false, stop);
                flare.syncWithServer(() -> sender.accept(LangKeys.SAMPLER_UPLOADED_REPORT,
                        msg -> msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_URL, url)), url));
            } catch (IOException e) {
                flare.syncWithServer(() -> {
                    sender.accept(LangKeys.SAMPLER_CANNOT_UPLOAD_REPORT, e.toString());
                    flare.logger().fatal(e);
                });
                exportProps.saveToFile(true);
            }
        }
        if (exportProps.saveToFile()) {
            Path profilerPath = flare.saveDirectory().resolve("profiler");
            String fileName = DateTimeFormatter.ofPattern("yyyy-MM-dd-hh_mm_ss").format(LocalDateTime.now()) + ".sparkprofile";
            Path profilerFile = profilerPath.resolve(fileName);
            try {
                Files.createDirectories(profilerPath);
                Files.write(profilerFile, getSamplerProto(flare, exportProps, sampler, stop).toByteArray());
                sender.accept(LangKeys.SAMPLER_SAVED_REPORT, text ->
                                      text.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, profilerFile.toAbsolutePath().toString())),
                              profilerFile.getFileName());
                sender.accept(LangKeys.SAMPLER_SAVED_REPORT_HINT, text -> text.getStyle().setClickEvent(new ClickEvent(Action.OPEN_URL, flare.viewerUrl())),
                              flare.viewerUrl());
            } catch (IOException e) {
                flare.syncWithServer(() -> {
                    sender.accept(LangKeys.CANNOT_SAVE_TO_DISK, e.toString());
                    flare.logger().fatal(e);
                });
            }
        }
    }

    private static SamplerData getSamplerProto(FlareAPI flare, ExportProps exportProps, Sampler sampler, boolean stop) {
        return ((AbstractSampler) sampler).toProto(flare, exportProps, stop);
    }

    private SamplerUtil() { }

}
