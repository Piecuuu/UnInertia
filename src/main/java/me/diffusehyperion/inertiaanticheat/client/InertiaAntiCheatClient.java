package me.diffusehyperion.inertiaanticheat.client;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InertiaAntiCheatClient implements ClientModInitializer {
    public static Toml clientConfig;
    public static final List<byte[]> allModData = new ArrayList<>();
    public static List<String> hiddenMods;

    @Override
    public void onInitializeClient() {
        InertiaAntiCheatClient.clientConfig = InertiaAntiCheat.initializeConfig("/config/client/InertiaAntiCheat.toml", InertiaAntiCheatConstants.CURRENT_CLIENT_CONFIG_VERSION);
        InertiaAntiCheatClient.hiddenMods = InertiaAntiCheatClient.clientConfig.getList("mods.hiddenMods");

        this.setupModDataList();
        ClientLoginModlistTransferHandler.init();
    }

    public void setupModDataList() {
        try {
            File modDirectory = FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
            for (File modFile : Objects.requireNonNull(modDirectory.listFiles())) {
                if (modFile.isDirectory() || InertiaAntiCheatClient.hiddenMods.contains(modFile.getName())) {
                    continue;
                }
                InertiaAntiCheatClient.allModData.add(Files.readAllBytes(modFile.toPath()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
