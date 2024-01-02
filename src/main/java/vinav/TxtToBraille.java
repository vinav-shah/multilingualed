package vinav;
import java.util.HashMap;
import java.util.Map;

public class TxtToBraille {
    // Braille representation for each alphabet and number using Unicode literals
    private static final Map<Character, Character> brailleMap = new HashMap<>();

    static {
        brailleMap.put('a', '\u2801');
        brailleMap.put('b', '\u2803');
        brailleMap.put('c', '\u2809');
        brailleMap.put('d', '\u2819');
        brailleMap.put('e', '\u2811');
        brailleMap.put('f', '\u280B');
        brailleMap.put('g', '\u281B');
        brailleMap.put('h', '\u2813');
        brailleMap.put('i', '\u280A');
        brailleMap.put('j', '\u281A');
        brailleMap.put('k', '\u2805');
        brailleMap.put('l', '\u2807');
        brailleMap.put('m', '\u280D');
        brailleMap.put('n', '\u281D');
        brailleMap.put('o', '\u2815');
        brailleMap.put('p', '\u280F');
        brailleMap.put('q', '\u281F');
        brailleMap.put('r', '\u2817');
        brailleMap.put('s', '\u280E');
        brailleMap.put('t', '\u281E');
        brailleMap.put('u', '\u2825');
        brailleMap.put('v', '\u2827');
        brailleMap.put('w', '\u283A');
        brailleMap.put('x', '\u282D');
        brailleMap.put('y', '\u283D');
        brailleMap.put('z', '\u2835');
        brailleMap.put(' ', '\u2800');
        brailleMap.put('0', '\u281A');
        brailleMap.put('1', '\u2801');
        brailleMap.put('2', '\u2803');
        brailleMap.put('3', '\u2809');
        brailleMap.put('4', '\u2819');
        brailleMap.put('5', '\u2811');
        brailleMap.put('6', '\u280B');
        brailleMap.put('7', '\u281B');
        brailleMap.put('8', '\u2813');
        brailleMap.put('9', '\u280A');
    }

    public static String translateToBraille(String text) {
        StringBuilder brailleText = new StringBuilder();
        boolean isNumber = false;
        for (char c : text.toLowerCase().toCharArray()) {
            if (Character.isDigit(c)) {
                if (!isNumber) {
                    brailleText.append('\u283C');  // Number sign
                    isNumber = true;
                }
            } else {
                isNumber = false;
            }
            Character brailleChar = brailleMap.get(c);
            if (brailleChar != null) {
                brailleText.append(brailleChar);
            }
        }
        return brailleText.toString();
    }

    public static void main(String[] args) {
        String text = "Hello123";
        String brailleTranslation = translateToBraille(text);
        System.out.println("Original Text: " + text);
        System.out.println("Braille Translation: " + brailleTranslation);
    }
}
