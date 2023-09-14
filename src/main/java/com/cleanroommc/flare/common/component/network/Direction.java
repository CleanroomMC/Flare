package com.cleanroommc.flare.common.component.network;

public enum Direction {

    RECEIVE("rx"),
    TRANSMIT("tx");

    private final String abbreviation;

    Direction(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String abbrev() {
        return this.abbreviation;
    }

}
