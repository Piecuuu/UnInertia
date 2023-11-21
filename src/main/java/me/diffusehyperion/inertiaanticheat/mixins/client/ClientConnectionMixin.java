package me.diffusehyperion.inertiaanticheat.mixins.client;

import me.diffusehyperion.inertiaanticheat.interfaces.ClientConnectionMixinInterface;
import me.diffusehyperion.inertiaanticheat.packets.ClientUpgradedQueryPacketListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin implements ClientConnectionMixinInterface {
    @Unique
    private boolean upgraded;

    @Unique
    @Override
    public void inertiaAntiCheat$connect(String string, int i, ClientUpgradedQueryPacketListener clientUpgradedQueryPacketListener) {
        this.upgraded = true;
        this.connect(string, i, clientUpgradedQueryPacketListener, ConnectionIntent.STATUS);
    }

    @Unique
    @Override
    public boolean inertiaAntiCheat$isUpgraded() {
        return upgraded;
    }

    @Shadow
    private void connect(String address, int port, PacketListener listener, ConnectionIntent intent) {}
}
