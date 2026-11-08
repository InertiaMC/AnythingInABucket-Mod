package net.inertiamc.anythinginabucket.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.inertiamc.anythinginabucket.AnythingInABucket;
import net.inertiamc.anythinginabucket.catching.Codes;
import net.inertiamc.anythinginabucket.item.BucketItems;
import net.inertiamc.anythinginabucket.util.Compat;
import net.inertiamc.anythinginabucket.util.Messages;
import net.inertiamc.anythinginabucket.util.Perms;
import net.inertiamc.anythinginabucket.util.Words;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

/** {@code /bucket help|give|admin info|admin reload}, alias {@code /aiab}. */
public final class BucketCommand {

    private BucketCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("bucket")
                .requires(Perms.COMMAND_HELP::has)
                .executes(BucketCommand::help)
                .then(Commands.literal("help").executes(BucketCommand::help))
                .then(Commands.literal("give")
                        .requires(source -> Perms.COMMAND_GIVE.has(source) && AnythingInABucket.settings().catchWith().capture())
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(c -> give(c, 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(c -> give(c, IntegerArgumentType.getInteger(c, "amount"))))))
                .then(Commands.literal("admin")
                        .requires(Perms.COMMAND_ADMIN::has)
                        .then(Commands.literal("info").requires(Perms.COMMAND_ADMIN_INFO::has).executes(BucketCommand::info))
                        .then(Commands.literal("reload").requires(Perms.COMMAND_ADMIN_RELOAD::has).executes(BucketCommand::reload))));
        dispatcher.register(Commands.literal("aiab").requires(Perms.COMMAND_HELP::has).executes(BucketCommand::help).redirect(root));
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        for (String line : Messages.HELP) context.getSource().sendSystemMessage(Messages.line(line));
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        for (ServerPlayer player : players) {
            int left = amount;
            while (left > 0) {
                ItemStack stack = BucketItems.captureBucket(Math.min(16, left));
                left -= stack.getCount();
                Compat.giveOrDrop(player, stack);
            }
            Messages.send(player, amount == 1 ? Messages.GIVE_RECEIVED_ONE : Messages.GIVE_RECEIVED, "amount", String.valueOf(amount));
        }
        String who = players.size() == 1 ? Compat.name(players.iterator().next()) : players.size() + " players";
        context.getSource().sendSystemMessage(Messages.prefixed(amount == 1 ? Messages.GIVE_GIVEN_ONE : Messages.GIVE_GIVEN,
                "amount", String.valueOf(amount), "player", who));
        return players.size();
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BucketItems.Filled filled = BucketItems.filledOf(player.getMainHandItem());
        if (filled == null) {
            Messages.send(player, Messages.INFO_NONE);
            return 0;
        }
        boolean live = Codes.of(context.getSource().getServer()).live(filled.code());
        String caught = filled.caughtAt() <= 0 ? "?" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(filled.caughtAt()));
        for (String line : Messages.INFO_BOX) {
            player.sendSystemMessage(Messages.line(line,
                    "mob", Words.pretty(Identifier.parse(filled.mob()).getPath()),
                    "player", filled.caughtBy(),
                    "caught", caught,
                    "code", filled.code().toString().substring(0, 8),
                    "state", live ? Messages.INFO_WORKS : Messages.INFO_COPY,
                    "version", String.valueOf(filled.dataVersion()),
                    "size", Words.number(filled.entity().sizeInBytes() / 1024.0) + " KB"));
        }
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        AnythingInABucket.Reload result = AnythingInABucket.reload(source.getServer());
        if (result.error() != null) {
            source.sendSystemMessage(Messages.prefixed(Messages.RELOAD_ERROR, "error", result.error()));
            return 0;
        }
        if (result.problems() > 0) {
            source.sendSystemMessage(Messages.prefixed(Messages.RELOAD_PROBLEMS, "count", String.valueOf(result.problems())));
        } else {
            source.sendSystemMessage(Messages.prefixed(Messages.RELOAD_SUCCESS));
        }
        if (result.recipeChanged()) source.sendSystemMessage(Messages.prefixed(Messages.RELOAD_RECIPE));
        return 1;
    }
}
