/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.network;

import java.nio.charset.StandardCharsets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.sundggs.avecoins.AvecoinsMod;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;

public final class ClientNetworking {
    public static void send(ServerboundAvecoinsPayload payload) {
        if (payload.action().length() > 64 || payload.data().getBytes(StandardCharsets.UTF_8).length > 65536) {
            AvecoinsMod.LOGGER.error("Refused oversized AVECOINS serverbound payload '{}'", (Object)payload.action());
            return;
        }
        ClientPlayNetworking.send((CustomPacketPayload)payload);
    }

    private ClientNetworking() {
    }
}

