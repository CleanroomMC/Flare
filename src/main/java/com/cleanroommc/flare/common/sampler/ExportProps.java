package com.cleanroommc.flare.common.sampler;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.sampler.node.MethodDescriptorResolver;
import com.cleanroommc.flare.api.sampler.source.ClassSourceLookup;
import com.cleanroommc.flare.proto.FlareSamplerProtos;
import net.minecraft.command.ICommandSender;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

public class ExportProps {

    public static void setDefault(FlareAPI flare, ExportProps exportProps) {
        exportProps.resolver(flare::methodDescriptorResolver).classSourceLookup(flare::classSourceLookup);
    }

    private WeakReference<ICommandSender> creator;
    private String comment;
    private Supplier<ClassSourceLookup> classSourceLookup;
    private FlareSamplerProtos.SocketChannelInfo channelInfo;
    private Supplier<MethodDescriptorResolver> resolver;
    private boolean separateParentCalls, saveToFile;

    public ExportProps copy() {
        return new ExportProps()
                .creator(this.creator())
                .comment(this.comment())
                .classSourceLookup(this.classSourceLookup())
                .channelInfo(this.channelInfo())
                .resolver(this.resolver())
                .separateParentCalls(this.separateParentCalls())
                .saveToFile(this.saveToFile());

    }

    public ICommandSender creator() {
        return this.creator == null ? null : this.creator.get();
    }

    public String comment() {
        return this.comment;
    }

    public Supplier<ClassSourceLookup> classSourceLookup() {
        return this.classSourceLookup;
    }

    public FlareSamplerProtos.SocketChannelInfo channelInfo() {
        return this.channelInfo;
    }

    public Supplier<MethodDescriptorResolver> resolver() {
        return resolver;
    }

    public boolean separateParentCalls() {
        return separateParentCalls;
    }

    public boolean saveToFile() {
        return saveToFile;
    }

    public ExportProps creator(ICommandSender creator) {
        this.creator = new WeakReference<>(creator);
        return this;
    }

    public ExportProps comment(String comment) {
        this.comment = comment;
        return this;
    }

    public ExportProps classSourceLookup(Supplier<ClassSourceLookup> classSourceLookup) {
        this.classSourceLookup = classSourceLookup;
        return this;
    }

    public ExportProps channelInfo(FlareSamplerProtos.SocketChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
        return this;
    }

    public ExportProps resolver(Supplier<MethodDescriptorResolver> resolver) {
        this.resolver = resolver;
        return this;
    }

    public ExportProps separateParentCalls(boolean separateParentCalls) {
        this.separateParentCalls = separateParentCalls;
        return this;
    }

    public ExportProps saveToFile(boolean saveToFile) {
        this.saveToFile = saveToFile;
        return this;
    }

}
