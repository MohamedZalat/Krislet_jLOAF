package Behaviour;

public class TurnDirectionOracle extends Behaviour {
	
	private int lastGoalDirection;
	private int lastBallDirection;
	
	public TurnDirectionOracle() {
		this.lastGoalDirection = -1;
		this.lastBallDirection = -1;
	}

	@Override
	public int getAction(boolean ballVisible, double ballDirection, double ballDistance, 
			boolean goalVisible, double goalDirection, double goalDistance) {
		
		boolean ballClose;
		
		// Check to see if the ball is visible, far or close, and set last ball direction.
		if (ballVisible) {
			if (ballDistance > 1.0) {
				ballClose = false;
			} else {
				ballClose = true;
			}
			if (ballDirection > 0) {
				this.lastBallDirection = 1;
			} else {
				this.lastBallDirection = -1;
			}
			
		} else {
			ballClose = false;
		}
		
		// Set last goal direction
		if (goalVisible) {
			if (goalDirection > 0) {
				this.lastGoalDirection = 1;
			} else {
				this.lastGoalDirection = -1;
			}
		}
		
		
   		// Select the action
		if (ballVisible) {
			if (ballClose) {
				if (goalVisible) {
					// seeBall && ballClose && seeGoal -> kick
					return Behaviour.kick;
				} else {
					// seeBall && ballClose && !seeGoal -> turn last known goal direction
					if (this.lastGoalDirection > 0) {
						return Behaviour.turnP;
					} else {
						return Behaviour.turnN;
					}
				}
			} else {
				
				if (Math.abs(ballDirection) > Behaviour.turnAngle) {
					// Ball is visible but not close... but it's also outside of the turning angle. Turn
					// to face it more head on.
					if (this.lastBallDirection > 0) {
						return Behaviour.turnP;
					} else {
						return Behaviour.turnN;
					}
				} else {
					// seeBall && !ballClose -> dash
					return Behaviour.dash;
				}
			}
		} else {
			// !seeBall -> turn last known ball direction
			if (this.lastBallDirection > 0) {
				return Behaviour.turnP;
			} else {
				return Behaviour.turnN;
			}
		}
	}
}
