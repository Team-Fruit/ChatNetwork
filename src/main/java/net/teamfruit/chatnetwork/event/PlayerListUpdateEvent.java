package net.teamfruit.chatnetwork.event;

import net.minecraftforge.fml.common.eventhandler.Event;
import net.teamfruit.chatnetwork.ChatData;

public class PlayerListUpdateEvent extends Event {
    public final ChatData data;

    public PlayerListUpdateEvent(ChatData data) {
        super();
        this.data = data;
    }
}