package com.vinurl.gui;

import com.vinurl.client.SoundManager;
import com.vinurl.exe.Executable;
import com.vinurl.net.ServerEvent;
import com.vinurl.util.Url;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

import static com.vinurl.client.VinURLConfig.CONFIG;
import static com.vinurl.util.Constants.*;

/**
 * Vanilla rewrite of the owo-ui XML screen of the original mod:
 * same layout (URL text field with simulate + lock buttons, duration slider below),
 * same textures, drawn as nine-patches with 4px corners.
 */
public class URLDiscScreen extends Screen {
        private static final int PANEL_PADDING = 15;
        private static final int TEXT_FIELD_SIZE = 110;
        private static final int BUTTON_SIZE = 16;
        private static final int BUTTON_MARGIN = 5;
        private static final int SLIDER_WIDTH = TEXT_FIELD_SIZE + BUTTON_SIZE + BUTTON_SIZE + BUTTON_MARGIN * 3;
        private static final int SLIDER_HEIGHT = 10;
        private static final int PANEL_WIDTH = SLIDER_WIDTH + PANEL_PADDING * 2;
        private static final int PANEL_HEIGHT = BUTTON_SIZE + BUTTON_MARGIN + SLIDER_HEIGHT + PANEL_PADDING * 2;

        private static final Identifier TEXT_FIELD_TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/text_field.png");
        private static final Identifier TEXT_FIELD_DISABLED_TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/text_field_disabled.png");

        private String url;
        private boolean lock;
        private boolean simulate;
        private int duration;

        private EditBox urlTextbox;
        private DurationSlider durationSlider;
        private TextureButton lockButton;
        private TextureButton simulateButton;

        public URLDiscScreen(String defaultURL, int defaultDuration) {
                super(Component.literal("VinURL"));
                this.url = defaultURL == null ? "" : defaultURL;
                this.duration = defaultDuration;
        }

        @Override
        protected void init() {
                int x = (this.width - PANEL_WIDTH) / 2 + PANEL_PADDING;
                int y = (this.height - PANEL_HEIGHT) / 2 + PANEL_PADDING;

                this.urlTextbox = new EditBox(this.font, x + 4, y + 3, 100, 10, Component.literal("URL"));
                this.urlTextbox.setMaxLength(ServerEvent.MAX_URL_LENGTH);
                this.urlTextbox.setValue(this.url);
                this.urlTextbox.setHint(Component.translatable("gui.vinurl.textfield.placeholder"));
                this.urlTextbox.setBordered(false);
                this.urlTextbox.setResponder((text) -> this.url = text);
                this.addRenderableWidget(this.urlTextbox);

                this.simulateButton = new TextureButton(x + TEXT_FIELD_SIZE + BUTTON_MARGIN, y, BUTTON_SIZE,
                        (button) -> this.simulateDuration(),
                        () -> this.simulate
                        ? SIMULATE_BUTTON_DISABLED_ID
                                : (this.simulateButton.isActive() && this.simulateButton.isHovered() ? SIMULATE_BUTTON_HOVER_ID : SIMULATE_BUTTON_ID));
                this.simulateButton.setTooltip(Tooltip.create(Component.translatable("gui.vinurl.button.duration.tooltip")));
                this.addRenderableWidget(this.simulateButton);

                this.lockButton = new TextureButton(x + TEXT_FIELD_SIZE + BUTTON_SIZE + BUTTON_MARGIN * 2, y, BUTTON_SIZE,
                        (button) -> this.lock = !this.lock,
                        () -> this.lock ? LOCK_BUTTON_ID : LOCK_BUTTON_DISABLED_ID);
                this.lockButton.setTooltip(Tooltip.create(Component.translatable("gui.vinurl.button.lock")));
                this.addRenderableWidget(this.lockButton);

                this.durationSlider = new DurationSlider(x, y + BUTTON_SIZE + BUTTON_MARGIN);
                this.addRenderableWidget(this.durationSlider);

                this.setInitialFocus(this.urlTextbox);
        }

        private void simulateDuration() {
                if (this.simulate) {return;}
                if (Url.parse(this.url) == null) {return;}

                this.simulate = true;
                this.simulateButton.setTooltip(Tooltip.create(Component.translatable("gui.vinurl.button.duration.tooltip.calculating")));
                Executable.executeCommand(
                        SoundManager.getFileName(this.url) + "/duration",
                        Executable.YT_DLP.getCommandLine().addArguments(new String[] {
                                this.url, "--print", "DURATION: %(duration)d", "--no-playlist",
                                "--js-runtimes", "deno:" + Executable.DENO.FILE_PATH
                        }, false).addArguments(CONFIG.parameters())
                ).subscribe("duration")
                        .onOutput((output) -> {
                                String type = output.substring(0, output.indexOf(':') + 1);
                                String message = output.substring(type.length()).trim();

                                switch (type) {
                                        case "DURATION:" -> this.durationSlider.setFromSeconds(Integer.parseInt(message));
                                        case "WARNING:" -> LOGGER.warn(message);
                                        case "ERROR:" -> LOGGER.error(message);
                                        default -> LOGGER.info(output);
                                }
                        })
                        .onError((error) -> {
                                this.simulateButton.setTooltip(Tooltip.create(Component.translatable("gui.vinurl.button.duration.tooltip")));
                                this.simulate = false;
                        })
                        .onComplete(() -> {
                                this.simulateButton.setTooltip(Tooltip.create(Component.translatable("gui.vinurl.button.duration.tooltip")));
                                this.simulate = false;
                        })
                .start();
        }

        @Override
        public boolean keyPressed(KeyEvent input) {
                if (input.key() == GLFW.GLFW_KEY_ESCAPE || input.key() == GLFW.GLFW_KEY_ENTER) {
                        this.sendSetURL();
                        this.onClose();
                        return true;
                }
                return super.keyPressed(input);
        }

        private void sendSetURL() {
                if (this.minecraft != null && this.minecraft.getConnection() != null) {
                        this.minecraft.getConnection().send(new ServerEvent.SetURLRecord(this.url, this.duration, this.lock));
                }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
                this.renderPanel(graphics);
                super.extractRenderState(graphics, mouseX, mouseY, delta);
                this.renderTextFieldTexture(graphics);

                if (this.durationSlider != null && this.durationSlider.isDragging()) {
                        graphics.setTooltipForNextFrame(
                                this.font,
                                Component.literal("%02d:%02d".formatted(this.duration / 60, this.duration % 60)),
                                mouseX, mouseY
                        );
                }
        }

        private void renderPanel(GuiGraphicsExtractor graphics) {
                int x = (this.width - PANEL_WIDTH) / 2;
                int y = (this.height - PANEL_HEIGHT) / 2;
                graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xF0111111);
                graphics.outline(x, y, PANEL_WIDTH, PANEL_HEIGHT, 0xFF333333);
        }

        private void renderTextFieldTexture(GuiGraphicsExtractor graphics) {
                if (this.urlTextbox == null) {return;}
                int x = (this.width - PANEL_WIDTH) / 2 + PANEL_PADDING;
                int y = (this.height - PANEL_HEIGHT) / 2 + PANEL_PADDING;
                Identifier texture = this.urlTextbox.isFocused() ? TEXT_FIELD_TEXTURE : TEXT_FIELD_DISABLED_TEXTURE;
                graphics.blit(texture, x, y, x + TEXT_FIELD_SIZE, y + BUTTON_SIZE, 0, 0, TEXT_FIELD_SIZE, BUTTON_SIZE);
        }

        @Override
        public boolean isPauseScreen() {
                return false;
        }

        /** Horizontal slider for the song duration in seconds (0..3600), like owo's SlimSlider. */
        private class DurationSlider extends AbstractSliderButton {
                public DurationSlider(int x, int y) {
                        super(x, y, SLIDER_WIDTH, SLIDER_HEIGHT, Component.empty(), URLDiscScreen.this.duration / (double) ServerEvent.MAX_DURATION);
                        this.updateMessage();
                }

                public void setFromSeconds(int seconds) {
                        this.setValue(seconds / (double) ServerEvent.MAX_DURATION);
                        this.updateMessage();
                }

                public boolean isDragging() {
                        return this.dragging;
                }

                @Override
                protected void updateMessage() {
                        this.setMessage(Component.literal("%02d:%02d".formatted(URLDiscScreen.this.duration / 60, URLDiscScreen.this.duration % 60)));
                }

                @Override
                protected void applyValue() {
                        URLDiscScreen.this.duration = (int) Math.round(this.value * ServerEvent.MAX_DURATION);
                }

                @Override
                public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
                        this.setValue(this.value + scrollY / ServerEvent.MAX_DURATION);
                        this.updateMessage();
                        return true;
                }
        }

        /** 16x16 button drawing one of the mod's nine-patch GUI textures. */
        private class TextureButton extends Button {
                private final Supplier<Identifier> texture;

                public TextureButton(int x, int y, int size, OnPress onPress, Supplier<Identifier> texture) {
                        super(x, y, size, size, Component.empty(), onPress, DEFAULT_NARRATION);
                        this.texture = texture;
                }

                @Override
                protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                        NinePatch.draw(graphics, this.texture.get(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
                }
        }
}
