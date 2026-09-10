import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionsDemo {
    public static void main(String[] args) {
        List<String> languages = new ArrayList<>();
        languages.add("Java");
        languages.add("Python");
        languages.add("Go");
        System.out.println("Languages: " + languages);

        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        scores.forEach((name, score) ->
            System.out.println(name + " scored " + score));

        languages.stream()
                 .filter(lang -> lang.length() > 2)
                 .map(String::toUpperCase)
                 .forEach(System.out::println);
    }
}
