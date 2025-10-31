

public class MergeSort1 {
	public static void MergeSort1(){}
	public static int[] MergeItems(int[] items1, int[] items2, int[]
			items3)
			{
			int ndx1 = 0, ndx2 = 0, ndx3 = 0;
			// => always add the smaller element first:
			while(ndx1 < items1.length && ndx2 < items2.length)
			{
			//System.out.println(
			//"items1 data:"+items1[ndx1]+" items2 data:"+items2[ndx2]);
			int data1 = items1[ndx1];
			int data2 = items2[ndx2];
			if(data1 < data2) {
			//System.out.println("adding data1: "+data1);
			items3[ndx3] = data1;
			ndx1 += 1;
			} else {
			//System.out.println("adding data2: "+data2);
			items3[ndx3] = data2;
			ndx2 += 1;
			}
			ndx3 += 1;
			}
			// append any remaining elements of items1:
			while(ndx1 < items1.length) {
			//System.out.println("MORE items1: "+items1[ndx1]);
			items3[ndx3] = items1[ndx1];
			ndx1 += 1;
			}
			// append any remaining elements of items2:
			while(ndx2 < items2.length) {
			//System.out.println("MORE items2: "+items2[ndx2]);
			items3[ndx3] = items2[ndx2];
			ndx2 += 1;
			}
			return items3;
			}
			public static void display_items(int[] items)
			{
			for(int item : items)
			System.out.print(item+" ");
			System.out.println("");
			}
			
	public static void main(String[] args) {
		
				int[] items1 = new int[]{20, 30, 50, 300};
				
				int[] items2 = new int[]{80, 100, 200};
				int[] items3 = new int[items1.length+items2.length];
				// display the initial and merged lists:
				System.out.println("First sorted array:");
				display_items(items1);
				System.out.println("");
				System.out.println("Second sorted array:");
				display_items(items2);
				System.out.println("");
				System.out.println("Merged array:");
				items3 = MergeItems(items1, items2, items3);
				display_items(items3);
	}

}
