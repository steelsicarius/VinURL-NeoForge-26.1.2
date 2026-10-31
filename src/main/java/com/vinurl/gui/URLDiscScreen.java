package com.vinurl.gui;

import com.vinurl.client.SoundManager;
import com.vinurl.client.VinURLClient;
import com.vinurl.exe.Executable;
import com.vinurl.net.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

public class URLDiscScreen extends Screen {
	private final String defaultUrl;
	private final int defaultDuration;
	private EditBox urlField;
	private Button lockButton;
	private Button simulateButton;
	private Button doneButton;
	private boolean lock;
	private boolean simulate;

	public URLDiscScreen(String defaultUrl, int defaultDuration) {
		super(Component.literal("VinURL Disc"));
		this.defaultUrl = defaultUrl;
		this.defaultDuration = defaultDuration;
		this.lock = false;
		this.simulate = false;
	}

	@Override
	protected void init() {
		super.init();

		int width = this.width;
		int height = this.height;

		// URL text field
		this.urlField = new EditBox(this.font, width / 2 - 150, height / 2 - 40, 300, 20, Component.literal("URL"));
		this.urlField.setValue(defaultUrl);
		this.addRenderableWidget(this.urlField);

		// Lock button
		this.lockButton = Button.builder(
			Component.literal("🔓"),
			(button) -> {
				lock = !lock;
				button.setMessage(Component.literal(lock ? "🔒" : "🔓"));
			}
		).bounds(width / 2 - 150, height / 2 - 10, 60, 20).build();
		this.addRenderableWidget(this.lockButton);

		// Simulate button
		this.simulateButton = Button.builder(
			Component.literal("Simulate"),
			(button) -> {
				if (simulate) {return;}
				simulate = true;
				String fileName = SoundManager.getFileName(defaultUrl);
				Executable.executeCommand(
					fileName + "/duration",
					Executable.YT_DLP.getCommandLine().addArguments(new String[] {
						defaultUrl, "--print", "DURATION: %(duration)d", "--no-playlist",
						"--js-runtimes", "deno:" + Executable.DENO.FILE_PATH
					}, false).addArguments(VinURLClient.CONFIG.parameters())
				).subscribe("duration")
					.onOutput((output) -> {
						String type = output.substring(0, output.indexOf(':') + 1);
						String message = output.substring(type.length()).trim();
						if ("DURATION:".equals(type)) {
							try {
								int duration = Integer.parseInt(message);
							} catch (NumberFormatException e) {}
						}
					})
					.onError((error) -> {simulate = false;})
					.onComplete(() -> {simulate = false;})
				.start();
			}
		).bounds(width / 2 + 90, height / 2 - 10, 100, 20).build();
		this.addRenderableWidget(this.simulateButton);

		// Done button
		this.doneButton = Button.builder(
			Component.literal("Done"),
			(button) -> {
				String url = urlField.getValue();
				int duration = defaultDuration;
				Payloads.SetURLRecord payload = new Payloads.SetURLRecord(url, duration, lock);
				VinURLClient.CLIENT.getConnection().send(new ServerboundCustomPayloadPacket(payload));
				this.onClose();
			}
		).bounds(width / 2 - 50, height / 2 + 30, 100, 20).build();
		this.addRenderableWidget(this.doneButton);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		context.centeredText(this.font, Component.literal("VinURL Disc"), this.width / 2, this.height / 2 - 60, 0xFFFFFF);
		context.text(this.font, Component.literal("URL:"), this.width / 2 - 160, this.height / 2 - 37, 0xAAAAAA);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
