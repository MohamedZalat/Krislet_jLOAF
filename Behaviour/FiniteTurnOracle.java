package Behaviour;

public class FiniteTurnOracle extends Behaviour {

	public FiniteTurnOracle() {
		super();
	}
	
	public int getAction(boolean ballVisible, double ballDirection, double ballDistance, 
			boolean goalVisible, double goalDirection, double goalDistance) {

		boolean ballClose;
		
		// Check to see if the ball is visible, far or close.
		if (ballVisible) {
			if (ballDistance > 1.0) {
				ballClose = false;
			} else {
				ballClose = true;
			}
		} else {
			ballClose = false;
		}

   		// Select the action
		if (ballVisible) {
			if (ballClose) {
				if (goalVisible) {
					// seeBall && ballClose && seeGoal -> kick
					return Behaviour.kick;
				} else {
					// seeBall && ballClose && !seeGoal -> turnP
					return Behaviour.turnP;
				}
			} else {
				// !ballClose, not facing it
				if ((ballDirection > Behaviour.turnAngle) || (ballDirection < (-1 * Behaviour.turnAngle))) {
					// !ballClose -> turn to it
					if(ballDirection > 0)
				    	return Behaviour.turnP;
				    else
				    	return Behaviour.turnN;
				} else {
					// seeBall && !ballClose -> dash
					return Behaviour.dash;
				}
			}
		} else {
			// !seeBall -> turn
			return Behaviour.turnP;
		}
	}
}