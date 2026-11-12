package com.lumenechallenge.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Small deterministic Russian inflector for the Minecraft vocabulary used by
 * ChallengeMod.  It intentionally exposes only the cases the challenge text
 * actually needs: accusative, instrumental and nominative.
 *
 * The important part is that a phrase is treated as a phrase.  Only its head
 * noun and the modifiers agreeing with that head are changed; complements
 * after prepositions such as "из", "с", "для" are left untouched.
 */
public final class RussianGrammar {
    public enum GrammaticalCase {
        NOMINATIVE,
        ACCUSATIVE,
        INSTRUMENTAL
    }

    private enum Gender {
        MASCULINE,
        FEMININE,
        NEUTER,
        PLURAL
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

    private record NounAnalysis(Gender gender) {
    }

    private static final Set<String> PREPOSITIONS = Set.of(
            "из", "от", "до", "без", "для", "у", "к", "ко", "по", "о", "об", "обо",
            "в", "во", "на", "за", "под", "над", "перед", "при", "с", "со"
    );

    /**
     * Minecraft names that are grammatically animate when they are used as
     * item objects (fish, rabbit, chicken, etc.).  Most Minecraft items are
     * inanimate, so the list is deliberately explicit instead of guessing
     * from the spelling of every noun in the game.
     */
    private static final Set<String> ANIMATE_ITEM_HEADS = Set.of(
            "курица", "кролик", "лосось", "треска", "иглобрюх", "рыба", "аксолотль",
            "тропическая рыба"
    );

    /** Common Minecraft plural heads that need genuine plural morphology. */
    private static final Set<String> PLURAL_HEADS = Set.of(
            "ворота", "листья", "доски", "ножницы", "семена", "прутья"
    );

    /** Masculine soft-sign nouns; other soft-sign nouns default to feminine. */
    private static final Set<String> MASCULINE_SOFT_SIGN = Set.of(
            "камень", "ремень", "день", "огонь", "корень", "пень", "путь",
            "лосось", "конь", "гость", "олень", "кремень", "уголь", "вихрь", "богатырь", "медведь", "аксолотль"
    );

    /**
     * Lexical exceptions are preferable to pretending that Russian can be
     * perfectly inferred from a suffix.  This map contains Minecraft words
     * whose stem changes or whose conventional form is otherwise irregular.
     */
    private static final Map<String, Forms> WORD_EXCEPTIONS = createWordExceptions();

    /** Full phrase exceptions for names whose entire construction is lexical. */
    private static final Map<String, Forms> PHRASE_EXCEPTIONS = createPhraseExceptions();

    private RussianGrammar() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static String item(String raw, GrammaticalCase grammaticalCase) {
        String phrase = normalize(raw);
        if (phrase.isBlank()) return "";
        return inflectPhrase(phrase, grammaticalCase, isAnimateItemPhrase(phrase));
    }

    public static String mob(String raw, GrammaticalCase grammaticalCase) {
        String phrase = normalize(raw);
        if (phrase.isBlank()) return "";
        return inflectPhrase(phrase, grammaticalCase, true);
    }

    public static String block(String raw, GrammaticalCase grammaticalCase) {
        String phrase = normalize(raw);
        if (phrase.isBlank()) return "";
        return inflectPhrase(phrase, grammaticalCase, false);
    }

    public static String structure(String raw, GrammaticalCase grammaticalCase) {
        return block(raw, grammaticalCase);
    }

    public static String biome(String raw, GrammaticalCase grammaticalCase) {
        return block(raw, grammaticalCase);
    }

    /** Effects and dimensions are intentionally never inflected. */
    public static String dimension(String raw) {
        return normalize(raw);
    }

    public static String effect(String raw) {
        return normalize(raw);
    }

    /** Returns the whole construction, e.g. "с коровой" / "со скелетом". */
    public static String withPreposition(String raw) {
        String phrase = mob(raw, GrammaticalCase.INSTRUMENTAL);
        if (phrase.isBlank()) return "";
        return (needsSo(phrase) ? "со " : "с ") + phrase;
    }

    private static Map<String, Forms> createWordExceptions() {
        Map<String, Forms> map = new HashMap<>();

        // Mobs used by the configured categories.
        exception(map, "осёл", "осёл", "осла", "ослом");
        exception(map, "зомби", "зомби", "зомби", "зомби");
        exception(map, "слизень", "слизень", "слизня", "слизнем");
        exception(map, "пиглин", "пиглин", "пиглина", "пиглином");
        exception(map, "скелет", "скелет", "скелета", "скелетом");
        exception(map, "страж", "страж", "стража", "стражем");
        exception(map, "спрут", "спрут", "спрута", "спрутом");
        exception(map, "паук", "паук", "паука", "пауком");
        exception(map, "паук", "паук", "паука", "пауком");
        exception(map, "броненосец", "броненосец", "броненосца", "броненосцем");
        exception(map, "поборник", "поборник", "поборника", "поборником");
        exception(map, "заклинатель", "заклинатель", "заклинателя", "заклинателем");
        exception(map, "разоритель", "разоритель", "разорителя", "разорителем");
        exception(map, "нюхач", "нюхач", "нюхача", "нюхачом");
        exception(map, "хранитель", "хранитель", "хранителя", "хранителем");
        exception(map, "вредина", "вредина", "вредину", "врединой");
        exception(map, "чешуйница", "чешуйница", "чешуйницу", "чешуйницей");
        exception(map, "лисица", "лисица", "лисицу", "лисицей");
        exception(map, "курица", "курица", "курицу", "курицей");
        exception(map, "свинья", "свинья", "свинью", "свиньёй");
        exception(map, "овца", "овца", "овцу", "овцой");
        exception(map, "собака", "собака", "собаку", "собакой");
        exception(map, "лошадь", "лошадь", "лошадь", "лошадью");
        exception(map, "мышь", "мышь", "мышь", "мышью");
        exception(map, "коза", "коза", "козу", "козой");
        exception(map, "пчела", "пчела", "пчелу", "пчелой");

        // Common Minecraft item / block nouns with stem changes.
        exception(map, "лосось", "лосось", "лосося", "лососем");
        exception(map, "треска", "треска", "треску", "треской");
        exception(map, "иглобрюх", "иглобрюх", "иглобрюха", "иглобрюхом");
        exception(map, "кролик", "кролик", "кролика", "кроликом");
        exception(map, "рисунок", "рисунок", "рисунок", "рисунком");
        exception(map, "камень", "камень", "камень", "камнем");
        exception(map, "саженец", "саженец", "саженца", "саженцем");
        exception(map, "ремень", "ремень", "ремень", "ремнём");
        exception(map, "день", "день", "день", "днём");
        exception(map, "огонь", "огонь", "огонь", "огнём");
        exception(map, "корень", "корень", "корень", "корнем");
        exception(map, "пень", "пень", "пень", "пнём");
        exception(map, "путь", "путь", "путь", "путём");
        exception(map, "конь", "конь", "коня", "конём");
        exception(map, "гость", "гость", "гостя", "гостем");
        exception(map, "каменщик", "каменщик", "каменщика", "каменщиком");

        // Plural heads.
        exception(map, "ворота", "ворота", "ворота", "воротами");
        exception(map, "листья", "листья", "листья", "листьями");
        exception(map, "доски", "доски", "доски", "досками");
        exception(map, "ножницы", "ножницы", "ножницы", "ножницами");

        return Map.copyOf(map);
    }

    private static Map<String, Forms> createPhraseExceptions() {
        Map<String, Forms> map = new HashMap<>();

        // Exact compound mob names from vanilla 1.21.5 RU.
        exception(map, "лошадь-скелет", "лошадь-скелет", "лошадь-скелета", "лошадью-скелетом");
        exception(map, "лошадь-зомби", "лошадь-зомби", "лошадь-зомби", "лошадью-зомби");
        exception(map, "крестьянин-зомби", "крестьянин-зомби", "крестьянина-зомби", "крестьянином-зомби");
        exception(map, "эндер-дракон", "эндер-дракон", "эндер-дракона", "эндер-драконом");
        exception(map, "визер-скелет", "визер-скелет", "визер-скелета", "визер-скелетом");
        exception(map, "тропическая рыба", "тропическая рыба", "тропическую рыбу", "тропической рыбой");
        exception(map, "пещерный паук", "пещерный паук", "пещерного паука", "пещерным пауком");
        exception(map, "пиглин", "пиглин", "пиглина", "пиглином");
        exception(map, "странствующий торговец", "странствующий торговец", "странствующего торговца", "странствующим торговцем");
        exception(map, "древний страж", "древний страж", "древнего стража", "древним стражем");
        exception(map, "брутальный пиглин", "брутальный пиглин", "брутального пиглина", "брутальным пиглином");
        exception(map, "светящийся спрут", "светящийся спрут", "светящегося спрута", "светящимся спрутом");
        exception(map, "зомбифицированный пиглин", "зомбифицированный пиглин", "зомбифицированного пиглина", "зомбифицированным пиглином");
        exception(map, "лама торговца", "лама торговца", "ламу торговца", "ламой торговца");
        exception(map, "полярный медведь", "полярный медведь", "полярного медведя", "полярным медведем");
        exception(map, "магмовый куб", "магмовый куб", "магмового куба", "магмовым кубом");

        return Map.copyOf(map);
    }

    private static void exception(Map<String, Forms> map, String key, String nominative, String accusative, String instrumental) {
        map.put(key, new Forms(nominative, accusative, instrumental));
    }

    private static boolean isAnimateItemPhrase(String phrase) {
        for (String word : phrase.split("\\s+")) {
            String cleaned = stripPunctuation(word);
            if (ANIMATE_ITEM_HEADS.contains(cleaned) || cleaned.equals("тропическая") && phrase.contains("рыба")) {
                return true;
            }
        }
        return false;
    }

    private static boolean needsSo(String phrase) {
        String first = firstWord(phrase);
        if (first.isBlank()) return false;

        // The patterns that matter for Minecraft: со скелетом, со слизнем,
        // со свиньёй, со скрученным..., etc.  The rule is phonetic rather than
        // a finite exception list: "с" + a difficult consonant cluster -> "со".
        if (first.equals("мной") || first.equals("мною")) return true;
        if (first.startsWith("с") || first.startsWith("з") || first.startsWith("ш") || first.startsWith("ж")) {
            return first.length() > 1 && isConsonant(first.charAt(1));
        }
        if (first.startsWith("вс") || first.startsWith("вз") || first.startsWith("встр")) {
            return true;
        }
        if (first.startsWith("л") || first.startsWith("р")) {
            return first.length() > 1 && first.charAt(1) == 'ь';
        }
        return false;
    }

    private static String firstWord(String phrase) {
        String[] words = phrase.trim().split("\\s+");
        return words.length == 0 ? "" : stripPunctuation(words[0]);
    }

    private static boolean isConsonant(char ch) {
        return "бвгджзйклмнпрстфхцчшщ".indexOf(ch) >= 0;
    }

    private static String stripPunctuation(String word) {
        return word.replaceAll("^[^\\p{IsAlphabetic}]+|[^\\p{IsAlphabetic}]+$", "");
    }

    private static String inflectPhrase(String phrase, GrammaticalCase grammaticalCase, boolean animate) {
        Forms exact = PHRASE_EXCEPTIONS.get(phrase);
        if (exact != null) return exact.form(grammaticalCase);

        String[] words = phrase.split("\\s+");
        int prepositionIndex = firstPrepositionIndex(words);
        if (prepositionIndex == 0) return phrase;

        // Inflect only the noun phrase before "из", "с", "для", etc.
        if (prepositionIndex > 0) {
            String prefix = inflectSegment(words, 0, prepositionIndex, grammaticalCase, animate, -1);
            return prefix == null ? phrase : prefix + " " + join(words, prepositionIndex, words.length);
        }

        int headIndex = chooseHeadIndex(words);
        String result = inflectSegment(words, 0, words.length, grammaticalCase, animate, headIndex);
        return result == null ? phrase : result;
    }

    private static int firstPrepositionIndex(String[] words) {
        for (int i = 0; i < words.length; i++) {
            if (PREPOSITIONS.contains(stripPunctuation(words[i]))) return i;
        }
        return -1;
    }

    private static int chooseHeadIndex(String[] words) {
        // A leading noun followed by another noun is normally a head +
        // genitive complement: "саженец акации", "кусок арбуза".
        String first = stripPunctuation(words[0]);
        if (canBeNoun(first) && !looksLikeAdjective(first) && words.length > 1) {
            return 0;
        }

        // Otherwise the final noun is the head: "золотой блок",
        // "жареный лосось", "пещерный паук".
        for (int i = words.length - 1; i >= 0; i--) {
            String token = stripPunctuation(words[i]);
            if (canBeNoun(token) && !looksLikeAdjective(token)) return i;
        }
        return words.length - 1;
    }

    private static String inflectSegment(String[] words, int start, int end, GrammaticalCase grammaticalCase,
                                         boolean animate, int requestedHeadIndex) {
        if (start >= end) return "";

        int headIndex = requestedHeadIndex;
        if (headIndex < start || headIndex >= end) {
            headIndex = chooseHeadIndex(java.util.Arrays.copyOfRange(words, start, end)) + start;
        }

        String headWord = stripPunctuation(words[headIndex]);
        if (!canBeNoun(headWord)) return null;

        NounAnalysis analysis = analyzeNoun(headWord);
        if (analysis == null) return null;

        StringBuilder out = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) out.append(' ');
            String token = words[i];
            if (i == headIndex) {
                out.append(inflectNoun(token, grammaticalCase, animate, analysis));
            } else if (i < headIndex || looksLikeAdjective(stripPunctuation(token))) {
                // Russian adjectives/participles may stand either before or after
                // the head noun: "золотой блок" / "хаустония серая".
                // Genitive complements such as "дыхание дракона" are nouns and
                // therefore remain untouched.
                out.append(inflectModifier(token, grammaticalCase, analysis.gender(), animate));
            } else {
                out.append(token);
            }
        }
        return out.toString();
    }

    private static boolean canBeNoun(String word) {
        if (word.isBlank()) return false;
        if (WORD_EXCEPTIONS.containsKey(word)) return true;
        if (PLURAL_HEADS.contains(word)) return true;
        return word.endsWith("а") || word.endsWith("я") || word.endsWith("о") || word.endsWith("е")
                || word.endsWith("ё") || word.endsWith("ь") || word.endsWith("й") || word.endsWith("и")
                || word.matches(".*[бвгджзклмнпрстфхцчшщ]$");
    }

    private static NounAnalysis analyzeNoun(String word) {
        if (PLURAL_HEADS.contains(word) || (word.endsWith("и") && !looksLikeAdjective(word))) {
            return new NounAnalysis(Gender.PLURAL);
        }
        if (word.endsWith("ия") || word.endsWith("ья") || word.endsWith("а") || word.endsWith("я")) {
            return new NounAnalysis(Gender.FEMININE);
        }
        if (word.endsWith("о") || word.endsWith("е") || word.endsWith("ё")) {
            return new NounAnalysis(Gender.NEUTER);
        }
        if (word.endsWith("й") || word.matches(".*[бвгджзклмнпрстфхцчшщ]$")) {
            return new NounAnalysis(Gender.MASCULINE);
        }
        if (word.endsWith("ь")) {
            return new NounAnalysis(MASCULINE_SOFT_SIGN.contains(word) ? Gender.MASCULINE : Gender.FEMININE);
        }
        return null;
    }

    private static String inflectNoun(String token, GrammaticalCase grammaticalCase, boolean animate, NounAnalysis analysis) {
        String lower = token.toLowerCase(Locale.ROOT);
        Forms exception = WORD_EXCEPTIONS.get(lower);
        if (exception != null) return preserveCaseShape(token, exception.form(grammaticalCase));

        return switch (analysis.gender()) {
            case PLURAL -> inflectPluralNoun(lower, grammaticalCase);
            case FEMININE -> inflectFeminineNoun(lower, grammaticalCase);
            case NEUTER -> inflectNeuterNoun(lower, grammaticalCase);
            case MASCULINE -> inflectMasculineNoun(lower, grammaticalCase, animate);
        };
    }

    private static String inflectPluralNoun(String word, GrammaticalCase grammaticalCase) {
        return switch (grammaticalCase) {
            case NOMINATIVE, ACCUSATIVE -> word;
            case INSTRUMENTAL -> {
                if (word.endsWith("ья") || word.endsWith("я")) yield word.substring(0, word.length() - 1) + "ями";
                if (word.endsWith("ья")) yield word.substring(0, word.length() - 2) + "ьями";
                if (word.endsWith("и")) yield word.substring(0, word.length() - 1) + "ями";
                if (word.endsWith("ы")) yield word.substring(0, word.length() - 1) + "ыми";
                yield word + "ми";
            }
        };
    }

    private static String inflectMasculineNoun(String word, GrammaticalCase grammaticalCase, boolean animate) {
        if (word.endsWith("ий") || word.endsWith("ый") || word.endsWith("ой")) {
            // This branch is normally reached only for a false-positive noun;
            // adjective detection prevents it for standard Minecraft phrases.
            return word;
        }
        if (word.endsWith("й")) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> animate ? word.substring(0, word.length() - 1) + "я" : word;
                case INSTRUMENTAL -> word.substring(0, word.length() - 1) + "ем";
            };
        }
        if (word.endsWith("ь")) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> animate ? word.substring(0, word.length() - 1) + "я" : word;
                case INSTRUMENTAL -> word.substring(0, word.length() - 1) + "ем";
            };
        }
        if (word.endsWith("ец")) {
            String stem = word.substring(0, word.length() - 2);
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> animate ? stem + "ца" : word;
                case INSTRUMENTAL -> stem + "цем";
            };
        }

        String instrumentalEnding = needsInstrumentalEm(word) ? "ем" : "ом";
        return switch (grammaticalCase) {
            case NOMINATIVE -> word;
            case ACCUSATIVE -> animate ? word + "а" : word;
            case INSTRUMENTAL -> word + instrumentalEnding;
        };
    }

    private static boolean needsInstrumentalEm(String word) {
        return word.endsWith("ж") || word.endsWith("ч") || word.endsWith("ш") || word.endsWith("щ")
                || word.endsWith("ц") || word.endsWith("й") || word.endsWith("ь");
    }

    private static String inflectFeminineNoun(String word, GrammaticalCase grammaticalCase) {
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
                case INSTRUMENTAL -> word.substring(0, word.length() - 2) + "ьёй";
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
                case NOMINATIVE, ACCUSATIVE -> word;
                case INSTRUMENTAL -> word.substring(0, word.length() - 1) + "ью";
            };
        }
        return word;
    }

    private static String inflectNeuterNoun(String word, GrammaticalCase grammaticalCase) {
        return switch (grammaticalCase) {
            case NOMINATIVE, ACCUSATIVE -> word;
            case INSTRUMENTAL -> word.endsWith("е") || word.endsWith("ё")
                    ? word.substring(0, word.length() - 1) + "ем"
                    : word.substring(0, word.length() - 1) + "ом";
        };
    }

    private static String inflectModifier(String word, GrammaticalCase grammaticalCase, Gender gender, boolean animate) {
        if (word.isBlank()) return word;

        String[] parts = word.split("-", -1);
        if (parts.length > 1) {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) out.append('-');
                out.append(inflectModifier(parts[i], grammaticalCase, gender, animate));
            }
            return out.toString();
        }

        String lower = word.toLowerCase(Locale.ROOT);
        if (isPreposition(lower) || lower.matches("^[0-9].*")) return lower;
        if (!looksLikeAdjective(lower)) return lower;
        return inflectAdjective(lower, grammaticalCase, gender, animate);
    }

    private static boolean looksLikeAdjective(String word) {
        // Words ending in -ание/-ение/-ование are overwhelmingly nouns in
        // Minecraft translations ("дыхание дракона", "состояние", etc.).
        // Do not mistake their final -ие for a plural/adjective ending.
        if (word.endsWith("ание") || word.endsWith("ение") || word.endsWith("ование")
                || word.endsWith("ирование")) {
            return false;
        }

        return word.endsWith("ый") || word.endsWith("ий") || word.endsWith("ой")
                || word.endsWith("ая") || word.endsWith("яя")
                || word.endsWith("ое") || word.endsWith("ее")
                || word.endsWith("ые") || word.endsWith("ие")
                || word.endsWith("енный") || word.endsWith("ённый")
                || word.endsWith("анные") || word.endsWith("янные")
                || word.endsWith("ённые")
                || word.endsWith("ованные") || word.endsWith("еванные")
                || word.endsWith("ированные")
                || word.endsWith("ящий") || word.endsWith("ящая") || word.endsWith("ящие")
                || word.endsWith("ующий") || word.endsWith("ующая") || word.endsWith("ующие")
                || word.endsWith("ющий") || word.endsWith("ющая") || word.endsWith("ющие")
                || word.endsWith("шийся") || word.endsWith("шаяся") || word.endsWith("шиеся")
                || word.endsWith("шийся") || word.endsWith("шаяся") || word.endsWith("шиеся")
                || word.endsWith("шийся") || word.endsWith("вшийся") || word.endsWith("вшаяся") || word.endsWith("вшиеся")
                || word.endsWith("евшийся") || word.endsWith("евшаяся") || word.endsWith("евшиеся")
                || word.endsWith("юшийся") || word.endsWith("ющаяся") || word.endsWith("ющееся") || word.endsWith("ющиеся");
    }

    private static String inflectAdjective(String word, GrammaticalCase grammaticalCase, Gender gender, boolean animate) {
        // Reflexive participles/adjectives: "вьющаяся лоза" ->
        // "вьющуюся лозу".  The trailing -ся stays unchanged.
        if (word.endsWith("аяся")) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> word.substring(0, word.length() - 4) + "уюся";
                case INSTRUMENTAL -> word.substring(0, word.length() - 4) + "ойся";
            };
        }
        if (word.endsWith("яяся")) {
            return switch (grammaticalCase) {
                case NOMINATIVE -> word;
                case ACCUSATIVE -> word.substring(0, word.length() - 4) + "ююся";
                case INSTRUMENTAL -> word.substring(0, word.length() - 4) + "ейся";
            };
        }
        if (word.endsWith("иеся") || word.endsWith("ыеся")) {
            return switch (grammaticalCase) {
                case NOMINATIVE, ACCUSATIVE -> word;
                case INSTRUMENTAL -> word.endsWith("иеся")
                        ? word.substring(0, word.length() - 4) + "имися"
                        : word.substring(0, word.length() - 4) + "ымися";
            };
        }
        if (gender == Gender.PLURAL) {
            return switch (grammaticalCase) {
                case NOMINATIVE, ACCUSATIVE -> word;
                case INSTRUMENTAL -> {
                    if (word.endsWith("ие")) yield word.substring(0, word.length() - 2) + "ими";
                    if (word.endsWith("ые")) yield word.substring(0, word.length() - 2) + "ыми";
                    yield word;
                }
            };
        }

        if (grammaticalCase == GrammaticalCase.ACCUSATIVE) {
            return switch (gender) {
                case FEMININE -> word.endsWith("ая")
                        ? word.substring(0, word.length() - 2) + "ую"
                        : word.endsWith("яя") ? word.substring(0, word.length() - 2) + "юю" : word;
                case NEUTER -> word;
                case MASCULINE -> {
                    if (!animate) yield word;
                    if (word.endsWith("ий")) yield word.substring(0, word.length() - 2) + "его";
                    if (word.endsWith("ый") || word.endsWith("ой")) yield word.substring(0, word.length() - 2) + "ого";
                    yield word;
                }
                case PLURAL -> word;
            };
        }

        if (grammaticalCase == GrammaticalCase.INSTRUMENTAL) {
            return switch (gender) {
                case FEMININE -> word.endsWith("ая")
                        ? word.substring(0, word.length() - 2) + "ой"
                        : word.endsWith("яя") ? word.substring(0, word.length() - 2) + "ей" : word;
                case NEUTER -> word.endsWith("ое")
                        ? word.substring(0, word.length() - 2) + "ым"
                        : word.endsWith("ее") ? word.substring(0, word.length() - 2) + "им" : word;
                case MASCULINE -> word.endsWith("ий")
                        ? word.substring(0, word.length() - 2) + "им"
                        : word.endsWith("ый") || word.endsWith("ой")
                        ? word.substring(0, word.length() - 2) + "ым" : word;
                case PLURAL -> word;
            };
        }

        return word;
    }

    private static boolean isPreposition(String word) {
        return PREPOSITIONS.contains(word);
    }

    private static String preserveCaseShape(String original, String replacement) {
        if (original.isEmpty()) return replacement;
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
        }
        return replacement;
    }

    private static String join(String[] words, int start, int end) {
        StringBuilder out = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) out.append(' ');
            out.append(words[i]);
        }
        return out.toString();
    }
}
