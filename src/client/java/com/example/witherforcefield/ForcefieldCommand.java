package com.example.witherforcefield;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/**
 * Client-side chat commands (they never go to the server):
 *   /forcefield                 shows the current settings
 *   /forcefield on | off | toggle
 *   /forcefield permanent on | off | toggle
 *   /forcefield speed <0-10>
 *   /forcefield color <#RRGGBB>
 *   /forcefield reset
 *   /forcefield reload          re-reads config/witherforcefield.json
 */
public final class ForcefieldCommand {

	private ForcefieldCommand() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
				dispatcher.register(ClientCommands.literal("forcefield")
						.executes(ForcefieldCommand::status)
						.then(ClientCommands.literal("on").executes(ctx -> setEnabled(ctx, true)))
						.then(ClientCommands.literal("off").executes(ctx -> setEnabled(ctx, false)))
						.then(ClientCommands.literal("toggle").executes(ForcefieldCommand::toggle))
						.then(ClientCommands.literal("permanent")
								.executes(ForcefieldCommand::permanentStatus)
								.then(ClientCommands.literal("on").executes(ctx -> setPermanent(ctx, true)))
								.then(ClientCommands.literal("off").executes(ctx -> setPermanent(ctx, false)))
								.then(ClientCommands.literal("toggle")
										.executes(ctx -> setPermanent(ctx, !ForcefieldConfig.get().permanent))))
						.then(ClientCommands.literal("speed")
								.then(ClientCommands.argument("value", FloatArgumentType.floatArg(0.0f, 10.0f))
										.executes(ForcefieldCommand::speed)))
						.then(ClientCommands.literal("color")
								.then(ClientCommands.argument("hex", StringArgumentType.greedyString())
										.executes(ForcefieldCommand::color)))
						.then(ClientCommands.literal("reset").executes(ForcefieldCommand::reset))
						.then(ClientCommands.literal("reload").executes(ForcefieldCommand::reload))
				));
	}

	private static void say(CommandContext<FabricClientCommandSource> ctx, String text) {
		ctx.getSource().sendFeedback(Component.literal("[Forcefield] " + text));
	}

	private static int status(CommandContext<FabricClientCommandSource> ctx) {
		ForcefieldConfig c = ForcefieldConfig.get();
		say(ctx, (c.enabled ? "ON" : "OFF") + ", permanent " + (c.permanent ? "on" : "off")
				+ ", speed " + c.speed + ", color " + c.color);
		return 1;
	}

	private static int setEnabled(CommandContext<FabricClientCommandSource> ctx, boolean value) {
		ForcefieldConfig.get().enabled = value;
		ForcefieldConfig.save();
		say(ctx, value ? "Enabled." : "Disabled.");
		return 1;
	}

	private static int toggle(CommandContext<FabricClientCommandSource> ctx) {
		return setEnabled(ctx, !ForcefieldConfig.get().enabled);
	}

	private static int permanentStatus(CommandContext<FabricClientCommandSource> ctx) {
		say(ctx, "Permanent mode is " + (ForcefieldConfig.get().permanent ? "on" : "off")
				+ ". Use /forcefield permanent on | off | toggle");
		return 1;
	}

	private static int setPermanent(CommandContext<FabricClientCommandSource> ctx, boolean value) {
		ForcefieldConfig.get().permanent = value;
		ForcefieldConfig.save();
		say(ctx, value
				? "Permanent mode on: full-body forcefield, always shown, not moving."
				: "Permanent mode off: shells follow your enchanted netherite pieces again.");
		return 1;
	}

	private static int speed(CommandContext<FabricClientCommandSource> ctx) {
		float value = FloatArgumentType.getFloat(ctx, "value");
		ForcefieldConfig.get().setSpeed(value);
		ForcefieldConfig.save();
		say(ctx, "Speed set to " + ForcefieldConfig.get().speed + " (1.0 = same as the Wither).");
		return 1;
	}

	private static int color(CommandContext<FabricClientCommandSource> ctx) {
		String hex = StringArgumentType.getString(ctx, "hex");
		if (ForcefieldConfig.get().setColor(hex)) {
			ForcefieldConfig.save();
			say(ctx, "Color set to " + ForcefieldConfig.get().color + ".");
			return 1;
		}
		say(ctx, "Invalid color. Use #RRGGBB, for example /forcefield color #00AAFF");
		return 0;
	}

	private static int reset(CommandContext<FabricClientCommandSource> ctx) {
		ForcefieldConfig.resetToDefaults();
		say(ctx, "Reset to defaults.");
		return 1;
	}

	private static int reload(CommandContext<FabricClientCommandSource> ctx) {
		ForcefieldConfig.load();
		say(ctx, "Reloaded config/witherforcefield.json.");
		return 1;
	}
}
