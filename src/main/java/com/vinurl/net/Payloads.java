package com.vinurl.net;

import com.vinurl.util.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class Payloads {

	public static final Identifier SET_URL_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "set_url");
	public static final Identifier PLAY_SOUND_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "play_sound");
	public static final Identifier STOP_SOUND_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "stop_sound");
	public static final Identifier GUI_RECORD_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "gui_record");

	public static final CustomPacketPayload.Type<SetURLRecord> SET_URL_TYPE = new CustomPacketPayload.Type<>(SET_URL_ID);
	public static final CustomPacketPayload.Type<PlaySoundRecord> PLAY_SOUND_TYPE = new CustomPacketPayload.Type<>(PLAY_SOUND_ID);
	public static final CustomPacketPayload.Type<StopSoundRecord> STOP_SOUND_TYPE = new CustomPacketPayload.Type<>(STOP_SOUND_ID);
	public static final CustomPacketPayload.Type<GUIRecord> GUI_RECORD_TYPE = new CustomPacketPayload.Type<>(GUI_RECORD_ID);

	// BlockPos.STREAM_CODEC is StreamCodec<ByteBuf, BlockPos>; cast to RegistryFriendlyByteBuf
	private static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> BLOCK_POS_CODEC = BlockPos.STREAM_CODEC.cast();

	private static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> OPTIONAL_BLOCK_POS_CODEC = StreamCodec.of(
		(buf, pos) -> {
			boolean present = pos != null;
			buf.writeBoolean(present);
			if (present) {
				BLOCK_POS_CODEC.encode(buf, pos);
			}
		},
		(buf) -> {
			if (buf.readBoolean()) {
				return BLOCK_POS_CODEC.decode(buf);
			}
			return null;
		}
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, SetURLRecord> SET_URL_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, SetURLRecord::url,
		ByteBufCodecs.INT, SetURLRecord::duration,
		ByteBufCodecs.BOOL, SetURLRecord::lock,
		SetURLRecord::new
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, PlaySoundRecord> PLAY_SOUND_CODEC = StreamCodec.composite(
		OPTIONAL_BLOCK_POS_CODEC, PlaySoundRecord::pos,
		ByteBufCodecs.INT, PlaySoundRecord::entityID,
		ByteBufCodecs.STRING_UTF8, PlaySoundRecord::url,
		PlaySoundRecord::new
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, StopSoundRecord> STOP_SOUND_CODEC = StreamCodec.composite(
		OPTIONAL_BLOCK_POS_CODEC, StopSoundRecord::pos,
		ByteBufCodecs.INT, StopSoundRecord::entityID,
		ByteBufCodecs.STRING_UTF8, StopSoundRecord::url,
		ByteBufCodecs.BOOL, StopSoundRecord::cancel,
		StopSoundRecord::new
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, GUIRecord> GUI_RECORD_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, GUIRecord::url,
		ByteBufCodecs.INT, GUIRecord::duration,
		GUIRecord::new
	);

	public record SetURLRecord(String url, int duration, boolean lock) implements CustomPacketPayload {
		@Override
		public CustomPacketPayload.Type<?> type() {
			return SET_URL_TYPE;
		}
	}

	public record PlaySoundRecord(BlockPos pos, int entityID, String url) implements CustomPacketPayload {
		@Override
		public CustomPacketPayload.Type<?> type() {
			return PLAY_SOUND_TYPE;
		}
	}

	public record StopSoundRecord(BlockPos pos, int entityID, String url, boolean cancel) implements CustomPacketPayload {
		@Override
		public CustomPacketPayload.Type<?> type() {
			return STOP_SOUND_TYPE;
		}
	}

	public record GUIRecord(String url, int duration) implements CustomPacketPayload {
		@Override
		public CustomPacketPayload.Type<?> type() {
			return GUI_RECORD_TYPE;
		}
	}
}
