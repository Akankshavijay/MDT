
public class BinarySearch {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int[] arr1 = new int[]{1,3,5, 20, 123, 127, 400};
		int left = 0, found = -1, item = 123;
		int right = arr1.length-1;
		System.out.print("array: ");
		for(int i=0; i<arr1.length; i++) {
		System.out.print(arr1[i]+" ");
		}
		System.out.println("");
		while(left <= right) {
		int mid = (int)(left + (right-left)/2);
		if(arr1[mid] == item) {
		found = mid;
		break;
		} else if (arr1[mid] < item) {
		left = mid+1;
		} else {
		right = mid-1;
		}
		}
		if(found >= 0)
		System.out.println("found "+item+" in position "+found);
		else
		System.out.println(item+" not found");
	}

}
