/*
 * Copyright (c) 2024 DexrnZacAttack
 * This file is part of LCEPlayerRedactor.
 * https://github.com/DexrnZacAttack/LCEPlayerRedactor
 *
 * Licensed under the MIT License. See LICENSE file for details.
 */

package io.github.dexrnzacattack.lceplayerredactor;

import java.io.File;
import java.io.IOException;

import static io.github.dexrnzacattack.lceplayerredactor.ConsoleSaveFileHandler.decompressSaveFile;
import static io.github.dexrnzacattack.lceplayerredactor.ConsoleSaveFileHandler.redactSaveFile;

public class LCEPlayerRedact {
    public static void main(String[] args) throws IOException {
        System.out.println("https://github.com/DexrnZacAttack/LCEPlayerRedactor");
        if (args.length < 1) {
            System.out.println("Please provide a file path.");
            return;
        }

        File file = new File(args[0]);
        redactSaveFile(decompressSaveFile(file));
    }
}