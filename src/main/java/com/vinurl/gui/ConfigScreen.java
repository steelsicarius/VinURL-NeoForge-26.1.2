package com.vinurl.gui;

import com.vinurl.client.ClientConfig;
import com.vinurl.client.VinURLClient;
import com.vinurl.util.Constants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
	private EditBox parametersField;
	private Button doneButton;

	public ConfigScreen() {
		super(Component.literal("VinURL Config"));
	}

	@Override
	protected void init() {
		super.init();

		int width = this.width;
		int height = this.height;

		// Download enabled toggle
		Button downloadButton = Button.builder(
			Component.literal("Download Enabled: " + VinURLClient.CONFIG.downloadEnabled()),
			(button) -> {
				boolean newVal = !VinURLClient.CONFIG.downloadEnabled();
				VinURLClient.CONFIG.DOWNLOAD_ENABLED.set(newVal);
				VinURLClient.CONFIG.DOWNLOAD_ENABLED.save();
				button.setMessage(Component.literal("Download Enabled: " + newVal));
			}
		).bounds(width / 2 - 150, height / 2 - 60, 200, 20).build();
		this.addRenderableWidget(downloadButton);

		// Updates on startup toggle
		Button updatesButton = Button.builder(
			Component.literal("Updates on Startup: " + VinURLClient.CONFIG.updatesOnStartup()),
			(button) -> {
				boolean newVal = !VinURLClient.CONFIG.updatesOnStartup();
				VinURLClient.CONFIG.UPDATES_ON_STARTUP.set(newVal);
				VinURLClient.CONFIG.UPDATES_ON_STARTUP.save();
				button.setMessage(Component.literal("Updates on Startup: " + newVal));
			}
		).bounds(width / 2 - 150, height / 2 - 30, 200, 20).build();
		this.addRenderableWidget(updatesButton);

		// Show description toggle
		Button descButton = Button.builder(
			Component.literal("Show Description: " + VinURLClient.CONFIG.showDescription()),
			(button) -> {
				boolean newVal = !VinURLClient.CONFIG.showDescription();
				VinURLClient.CONFIG.SHOW_DESCRIPTION.set(newVal);
				VinURLClient.CONFIG.SHOW_DESCRIPTION.save();
				button.setMessage(Component.literal("Show Description: " + newVal));
			}
		).bounds(width / 2 - 150, height / 2, 200, 20).build();
		this.addRenderableWidget(descButton);

		// Parameters text field
		this.parametersField = new EditBox(this.font, width / 2 - 150, height / 2 + 30, 300, 20, Component.literal("Parameters"));
		this.parametersField.setValue(VinURLClient.CONFIG.parameters());
		this.addRenderableWidget(this.parametersField);

		// Done button
		this.doneButton = Button.builder(
			Component.literal("Done"),
			(button) -> {
				VinURLClient.CONFIG.PARAMETERS.set(this.parametersField.getValue());
				VinURLClient.CONFIG.PARAMETERS.save();
				this.onClose();
			}
		).bounds(width / 2 - 50, height / 2 + 70, 100, 20).build();
		this.addRenderableWidget(this.doneButton);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		context.centeredText(this.font, Component.literal("VinURL Config"), this.width / 2, this.height / 2 - 100, 0xFFFFFF);
		context.text(this.font, Component.literal("Parameters:"), this.width / 2 - 160, this.height / 2 + 33, 0xAAAAAA);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
