package at.hugo.bukkit.plugin.example;

import at.hugob.plugin.library.command.CommandManager;
import at.hugob.plugin.library.config.YamlFileConfig;
import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.CommandHelpHandler;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;

public class ExamplePlugin extends JavaPlugin {
    private final static String commandName = "pumpkintrees";
    private YamlFileConfig messages;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        messages = new YamlFileConfig(this, "messages.yml");
        reloadConfig();
        try {
            commandManager = new CommandManager(this,
                    component -> messages.getComponent("prefix").append(text(" ")).append(component),
                    "/" + commandName + " help",
                    new CommandConfirmationManager<>(
                            30L,
                            TimeUnit.SECONDS,
                            context -> context.getCommandContext().getSender().sendMessage(messages.getComponent("commands.confirm.needed")),
                            sender -> sender.sendMessage(messages.getComponent("commands.confirm.nothing"))
                    )
            );
        } catch (InstantiationException e) {
            e.printStackTrace();
            setEnabled(false);
        }
        createCommands();
    }

    @Override
    public void reloadConfig() {
        saveDefaultConfig();
        super.reloadConfig();
        messages.reload();
    }

    private void createCommands() {
        var builder = commandManager.manager().commandBuilder(commandName);

        commandManager.command(builder.literal("help", ArgumentDescription.of("The main help command"))
                .permission("ah.commands.help")
                .argument(StringArgument.<CommandSender>builder("query").greedy().asOptional().withSuggestionsProvider((context, string) ->
                        commandManager.manager().createCommandHelpHandler().queryRootIndex(context.getSender()).getEntries().stream()
                                .map(CommandHelpHandler.VerboseHelpEntry::getSyntaxString).collect(Collectors.toList())
                ).withDefaultDescription(ArgumentDescription.of("The start of the command to query")))
                .handler(commandContext -> {
                    String query = commandContext.getOrDefault("query", "");
                    commandManager.queryCommands(query == null ? "" : query, commandContext.getSender());
                })
        );
        commandManager.command(builder.literal("reload", ArgumentDescription.of("Reloads this plugin"))
                .permission("ah.admin.reload")
                .handler(commandContext -> {
                    final var sender = commandContext.getSender();
                    sender.sendMessage(this.getMessagesConfig().getComponent("commands.reload.start"));
                    this.reloadConfig();
                    sender.sendMessage(this.getMessagesConfig().getComponent("commands.reload.finish"));
                })
        );
        commandManager.command(builder.literal("version")
                .permission("ah.admin.version")
                .handler(commandContext ->
                        commandContext.getSender().sendMessage(Component.text(getName() + " version " + getPluginMeta().getVersion()))
                ));
    }

    public YamlFileConfig getMessagesConfig() {
        return messages;
    }
}