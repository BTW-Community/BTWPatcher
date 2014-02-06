package com.prupe.mcpatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.regex.Pattern;

class ClassFileStringReplacer {
    private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final BitSet DESCRIPTOR_BYTES = new BitSet(256);

    private final byte[] input;
    private final String oldString;
    private final String newString;
    private final byte oldLenHigh;
    private final byte oldLenLow;
    private final ByteArrayOutputStream output;
    private final String dataAsString;
    private final String oldStringAsRegex;
    private final byte[] newStringAsBytes;

    private int inputPosition;
    private int matchPosition;

    static {
        for (byte b : "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_$/.[<>;".getBytes()) {
            DESCRIPTOR_BYTES.set(b & 0xff);
        }
    }

    ClassFileStringReplacer(byte[] input, String oldString, String newString) {
        this.input = input;
        this.oldString = oldString;
        this.newString = newString;
        oldLenHigh = (byte) (oldString.length() >> 8);
        oldLenLow = (byte) oldString.length();
        output = new ByteArrayOutputStream(input.length);
        dataAsString = new String(input, ISO_8859_1);
        oldStringAsRegex = "\\b(L?)" + Pattern.quote(oldString) + "\\b";
        newStringAsBytes = Util.marshalString(newString);
    }

    byte[] replace() throws IOException {
        if (oldString.equals(newString)) {
            return input;
        }
        while ((matchPosition = dataAsString.indexOf(oldString, inputPosition)) >= 0) {
            boolean b = replaceExact() || replaceSubstring() || copyChunk();
        }
        output.write(input, inputPosition, input.length - inputPosition);
        return output.toByteArray();
    }

    private boolean replaceExact() throws IOException {
        if (matchPosition >= 2 && input[matchPosition - 2] == oldLenHigh && input[matchPosition - 1] == oldLenLow) {
            Logger.log(Logger.LOG_METHOD, "string replace %s -> %s @%d", oldString, newString, matchPosition);
            output.write(input, inputPosition, matchPosition - inputPosition - 2);
            output.write(newStringAsBytes);
            inputPosition = matchPosition + oldString.length();
            return true;
        } else {
            return false;
        }
    }

    private boolean replaceSubstring() throws IOException {
        if (matchPosition < 1 || input[matchPosition - 1] != 'L' || input[matchPosition + oldString.length()] != ';') {
            return false;
        }
        int start = dataAsString.lastIndexOf(0, matchPosition);
        if (start < inputPosition || start + 2 >= input.length) {
            return false;
        }
        int len = Util.demarshal(input, start, 2);
        int end = start + 2 + len;
        if (end >= input.length || end < matchPosition + oldString.length() + 1) {
            return false;
        }
        for (int i = start + 2; i < end; i++) {
            if (!DESCRIPTOR_BYTES.get(dataAsString.charAt(i) & 0xff)) {
                return false;
            }
        }
        try {
            String oldStringFull = new String(input, start + 2, len, UTF_8);
            String newStringFull = oldStringFull.replaceAll(oldStringAsRegex, "$1" + newString);
            if (newStringFull.equals(oldStringFull)) {
                return false;
            }
            Logger.log(Logger.LOG_METHOD, "string replace %s -> %s @%d", oldStringFull, newStringFull, start);
            output.write(input, inputPosition, start - inputPosition);
            Util.marshalString(output, newStringFull);
            inputPosition = end;
            return true;
        } catch (Throwable e) {
            Logger.log(e);
            return false;
        }
    }

    private boolean copyChunk() throws IOException {
        output.write(input, inputPosition, matchPosition - inputPosition + oldString.length());
        inputPosition = matchPosition + oldString.length();
        return true;
    }
}
