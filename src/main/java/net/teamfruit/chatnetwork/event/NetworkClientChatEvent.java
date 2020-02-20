package net.teamfruit.chatnetwork.event;

import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.teamfruit.chatnetwork.ChatData;

import javax.annotation.Nullable;

@Cancelable
public class NetworkClientChatEvent extends Event {
    public final ChatData data;
    @Nullable
    public ITextComponent component;

    public NetworkClientChatEvent(ChatData data) {
        super();
        this.data = data;
    }
}