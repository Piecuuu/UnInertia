package me.diffusehyperion.inertiaanticheat.packets.C2S;

import me.diffusehyperion.inertiaanticheat.packets.ServerUpgradedQueryPacketListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class CommunicateRequestEncryptedC2SPacket implements Packet<ServerUpgradedQueryPacketListener> {
    private final byte[] encryptedAESSerializedModlist;
    private final byte[] encrypytedRSAAESKey;

    public CommunicateRequestEncryptedC2SPacket(byte[] encryptedAESModlistHash, byte[] encrypytedRSAAESKey) {
        this.encryptedAESSerializedModlist = encryptedAESModlistHash;
        this.encrypytedRSAAESKey = encrypytedRSAAESKey;
    }

    public CommunicateRequestEncryptedC2SPacket(PacketByteBuf packetByteBuf) {
        int length = packetByteBuf.readInt();

        byte[] encryptedModlistHash = new byte[length];
        packetByteBuf.readBytes(encryptedModlistHash);
        this.encryptedAESSerializedModlist = encryptedModlistHash;

        byte[] encryptedAESKey = new byte[packetByteBuf.readableBytes()];
        packetByteBuf.readBytes(encryptedAESKey);
        this.encrypytedRSAAESKey = encryptedAESKey;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(this.encryptedAESSerializedModlist.length);
        buf.writeBytes(this.encryptedAESSerializedModlist);
        buf.writeBytes(this.encrypytedRSAAESKey);
    }

    @Override
    public void apply(ServerUpgradedQueryPacketListener listener) {
        listener.onCommunicateEncryptedRequest(this);
    }

    public byte[] getEncryptedAESSerializedModlist() {
        return this.encryptedAESSerializedModlist;
    }

    public byte[] getEncrypytedRSAAESKey() {
        return this.encrypytedRSAAESKey;
    }
}