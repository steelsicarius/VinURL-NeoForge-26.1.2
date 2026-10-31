package com.vinurl.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Util;

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
