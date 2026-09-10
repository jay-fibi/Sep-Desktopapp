public class LoopsDemo {
    public static void main(String[] args) {
        System.out.println("For loop:");
        for (int i = 1; i <= 5; i++) {
            System.out.println("  i = " + i);
        }

        System.out.println("While loop:");
        int count = 3;
        while (count > 0) {
            System.out.println("  count = " + count);
            count--;
        }

        System.out.println("Enhanced for loop:");
        String[] fruits = {"Apple", "Banana", "Cherry"};
        for (String fruit : fruits) {
            System.out.println("  " + fruit);
        }
    }
}
