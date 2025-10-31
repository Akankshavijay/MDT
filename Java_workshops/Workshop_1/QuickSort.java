

public class QuickSort {

	
		public static void sort(int[] values) {
			quicksort(values);
		}
		
		public static void quicksort(int[] arr) {
		if(arr.length == 0) 		return;
		else 		quicksort(arr, 0, arr.length - 1);
				}
		// Sort interval [lo, hi] inplace recursively
		public static void quicksort (int[] arr, int lo, int hi)
		{
		if (lo < hi) {
		int splitPoint = partition(arr, lo, hi);
		quicksort(arr, lo, splitPoint);
		quicksort(arr, splitPoint + 1, hi);
		}
		}	
		// Performs Hoare partition algorithm for quicksort
		public static int partition(int[] arr, int lo, int hi)
		{
		int pivot = arr[lo];
		int i = lo - 1;
		int j = hi + 1;
		while (true) {
		do {
		i += 1;
		}
		while (arr[i] < pivot);
		do {
		j -= 1;
		}
		while (arr[j] > pivot);
		if (i < j) swap(arr, i, j);
		else return j;
		}
		}
		// Swap two elements
		public static void swap(int[] arr, int i, int j)
		{
		int tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
		}
		
		
	public static void main(String[] args) {
		
		int[] array = new int[]{10, 4, 6, 4, 8, -13, 2, 3};
		System.out.println("Initial array:");
		for (int num : array )
		System.out.print(num+" ");
		System.out.println();
		sort(array);
		System.out.println("Sorted array:");
		for (int num : array )
		System.out.print(num+" ");
		System.out.println();
	}

}
