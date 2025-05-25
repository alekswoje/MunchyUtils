package munchyutils.client;

import com.google.gson.annotations.SerializedName;

public class CooldownTrigger {
    public enum Type { HELD, WORN }
    public enum Action { CROUCH, RCLICK, LCLICK, BREAK, CHAT }

    @SerializedName("name")
    public String name;
    @SerializedName("type")
    public Type type;
    @SerializedName("action")
    public Action action;
    @SerializedName("itemNamePart")
    public String itemNamePart;
    @SerializedName("cooldownMs")
    public long cooldownMs;

    public CooldownTrigger(String name, Type type, Action action, String itemNamePart, long cooldownMs) {
        this.name = name;
        this.type = type;
        this.action = action;
        this.itemNamePart = itemNamePart;
        this.cooldownMs = cooldownMs;
    }

    public CooldownTrigger() {}
} 