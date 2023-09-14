package com.cleanroommc.flare.common.command.sub.sampler.sub;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.content.BytebinClient;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand.CommandSender;
import com.cleanroommc.flare.common.sampler.AbstractSampler;
import com.cleanroommc.flare.common.sampler.ExportProps;
import com.cleanroommc.flare.proto.FlareSamplerProtos.SamplerData;
import com.cleanroommc.flare.util.LangKeys;
import com.google.common.base.Stopwatch;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;

import java.io.IOException;

public final class SamplerUtil {

    static String upload(FlareAPI flare, ExportProps exportProps, Sampler sampler, boolean live, boolean stop) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        SamplerData samplerData = getSamplerProto(flare, exportProps, sampler, stop);
        flare.logger().info("SamplerData Gathered in {}", stopwatch.stop());
        stopwatch.reset();
        stopwatch.start();
        String key;
        if (live) {
            key = flare.bytebinClient().postContent(BytebinClient.FLARE_SAMPLER_MEDIA_TYPE, "live", samplerData);
        } else {
            key = flare.bytebinClient().postContent(BytebinClient.FLARE_SAMPLER_MEDIA_TYPE, samplerData);
        }
        flare.logger().info("SamplerData Uploaded in {}", stopwatch.stop());
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

        }
    }

    private static SamplerData getSamplerProto(FlareAPI flare, ExportProps exportProps, Sampler sampler, boolean stop) {
        return ((AbstractSampler) sampler).toProto(flare, exportProps, stop);
    }

    private SamplerUtil() { }

}
