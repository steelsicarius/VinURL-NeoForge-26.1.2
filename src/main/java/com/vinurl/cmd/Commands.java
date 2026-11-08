package com.vinurl.cmd;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.vinurl.client.SoundManager;
import com.vinurl.client.VinURLClient;
import com.vinurl.client.VinURLConfig;
import com.vinurl.exe.Executable;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static com.vinurl.util.Constants.MOD_ID;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class Commands {

        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
                event.getDispatcher().register(literal(MOD_ID)
                        .then(literal("delete").executes(Commands::deleteAudioFiles))
                        .then(literal("update").executes(Commands::updateExecutables))
                        .then(literal("config").executes(Commands::openConfig))
                );
        }

        private static int deleteAudioFiles(CommandContext<CommandSourceStack> ctx) {
                try {
                        FileUtils.deleteDirectory(SoundManager.AUDIO_DIRECTORY.toFile());
                        ctx.getSource().sendSuccess(() -> Component.translatable("command.vinurl.delete.success"), false);
                        return Command.SINGLE_SUCCESS;
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
                return Command.SINGLE_SUCCESS;
        }

        private static int openConfig(CommandContext<CommandSourceStack> ctx) {
                Minecraft client = VinURLClient.getClient();
                client.execute(() -> ModList.get().getModContainerById(MOD_ID).ifPresent((container) ->
                        client.setScreen(new ConfigurationScreen(container, null))
                ));
                return Command.SINGLE_SUCCESS;
        }
}
