package com.vinurl.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Draws a 16x16 texture as a nine-patch with 4px corners.
 * Equivalent to owo's NinePatchTexture with the definitions in the original mod's
 * nine_patch_textures/*.json (texture_width/height 16, corner_patch_size 4, no repeat).
 */
public final class NinePatch {
	private static final int TEXTURE_SIZE = 16;
	private static final int CORNER = 4;

	private NinePatch() {}

	public static void draw(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height) {
		int w = width;
		int h = height;
		int c = CORNER;
		int s = TEXTURE_SIZE;
		int xe = x + w - c;
		int ye = y + h - c;

		// corners
		graphics.blit(texture, x, y, x + c, y + c, 0, 0, c, c);
		graphics.blit(texture, xe, y, x + w, y + c, s - c, 0, s, c);
		graphics.blit(texture, x, ye, x + c, y + h, 0, s - c, c, s);
		graphics.blit(texture, xe, ye, x + w, y + h, s - c, s - c, s, s);

		// edges (stretched)
		graphics.blit(texture, x + c, y, xe, y + c, c, 0, s - c, c);
		graphics.blit(texture, x + c, ye, xe, y + h, c, s - c, s - c, s);
		graphics.blit(texture, x, y + c, x + c, ye, 0, c, c, s - c);
		graphics.blit(texture, xe, y + c, x + w, ye, s - c, c, s, s - c);

		// center (stretched)
		graphics.blit(texture, x + c, y + c, xe, ye, c, c, s - c, s - c);
	}
}
