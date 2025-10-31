
public class BinarySearchRecursive {
	
	public static Boolean binary_search(int[] data, int item, int left,
			int right)
			{
			if(left > right) {
			return false;
			} else {
			
			//incorrect (can result in overflow):
			//int mid = (left + right) / 2
			int mid = (int)(left + (right-left)/2);
			if(item == data[mid]) {
			return true;
			} else if(item < data[mid]) {
			//recursively search the left half
			return binary_search(data, item, left, mid-1);
			} else {
			//recursively search the right half
			return binary_search(data, item, mid+1, right);
			}
			}
			}

	public static void main(String[] args) {
		
		
		int[] items = new int[]{-100, 123, 200, 400};
		int[] arr1 = new int[]{1,3,5,123,400};
		int[] arr2 = new int[]{1,3,5,123,400};
		int found = -1, item = 123;
		int left = 0, right = arr1.length-1;
		System.out.print("array: ");
		for(int i=0; i<arr1.length; i++) {
		System.out.print(arr1[i]+" ");
		}
		System.out.println("");
		for(Integer item2 : items) {
		arr1 = arr2;
		System.out.println("searching for item: "+item2);
		left = 0;
		right = arr1.length-1;
		Boolean result = binary_search(arr1, item2, left, right);
		System.out.println("item: "+item2+ " found: "+result);
		}
		
		if(found >= 0)
		System.out.println("found "+item+" in position "+found);
		else
		System.out.println(item+" not found");
	}

}
