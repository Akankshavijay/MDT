
import java.util.Random;

public class RandArr {
	 public static int[] generateRandomArray(int size, int min, int max) {
	        Random random = new Random();
	        int[] array = new int[size];

	        for (int i = 0; i < size; i++) {
	            array[i] = random.nextInt((max - min) + 1) + min;
	        }

	        return array;
	    }
	public static void main(String[] args) {
		
		int arraySize = 10; // Size of the array
        int minValue = 1;   // Minimum value for random numbers
        int maxValue = 100; // Maximum value for random numbers

        int[] randomArray = generateRandomArray(arraySize, minValue, maxValue);

        // Print the array
        for (int value : randomArray) {
            System.out.print(value + " ");
        }
	}

}
