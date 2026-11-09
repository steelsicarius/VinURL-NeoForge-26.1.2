package com.vinurl.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Draws a 16x16 texture as a nine-patch with 4px corners.
 * Equivalent to owo's NinePatchTexture with the definitions in the original mod's
 * nine_patch_textures/*.json (texture_width/height 16, corner_patch_size 4, no repeat).
 *
 * GuiGraphicsExtractor.blit(Identifier, x0, y0, x1, y1, u0, u1, v0, v1) takes NORMALIZED
 * UV PAIRS (source rect from (u0,v0) to (u1,v1)), not offset+size. The 2.6.4 version
 * passed offset+size pairs here, so every patch except the top-left/edges sampled an
 * inverted or empty UV range and the buttons rendered invisible/mangled.
 */
public final class NinePatch {
	private static final float TEXTURE_SIZE = 16;
	private static final float CORNER = 4;

	private NinePatch() {}

	public static void draw(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height) {
		float s = TEXTURE_SIZE;
		float c = CORNER;
		float u0 = 0f, uc = c / s, ue = (s - c) / s, u1 = 1f;
		float v0 = 0f, vc = c / s, ve = (s - c) / s, v1 = 1f;

		int xe = x + width - (int) c;
		int ye = y + height - (int) c;

		// corners (1:1)
		graphics.blit(texture, x, y, x + (int) c, y + (int) c, u0, uc, v0, vc);
		graphics.blit(texture, xe, y, x + width, y + (int) c, ue, u1, v0, vc);
		graphics.blit(texture, x, ye, x + (int) c, y + height, u0, uc, ve, v1);
		graphics.blit(texture, xe, ye, x + width, y + height, ue, u1, ve, v1);

		// edges (stretched)
		graphics.blit(texture, x + (int) c, y, xe, y + (int) c, uc, ue, v0, vc);
		graphics.blit(texture, x + (int) c, ye, xe, y + height, uc, ue, ve, v1);
		graphics.blit(texture, x, y + (int) c, x + (int) c, ye, u0, uc, vc, ve);
		graphics.blit(texture, xe, y + (int) c, x + width, ye, ue, u1, vc, ve);

		// center (stretched)
		graphics.blit(texture, x + (int) c, y + (int) c, xe, ye, uc, ue, vc, ve);
	}
}
