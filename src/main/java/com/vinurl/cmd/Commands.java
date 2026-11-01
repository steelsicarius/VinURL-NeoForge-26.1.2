package com.vinurl.cmd;

import com.mojang.brigadier.context.CommandContext;
import com.vinurl.client.SoundManager;
import com.vinurl.client.VinURLClient;
import com.vinurl.exe.Executable;
import com.vinurl.gui.ConfigScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.commons.io.FileUtils;

import java.util.concurrent.CompletableFuture;
import java.io.IOException;

import static com.vinurl.client.VinURLClient.CLIENT;
import static com.vinurl.util.Constants.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class Commands {

	@SubscribeEvent
	public static void register(RegisterClientCommandsEvent event) {
		event.getDispatcher().register(
			net.minecraft.commands.Commands.literal(MOD_ID)
				.then(net.minecraft.commands.Commands.literal("delete").executes(Commands::deleteAudioFiles))
				.then(net.minecraft.commands.Commands.literal("update").executes(Commands::updateExecutables))
				.then(net.minecraft.commands.Commands.literal("config").executes(Commands::openConfig))
		);
	}

	private static int deleteAudioFiles(CommandContext<CommandSourceStack> ctx) {
		try {
			FileUtils.deleteDirectory(SoundManager.AUDIO_DIRECTORY.toFile());
			ctx.getSource().sendSuccess(() -> Component.translatable("command.vinurl.delete.success"), false);
			return 1;
		} catch (IOException e) {
			ctx.getSource().sendSuccess(() -> Component.translatable("command.vinurl.delete.exception"), false);
			return 0;
		}
	}

	private static int updateExecutables(CommandContext<CommandSourceStack> ctx) {
		ctx.getSource().sendSuccess(() -> Component.translatable("command.vinurl.update.check"), false);
		CompletableFuture.runAsync(() -> {
			for (Executable exe : Executable.values()) {
				String current = exe.currentVersion();
				if (exe.checkForUpdates()) {
					String latest = exe.currentVersion();
					ctx.getSource().sendSuccess(() -> Component.literal("%s: %s -> %s".formatted(exe, current, latest)), false);
				}
			}
			ctx.getSource().sendSuccess(() -> Component.translatable("command.vinurl.update.latest"), false);
		});
		return 1;
	}

	private static int openConfig(CommandContext<CommandSourceStack> ctx) {
		CLIENT.execute(() -> CLIENT.setScreen(new ConfigScreen()));
		return 1;
	}
}
