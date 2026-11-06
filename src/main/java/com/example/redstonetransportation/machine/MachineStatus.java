package com.example.redstonetransportation.machine;

import net.minecraft.util.StringRepresentable;

/** The indicator shown on a machine's top face. */
public enum MachineStatus implements StringRepresentable {
    /** No signal: the machine is switched off. */
    OFF("off"),
    /** Powered, with nothing to do right now. */
    READY("ready"),
    /** Working at the rate its signal sets. */
    WORKING("working"),
    /** Stopped until its buffer is emptied or it is given input. */
    BLOCKED("blocked");

    private final String name;

    MachineStatus(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
