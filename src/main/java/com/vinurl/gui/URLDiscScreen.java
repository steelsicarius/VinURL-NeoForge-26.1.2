package com.vinurl.gui;

import com.vinurl.client.SoundManager;
import com.vinurl.client.VinURLClient;
import com.vinurl.exe.Executable;
import com.vinurl.net.Payloads;
import com.vinurl.util.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;

public class URLDiscScreen extends Screen {
	private final String defaultUrl;
	private final int defaultDuration;
	private EditBox urlField;
	private Button lockButton;
	private Button simulateButton;
	private Button doneButton;
	private DurationSlider durationSlider;
	private boolean lock;
	private boolean simulate;
	private int duration;
	private static final Identifier ICON_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/icon.png");
	// 26.1.2: blit() resolves the Identifier directly as a resource path (assets/<ns>/<path>),
	// with NO automatic textures/ prefix or .png suffix — vanilla uses full paths too
	// (e.g. LogoRenderer: "textures/gui/title/minecraft.png").

	public URLDiscScreen(String defaultUrl, int defaultDuration) {
		super(Component.literal("VinURL Disc"));
		this.defaultUrl = defaultUrl;
		this.defaultDuration = defaultDuration;
		this.duration = defaultDuration;
		this.lock = false;
		this.simulate = false;
	}

	@Override
	protected void init() {
		super.init();

		int width = this.width;
		int height = this.height;
		int centerX = width / 2;
		int centerY = height / 2;

		// URL text field
		this.urlField = new EditBox(this.font, centerX - 150, centerY - 50, 300, 20, Component.literal("URL"));
		this.urlField.setMaxLength(2048);
		this.urlField.setValue(defaultUrl);
		this.addRenderableWidget(this.urlField);

		// Lock button (toggle, shows 🔓/🔒)
		this.lockButton = Button.builder(
			Component.literal("🔓"),
			(button) -> {
				lock = !lock;
				button.setMessage(Component.literal(lock ? "🔒" : "🔓"));
			}
		).bounds(centerX - 150, centerY - 20, 40, 20).build();
		this.addRenderableWidget(this.lockButton);

		// Simulate button
		this.simulateButton = Button.builder(
			Component.literal("Simulate"),
			(button) -> {
				if (simulate) { return; }
				simulate = true;
				button.setMessage(Component.literal("Calculating..."));
				String url = urlField.getValue();
				String fileName = SoundManager.getFileName(url);

				// Ensure yt-dlp and deno executables are available before running
				if (!Executable.YT_DLP.checkForExecutable()) {
					Minecraft.getInstance().execute(() -> {
						button.setMessage(Component.literal("Error: yt-dlp"));
					});
					simulate = false;
					return;
				}
				if (!Executable.DENO.checkForExecutable()) {
					Minecraft.getInstance().execute(() -> {
						button.setMessage(Component.literal("Error: deno"));
					});
					simulate = false;
					return;
				}

				Executable.executeCommand(
					fileName + "/duration",
					Executable.YT_DLP.getCommandLine().addArguments(new String[] {
						url, "--print", "DURATION: %(duration)d", "--no-playlist",
						"--js-runtimes", "deno:" + Executable.DENO.FILE_PATH
					}, false).addArguments(VinURLClient.CONFIG.parameters())
				).subscribe("duration")
					.onOutput((output) -> {
						String type = output.substring(0, output.indexOf(':') + 1);
						String message = output.substring(type.length()).trim();
						if ("DURATION:".equals(type)) {
							try {
								duration = Integer.parseInt(message);
								durationSlider.setSliderValue((double) duration / 3600.0);
								Minecraft.getInstance().execute(() -> {
									button.setMessage(Component.literal("Duration: " + duration + "s"));
								});
							} catch (NumberFormatException e) {
								Minecraft.getInstance().execute(() -> {
									button.setMessage(Component.literal("Simulate"));
								});
							}
						}
					})
					.onError((error) -> {
						simulate = false;
						Minecraft.getInstance().execute(() -> {
							button.setMessage(Component.literal("Simulate"));
						});
					})
					.onComplete(() -> {
						simulate = false;
						Minecraft.getInstance().execute(() -> {
							button.setMessage(Component.literal("Simulate"));
						});
					})
				.start();
			}
		).bounds(centerX + 110, centerY - 20, 100, 20).build();
		this.addRenderableWidget(this.simulateButton);

		// Duration slider (0-3600 seconds)
		this.durationSlider = new DurationSlider(centerX - 150, centerY + 10, 300, 20);
		this.addRenderableWidget(this.durationSlider);

		// Done button
		this.doneButton = Button.builder(
			Component.literal("Done"),
			(button) -> {
				String url = urlField.getValue();
				Payloads.SetURLRecord payload = new Payloads.SetURLRecord(url, duration, lock);
				Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(payload));
				this.onClose();
			}
		).bounds(centerX - 50, centerY + 45, 100, 20).build();
		this.addRenderableWidget(this.doneButton);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		// Mod icon on the left (like Fabric version)
		int iconSize = 48;
		int iconX = this.width / 2 - 200;
		int iconY = this.height / 2 - iconSize / 2;
		// GuiGraphicsExtractor.blit takes x0,y0,x1,y1 (corners), not x,y,width,height
		context.blit(ICON_ID, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
		context.centeredText(this.font, Component.literal("VinURL Disc"), this.width / 2, this.height / 2 - 70, 0xFFFFFF);
		context.text(this.font, Component.literal("URL:"), this.width / 2 - 170, this.height / 2 - 47, 0xAAAAAA);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == 256 || event.key() == 257) { // GLFW_KEY_ESCAPE or GLFW_KEY_ENTER
			String url = urlField.getValue();
			Payloads.SetURLRecord payload = new Payloads.SetURLRecord(url, duration, lock);
			Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(payload));
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// Slider subclass with public setSliderValue to update position from simulate callback
	private class DurationSlider extends AbstractSliderButton {
		public DurationSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.literal("Duration"), (double) defaultDuration / 3600.0);
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal("Duration: %02d:%02d".formatted(duration / 60, duration % 60)));
		}

		@Override
		protected void applyValue() {
			duration = (int) (this.value * 3600.0);
		}

		public void setSliderValue(double value) {
			this.setValue(value);
		}
	}
}
