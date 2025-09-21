package Aissignment_task_2_3;

public class prime {

	public static void main(String[] args)
	{
		int nValues = 50;
		number_loop: for(int i = 2; i <= nValues; i++)	{
						for (int j = 2; j * j <= i; j++)	{
							if (i % j == 0)
							{
								continue number_loop;
							}
						}
						System.out.println(i);
					}

	}
}