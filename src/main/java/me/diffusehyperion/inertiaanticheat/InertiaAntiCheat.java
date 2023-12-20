package me.diffusehyperion.inertiaanticheat;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants.MODLOGGER;
import static me.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient.clientConfig;
import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.serverConfig;

public class InertiaAntiCheat implements ModInitializer {

    @Override
    public void onInitialize() {
        info("Initializing InertiaAntiCheat!");
        info(FabricLoader.getInstance().getGameDir().resolve("mods").resolve("InertiaAntiCheat-0.0.6.2.jar").toFile().getName());
        info(FabricLoader.getInstance().getGameDir().resolve("mods").resolve("InertiaAntiCheat-0.0.6.2.jar").toFile().getPath());
        info("Exists: " + FabricLoader.getInstance().getGameDir().resolve("mods").resolve("InertiaAntiCheat-0.0.6.2.jar").toFile().exists());
        try {
            Files.createDirectories(getConfigDir());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void info(String info) {
        MODLOGGER.info("[InertiaAntiCheat] " + info);
    }
    public static void warn(String info) {
        MODLOGGER.warn("[InertiaAntiCheat] " + info);
    }
    public static void error(String info) {
        MODLOGGER.error("[InertiaAntiCheat] " + info);
    }

    public static void debugInfo(String info) {
        if ((Objects.nonNull(serverConfig) && serverConfig.getBoolean("debug.debug")) || (Objects.nonNull(clientConfig) && clientConfig.getBoolean("debug.debug"))) {
            info(info);
        }
    }

    public static void debugWarn(String info) {
        if ((Objects.nonNull(serverConfig) && serverConfig.getBoolean("debug.debug")) || (Objects.nonNull(clientConfig) && clientConfig.getBoolean("debug.debug"))){
            warn(info);
        }
    }

    public static void debugError(String info) {
        if ((Objects.nonNull(serverConfig) && serverConfig.getBoolean("debug.debug")) || (Objects.nonNull(clientConfig) && clientConfig.getBoolean("debug.debug"))){
            error(info);
        }
    }

    public static void debugException(Exception exception) {
        if ((Objects.nonNull(serverConfig) && serverConfig.getBoolean("debug.debug")) || (Objects.nonNull(clientConfig) && clientConfig.getBoolean("debug.debug"))){
            error(exception.getMessage());
            error(Arrays.toString(exception.getStackTrace()));
        }
    }

    public static String listToPrettyString(List<String> list) {
        switch (list.size()) {
            case 0 -> {
                return "";
            }
            case 1 -> {
                return list.get(0);
            }
            default -> {
                StringBuilder builder = new StringBuilder();
                builder.append(list.get(0));
                for (int i = 1; i < list.size(); i++) {
                    if (i != (list.size() - 1)) {
                        builder.append(", ");
                        builder.append(list.get(i));
                    } else {
                        builder.append(" and ");
                        builder.append(list.get(i));
                    }
                }
                return builder.toString();
            }
        }
    }

    public static String getHash(String input, String defaultAlgorithm) {
        try {
            String algorithm;
            if (serverConfig == null) {
                algorithm = defaultAlgorithm;
            } else {
                algorithm = serverConfig.getString("hash.algorithm", defaultAlgorithm);
            }
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] arr = md.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(arr);
        } catch (NoSuchAlgorithmException e){
            throw new RuntimeException("Invalid algorithm provided! Did you use an accepted algorithm in your config?", e);
        }
    }

    public static Toml initializeConfig(String defaultConfigPath, Long currentConfigVersion) {
        File configFile = getConfigDir().resolve("./InertiaAntiCheat.toml").toFile();
        if (!configFile.exists()) {
            warn("No config file found! Creating a new one now...");
            try {
                Files.copy(Objects.requireNonNull(InertiaAntiCheatServer.class.getResourceAsStream(defaultConfigPath)), configFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't a create default config!", e);
            }
        }
        Toml config = new Toml().read(configFile);
        if (!Objects.equals(config.getLong("debug.version", 0L), currentConfigVersion)) {
            warn("Looks like your config file is outdated! Backing up current config, then creating an updated config.");
            warn("Your config file will be backed up to \"BACKUP-InertiaAntiCheat.toml\".");
            File backupFile = getConfigDir().resolve("BACKUP-InertiaAntiCheat.toml").toFile();
            try {
                Files.copy(configFile.toPath(), backupFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't copy existing config file into a backup config file! Please do it manually.", e);
            }
            if (!configFile.delete()) {
                throw new RuntimeException("Couldn't delete config file! Please delete it manually.");
            }
            try {
                Files.copy(Objects.requireNonNull(InertiaAntiCheatServer.class.getResourceAsStream(defaultConfigPath)), configFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't create a default config!", e);
            }
            info("Done! Please readjust the configs in the new file accordingly.");
        }
        return config;
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("InertiaAntiCheat");
    }

    public static PublicKey retrievePublicKey(PacketByteBuf packetByteBuf) {
        byte[] rawPublicKeyBytes = new byte[packetByteBuf.readableBytes()];
        packetByteBuf.readBytes(rawPublicKeyBytes);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(rawPublicKeyBytes);
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encryptAESBytes(byte[] input, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(input);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decryptAESBytes(byte[] input, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(input);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encryptRSABytes(byte[] input, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(input);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decryptRSABytes(byte[] input, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(input);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static SecretKey createAESKey(File secretKeyFile) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();

            secretKeyFile.createNewFile();
            Files.write(secretKeyFile.toPath(), secretKey.getEncoded());

            debugInfo("Secret key MD5 hash: " + InertiaAntiCheat.getHash(Arrays.toString(secretKey.getEncoded()), "MD5"));
            return secretKey;
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SecretKey loadAESKey(File secretKeyFile) {
        try {
            Path privateKeyFilePath = Paths.get(secretKeyFile.toURI());
            byte[] privateKeyFileBytes = Files.readAllBytes(privateKeyFilePath);
            return new SecretKeySpec(privateKeyFileBytes, "AES");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyPair createRSAPair(File publicKeyFile, File privateKeyFile) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            privateKeyFile.createNewFile();
            publicKeyFile.createNewFile();
            Files.write(privateKeyFile.toPath(), privateKey.getEncoded());
            Files.write(publicKeyFile.toPath(), publicKey.getEncoded());

            debugInfo("Private key MD5 hash: " + InertiaAntiCheat.getHash(Arrays.toString(privateKey.getEncoded()), "MD5"));
            debugInfo("Public key MD5 hash: " + InertiaAntiCheat.getHash(Arrays.toString(publicKey.getEncoded()), "MD5"));

            return new KeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Something went wrong while generating new key pairs!", e);
        }
    }

    public static KeyPair loadRSAPair(File publicKeyFile, File privateKeyFile) {
        try {
            Path privateKeyFilePath = Paths.get(privateKeyFile.toURI());
            byte[] privateKeyFileBytes = Files.readAllBytes(privateKeyFilePath);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyFileBytes);

            Path publicKeyFilePath = Paths.get(publicKeyFile.toURI());
            byte[] publicKeyFileBytes = Files.readAllBytes(publicKeyFilePath);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyFileBytes);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            debugInfo("Private key MD5 hash: " + InertiaAntiCheat.getHash(Arrays.toString(privateKey.getEncoded()), "MD5"));
            debugInfo("Public key MD5 hash: " + InertiaAntiCheat.getHash(Arrays.toString(publicKey.getEncoded()), "MD5"));

            return new KeyPair(publicKey, privateKey);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Something went wrong while reading key pairs!", e);
        }
    }
}
