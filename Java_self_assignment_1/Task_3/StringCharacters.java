package Aissignment_task_2_3;

public class StringCharacters
{
	public static void main(String[] args)
	{
		String text = "To be or not to be, that is the question;"
						+"Whether `tis nobler in the mind to suffer"
						+" the slings and arrows of outrageous fortune,"
						+" or to take arms against a sea of troubles,"
						+" and by opposing end them?";
		int spaces = 0,
			vowels = 0,
			letters = 0;
		
		for (int i = 0; i < text.length(); i++) {
            char ch_text = Character.toLowerCase(text.charAt(i));

            if (Character.isLetter(ch_text)) {
                letters++;

                if (ch_text == 'a' || ch_text == 'e' || ch_text == 'i' || ch_text == 'o' || ch_text == 'u') {
                    vowels++;
                }
            } else if (ch_text == ' ') {
                spaces++;
            }
        }
		
		int consonants = letters - vowels;
		 System.out.println("The text contained vowels: " + vowels + "\n" + "consonants: " + consonants + "\n" + "spaces: " + spaces);
	}
} 
