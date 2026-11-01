package com.vinurl.client;

import com.jcraft.jorbis.JOrbisException;
import com.jcraft.jorbis.VorbisFile;
import com.vinurl.exe.Executable;
import com.vinurl.exe.ProcessStream;
import com.vinurl.gui.ProgressOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.vinurl.client.VinURLClient.CONFIG;
import static com.vinurl.util.Constants.LOGGER;
import static com.vinurl.util.Constants.VINURLPATH;

public class SoundManager {
	public static final Path AUDIO_DIRECTORY = VINURLPATH.resolve("downloads");
	private static final HashSet<FileSound> playingSounds = new HashSet<>();
	private static final HashMap<String, String> descriptionCache = new HashMap<>();

	public static void downloadSound(String url, String fileName) {
		ProgressOverlay.set(fileName, 0);

		Executable.executeCommand(
			fileName + "/download",
			Executable.YT_DLP.getCommandLine().addArguments(new String[] {
				url, "-x", "--no-simulate", "-q", "--progress", "--add-metadata", "--no-playlist",
				"--progress-template", "PROGRESS: %(progress._percent)d", "--newline",
				"--break-match-filter", "ext~=3gp|aac|flv|m4a|mov|mp3|mp4|ogg|wav|webm|opus",
				"--audio-format", "vorbis", "--audio-quality", CONFIG.audioBitrate().getValue(),
				"--postprocessor-args", "ffmpeg:-ac 1 -c:a libvorbis",
				"--ffmpeg-location", Executable.FFMPEG.FILE_PATH.toString(),
				"--js-runtimes", "deno:" + Executable.DENO.FILE_PATH,
				"-P", AUDIO_DIRECTORY.toString(), "-o", fileName + ".%(ext)s"
			}, false).addArguments(CONFIG.parameters())
		).subscribe("main")
			.onOutput((output) -> {
				String type = output.substring(0, output.indexOf(':') + 1);
				String message = output.substring(type.length()).trim();

				switch (type) {
					case "PROGRESS:" -> ProgressOverlay.set(fileName, Integer.parseInt(message));
					case "WARNING:" -> LOGGER.warn(message);
					case "ERROR:" -> LOGGER.error(message);
					default -> LOGGER.info(output);
				}
			})
			.onError((error) -> {
				ProgressOverlay.stopFailed(fileName);
				deleteSound(fileName);
			})
			.onComplete(() -> {
				ProgressOverlay.stop(fileName);
				setDescription(fileName);
				// Convert Ogg to WAV for JavaSound playback
				convertToWav(fileName);
			})
			.start();
	}

	private static void convertToWav(String fileName) {
		File oggFile = getAudioFile(fileName);
		File wavFile = getWavFile(fileName);
		if (!oggFile.exists()) {return;}
		if (wavFile.exists()) {return;}

		try {
			Process process = new ProcessBuilder()
				.command(
					Executable.FFMPEG.FILE_PATH.toString(),
					"-y", "-i", oggFile.getAbsolutePath(),
					"-ac", "1", "-ar", "44100", "-sample_fmt", "s16",
					wavFile.getAbsolutePath()
				)
				.redirectErrorStream(true)
				.start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				LOGGER.error("ffmpeg conversion failed for {} with code {}", fileName, exitCode);
			}
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Failed to convert {} to WAV", fileName, e);
		}
	}

	public static void deleteSound(String fileName) {
		FileUtils.listFiles(AUDIO_DIRECTORY.toFile(), TrueFileFilter.INSTANCE, null)
			.stream()
			.filter(f -> f.getName().contains(fileName))
			.forEach(FileUtils::deleteQuietly);
	}

	public static FileSound getSound(BlockPos pos) {
		return playingSounds.stream()
			.filter(s -> Objects.equals(s.position, pos))
			.findFirst()
			.orElse(null);
	}

	public static FileSound getSound(int entityID) {
		return playingSounds.stream()
			.filter(s -> s.entity != null && Objects.equals(s.entity.getId(), entityID))
			.findFirst()
			.orElse(null);
	}

	public static void setSound(FileSound fileSound) {
		if (fileSound == null) {return;}
		playingSounds.add(fileSound);
	}

	public static void playSound(FileSound fileSound) {
		if (fileSound == null) {return;}
		Minecraft.getInstance().getSoundManager().play(fileSound);
		Minecraft.getInstance().gui.setNowPlaying(Component.literal(getDescription(fileSound.fileName)));
	}

	public static void stopSound(FileSound fileSound) {
		if (fileSound == null) {return;}
		playingSounds.remove(fileSound);
		Minecraft.getInstance().getSoundManager().stop(fileSound);
	}

	public static void queueSound(FileSound fileSound) {
		if (fileSound == null) {return;}
		ProcessStream processStream = Executable.getProcessStream(fileSound.fileName + "/download");
		if (processStream != null) {
			processStream.subscribe(Objects.toString(fileSound.position))
				.onComplete(() -> playSound(fileSound)).start();
		}
	}

	public static void unqueueSound(FileSound fileSound, boolean cancel) {
		if (fileSound == null) {return;}
		ProcessStream processStream = Executable.getProcessStream(fileSound.fileName + "/download");
		if (processStream != null) {
			processStream.unsubscribe(Objects.toString(fileSound.position));
			if (cancel && processStream.subscriberCount() <= 1) {
				Executable.killProcess(processStream.getId());
			}
		}
	}

	public static synchronized String getDescription(String fileName) {
		return descriptionCache.computeIfAbsent(fileName, SoundManager::computeDescription);
	}

	public static synchronized void setDescription(String fileName) {
		descriptionCache.put(fileName, computeDescription(fileName));
	}

	private static String computeDescription(String fileName) {
		return getAttribute(fileName, "artist") + " - " + getAttribute(fileName, "title");
	}

	private static String getAttribute(String fileName, String attribute) {
		VorbisFile vorbisFile = null;
		try {
			vorbisFile = new VorbisFile(getAudioFile(fileName).toString());
			String metadata = vorbisFile.getComment(0).toString();

			String filter = "Comment: " + attribute + "=";
			return Stream.of(metadata.split("\n"))
				.filter((line) -> line.startsWith(filter))
				.map((line) -> line.substring(filter.length()))
				.findFirst()
				.orElse("N/A");
		} catch (JOrbisException e) {
			return "N/A";
		} finally {
			if (vorbisFile != null) {
				try {
					vorbisFile.close();
				} catch (IOException e) {
					LOGGER.error("Error closing vorbis file", e);
				}
			}
		}
	}

	public static File getAudioFile(String fileName) {
		return AUDIO_DIRECTORY.resolve(fileName + ".ogg").toFile();
	}

	public static File getWavFile(String fileName) {
		return AUDIO_DIRECTORY.resolve(fileName + ".wav").toFile();
	}

	public static String getFileName(String url) {
		return (url == null || url.isEmpty()) ? "" : DigestUtils.sha256Hex(url);
	}

	/**
	 * AudioStream implementation that reads PCM data from a WAV file using JavaSound.
	 */
	public static final class WavAudioStream implements net.minecraft.client.sounds.AudioStream {
		private final AudioInputStream inputStream;
		private boolean closed;

		public WavAudioStream(File file) throws IOException, UnsupportedAudioFileException {
			this.inputStream = AudioSystem.getAudioInputStream(file);
		}

		@Override
		public AudioFormat getFormat() {
			return inputStream.getFormat();
		}

		@Override
		public ByteBuffer read(int length) throws IOException {
			if (closed) return null;
			byte[] bytes = new byte[length];
			int read = inputStream.read(bytes);
			if (read < 0) {
				closed = true;
				return null;
			}
			return ByteBuffer.wrap(bytes, 0, read);
		}

		@Override
		public void close() throws IOException {
			closed = true;
			inputStream.close();
		}
	}
}
