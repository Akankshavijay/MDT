public class TextSort {
    public static void main(String[] args) {
        String text = "To be or not to be, that is the question;"
						+" Whether `tis nobler in the mind to suffer"
						+" the slings and arrows of outrageous fortune,"
						+" or to take arms against a sea of troubles,"
						+" and by opposing end them?";
						
		//word is a String without the space
		String[] splited = text.split(" ");
		
		int n = splited.length;
      
        for (int i = 0; i < n - 1; i++)
            for (int j = 0; j < n - i - 1; j++)
                if (splited[j].compareToIgnoreCase(splited[j + 1]) > 0) {
                    String temp = splited[j];
                    splited[j] = splited[j + 1];
                    splited[j + 1] = temp;
                }
                
        for (int i = 0; i < n; ++i)
            System.out.print(splited[i] + " ");
    }
}
