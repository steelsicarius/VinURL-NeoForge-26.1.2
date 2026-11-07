package com.vinurl.item;

import com.vinurl.net.ClientEvent;
import com.vinurl.util.DiscData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import static com.vinurl.util.Constants.ITEM_KEY;
import static com.vinurl.util.Constants.SONG_KEY;

public class URLDisc extends Item {

        public URLDisc() {
                super(new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.RARE)
                        .jukeboxPlayable(SONG_KEY)
                        .setId(ITEM_KEY));
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                if (!level.isClientSide()) {
                        CompoundTag tag = DiscData.copyTag(stack);
                        if (!DiscData.lock(tag)) {
                                if (player instanceof ServerPlayer serverPlayer) {
                                        ClientEvent.GUIRecord.sendTo(serverPlayer, DiscData.url(tag), DiscData.duration(tag));
                                }
                        } else {
                                player.sendOverlayMessage(Component.translatable("item.vinurl.custom_record.message.locked"));
                        }
                }
                return InteractionResult.SUCCESS;
        }
}
