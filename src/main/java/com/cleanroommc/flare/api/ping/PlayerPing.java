package com.cleanroommc.flare.api.ping;

public class PlayerPing {

    private final String name;
    private final int ping;

    public PlayerPing(String name, int ping) {
        this.name = name;
        this.ping = ping;
    }

    public String name() {
        return this.name;
    }

    public int ping() {
        return this.ping;
    }

}
