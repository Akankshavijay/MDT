

public class BubbleSort {

	public static void main(String[] args) 
	{
		
		int[] arr1 = new int[]{40, 10, 25, 17, 30, 20};
		System.out.print("Initial: ");
		
		for(int i=0; i<arr1.length; i++) {
		System.out.print(arr1[i]+" ");
		}
		System.out.println("");
		for(int i=0; i<arr1.length-1; i++) {
		for(int j=i+1; j<arr1.length; j++) {
		if(arr1[i] > arr1[j]) {
		int temp = arr1[i];
		arr1[i] = arr1[j];
		arr1[j] = temp;
		}
		}
		}
		System.out.print("Sorted: ");
		for(int i=0; i<arr1.length; i++) {
		System.out.print(arr1[i]+" ");
		}
		System.out.println("");
		}

	}

