package com.prupe.mcpatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing a Minecraft version number, e.g., 1.8.1 or 1.9pre1.
 */
final public class MinecraftVersion implements Comparable<MinecraftVersion> {
    private static final String VERSION_PATTERN = "(\\d[_.0-9]*[a-z]?)";
    private static final Pattern PRERELEASE_PATTERN = Pattern.compile(
        "-?(?:pre|rc)(\\d+)$",
        Pattern.CASE_INSENSITIVE
    );
    private static final int NOT_PRERELEASE = 9999;

    private static enum Era {
        RD("rd-(\\d+)"),
        CLASSIC("c(" + VERSION_PATTERN + "([a-z][_.0-9]*)?)"),
        //INDEV("..."), TODO
        INFDEV("inf-(\\d+)"),
        ALPHA("a" + VERSION_PATTERN),
        BETA("b" + VERSION_PATTERN),
        RC("rc\\s?(\\d+)"),
        FINAL(VERSION_PATTERN),
        SNAPSHOT("(\\d+w\\d+[a-z])", FINAL);

        private final Pattern pattern;
        private final Era parent;

        Era(String pattern) {
            this(pattern, null);
        }

        Era(String pattern, Era parent) {
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            this.parent = parent;
        }

        boolean isSnapshot() {
            return parent != null;
        }

        int effectiveOrder() {
            return parent == null ? ordinal() : parent.effectiveOrder();
        }

        Matcher matcher(CharSequence input) {
            return pattern.matcher(input);
        }
    }

    private final String versionString;
    private final int[] versionNumbers;
    private final boolean snapshot;
    private final int preRelease;

    private static final Map<String, MinecraftVersion> knownVersions = new HashMap<String, MinecraftVersion>();
    private static final Map<MinecraftVersion, String> knownMD5s = new HashMap<MinecraftVersion, String>();
    private static final Map<String, MinecraftVersion> alternateMD5s = new HashMap<String, MinecraftVersion>();
    private static final List<MinecraftVersion> versionOrdering = new ArrayList<MinecraftVersion>();
    private static MinecraftVersion highestKnownVersion;

    static {
        try {
            addKnownVersion("a1.2.6", "ddd5e39467f28d1ea1a03b4d9e790867");

            addKnownVersion("b1.0.2", "d200c465b8c167cc8df6537531fc9a48");

            addKnownVersion("b1.1_02", "7d547e495a770c62054ef136add43034");

            addKnownVersion("b1.2", "6426223efe23c3931a4ef89685be3349");
            addKnownVersion("b1.2_01", "486d83ec00554b45ffa21af2faa0116a");
            addKnownVersion("b1.2_02", "6d0d87905008db07bb80c61d7e8f9fb6");

            addKnownVersion("b1.3", "de2164df461d028229ed2e101181bbd4");
            addKnownVersion("b1.3_01", "4203826f35e1036f089919032c3d19d1");

            addKnownVersion("b1.4", "71e64b61175b371ed148b385f2d14ebf");
            addKnownVersion("b1.4_01", "9379e54b581ba4ef3acc3e326e87db91");

            addKnownVersion("b1.5", "24289130902822d73f8722b52bc07cdb");
            addKnownVersion("b1.5_01", "d02fa9998e30693d8d989d5f88cf0040");

            addKnownVersion("b1.6", "d531e221227a65392259d3141893280d");
            addKnownVersion("b1.6.1", "a7e82c441a57ef4068c533f4d777336a");
            addKnownVersion("b1.6.2", "01330b1c930102a683a4dd8d792e632e");
            addKnownVersion("b1.6.4", "5c4df6f120336f113180698613853dba");
            addKnownVersion("b1.6.5", "2aba888864b32038c8d22ee5df71b7c8");
            addKnownVersion("b1.6.6", "ce80072464433cd5b05d505aa8ff29d1");

            addKnownVersion("b1.7_01", "defc33265dfbb21afb37da7c400800e9");
            addKnownVersion("b1.7.2", "dd9215ab1141170d4871f42bff4ab302");
            addKnownVersion("b1.7.3", "eae3353fdaa7e10a59b4cb5b45bfa10d");

            addKnownVersion("b1.8pre1", "7ce3238b148bb67a3b84cf59b7516f55");
            addKnownVersion("b1.8pre2", "bff1cf2e4586012ac8907b8e7945d4c3");
            addKnownVersion("b1.8", "a59a9fd4c726a573b0a2bdd10d857f59");
            addKnownVersion("b1.8.1", "f8c5a2ccd3bc996792bbe436d8cc08bc");

            addKnownVersion("b1.9pre1", "b4d9681a1118949d7753e19c35c61ec7");
            addKnownVersion("b1.9pre2", "962d79abeca031b44cf8dac8d4fcabe9");
            addKnownVersion("b1.9pre3", "334827dbe9183af6d650b39321a99e21");
            addKnownVersion("b1.9pre4", "cae41f3746d3c4c440b2d63a403770e7");
            addKnownVersion("b1.9pre5", "6258c4f293b939117efe640eda76dca4");
            addKnownVersion("b1.9pre6", "2468205154374afe5f9caaba2ffbf5f8");

            addKnownVersion("rc1", "22d708f84dc44fba200c2a5e4261959c");
            addKnownVersion("rc2pre1", "e8e264bcff34aecbc7ef7f850858c1d6");
            addKnownVersion("rc2", "bd569d20dd3dd898ff4371af9bbe14e1");

            addKnownVersion("1.0.0", "3820d222b95d0b8c520d9596a756a6e6");

            addKnownVersion("11w47a", "2ad75c809570663ec561ca707983a45b");
            addKnownVersion("11w48a", "cd86517284d62a0854234ae12abd019c");
            addKnownVersion("11w49a", "a1f7969b6b546c492fecabfcb8e8525a");
            addKnownVersion("11w50a", "8763eb2747d57e2958295bbd06e764b1");

            addKnownVersion("12w01a", "468f1b4022eb81d5ca2f316e24a7ffe5");

            addKnownVersion("1.1", "e92302d2acdba7c97e0d8df1e10d2006");

            addKnownVersion("12w03a", "ea85d9c4058ba9e47d8130bd1bff8be9");
            addKnownVersion("12w04a", "c2e2d8c38288ac122001f2ed11c4d83a");
            addKnownVersion("12w05a", "feabb7967bd528a9f3309a2d660d555d");
            addKnownVersion("12w05b", "70affb4ae7da7e8b24f1bbbcbe58cf0f");
            addKnownVersion("12w06a", "9cfaa4adec02642574ffb7c23a084d74");
            addKnownVersion("12w07a", "d60621a26a64f3bda2849c32da6765c6");
            addKnownVersion("12w07b", "88a9a9055d0d1d17b1c797e280508d83");
            addKnownVersion("12w08a", "1d04d6b190a2ad14d8996802b9286bef");

            addKnownVersion("1.2", "ee18a8cc1db8d15350bceb6ee71292f4");
            addKnownVersion("1.2.2", "6189e96efaea11e5164b4a4755574324");
            addKnownVersion("1.2.3", "12f6c4b1bdcc63f029e3c088a364b8e4");
            addKnownVersion("1.2.4", "25423eab6d8707f96cc6ad8a21a7250a");
            addKnownVersion("1.2.5", "8e8778078a175a33603a585257f28563");

            addKnownVersion("12w15a", "90626a5c36f87aadbc7e79da1f076e93");
            addKnownVersion("12w16a", "19ec24b0987e93da972147d1788c5227");
            addKnownVersion("12w17a", "fc5826a699541df023762c6b8516e20e");
            addKnownVersion("12w18a", "63bdc3586a192ddd13e7a8c08e864ec4");
            addKnownVersion("12w19a", "113b505ad24b11a6cf801bd3516e7cc3");
            addKnownVersion("12w21a", "51ea290e859130e14077758b545e8e91");
            addKnownVersion("12w21b", "57b7376824b6635ea36b7591dd4da3ef");
            addKnownVersion("12w22a", "fed0bfb2b0de4596c81dd698f73bdf4b");
            addKnownVersion("12w23a", "42a509057902760abc3abd7227d028fc");
            addKnownVersion("12w23b", "5798c9af6844333ee82fc9b11c6c47ea");
            addKnownVersion("12w24a", "ac908492cdfe6c1d81183d2d2d7959a1");
            addKnownVersion("12w25a", "b904c9d0d976039047e421a66a1a912d");
            addKnownVersion("12w26a", "c3b849226a93a5aeffcabed61720cf45");
            addKnownVersion("12w27a", "a3412d58aa1e5bcb6472fcf1c7e72ac1");
            addKnownVersion("12w30a", "07dbdc7266019ab1d42c61f42b809f4d");
            addKnownVersion("12w30b", "287bc65621e66a0d2287ff4eb424e90a");
            addKnownVersion("12w30c", "1a1fb1f68354ca0a71fc723f36b97a81");
            addKnownVersion("12w30d", "f2b0315ce33a3b473a523f5fa151a06d");
            addKnownVersion("12w30e", "d466309bafbdece16b0a74a4290dbee1");

            addKnownVersion("1.3", "a6effac1eaccf5d429aae340cf95ed5d");
            addKnownVersion("1.3.1", "266ccbc9798afd2eadf3d6c01b4c562a");
            addKnownVersion("1.3.2", "969699f13e5bbe7f12e40ac4f32b7d9a");

            addKnownVersion("12w32a", "0de5595692a736307e96e3fec050a98e");
            addKnownVersion("12w34a", "562b82c59fa6870c10e79d9474edb356");
            addKnownVersion("12w34b", "d71d0a4013555753dbaa31b3aed02815");
            addKnownVersion("12w36a", "a2924e04c571a3cf7a9d8fb1955e6f4b");
            addKnownVersion("12w37a", "cd025f4b67f5b7811dc5c96542bdaf5e");
            addKnownVersion("12w38a", "8e6c588f9cf2502076ee64ad77d5f54d");
            addKnownVersion("12w38b", "211bf5190199fa59d1b6fa0997daa1d7");
            addKnownVersion("12w39a", "f8e8d69ee0a5ef5a70db1c631f93ef5d");
            addKnownVersion("12w39b", "afcc7775ab7b2b6659bcee50dfb9dbc8");
            addKnownVersion("12w40a", "fd1f030c0230db3f24c5ecfb971a977d");
            addKnownVersion("12w40b", "c549b63c7bafec08f0b2fa291c881be6");
            addKnownVersion("12w41a", "b6f1227e4bb0a8d9155fc2093319fc26");
            addKnownVersion("12w41b", "a6269139ef11b1815eb597132533cdfb");
            addKnownVersion("12w42a", "5d25fdfe0f202ec380e8d429b2d6a81f");
            addKnownVersion("12w42b", "c610c644ef5c27d2f91cf512e2e23c28");

            addKnownVersion("1.4", "32a654388b54d3e4bb29c1a46e7d6a12");
            addKnownVersion("1.4.1", "542621a5298659dc65f383f35170fc4c");
            addKnownVersion("1.4.2", "771175c01778ea67395bc6919a5a9dc5");
            addKnownVersion("1.4.3", "9cc3295931edb6339f22989fe1b612a6");
            addKnownVersion("1.4.4", "7aa46c8058cba2f38e9d2ddddcc77c72");
            // First pre-release of 1.4.5 still had the particle bug MC-2497.
            // Mojang reused the version number when they issued a fix.
            addKnownVersion("1.4.5", "469c9743ba88b7aa498769db75e31b1c");
            addKnownVersion("1.4.5", "b15e2b2b6b4629f0d99a95b6b44412a0");

            addKnownVersion("12w49a", "258b8a5922d046e0f93b338dfa79df36");
            addKnownVersion("12w50a", "47bb6d5db217b39c44f5e116fc59d7ba");
            addKnownVersion("12w50b", "ce8bf157d021157b0a122c03501e5973");

            addKnownVersion("1.4.6", "48677dc4c2b98c29918722b5ab27b4fd");
            addKnownVersion("1.4.7", "8e80fb01b321c6b3c7efca397a3eea35");

            addKnownVersion("13w01a", "57dd5c298dff8906b4fe406f81d6914d");
            addKnownVersion("13w01b", "0a1cb9b3ea4381d898316109e58f5635");

            // New texture pack format
            addKnownVersion("13w02a", "1b794176aabd9c0e2be09ee9c8a45d77");
            addKnownVersion("13w02b", "13891f6610b6739ac79f40147cca31d5");
            addKnownVersion("13w03a", "1c8698800c60ee72589165914f860554");
            addKnownVersion("13w03a", "599a59bfd0a1645453b51b343b4c78c8");
            addKnownVersion("13w04a", "7bfbc543df06f7eb5fe6a2971a085c39");
            addKnownVersion("13w05a", "e4e99faf111be767a07fab45ca386613");
            addKnownVersion("13w05a", "e8f17d5e7c54eb38feb3034fb0385ee5");
            addKnownVersion("13w05b", "496002660846f2eb455cba407cd26818");
            addKnownVersion("13w06a", "1bdd3df77601b37f127d057ac6d686cd");
            addKnownVersion("13w06a", "62bbd3e4ed36e7572f45c4e802759a09");
            addKnownVersion("13w07a", "6c048588e57aee5b02b6bde7978d8f95");
            addKnownVersion("13w09a", "2821913edf037588e7df890dfa6c0d2a");
            addKnownVersion("13w09b", "fb5ba640847d430c3b45f6c3a9649ab8");
            addKnownVersion("13w09c", "d2534381656da5069b96cc89e28158ef");
            addKnownVersion("13w10a", "825d813c9c3fb9341b04ff0759f028d8");
            addKnownVersion("13w10b", "12bb0e9aa07c12073458fc8b93e23f0f");

            addKnownVersion("1.5", "d39baadeeb124a29b2542d778713493f");
            addKnownVersion("1.5", "fd11cbc5b01aae1d62cff0145171f3d9");
            addKnownVersion("13w11a", "7ea28c690bec31ddac16c231e4d6d92c");
            addKnownVersion("1.5.1", "dcee28c1b1cea2b36816a60e8adafea4");
            addKnownVersion("1.5.1", "5c1219d869b87d233de3033688ec7567");

            // April 1, 2013
            addKnownVersion("2.0_blue", "b16c06933f2aa43594ea7a150a126a07");
            addKnownVersion("2.0_purple", "6023b7c3626d987d9c85c8e2617d3705");
            addKnownVersion("2.0_red", "ee00fb8745c03af3fabe8a92cd52ebaf");

            addKnownVersion("1.5.2", "c79b8234a7a3e8e457ac6e37c478691f");
            addKnownVersion("1.5.2", "6897c3287fb971c9f362eb3ab20f5ddd");

            // new launcher
            addKnownVersion("13w16a", "d8743a2434a57ca5c1d8708f3e8bd666");
            addKnownVersion("13w16a", "01d0a4013bf416eaeb3c1f67a81ba4db");
            addKnownVersion("13w16b", "b0ebfdaf3c7baeb892da2c9138e379e9");
            addKnownVersion("13w17a", "ea6b4cd61880d12c6781725323e1d0e1");

            // new texture loading
            addKnownVersion("13w18a", "4f899d67d62dd605ad10b4da8d221d7f");
            addKnownVersion("13w18b", "1f5ec00f68592fcf298cfa22214a7e91");
            addKnownVersion("13w18c", "f5f847516e11101aca961e89c7163d41");
            addKnownVersion("13w19a", "4c2ecdebff5f2c7b41e423c58be67b98");
            addKnownVersion("13w21a", "6df497d9b4e425f54c6ce50d2fd5f68f");
            addKnownVersion("13w21b", "b1fe1a3ccccf8bf032ed6c73d5f31ee6");
            addKnownVersion("13w22a", "bd610b1dc3bb9671dc72391db5918ffa");
            addKnownVersion("13w23a", "705c39f24f367e61947c8971cab7eff9");
            addKnownVersion("13w23b", "447dc4eb124ef2a0e3f45f3dda617cbd");

            // resource packs replace texture packs
            addKnownVersion("13w24a", "6164313863f51af4f0ea206fc351dacf");
            addKnownVersion("13w24b", "83159880733a65b47c69a4e7da6b44c7");
            addKnownVersion("13w25a", "72467d2bde4354800f6cc19442052ed5");
            addKnownVersion("13w25b", "f2e460ccece33d996284d9e02b6f9afc");
            addKnownVersion("13w25c", "4face5c5bc04f50c58ddbd94e88b9d80");
            addKnownVersion("13w26a", "7f13896abb48ff5b3d391daf5580388d");
            addKnownVersion("1.6", "7761a19e4d6880451bf3a4943d298379");
            addKnownVersion("1.6", "6a64a2136e3a3fe4d29b0a0df30b25fc");
            addKnownVersion("1.6.1", "3c56fb4b77b1ca37dde69b5eb896fc4d");
            addKnownVersion("1.6.2", "87dec34e35f3323a92474cd0b45fff8e");
            addKnownVersion("1.6.2", "349e373456ea2a97fab42dd9d938e784");
            addKnownVersion("1.6.2", "1d43cdba8b9752d7f557ab7d3e365964");
            addKnownVersion("1.6.3", "3aba46ba846ced2093c0cd37af370d65");
            addKnownVersion("1.6.3", "233effe2110d40a0519cf1489e4ccb1c");
            addKnownVersion("1.6.4", "2e5044f5359e82245551167a237f3167");

            // https://twitter.com/_grum/status/373028865695682560
            addKnownVersion("13w36a", "36e6ccf248f0d28bd682f1ac2971ae8d");
            addKnownVersion("13w36b", "7a517f489ecad1190c4aa6de5b9962fb");
            addKnownVersion("13w37a", "1ecf5d29e066851a67680e1b3913f985");
            addKnownVersion("13w37b", "32852c180835fcc7ce8784990dbe459a");
            addKnownVersion("13w38a", "b66bc2e70faa94ff2ddf8f4bf0bec948");
            addKnownVersion("13w38b", "02123a03b389443c8fa8a6f6cbccb34d");
            addKnownVersion("13w38c", "eba542003ead603b85051c3498543e7b");
            addKnownVersion("13w39a", "1bd52bdfeda64c3d3845245a5067918d");
            addKnownVersion("13w39b", "c51a147ed0a629c479320e5e9e63bb3e");
            addKnownVersion("13w41a", "063265e1ce84241c01edf7163a7815df");
            addKnownVersion("13w41b", "e3dd0af40c588f4699b48ec865f8793e");
            addKnownVersion("13w42a", "0709f20f4e8d3e8581c6aaab008158b8");
            addKnownVersion("13w42b", "c839b4f314a687c75fa73a77a1480f0d");
            addKnownVersion("13w43a", "62bfa5d9712b0ab856b78425326bc878");
            addKnownVersion("1.7", "9f7754f6633b15aa695898c9a93c32c8");
            addKnownVersion("1.7.1", "08c354313f902913b6e66f6d73938479");
            addKnownVersion("1.7.2", "7a304554fdea879a799efe6eaedd8c95");

            // twitch.tv integration
            addKnownVersion("13w47a", "913c58d836a89d8913bfcef1e6ab104c");
            addKnownVersion("13w47b", "52697f0bdc81078fde1ae195c900a961");
            addKnownVersion("13w47c", "586000b0009c5c691d0f74488b0cbfc4");
            addKnownVersion("13w47d", "d3f0a86ebe4dac5fc7ed49e6f35a2859");
            addKnownVersion("13w47e", "677ba6599a809e5308a5d1612749b07f");
            addKnownVersion("13w48a", "d313787a902321efc9ad50b12b44dc95");
            addKnownVersion("13w48b", "ec41b5fb78dbb90b232b1bb6c145c8ff");
            addKnownVersion("13w49a", "10579faf3a9cef8767b8c4d55f91d24f");
            addKnownVersion("1.7.3", "6bffc630d8d3a0e834d2a91e5d9f5418");
            addKnownVersion("1.7.4", "f9ffe7e56b26d459560c48e228cc6ad4");
            addKnownVersion("1.7.5", "02cf75768f19afe30ab2da65dbc79cf6");
            addKnownVersion("1.7.6-pre1", "c40684ebff725143bf00317d7aa67370");
            addKnownVersion("1.7.6-pre2", "435999291d74cbf5fcf063c237711e9b");
            addKnownVersion("1.7.6", "1a9c5836a15f7b1cdb28edb3632f5f2b");
            addKnownVersion("1.7.7", "088506a375d9200e2d935d7df872366b");
            addKnownVersion("1.7.8", "befddbe9e0bb5fe85c48ab8a42638953");
            addKnownVersion("1.7.9", "5f7cc7eb01cbd935a9973cb6e9c4c10e");
            addKnownVersion("1.7.10-pre1", "06ad980b920e64b387ba3ec18a9779ee");
            addKnownVersion("1.7.10-pre2", "7acd7cfc93739b34fdbe0dfc043a6f3e");
            addKnownVersion("1.7.10-pre3", "595007b790548806f66762507203b896");
            addKnownVersion("1.7.10-pre4", "3145b95f485932d9a25f75a9d5498eec");
            addKnownVersion("1.7.10", "e6b7a531b95d0c172acb704d1f54d1b3");
            addKnownVersion("1.7.9999", null);

            // Position, Direction classes
            addKnownVersion("14w02a", "c881828638a29c59c4f3863d2febde02");
            addKnownVersion("14w02b", "d72fb1d7152eb1a82856e84bb34d43be");
            addKnownVersion("14w02c", "206691e4e99fa0daefd106210fa8a14d");

            // RenderPassEnum
            addKnownVersion("14w03a", "fc2b0ff52c0ff09bdc0aadc1da3f04df");
            addKnownVersion("14w03b", "440954c34c7972dfbc9bdf97684ef318");

            // RenderBlocks split
            addKnownVersion("14w04a", "4da20c10fa7c39243664608d97829824");
            addKnownVersion("14w04b", "772017e3a41e7e9767e1010f7870528a");

            // new entity rendering
            addKnownVersion("14w05a", "19b78debf019f03a312015ac36e9eab1");
            addKnownVersion("14w05b", "dc4af817d4148c5961266f3bad564968");

            // custom block models
            addKnownVersion("14w06a", "f7fffd098920dfa71a74cefdc8ab04a9");
            addKnownVersion("14w06b", "9c3aa18bce51dbe586496d70e8269da7");
            addKnownVersion("14w07a", "da7e7c1e673c38d4ffb02b3c8451eae4");
            addKnownVersion("14w08a", "a7fce896c741ed7dc79625cf9ce825b0");
            addKnownVersion("14w10a", "8d318920a4d1dbfa6d139a8556cc31b6");
            addKnownVersion("14w10b", "9cd3f6938c7a14f23751aa0100a29bcc");
            addKnownVersion("14w10c", "eeb76d42f89d7ca43866efaf7445bce4");
            addKnownVersion("14w11a", "65518da998381074b646e66f425768e8");
            addKnownVersion("14w11b", "4225294f2cc7c46d692c0d9c2b5b93cd");
            addKnownVersion("14w17a", "3db0dccfc6585a16616ea2bcdff71687");
            addKnownVersion("14w18a", "6afbc45b49d12a25934007ab4758d3eb");
            addKnownVersion("14w18b", "b79db7340036c1d7088d13d30fdb9e95");
            addKnownVersion("14w19a", "33c05b97aad8c357bafc93d71c97739d");
            addKnownVersion("14w20a", "839936768a6213c0d026e7919c2fd30c");
            addKnownVersion("14w20b", "e3974165c5f4a4d6598f5cf9930e9bd3");
            addKnownVersion("14w21a", "a450c63285b983fab1eb1973ba8f6878");
            addKnownVersion("14w21b", "8153688bdfe02eca940cbb32ffd1bb65");

            // new item/block rendering, block states
            addKnownVersion("14w25a", "22ffc07d66bb622edba5e6c10acbba58");
            addKnownVersion("14w25b", "d03a9bcc88fcb961c295abbaf2877040");
            addKnownVersion("14w26a", "a384f8d7a9c17ba774f906e1247a292a");
            addKnownVersion("14w26b", "d4c0f61ae3ce5c2b834ce1ad322c7708");
            addKnownVersion("14w26c", "898bdfdef6c2dd391ba9c0275e68827c");
            addKnownVersion("14w27a", "127be54023a8f1192c6f8bd25135e423");
            addKnownVersion("14w27b", "7798ebd98823b1f8915bd33dcd33f4b5");
            addKnownVersion("14w28a", "8ee38b7738084188067ebd4ead671279");
            addKnownVersion("14w28b", "b83968845c4c31e0040df95880df357b");
            addKnownVersion("14w29a", "507ef5acf4d17e7344613cc822001ca5");
            addKnownVersion("14w29b", "02a9a7fdbf9c67527e26080eef8cdfb1");
            addKnownVersion("14w30a", "17e4e7782b4420d269b3618d5ebc3708");
            addKnownVersion("14w30b", "0feabc325133c740efe3c99786fd7fe8");
            addKnownVersion("14w30c", "d7afbc8ab12a5d0f9978cc02c83db447");
            addKnownVersion("14w31a", "f0968f0fc7f1f018b1d312a44a1b738f");
            addKnownVersion("14w32a", "c0353d07e7f1a8d3b7ac359dae611dac");
            addKnownVersion("14w32b", "34169165e621bb9a5bff8cd950b1c7d2");
            addKnownVersion("14w32c", "838d6381302235c7f75fb2ba6a864e11");
            addKnownVersion("14w32d", "51ae0e11e078bb64a04fce7ac2d29d80");
            addKnownVersion("14w33a", "95ca8646c30f5b9e00421a81e63a3046");
            addKnownVersion("14w33b", "20fc85dda05c5de89d51e9f72f7aee94");
            addKnownVersion("14w33c", "b098b0fe4b0177f2f4060e22f038cc64");
            addKnownVersion("14w34a", "4ab10bafb8cf415466c1ff8b40f7291d");
            addKnownVersion("14w34b", "47ab8e468d8e6cac4128656d6ba35378");
            addKnownVersion("14w34c", "9d2a999d1bcbb863090e5ef008eb2ecc");
            addKnownVersion("14w34d", "612c4bd131dd83be9a80c11048ddde59");
            addKnownVersion("1.8-pre1", "b7f893c6cc67b52e048247438193dccf");
            addKnownVersion("1.8-pre2", "81e2bd9278b501872bc9158733dd98b9");
            addKnownVersion("1.8-pre3", "af436ac2cc2c8333294e5e9dad4a2988");
            addKnownVersion("1.8", "8663a10cecc10eaa683a927ef5371852");
            addKnownVersion("1.8.1-pre1", "f125dc0879eecab1c841085870b36db8");
            addKnownVersion("1.8.1-pre2", "c92bd6e2ea6a5b3614d289f5ce74cba1");

            for (int i = 0; i < versionOrdering.size(); i++) {
                MinecraftVersion a = versionOrdering.get(i);
                for (int j = 0; j < versionOrdering.size(); j++) {
                    MinecraftVersion b = versionOrdering.get(j);
                    Integer result1 = comparePartial(a, b);
                    int result2 = a.compareTo(b);
                    if (i == j) {
                        if (result1 == null || result1 != 0 || result2 != 0) {
                            throw new AssertionError("incorrect ordering in known version table: " + a.getVersionString() + " != " + b.getVersionString());
                        }
                    } else if (i > j) {
                        if (result1 != null && result1 <= 0 || result2 <= 0) {
                            throw new AssertionError("incorrect ordering in known version table: " + a.getVersionString() + " <= " + b.getVersionString());
                        }
                    } else {
                        if (result1 != null && result1 >= 0 || result2 >= 0) {
                            throw new AssertionError("incorrect ordering in known version table: " + a.getVersionString() + " >= " + b.getVersionString());
                        }
                    }
                }
            }
            highestKnownVersion = versionOrdering.get(versionOrdering.size() - 1);
        } catch (Throwable e) {
            Logger.log(e);
        }
    }

    private static void addKnownVersion(String versionString, String md5) {
        if (md5 != null && !md5.matches("\\p{XDigit}{32}")) {
            throw new IllegalArgumentException("bad md5 sum for known version " + versionString);
        }
        MinecraftVersion version = parseVersion1(versionString);
        if (version == null) {
            throw new IllegalArgumentException("bad known version " + versionString);
        }
        knownVersions.put(version.getVersionString(), version);
        versionOrdering.remove(version);
        versionOrdering.add(version);
        if (md5 != null) {
            knownMD5s.put(version, md5);
            alternateMD5s.put(md5, version);
        }
    }

    private static Integer comparePartial(MinecraftVersion a, MinecraftVersion b) {
        if (a.snapshot != b.snapshot) {
            return null;
        }
        int[] u = a.versionNumbers;
        int[] v = b.versionNumbers;
        int i;
        for (i = 0; i < u.length && i < v.length; i++) {
            if (u[i] != v[i]) {
                return u[i] - v[i];
            }
        }
        if (i < u.length) {
            return u[i];
        }
        if (i < v.length) {
            return -v[i];
        }
        i = a.preRelease - b.preRelease;
        if (i != 0) {
            return i;
        }
        return a.getVersionString().compareTo(b.getVersionString());
    }

    private static int findClosestKnownVersion(MinecraftVersion version) {
        int i;
        for (i = 0; i < versionOrdering.size(); i++) {
            Integer partialResult = comparePartial(version, versionOrdering.get(i));
            if (partialResult != null && partialResult <= 0) {
                break;
            }
        }
        return i;
    }

    /**
     * Attempt to parse a string into a minecraft version.
     *
     * @param versionString original version string as it appears in the launcher, e.g., "1.6.2"
     * @return MinecraftVersion object or null
     */
    public static MinecraftVersion parseVersion(String versionString) {
        MinecraftVersion version = parseVersion1(versionString);
        if (version != null && !versionOrdering.contains(version)) {
            versionOrdering.add(findClosestKnownVersion(version), version);
        }
        return version;
    }

    private static MinecraftVersion parseVersion1(String versionString) {
        if (versionString == null) {
            return null;
        }
        MinecraftVersion version = knownVersions.get(versionString);
        if (version != null) {
            return version;
        }
        versionString = shortenVersionString(versionString);
        int preRelease = NOT_PRERELEASE;
        Matcher preMatcher = PRERELEASE_PATTERN.matcher(versionString);
        if (preMatcher.find()) {
            try {
                preRelease = Integer.parseInt(preMatcher.group(1));
            } catch (NumberFormatException e) {
            }
            if (preMatcher.start() > 0) {
                versionString = preMatcher.replaceFirst("");
            }
        }
        if (versionString.matches("2\\.0_(blue|purple|red)")) {
            return new MinecraftVersion(Era.FINAL, versionString, "1.5.1." + versionString, preRelease);
        }
        for (Era era : Era.values()) {
            Matcher matcher = era.matcher(versionString);
            if (matcher.matches()) {
                return new MinecraftVersion(era, matcher, preRelease);
            }
        }
        return null;
    }

    private static String shortenVersionString(String versionString) {
        versionString = versionString.toLowerCase();
        versionString = versionString.replaceFirst("^minecraft", "");
        versionString = versionString.replaceFirst("prerelease", "pre");
        versionString = versionString.replaceAll("\\s", "");
        versionString = versionString.replaceFirst("^alpha-", "a");
        versionString = versionString.replaceFirst("^beta-", "b");
        return versionString;
    }

    private static int[] splitVersionNumbers(Era era, String versionNumbers) {
        String[] token = versionNumbers.split("[^0-9]+");
        int[] numbers = new int[token.length + 1];
        numbers[0] = era.effectiveOrder();
        for (int i = 0; i < token.length; i++) {
            if (!token[i].isEmpty()) {
                try {
                    numbers[i + 1] = Integer.parseInt(token[i]);
                } catch (NumberFormatException e) {
                }
            }
        }
        return numbers;
    }

    private MinecraftVersion(Era era, Matcher matcher, int preRelease) {
        this(era, matcher.group(0), matcher.group(1), preRelease);
    }

    private MinecraftVersion(Era era, String versionString, String versionNumbers, int preRelease) {
        this.versionString = versionString;
        this.preRelease = preRelease;
        this.versionNumbers = splitVersionNumbers(era, versionNumbers);
        snapshot = era.isSnapshot();
    }

    /**
     * Returns true if version is a prerelease or snapshot.
     *
     * @return true if prerelease
     */
    public boolean isPrerelease() {
        return preRelease != NOT_PRERELEASE || snapshot;
    }

    /**
     * Gets version as a string, e.g., b1.8.1 or 1.0.0.
     *
     * @return version string
     */
    public String getVersionString() {
        return versionString;
    }

    /**
     * @see #getVersionString()
     */
    @Override
    public String toString() {
        return getVersionString();
    }

    /**
     * Compare two MinecraftVersions.
     *
     * @param that version to compare with
     * @return 0, &lt; 0, or &gt; 0
     */
    @Override
    public int compareTo(MinecraftVersion that) {
        if (that == null) {
            throw new NullPointerException();
        }
        int i = versionOrdering.indexOf(this);
        int j = versionOrdering.indexOf(that);
        if (i >= 0 && j >= 0) {
            return i - j;
        } else {
            throw new AssertionError("Unreachable");
        }
    }

    /**
     * Compare to version string.
     *
     * @param versionString version to compare with
     * @return 0, &lt; 0, or &gt; 0
     */
    public int compareTo(String versionString) {
        return compareTo(parseVersion(versionString));
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        } else if (!(that instanceof MinecraftVersion)) {
            return false;
        }
        return getVersionString().equals(((MinecraftVersion) that).getVersionString());
    }

    @Override
    public int hashCode() {
        return getVersionString().hashCode();
    }

    static String getOriginalMD5(MinecraftVersion version) {
        return knownMD5s.get(version);
    }

    boolean isNewerThanAnyKnownVersion() {
        return compareTo(highestKnownVersion) > 0;
    }

    static boolean isKnownMD5(String md5) {
        return md5 != null && (knownMD5s.containsValue(md5) || alternateMD5s.containsKey(md5));
    }
}
