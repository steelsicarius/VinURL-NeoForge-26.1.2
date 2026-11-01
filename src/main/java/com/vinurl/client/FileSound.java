package com.vinurl.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.vinurl.VinURL.PLACEHOLDER_SOUND;

public class FileSound extends AbstractTickableSoundInstance {
	public final String fileName;
	private final long startTime;

	public final BlockPos position;
	public final Entity entity;

	public FileSound(String fileName, BlockPos pos, Entity entity) {
		super(PLACEHOLDER_SOUND.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
		this.fileName = fileName;
		this.startTime = Util.getMillis();
		this.position = pos;
		this.entity = entity;
	}

	// Streams the downloaded .ogg directly (origin parity: JOrbisAudioStream is a vanilla class,
	// org.jcraft:jorbis is a vanilla launcher library). The previous WAV-conversion approach was
	// removed: if ffmpeg conversion failed or lost the race against play(), getStream returned a
	// failed future that SoundEngine swallows silently -> no audio at all.
	@Override
	public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				InputStream inputStream = new FileInputStream(SoundManager.getAudioFile(fileName));
				return SkippableAudioStream.offset(new JOrbisAudioStream(inputStream), Util.getMillis() - startTime);
			} catch (IOException e) {
				throw new CompletionException(e);
			}
		}, Util.nonCriticalIoPool());
	}

	@Override
	public void tick() {
		if (entity != null) {
			this.x = entity.getX();
			this.y = entity.getY();
			this.z = entity.getZ();

			if (entity.isRemoved()) {
				this.stop();
			}
		} else if (position != null) {
			this.x = position.getCenter().x;
			this.y = position.getCenter().y;
			this.z = position.getCenter().z;
		}
	}
}
