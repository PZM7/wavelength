package com.wavelength.common;

/** Preserves the existing Unicode whitespace rules at authentication/configuration boundaries. */
public final class TextInput {
    private TextInput() {}

    public static boolean isBlank(String value) {
        return value.chars().allMatch(TextInput::isWhitespace);
    }

    public static String trim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isWhitespace(value.charAt(start))) start++;
        while (end > start && isWhitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }

    private static boolean isWhitespace(int character) {
        return Character.isWhitespace(character) || Character.isSpaceChar(character);
    }
}
