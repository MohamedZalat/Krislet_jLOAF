package Behaviour;

public class KickSpinOracle extends Behaviour {

	private int turnCount;
	private int turnDirection;
	
	public KickSpinOracle() {
		this.turnCount = 0;
		this.turnDirection = -1;
	}

	@Override
	public int getAction(boolean ballVisible, double ballDirection, double ballDistance, 
			boolean goalVisible, double goalDirection, double goalDistance) {
		
		boolean ballClose;
		
		// Perform 360 turn if the flag is set (state based behavior)
		if (this.turnCount > 0) {
			this.turnCount--;
			if (turnDirection > 0) {
				return Behaviour.turnP;
			} else {
				return Behaviour.turnN;
			}
		}
		
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
					// Set parameters for the state based behaviour
					this.turnCount = 360/Behaviour.turnAngle;
					this.turnDirection = this.turnDirection * -1;
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
