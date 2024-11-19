package de.skyh.skyhaven.config;

import de.skyh.skyhaven.Skyhaven;
import de.skyh.skyhaven.util.Utils;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.http.conn.ssl.SSLContexts;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Key and secret holder in its own file to avoid people leaking their keys accidentally.
 */
public class CredentialStorage {
    public static String skyh;
    public static boolean isSkyhValid;
    public static SSLContext sslContext;
    private Property propSkyh;
    private Property propIsSkyhValid;
    private final Configuration cfg;

    public CredentialStorage(Configuration configuration) {
        cfg = configuration;
        initConfig();
        verifyNewerHttpsSupport();
    }

    private void initConfig() {
        cfg.load();
        propSkyh = cfg.get(Configuration.CATEGORY_CLIENT,
                        "skyh", "", "Don't share this with anybody! Do not edit this entry manually either!", Utils.VALID_UUID_PATTERN)
                .setShowInGui(false);
        propSkyh.setLanguageKey(Skyhaven.MODID + ".config." + propSkyh.getName());

        propIsSkyhValid = cfg.get(Configuration.CATEGORY_CLIENT,
                        "isSkyhValid", false, "Is the value valid?")
                .setShowInGui(false);
        skyh = propSkyh.getString();
        isSkyhValid = propIsSkyhValid.getBoolean();
        if (cfg.hasChanged()) {
            cfg.save();
        }
    }

    private void verifyNewerHttpsSupport() {
        String javaVersion = System.getProperty("java.version", "unknown java version");
        Pattern javaVersionPattern = Pattern.compile("1\\.8\\.0_(\\d+)"); // e.g. 1.8.0_51
        Matcher javaVersionMatcher = javaVersionPattern.matcher(javaVersion);
        if (!javaVersionMatcher.matches()
                || MathHelper.parseIntWithDefault(javaVersionMatcher.group(1), 1337) >= 101) {
            // newer Java version (>=8u101): *should* already have support by default
            return;
        }

        // running Java <8u101
        if (testNewHttps()) {
            // uhm... looks like someone added the certs to the default JKS already
            return;
        }
        Skyhaven.getInstance().getLogger().info("Injecting Let's Encrypt support due to ancient Java version...");
        try {
            KeyStore originalKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            originalKeyStore.load(Files.newInputStream(Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts")), "changeit".toCharArray());

            KeyStore skyhKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            skyhKeyStore.load(getClass().getResourceAsStream("/https-for-ancient-java.jks"), "skyhveit".toCharArray());

            KeyStore tempKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            tempKeyStore.load(null, "skyhvedit".toCharArray());

            for (Enumeration<String> aliases = originalKeyStore.aliases(); aliases.hasMoreElements(); ) {
                String alias = aliases.nextElement();
                tempKeyStore.setCertificateEntry(alias, originalKeyStore.getCertificate(alias));
            }
            for (Enumeration<String> aliases = skyhKeyStore.aliases(); aliases.hasMoreElements(); ) {
                String alias = aliases.nextElement();
                tempKeyStore.setCertificateEntry(alias, skyhKeyStore.getCertificate(alias));
            }
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(tempKeyStore, "skyhvedit".toCharArray())
                    .loadTrustMaterial(tempKeyStore, null)
                    .build();

            if (!testNewHttps()) {
                System.err.println("Error while trying to add Let's Encrypt support: Could not contact site after running setup");
            }
        } catch (GeneralSecurityException | IOException e) {
            System.err.println("Error while trying to add Let's Encrypt support:");
            e.printStackTrace();
        }
    }

    /**
     * Tests accessing a site that uses Let's Encrypt
     *
     * @return true, if connection was successful
     */
    private boolean testNewHttps() {
        try {
            URLConnection connection = new URL("https://helloworld.letsencrypt.org/").openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(8000);
            connection.setDoInput(false);
            connection.setDoOutput(false);
            connection.addRequestProperty("User-Agent", "Forge Mod " + Skyhaven.MODNAME + "/" + Skyhaven.VERSION + " (" + Skyhaven.GITURL + ")");
            if (CredentialStorage.sslContext != null && connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(CredentialStorage.sslContext.getSocketFactory());
            }

            connection.connect();

            // seems to be working since there was no IOException!
            return true;
        } catch (IOException ignored) {
            // most likely a newer https related issue, so setup might fix things
        }
        return false;
    }

    public void setSkyh(String skyh) {
        CredentialStorage.skyh = skyh;
        propSkyh.set(skyh);
        setSkyhValidity(true);
    }

    public void setSkyhValidity(boolean isSkyhValid) {
        CredentialStorage.isSkyhValid = isSkyhValid;
        propIsSkyhValid.set(isSkyhValid);
        cfg.save();
    }

    public Property getPropIsSkyhValid() {
        return propIsSkyhValid;
    }
}
