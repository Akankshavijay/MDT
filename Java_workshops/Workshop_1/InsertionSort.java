

public class InsertionSort {
	public static void insertionSort(int[] arr1)
	{
	// Traverse through 1 to len(arr1)
	for(int i=1; i<arr1.length; i++) {
	int key = arr1[i];
	//Move elements of arr1[0..i-1], that are
	//greater than key, to one position ahead
	//of their current position
	int j = i-1;
	while((j >=0) && (key < arr1[j])) {
	arr1[j+1] = arr1[j];
	j -= 1;
	}
	arr1[j+1] = key;
	//System.out.println("New order: "+arr1);
	}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	
		int[] arr1 = new int[]{12, 11, 13, 5, 6};
		System.out.print("Initial: ");
		for(int i=0; i<arr1.length; i++) {
		System.out.print(arr1[i]+" ");
		}
		System.out.println();
		insertionSort(arr1);
		System.out.print("Sorted: ");
		for(int i=0; i<arr1.length; i++) {
		System.out.print(arr1[i]+" ");
		}
		System.out.println();
		
	}

}
