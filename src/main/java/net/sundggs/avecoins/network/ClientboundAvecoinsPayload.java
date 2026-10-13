/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClientboundAvecoinsPayload(String action, String data) implements CustomPacketPayload
{
    public static final int MAX_ACTION_LENGTH = 64;
    public static final int MAX_DATA_LENGTH = 0x200000;
    public static final CustomPacketPayload.Type<ClientboundAvecoinsPayload> TYPE = new CustomPacketPayload.Type(Identifier.fromNamespaceAndPath((String)"avecoins", (String)"client"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundAvecoinsPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.stringUtf8((int)64), ClientboundAvecoinsPayload::action, (StreamCodec)ByteBufCodecs.stringUtf8((int)0x200000), ClientboundAvecoinsPayload::data, ClientboundAvecoinsPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

