public class Palindrome {
    /* Case, spaces and punctuation are ignored, so "A man, a plan, a canal:
     * Panama" counts as a palindrome. */
    public static boolean isPalindrome(String text) {
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                cleaned.append(Character.toLowerCase(ch));
            }
        }
        String value = cleaned.toString();
        return value.equals(cleaned.reverse().toString());
    }

    public static void main(String[] args) {
        String text = "A man, a plan, a canal: Panama";
        if (args.length > 0) {
            text = String.join(" ", args);
        }
        if (isPalindrome(text)) {
            System.out.println("\"" + text + "\" is a palindrome.");
        } else {
            System.out.println("\"" + text + "\" is not a palindrome.");
        }
    }
}
