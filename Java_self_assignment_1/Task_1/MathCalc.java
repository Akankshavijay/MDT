public class MathCalc
{
//	Just an example main() from task as it is
//	public static void main(String[] args)
//	{
//		double radius = 0.0;
//		double circleArea = 100.0;
//		int feet = 0;
//		int inches = 0;
//		radius = Math.sqrt(circleArea/Math.PI);
//		feet = (int)Math.floor(radius);
//		inches = (int)Math.round(12.0*(radius - feet));
//		System.out.println("The radius of a circle with area " + circleArea + " square feet is\n " + feet + " feet " + inches + " inches");
//	}

    public static void main(String[] args) {
        double earthDiameter = 7600.0;
        double sunDiameter = 865000.0;

        double earthRadius = earthDiameter / 2.0;
        double sunRadius = sunDiameter / 2.0;

        double earthVolume = (4.0 / 3.0) * Math.PI * Math.pow(earthRadius, 3);
        double sunVolume = (4.0 / 3.0) * Math.PI * Math.pow(sunRadius, 3);
        double ratio = sunVolume / earthVolume;

        System.out.printf("The volume of the Earth is %.2f cubic miles, the volume of the sun is %.2f cubic miles, and the ratio of the volume of the Sun to the volume of the Earth is %.2f.%n", earthVolume, sunVolume, ratio);
    }
}
