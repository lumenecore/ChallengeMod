package com.lumenechallenge.util;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RussianGrammar {
    public enum GrammaticalCase {
        NOMINATIVE,
        ACCUSATIVE,
        INSTRUMENTAL
    }

    private enum Gender {
        MASCULINE,
        FEMININE,
        NEUTER
    }

    private record Forms(String nominative, String accusative, String instrumental) {
        String form(GrammaticalCase grammaticalCase) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> nominative;
                case ACCUSATIVE -> accusative;
                case INSTRUMENTAL -> instrumental;
            };
        }
    }

    private record NounAnalysis(Gender gender, boolean softSignMasculine) {
    }

    private static final Set<String> PREPOSITIONS = Set.of(
            "из", "от", "до", "без", "для", "у", "к", "ко", "по", "о", "об", "обо",
            "в", "во", "на", "за", "под", "над", "перед", "при", "с", "со"
    );

    private static final Set<String> ANIMATE_ITEM_HEADS = Set.of(
            "лосось",
            "кролик",
            "треска",
            "иглобрюх",
            "рыба",
            "дельфин",
            "черепаха",
            "хоглин",
            "зимогор"
    );

    private static final Set<String> SOFT_SIGN_MASCULINE = Set.of(
            "лосось",
            "конь",
            "гость",
            "путь",
            "огонь",
            "день",
            "камень",
            "ремень",
            "корень",
            "пень",
            "карась"
    );

    private static final Map<String, Forms> WORD_EXCEPTIONS = Map.ofEntries(
            Map.entry("осёл", new Forms("осёл", "осла", "ослом")),
            Map.entry("зомби", new Forms("зомби", "зомби", "зомби"))
    );

    private static final Map<String, Forms> PHRASE_EXCEPTIONS = Map.of();

    private RussianGrammar() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.toLowerCase(Locale.ROOT).trim();
    }

    public static String item(String raw, GrammaticalCase grammaticalCase) {
        String phrase = normalize(raw);
        if (phrase.isBlank()) {
            return "";
        }
        return inflectPhrase(phrase, grammaticalCase, isAnimateItemPhrase(phrase));
    }

    public static String mob(String raw, GrammaticalCase grammaticalCase) {
        String phrase = normalize(raw);
        if (phrase.isBlank()) {
            return "";
        }
        return inflectPhrase(phrase, grammaticalCase, true);
    }

    public static String block(String raw, GrammaticalCase grammaticalCase) {
        String phrase = normalize(raw);
        if (phrase.isBlank()) {
            return "";
        }
        return inflectPhrase(phrase, grammaticalCase, false);
    }

    public static String structure(String raw, GrammaticalCase grammaticalCase) {
        return block(raw, grammaticalCase);
    }

    public static String biome(String raw, GrammaticalCase grammaticalCase) {
        return block(raw, grammaticalCase);
    }

    public static String dimension(String raw) {
        return normalize(raw);
    }

    public static String effect(String raw) {
        return normalize(raw);
    }

    public static String withPreposition(String raw) {
        String phrase = mob(raw, GrammaticalCase.INSTRUMENTAL);
        if (phrase.isBlank()) {
            return "";
        }
        return (needsSo(phrase) ? "со " : "с ") + phrase;
    }

    private static boolean isAnimateItemPhrase(String phrase) {
        for (String word : phrase.split("\\s+")) {
            if (ANIMATE_ITEM_HEADS.contains(stripPunctuation(word))) {
                return true;
            }
        }
        return false;
    }

    private static boolean needsSo(String phrase) {
        String trimmed = phrase.trim();
        if (trimmed.isBlank()) {
            return false;
        }
        String firstWord = stripPunctuation(trimmed.split("\\s+")[0]);
        if (firstWord.equals("мной")) {
            return true;
        }
        if (firstWord.isBlank()) {
            return false;
        }
        if (firstWord.startsWith("щ")) {
            return true;
        }
        if (firstWord.length() >= 2) {
            char first = firstWord.charAt(0);
            char second = firstWord.charAt(1);
            if (isSoStarter(first) && isConsonant(second)) {
                return true;
            }
        }
        if (firstWord.length() >= 3) {
            int consonants = 0;
            for (int i = 0; i < firstWord.length(); i++) {
                if (isConsonant(firstWord.charAt(i))) {
                    consonants++;
                    if (consonants >= 3) {
                        return true;
                    }
                } else {
                    break;
                }
            }
        }
        return false;
    }

    private static boolean isSoStarter(char ch) {
        return ch == 'с' || ch == 'з' || ch == 'ш' || ch == 'ж';
    }

    private static boolean isConsonant(char ch) {
        return "бвгджзйклмнпрстфхцчшщ".indexOf(ch) >= 0;
    }

    private static String stripPunctuation(String word) {
        return word.replaceAll("^[^\\p{IsAlphabetic}]+|[^\\p{IsAlphabetic}]+$", "");
    }

    private static String inflectPhrase(String phrase, GrammaticalCase grammaticalCase, boolean animate) {
        if (phrase.isBlank()) {
            return "";
        }

        Forms phraseException = PHRASE_EXCEPTIONS.get(phrase);
        if (phraseException != null) {
            return phraseException.form(grammaticalCase);
        }

        String[] words = phrase.split("\\s+");
        int prepositionIndex = firstPrepositionIndex(words);
        if (prepositionIndex == 0) {
            return phrase;
        }

        if (prepositionIndex > 0) {
            String prefix = inflectSegment(words, 0, prepositionIndex, grammaticalCase, animate);
            if (prefix == null) {
                return phrase;
            }
            return prefix + " " + join(words, prepositionIndex, words.length);
        }

        String inflected = inflectSegment(words, 0, words.length, grammaticalCase, animate);
        return inflected == null ? phrase : inflected;
    }

    private static int firstPrepositionIndex(String[] words) {
        for (int i = 0; i < words.length; i++) {
            if (isPreposition(stripPunctuation(words[i]))) {
                return i;
            }
        }
        return -1;
    }

    private static String inflectSegment(String[] words, int start, int end, GrammaticalCase grammaticalCase, boolean animate) {
        if (start >= end) {
            return "";
        }

        int headIndex = end - 1;
        String headWord = stripPunctuation(words[headIndex]);
        if (!canBeNoun(headWord)) {
            return null;
        }

        NounAnalysis analysis = analyzeNoun(headWord);
        if (analysis == null) {
            return null;
        }

        StringBuilder out = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                out.append(' ');
            }

            String token = words[i];
            if (i == headIndex) {
                out.append(inflectNoun(token, grammaticalCase, animate, analysis));
            } else {
                out.append(inflectModifier(token, grammaticalCase, analysis.gender(), animate));
            }
        }
        return out.toString();
    }

    private static boolean isPreposition(String word) {
        return PREPOSITIONS.contains(word);
    }

    private static boolean canBeNoun(String word) {
        if (word.isBlank()) {
            return false;
        }
        if (WORD_EXCEPTIONS.containsKey(word)) {
            return true;
        }
        return switch (word.substring(word.length() - 1)) {
            case "а", "я", "о", "е", "ё", "ь", "й" -> true;
            default -> !word.matches(".*[ыи]$") && word.matches(".*[бвгджзклмнпрстфхцчшщ]$");
        };
    }

    private static NounAnalysis analyzeNoun(String word) {
        if (WORD_EXCEPTIONS.containsKey(word)) {
            return new NounAnalysis(Gender.MASCULINE, false);
        }

        if (word.endsWith("мя")) {
            return new NounAnalysis(Gender.NEUTER, false);
        }
        if (word.endsWith("ия") || word.endsWith("ья") || word.endsWith("а") || word.endsWith("я")) {
            return new NounAnalysis(Gender.FEMININE, false);
        }
        if (word.endsWith("о") || word.endsWith("е") || word.endsWith("ё")) {
            return new NounAnalysis(Gender.NEUTER, false);
        }
        if (word.endsWith("й")) {
            return new NounAnalysis(Gender.MASCULINE, false);
        }
        if (word.endsWith("ь")) {
            boolean masculine = isMasculineSoftSign(word);
            return new NounAnalysis(masculine ? Gender.MASCULINE : Gender.FEMININE, masculine);
        }
        if (word.matches(".*[бвгджзклмнпрстфхцчшщ]$")) {
            return new NounAnalysis(Gender.MASCULINE, false);
        }
        return null;
    }

    private static boolean isMasculineSoftSign(String word) {
        return SOFT_SIGN_MASCULINE.contains(word)
                || word.endsWith("ель")
                || word.endsWith("тель")
                || word.endsWith("итель");
    }

    private static String inflectNoun(String word, GrammaticalCase grammaticalCase, boolean animate, NounAnalysis analysis) {
        Forms exception = WORD_EXCEPTIONS.get(word);
        if (exception != null) {
            return exception.form(grammaticalCase);
        }

        String lower = word.toLowerCase(Locale.ROOT);
        return switch (analysis.gender()) {
            case FEMININE -> inflectFeminineNoun(lower, grammaticalCase);
            case NEUTER -> inflectNeuterNoun(lower, grammaticalCase);
            case MASCULINE -> inflectMasculineNoun(lower, grammaticalCase, animate || WORD_EXCEPTIONS.containsKey(lower));
        };
    }

    private static String inflectMasculineNoun(String word, GrammaticalCase grammaticalCase, boolean animate) {
        if (word.endsWith("й")) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> animate ? word.substring(0, word.length() - 1) + "я" : word;
                case INSTRUMENTAL -> word.substring(0, word.length() - 1) + "ем";
            };
        }
        if (word.endsWith("ь")) {
            if (isMasculineSoftSign(word)) {
                return switch (grammaticalCase) {
                    case NOMINATIVE -> word;
                    case ACCUSATIVE -> animate ? word.substring(0, word.length() - 1) + "я" : word;
                    case INSTRUMENTAL -> word.substring(0, word.length() - 1) + "ем";
                };
            }
            return word;
        }
        if (word.endsWith("ель") || word.endsWith("тель") || word.endsWith("итель")) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> animate ? word.substring(0, word.length() - 1) + "я" : word;
                case INSTRUMENTAL -> word.substring(0, word.length() - 1) + "ем";
            };
        }

        return switch (grammaticalCase) {
            case NOMINATIVE -> word;
            case ACCUSATIVE -> animate ? word + "а" : word;
            case INSTRUMENTAL -> word + "ом";
        };
    }

    private static String inflectFeminineNoun(String word, GrammaticalCase grammaticalCase) {
        if (word.endsWith("мя")) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> word;
                case INSTRUMENTAL -> word.substring(0, word.length() - 2) + "енем";
            };
        }
        if (word.endsWith("ия")) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> word.substring(0, word.length() - 2) + "ию";
                case INSTRUMENTAL -> word.substring(0, word.length() - 2) + "ией";
            };
        }
        if (word.endsWith("ья")) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> word.substring(0, word.length() - 2) + "ью";
                case INSTRUMENTAL -> word.substring(0, word.length() - 2) + "ьей";
            };
        }
        if (word.endsWith("я")) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> word.substring(0, word.length() - 1) + "ю";
                case INSTRUMENTAL -> word.substring(0, word.length() - 1) + "ей";
            };
        }
        if (word.endsWith("а")) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> word.substring(0, word.length() - 1) + "у";
                case INSTRUMENTAL -> word.substring(0, word.length() - 1) + "ой";
            };
        }
        if (word.endsWith("ь")) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> word;
                case INSTRUMENTAL -> word.substring(0, word.length() - 1) + "ью";
            };
        }
        return word;
    }

    private static String inflectNeuterNoun(String word, GrammaticalCase grammaticalCase) {
        return switch (grammaticalCase) {
            case NOMINATIVE -> word;
            case ACCUSATIVE -> word;
            case INSTRUMENTAL -> word.endsWith("е") || word.endsWith("ё")
                    ? word.substring(0, word.length() - 1) + "ем"
                    : word.substring(0, word.length() - 1) + "ом";
        };
    }

    private static String inflectModifier(String word, GrammaticalCase grammaticalCase, Gender gender, boolean animate) {
        if (word.isBlank()) {
            return word;
        }

        String[] hyphen = word.split("-", -1);
        if (hyphen.length > 1) {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < hyphen.length; i++) {
                if (i > 0) {
                    out.append('-');
                }
                out.append(inflectModifier(hyphen[i], grammaticalCase, gender, animate));
            }
            return out.toString();
        }

        String lower = word.toLowerCase(Locale.ROOT);

        if (isPreposition(lower) || lower.matches("^[\\p{IsDigit}].*")) {
            return lower;
        }

        if (!looksLikeAdjective(lower)) {
            return lower;
        }

        return inflectAdjective(lower, grammaticalCase, gender, animate);
    }

    private static boolean looksLikeAdjective(String word) {
        return word.endsWith("ый") || word.endsWith("ий") || word.endsWith("ой")
                || word.endsWith("ая") || word.endsWith("яя")
                || word.endsWith("ое") || word.endsWith("ее")
                || word.endsWith("ые") || word.endsWith("ие")
                || word.endsWith("анный") || word.endsWith("янный")
                || word.endsWith("енный") || word.endsWith("ённый")
                || word.endsWith("ованный") || word.endsWith("еванный")
                || word.endsWith("ированный");
    }

    private static String inflectAdjective(String word, GrammaticalCase grammaticalCase, Gender gender, boolean animate) {
        if (grammaticalCase == GrammaticalCase.ACCUSATIVE) {
            return switch (gender) {
                case FEMININE -> {
                    if (word.endsWith("ая")) yield word.substring(0, word.length() - 2) + "ую";
                    if (word.endsWith("яя")) yield word.substring(0, word.length() - 2) + "юю";
                    yield word;
                }
                case NEUTER -> word;
                case MASCULINE -> {
                    if (!animate) yield word;
                    if (word.endsWith("ый") || word.endsWith("ой")) yield word.substring(0, word.length() - 2) + "ого";
                    if (word.endsWith("ий")) yield word.substring(0, word.length() - 2) + "его";
                    if (word.endsWith("ый")) yield word.substring(0, word.length() - 2) + "ого";
                    yield word;
                }
            };
        }

        if (grammaticalCase == GrammaticalCase.INSTRUMENTAL) {
            return switch (gender) {
                case FEMININE -> {
                    if (word.endsWith("ая")) yield word.substring(0, word.length() - 2) + "ой";
                    if (word.endsWith("яя")) yield word.substring(0, word.length() - 2) + "ей";
                    yield word;
                }
                case NEUTER -> {
                    if (word.endsWith("ое")) yield word.substring(0, word.length() - 2) + "ым";
                    if (word.endsWith("ее")) yield word.substring(0, word.length() - 2) + "им";
                    yield word;
                }
                case MASCULINE -> {
                    if (word.endsWith("ый") || word.endsWith("ой")) yield word.substring(0, word.length() - 2) + "ым";
                    if (word.endsWith("ий")) yield word.substring(0, word.length() - 2) + "им";
                    yield word;
                }
            };
        }

        return word;
    }

    private static String join(String[] words, int start, int end) {
        if (start >= end) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                out.append(' ');
            }
            out.append(words[i]);
        }
        return out.toString();
    }
}
