//
//	File:			Brain.java
//	Author:		Krzysztof Langner
//	Date:			1997/04/28
//
//    Modified by:	Paul Marlow

//    Modified by:      Edgar Acosta
//    Date:             March 4, 2008

//	  Modified by: 		Sacha Gunaratne
// 	  Date:				April 16 2017

import java.lang.Math;
import java.util.Vector;
import java.util.regex.*;

import org.jLOAF.action.Action;
import org.jLOAF.casebase.CaseBase;
import org.jLOAF.inputs.AtomicInput;
import org.jLOAF.inputs.ComplexInput;
import org.jLOAF.inputs.Feature;
import org.jLOAF.inputs.Input;
import org.jLOAF.preprocessing.filter.casebasefilter.Sampling;
import org.jLOAF.sim.SimilarityMetricStrategy;
import org.jLOAF.sim.atomic.EuclideanDistance;
import org.jLOAF.sim.complex.GreedyMunkrezMatching;
import org.jLOAF.sim.complex.Mean;
import org.jLOAF.sim.complex.WeightedMean;
import org.jLOAF.weights.SimilarityWeights;

import AgentModules.RoboCupAction;
import AgentModules.RoboCupAgent;
import AgentModules.RoboCupInput;

class Brain extends Thread implements SensorInput
{
    //---------------------------------------------------------------------------
    // This constructor:
    // - stores connection to krislet
    // - starts thread for this object
    public Brain(SendCommand krislet, 
		 String team, 
		 char side, 
		 int number, 
		 String playMode,
		 String matchType, 
		 String cbname)
    {
	m_timeOver = false;
	m_krislet = krislet;
	m_memory = new Memory();
	//m_team = team;
	m_side = side;
	// m_number = number;
	m_playMode = playMode;
	
	
	//add agent
	CaseBase cb = CaseBase.load(cbname);
	//Sampling s = new Sampling();
	//CaseBase processed_cb = s.filter(cb);
	agent = new RoboCupAgent();
	agent.train(cb);
	
	start();
    }

    public void run()
    {
	// first put it somewhere on my side
	if(Pattern.matches("^before_kick_off.*",m_playMode))
	    m_krislet.move( -Math.random()*52.5 , 34 - Math.random()*68.0 );

	while( !m_timeOver )
	    {	
		//get input and action - make decision
		Input input = this.Convert2Complex(m_memory);
		//Input input = ((RoboCupPerception)agent.getP()).sense(m_memory);
		if(input!=null){
//			Action action = agent.getR().selectAction(input);
//			agent.run(input);

			//cast to RoboCupAction
			RoboCupAction a = agent.run(input);

			//cases
			if(a.getName().equals("turn")){
				//get the angle of the feature
				
				m_krislet.turn(a.getFeature(0).getValue());
				System.out.println("turn "+ a.getFeature(0).getValue());
				m_memory.waitForNewInfo();
			}
			else if(a.getName().equals("dash")){	
				//get the power of the dash
				System.out.println("dash "+ a.getFeature(0).getValue());
				m_krislet.dash(a.getFeature(0).getValue());
			}
			else if(a.getName().equals("kick")){
				System.out.println("kick "+ a.getFeature(0).getValue() + " " +  a.getFeature(1).getValue());
				m_krislet.kick(a.getFeature(0).getValue(), a.getFeature(1).getValue());
			}

		}else{
			m_memory.waitForNewInfo();
		}
		// sleep one step to ensure that we will not send
				// two commands in one cycle.
		try{
		    Thread.sleep(2*SoccerParams.simulator_step);
		}catch(Exception e){}
		
	    }
	    
	
	m_krislet.bye();
    }


    //===========================================================================
    // Here are suporting functions for implement logic


    //===========================================================================
    // Implementation of SensorInput Interface

    //---------------------------------------------------------------------------
    // This function sends see information
    public void see(VisualInfo info)
    {
	m_memory.store(info);
    }


    //---------------------------------------------------------------------------
    // This function receives hear information from player
    public void hear(int time, int direction, String message)
    {
    }

    //---------------------------------------------------------------------------
    // This function receives hear information from referee
    public void hear(int time, String message)
    {						 
	if(message.compareTo("time_over") == 0)
	    m_timeOver = true;

    }
    //----------------------------------------------------------------------------
    //converts a memory into a complex input
    private RoboCupInput Convert2Complex(Memory m) {
		//
    	boolean want_flags = true;
    	//get visualinfo
		VisualInfo info = m.getVisualInfo();
		//get objectInfo vector
		
		//similarityMetrics
		//atomic
		SimilarityMetricStrategy Atomic_strat = new EuclideanDistance();
		//complex
		SimilarityMetricStrategy ballGoal_strat = new Mean();
		SimilarityMetricStrategy flag_strat = new GreedyMunkrezMatching();

		//weights
		SimilarityWeights sim_weights = new SimilarityWeights(1.0);
		sim_weights.setFeatureWeight("ball", 10);
		sim_weights.setFeatureWeight("goal r", 10);
		sim_weights.setFeatureWeight("goal l", 10); 

		SimilarityMetricStrategy RoboCup_strat = new WeightedMean(sim_weights);
				
		if(info!=null){
			Vector<ObjectInfo> m_objects = info.m_objects;
			
			RoboCupInput input = new RoboCupInput("SenseEnvironment", RoboCup_strat);
			
			for(ObjectInfo obj: m_objects){
				//add ball info
				
				if(obj.m_type.equals("ball")){
					ComplexInput ball = new ComplexInput("ball", ballGoal_strat);
					Feature dist = new Feature(obj.m_distance);
					Feature dir = new Feature(obj.m_direction);
					AtomicInput b_dist = new AtomicInput("ball_dist",dist, Atomic_strat);
					AtomicInput b_dir = new AtomicInput("ball_dir",dir, Atomic_strat);
					ball.add(b_dist);
					ball.add(b_dir);
					input.add(ball);
				}
				
				//add goal r info
				
				if(obj.m_type.equals("goal r") && m_side == 'l'){
					ComplexInput goal_r = new ComplexInput("goal r", ballGoal_strat);
					Feature dist = new Feature(obj.m_distance);
					Feature dir = new Feature(obj.m_direction);
					AtomicInput g_dist = new AtomicInput("goal_dist",dist, Atomic_strat);
					AtomicInput g_dir = new AtomicInput("goal_dir",dir, Atomic_strat);
					goal_r.add(g_dist);
					goal_r.add(g_dir);
					input.add(goal_r);
				}
				
				
				//add goal l info
				
				if(obj.m_type.equals("goal l") && m_side == 'r'){
					ComplexInput goal_l = new ComplexInput("goal l", ballGoal_strat);
					Feature dist = new Feature(obj.m_distance);
					Feature dir = new Feature(obj.m_direction);
					AtomicInput g_dist = new AtomicInput("goal_dist",dist, Atomic_strat);
					AtomicInput g_dir = new AtomicInput("goal_dir",dir, Atomic_strat);
					goal_l.add(g_dist);
					goal_l.add(g_dir);
					input.add(goal_l);
				}
				
				
				if (want_flags){
					ComplexInput allflags = new ComplexInput("flags", flag_strat); 
					String [] flags = {"flag c b", "flag l b","flag r b", "flag c t","flag l t", "flag r t", "flag c", "flag p l t", "flag p l c", "flag p l b", "flag p r t", "flag p r c", "flag p r b" };
					String [] flag_names = {"fcb", "flb","frb", "fct","flt", "frt", "fc","fplt", "fplc", "fplb", "fprt", "fprc", "fprb"}; 
					
					for(int i =0;i<flags.length;i++){
						if(obj.m_type.equals(flags[i]) && ((FlagInfo) obj).m_num==0){
							Feature dist = new Feature(obj.m_distance);
							Feature dir = new Feature(obj.m_direction);
							AtomicInput fdist = new AtomicInput(flag_names[i]+"_dist",dist, Atomic_strat);
							AtomicInput fdir = new AtomicInput(flag_names[i]+"_dir",dir, Atomic_strat);
							ComplexInput f = new ComplexInput(flag_names[i], ballGoal_strat);
							f.add(fdist);
							f.add(fdir);
							allflags.add(f);
						}
					}
					
					String [] flags_top_bot = {"flag t l", "flag t r","flag b l", "flag b r"};
					String [] flag_names_top_bot = {"ftl","ftr","fbl","fbr"};
					int [] flag_dist_top_bot = {50, 40,30,20,10};
					
					for(int i =0;i<flags_top_bot.length;i++){
						for(int j =0; j<flag_dist_top_bot.length;j++){
							if(obj.m_type.equals(flags_top_bot[i]) && ((FlagInfo) obj).m_num==flag_dist_top_bot[j]){
								Feature dist = new Feature(obj.m_distance);
								Feature dir = new Feature(obj.m_direction);
								AtomicInput fdist = new AtomicInput(flag_names_top_bot[i]+String.valueOf(flag_dist_top_bot[j])+"_dist",dist, Atomic_strat);
								AtomicInput fdir = new AtomicInput(flag_names_top_bot[i]+String.valueOf(flag_dist_top_bot[j])+"_dir",dir, Atomic_strat);
								ComplexInput f = new ComplexInput(flag_names_top_bot[i]+String.valueOf(flag_dist_top_bot[j]), ballGoal_strat);
								f.add(fdist);
								f.add(fdir);
								allflags.add(f);
							}
						}
					}
					
					String [] flags_r_l = {"flag r t", "flag r b","flag l t", "flag l b"};
					String [] flag_names_r_l = {"frt","frb","flt","flb"};
					int [] flag_dist_r_l = {30,20,10};
					
					for(int i =0;i<flags_r_l.length;i++){
						for(int j =0; j<flag_dist_r_l.length;j++){
							if(obj.m_type.equals(flags_r_l[i]) && ((FlagInfo) obj).m_num==flag_dist_r_l[j]){
								Feature dist = new Feature(obj.m_distance);
								Feature dir = new Feature(obj.m_direction);
								AtomicInput fdist = new AtomicInput(flag_names_r_l[i]+String.valueOf(flag_dist_r_l[j])+"_dist",dist, Atomic_strat);
								AtomicInput fdir = new AtomicInput(flag_names_r_l[i]+String.valueOf(flag_dist_r_l[j])+"_dir",dir, Atomic_strat);
								ComplexInput f = new ComplexInput(flag_names_r_l[i]+String.valueOf(flag_dist_r_l[j]), ballGoal_strat);
								f.add(fdist);
								f.add(fdir);
								allflags.add(f);
							}
						}
					}
					input.add(allflags);
				}
				
			}
			return input;
		}else{
			System.out.println("Error :There is no visual information");
			return null;
		}
		
	}

    //===========================================================================
    
    // Private members
    private SendCommand	                m_krislet;			// robot which is controled by this brain
    private Memory			m_memory;				// place where all information is stored
    private char			m_side;
    volatile private boolean		m_timeOver;
    private String                      m_playMode;
    private RoboCupAgent agent; //robocup agent
    
}
