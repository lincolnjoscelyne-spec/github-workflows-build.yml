package com.example.dupebutton;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;

/**
 * The "Dupe" macro, as a small tick-driven state machine:
 * <ol>
 *   <li>Start holding outgoing container packets and shift-click the first item in the chest.</li>
 *   <li>Wait {@link DupeConfig#delayTicks} ticks.</li>
 *   <li>Close the screen (the close packet is captured, not sent).</li>
 *   <li>Release: the held click is sent, the held close packet is discarded.</li>
 * </ol>
 * Whether this duplicates anything depends entirely on the server having a matching bug.
 * Tune the sequence here.
 */
public final class MacroRunner {
	private MacroRunner() {}

	private static final int MAX_TICKS = 200; // safety net so packets are never held forever

	private static int stage = 0;
	private static int wait = 0;
	private static int elapsed = 0;

	public static boolean isRunning() {
		return stage != 0;
	}

	public static void start(Minecraft mc) {
		if (stage != 0 || mc.player == null || mc.gameMode == null) {
			return;
		}
		if (!(mc.screen instanceof ContainerScreen)) {
			return;
		}
		elapsed = 0;
		stage = 1;
	}

	public static void tick(Minecraft mc) {
		if (stage == 0) {
			return;
		}
		if (mc.player == null || mc.gameMode == null || mc.getConnection() == null || ++elapsed > MAX_TICKS) {
			abort(mc);
			return;
		}

		switch (stage) {
			case 1 -> {
				AbstractContainerMenu menu = mc.player.containerMenu;
				int slot = firstFilledChestSlot(menu);
				if (slot < 0) {
					stage = 0;
					return;
				}
				PacketHold.begin();
				// Version-sensitive: older versions call this handleInventoryMouseClick with a ClickType.
				mc.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.QUICK_MOVE, mc.player);
				wait = DupeConfig.delayTicks;
				stage = 2;
			}
			case 2 -> {
				if (wait-- > 0) {
					return;
				}
				// Closing the screen directly never sends a close packet, and the client keeps the chest
				// as its current menu. That is the "close without packet" desync. (If a close packet is
				// ever sent here, PacketHold captures it and discards it on release.)
				mc.setScreen(null);
				stage = 3;
			}
			case 3 -> {
				PacketHold.release(mc);
				stage = 0;
			}
			default -> stage = 0;
		}
	}

	private static void abort(Minecraft mc) {
		stage = 0;
		if (mc.getConnection() != null) {
			PacketHold.release(mc);
		} else {
			PacketHold.clear();
		}
	}

	private static int firstFilledChestSlot(AbstractContainerMenu menu) {
		if (!(menu instanceof ChestMenu chest)) {
			return -1;
		}
		int chestSlots = chest.getRowCount() * 9;
		for (int i = 0; i < chestSlots; i++) {
			if (menu.slots.get(i).hasItem()) {
				return i;
			}
		}
		return -1;
	}
}
