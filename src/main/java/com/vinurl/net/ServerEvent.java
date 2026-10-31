package com.vinurl.net;

import com.vinurl.util.Url;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.vinurl.VinURL.CUSTOM_RECORD;
import static com.vinurl.util.Constants.*;

public class ServerEvent {
	public static final int MAX_URL_LENGTH = 400;
	public static final int MIN_DURATION = 0;
	public static final int MAX_DURATION = 3600;

	public static void handleSetURL(Payloads.SetURLRecord message, IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			if (player == null) {return;}

			ItemStack stack = java.util.stream.Stream.of(InteractionHand.values())
				.map(player::getItemInHand)
				.filter((stackInHand) -> stackInHand.is(CUSTOM_RECORD.get()))
				.findFirst()
				.orElse(ItemStack.EMPTY);

			if (stack.isEmpty()) {
				player.sendOverlayMessage(Component.translatable("message.vinurl.custom_record.missing"));
				return;
			}

			CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
			if (tag.getBoolean("lock").orElse(false)) {
				player.sendOverlayMessage(Component.translatable("item.vinurl.custom_record.message.locked"));
				return;
			}

			Url url = Url.parse(message.url());
			if (url == null) {
				player.sendOverlayMessage(Component.translatable("message.vinurl.custom_record.url.invalid"));
				return;
			}

			if (url.length() > MAX_URL_LENGTH) {
				player.sendOverlayMessage(Component.translatable("message.vinurl.custom_record.url.long"));
				return;
			}

			if (message.duration() < MIN_DURATION || message.duration() > MAX_DURATION) {
				player.sendOverlayMessage(Component.translatable("message.vinurl.custom_record.duration.invalid"));
				return;
			}

			tag.putString("music_url", url.toString());
			tag.putInt("duration", message.duration());
			tag.putBoolean("lock", message.lock());
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

			player.level().playSound(null, player, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.MASTER, 1.0f, 1.0f);
		});
	}
}
