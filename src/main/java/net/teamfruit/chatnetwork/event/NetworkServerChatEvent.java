package net.teamfruit.chatnetwork.event;

import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.teamfruit.chatnetwork.ChatData;

@Cancelable
public class NetworkServerChatEvent extends Event {
    public final ChatData data;
    public ITextComponent component;

    public NetworkServerChatEvent(ChatData data) {
        super();
        this.data = data;
    }
}