package com.example.dupebutton;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class DupeButtonClient implements ClientModInitializer {
	public static final String MOD_ID = "dupebutton";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final int GUI_WIDTH = 176;
	private static final int BUTTON_WIDTH = 44;
	private static final int BUTTON_HEIGHT = 20;
	private static final long REPEAT_GUARD_MS = 250;

	private static long lastToggleKeyEvent = 0;

	/** Rebindable in Options > Controls > Key Binds > "Dupe Button". Default: Right Alt. */
	public static KeyMapping toggleKey;

	/** The button on the currently open chest screen, if any. */
	private static Button dupeButton;

	@Override
	public void onInitializeClient() {
		DupeConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.dupebutton.toggle_label",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_ALT,
				category));

		// Toggle while no screen is open, and drive the macro.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				toggleButton();
			}
			MacroRunner.tick(client);
		});

		// Add the button to chest screens and listen for the toggle key while they are open.
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof ContainerScreen chestScreen)) {
				dupeButton = null;
				return;
			}

			int rows = chestScreen.getMenu().getRowCount();
			int imageHeight = 114 + rows * 18;
			int x = (scaledWidth + GUI_WIDTH) / 2 + 4; // just right of the chest GUI
			int y = (scaledHeight - imageHeight) / 2;

			Button button = Button.builder(Component.translatable("dupebutton.button"),
							b -> MacroRunner.start(Minecraft.getInstance()))
					.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
					.build();
			button.visible = DupeConfig.showButton;
			button.active = DupeConfig.showButton;

			dupeButton = button;
			Screens.getWidgets(screen).add(button);

			ScreenKeyboardEvents.beforeKeyPress(screen).register((s, keyEvent) -> {
				if (!toggleKey.matches(keyEvent)) {
					return;
				}
				// Holding a key sends repeat events every few ms; only toggle on a fresh press.
				long now = System.currentTimeMillis();
				boolean freshPress = now - lastToggleKeyEvent > REPEAT_GUARD_MS;
				lastToggleKeyEvent = now;
				if (freshPress) {
					toggleButton();
				}
			});
		});
	}

	private static void toggleButton() {
		DupeConfig.showButton = !DupeConfig.showButton;
		DupeConfig.save();
		if (dupeButton != null) {
			dupeButton.visible = DupeConfig.showButton;
			dupeButton.active = DupeConfig.showButton;
		}
	}
}
