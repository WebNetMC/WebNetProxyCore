package net.cytonic.mainproxyplugin.data.enums;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum ChatChannel {
    ALL(Component.empty()),
    PRIVATE_MESSAGE(Component.empty()),
    PARTY(Component.text("Party > ", NamedTextColor.GOLD)),
    LEAGUE(Component.text("League > ", NamedTextColor.DARK_PURPLE)),
    MOD(Component.text("Mod > ", NamedTextColor.DARK_GREEN)),
    ADMIN(Component.text("Admin > ", NamedTextColor.DARK_RED)),
    STAFF(Component.text("Staff > ", NamedTextColor.LIGHT_PURPLE));

    private final Component prefix;
    ChatChannel(Component prefix){
        this.prefix = prefix;
    }

    public Component getPrefix(){
        return prefix;
    }
}