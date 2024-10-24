/*
 * Copyright (c) 2024 DexrnZacAttack
 * This file is part of LCEPlayerRedactor.
 * https://github.com/DexrnZacAttack/LCEPlayerRedactor
 *
 * Licensed under the MIT License. See LICENSE file for details.
 */

package io.github.dexrnzacattack.lceplayerredactor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

public class ConsoleSaveFileHandler {
    // todo: recompress
    // todo: other compression types (lzx my behated)
    // this code sucks because I am not a java dev (but need to learn for different project)
    // it's also very buggy (probably)
    public static void redactSaveFile(File file) throws IOException {
        // is little endian
        boolean isLE = false;
        // is pre-release
        boolean isPR = false;

        System.out.printf("Reading save file %s%n", file.getName());
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             RandomAccessFile oRaf = new RandomAccessFile(file.getName() + "_LCEPlayerRedact", "rw")) {
                byte[] buffer = new byte[1024];
                int read;

                while ((read = raf.read(buffer)) != -1) {
                    if (read > 0) {
                        oRaf.write(buffer, 0, read);
                    }
                }

                raf.seek(0);

                int offset = raf.readInt();
                int count = raf.readInt();
                raf.readShort(); // don't care about minimum version
                short version = raf.readShort();

                // lce crashes upon loading a save with a version that's too high so we can use this to detect endian
                if (version > 20) {
                    System.out.printf("Version is too high (%s), probably little endian%n", version);
                    isLE = true;
                    offset = Integer.reverseBytes(offset);
                    count = Integer.reverseBytes(count);
                    version = (short) Integer.reverseBytes(version);
                };

                System.out.println("Save file version is " + version);

                if (version == 1) {
                    System.out.println("Seems to be a pre-release save file");
                    isPR = true;
                    count = count / 136;
                };

                List<Long> playerEntries = new ArrayList<>();

                // read the index
                raf.seek(offset);
                for (var i = 0; i < count; i++) {
                    // gotta remove all the null bytes
                    String fileName = readUTF16(raf, isLE).replaceAll("\u0000+$", "");
                    System.out.printf("Found file %s%n", fileName);
                    // mmmmm funny name check (maybe I should just read the data and check for an NBT key instead)
                    if (fileName.startsWith("players/") || fileName.startsWith("players_") || fileName.startsWith("P_")) {
                        playerEntries.add(raf.getFilePointer() - 0x80);
                        System.out.printf("Found player %s%n", fileName);
                    }

                    // never gonna use these
                    raf.readInt(); // size
                    raf.readInt(); // offset
                    if (!isPR)
                        raf.readLong(); // timestamp
                }

                // now we redact
                for (int p = 0; p < playerEntries.size(); p++) {
                    oRaf.seek(playerEntries.get(p)); // get the offset
                    writeUTF16(oRaf, "LCEPlayerRedactor_" + p + ".dat", isLE);
            }
        }
        System.out.println("Wrote to " + file.getName() + "_LCEPlayerRedact");
        System.out.println("Done!");
    }

    public static File decompressSaveFile(File file) throws IOException {
        // holy hell so much jank with raf cuz I have to like do a ton of shit instead of just writing it to a temp array and replacing the old var
        // maybe just skill issue
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(8);
            if (raf.readShort() == 30876) {
                raf.seek(8);
                System.out.println("ZLib compressed, decompressing...");
                File decompressed = new File(file.getAbsolutePath() + "_decompressed");
                try (RandomAccessFile oRaf = new RandomAccessFile(decompressed, "rw")) {
                    byte[] compressedData = new byte[(int) (raf.length() - raf.getFilePointer())];

                    raf.readFully(compressedData);
                    byte[] decompressedData = decompressZlib(compressedData);

                    oRaf.setLength(0);
                    oRaf.write(decompressedData);
                }
                return decompressed;
            } else {
                return file;
            }
        }
    }

    public static byte[] decompressZlib(byte[] compressedData) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             // microsoft iis
             InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(compressedData))) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = iis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("Save decompression failed (zlib): " + e.getMessage());
            throw e;
        }
    }

    public static String readUTF16(RandomAccessFile raf, boolean littleEndian) throws IOException {
        byte[] utf16 = new byte[0x80];
        raf.readFully(utf16);

        return new String(utf16, littleEndian ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_16BE);
    }

    public static void writeUTF16(RandomAccessFile raf, String str, boolean littleEndian) throws IOException {
        byte[] utf16 = new byte[0x80];
        byte[] string = str.getBytes(littleEndian ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_16BE);

        System.arraycopy(string, 0, utf16, 0, Math.min(string.length, utf16.length));
        raf.write(utf16);
    }

}