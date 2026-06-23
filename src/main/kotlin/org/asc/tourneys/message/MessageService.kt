package org.asc.tourneys.message

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.asc.tourneys.model.TournamentConfig
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class MessageService(private var config: TournamentConfig) {

    private val mini = MiniMessage.miniMessage()

    fun update(config: TournamentConfig) {
        this.config = config
    }

    fun component(path: String, placeholders: Map<String, String> = emptyMap()): Component {
        var text = "${config.messagePrefix}${config.messages[path] ?: "<red>Missing message: $path</red>"}"
        for ((key, value) in placeholders) {
            text = text.replace("{$key}", value)
        }
        return mini.deserialize(text)
    }

    fun raw(path: String, placeholders: Map<String, String> = emptyMap()): String {
        var text = config.messages[path] ?: ""
        for ((key, value) in placeholders) {
            text = text.replace("{$key}", value)
        }
        return text
    }

    fun send(sender: CommandSender, path: String, placeholders: Map<String, String> = emptyMap()) {
        sender.sendMessage(component(path, placeholders))
    }

    fun actionbar(player: Player, path: String, placeholders: Map<String, String> = emptyMap()) {
        var text = config.actionbars[path] ?: return
        for ((key, value) in placeholders) {
            text = text.replace("{$key}", value)
        }
        player.sendActionBar(mini.deserialize(text))
    }

    fun title(player: Player, path: String, placeholders: Map<String, String> = emptyMap()) {
        val title = config.titles[path] ?: return
        var main = title.title
        var sub = title.subtitle
        for ((key, value) in placeholders) {
            main = main.replace("{$key}", value)
            sub = sub.replace("{$key}", value)
        }
        player.showTitle(net.kyori.adventure.title.Title.title(mini.deserialize(main), mini.deserialize(sub)))
    }
}
