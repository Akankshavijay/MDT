public class LinearSearch {

	public static void main(String[] args) {
		
		int found = -1, item = 123;
		int[] arr1 = new int[]{1,3,5,123,400};
		for(int i=0; i<arr1.length; i++) {
		if (item == arr1[i]) {
		found = i;
		break;
		}
		}
		if (found >= 0)
		System.out.println("found "+item+" in position "+found);
		else
		System.out.println(item+" not found");
	}

}
