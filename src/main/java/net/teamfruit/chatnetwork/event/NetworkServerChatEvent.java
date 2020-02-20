package net.teamfruit.chatnetwork.event;

import com.mojang.authlib.GameProfile;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class NetworkServerChatEvent extends Event {
    private final String message, username;
    private final GameProfile player;
    private ITextComponent component;

    public NetworkServerChatEvent(GameProfile player, String message, ITextComponent component) {
        super();
        this.message = message;
        this.player = player;
        this.username = player.getName();
        this.component = component;
    }

    public void setComponent(ITextComponent e) {
        this.component = e;
    }

    public ITextComponent getComponent() {
        return this.component;
    }

    public String getMessage() {
        return this.message;
    }

    public String getUsername() {
        return this.username;
    }

    public GameProfile getPlayer() {
        return this.player;
    }
}