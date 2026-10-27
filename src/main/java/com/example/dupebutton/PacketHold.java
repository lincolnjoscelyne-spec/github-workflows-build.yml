package com.example.dupebutton;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

/**
 * Holds back outgoing container packets while the macro runs. Fed by
 * {@code ClientCommonPacketListenerImplMixin}. On release, queued click packets are sent and
 * queued close packets are discarded, so the server never hears the screen was closed.
 */
public final class PacketHold {
	private PacketHold() {}

	private static final List<Packet<?>> QUEUE = new ArrayList<>();
	private static boolean holding = false;

	public static void begin() {
		QUEUE.clear();
		holding = true;
	}

	/** Returns true if the packet was captured and must NOT be sent now. */
	public static boolean intercept(Packet<?> packet) {
		if (!holding) {
			return false;
		}
		if (packet instanceof ServerboundContainerClickPacket || packet instanceof ServerboundContainerClosePacket) {
			QUEUE.add(packet);
			return true;
		}
		return false;
	}

	public static void release(Minecraft mc) {
		holding = false;
		List<Packet<?>> toSend = new ArrayList<>(QUEUE);
		QUEUE.clear();

		ClientPacketListener connection = mc.getConnection();
		if (connection == null) {
			return;
		}
		for (Packet<?> packet : toSend) {
			if (packet instanceof ServerboundContainerClosePacket) {
				continue; // dropped on purpose
			}
			connection.send(packet);
		}
	}

	/** Drops everything without sending (used when aborting, e.g. on disconnect). */
	public static void clear() {
		holding = false;
		QUEUE.clear();
	}
}
