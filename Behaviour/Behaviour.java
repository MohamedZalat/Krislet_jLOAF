package Behaviour;

public abstract class Behaviour {

	// actions correspond to ['turn+','turn-', 'dash', 'kick']
	public static final int turnP = 0;
	public static final int turnN = 1;
	public static final int dash = 2;
	public static final int kick = 3;
	
	public static final int turnAngle = 10;
	
	protected Behaviour() {}
	
	public abstract int getAction(boolean ballVisible, double ballDirection, double ballDistance, 
			boolean goalVisible, double goalDirection, double goalDistance);
	
}
