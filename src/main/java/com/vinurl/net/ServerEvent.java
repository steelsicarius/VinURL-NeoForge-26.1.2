package com.vinurl.net;

import com.vinurl.util.DiscData;
import com.vinurl.util.Url;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.stream.Stream;

import static com.vinurl.VinURL.CUSTOM_RECORD;
import static com.vinurl.util.Constants.MOD_ID;

public class ServerEvent {
        public static final int MAX_URL_LENGTH = 400;
        public static final int MIN_DURATION = 0;
        public static final int MAX_DURATION = 3600;

        public record SetURLRecord(String url, int duration, boolean lock) implements CustomPacketPayload {
                public static final Type<SetURLRecord> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "set_url_record"));
                public static final StreamCodec<ByteBuf, SetURLRecord> STREAM_CODEC = StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, SetURLRecord::url,
                        ByteBufCodecs.VAR_INT, SetURLRecord::duration,
                        ByteBufCodecs.BOOL, SetURLRecord::lock,
                        SetURLRecord::new
                );

                @Override
                public Type<? extends CustomPacketPayload> type() {
                        return TYPE;
                }
        }

        public static void handleSetURLRecord(SetURLRecord message, IPayloadContext context) {
                Player player = context.player();
                ItemStack stack = Stream.of(InteractionHand.values())
                        .map(player::getItemInHand)
                        .filter((stackInHand) -> stackInHand.is(CUSTOM_RECORD.get()))
                        .findFirst()
                        .orElse(ItemStack.EMPTY);

                if (stack.isEmpty()) {
                        player.sendOverlayMessage(Component.translatable("message.vinurl.custom_record.missing"));
                        return;
                }

                CompoundTag tag = DiscData.copyTag(stack);
                if (DiscData.lock(tag)) {
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

                tag.putString(DiscData.URL_KEY, url.toString());
                tag.putInt(DiscData.DURATION_KEY, message.duration());
                tag.putBoolean(DiscData.LOCK_KEY, message.lock());
                DiscData.save(stack, tag);

                player.level().playSound(null, player, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.MASTER, 1.0f, 1.0f);
        }
}
