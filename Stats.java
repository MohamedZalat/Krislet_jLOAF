import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.lang.String;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.io.BufferedReader;
import java.io.FileReader;


class Stats{
    private static final String MODEL = "model";
    private static final String COND_FMT = "%1$s|%2$s";
    private static final String CHI_SCORE_KEY_FMT = "%1$s: %2$s and %3$s";
    private static final String TRAJ_FILE_NAME_FMT = "%1$s_trajectory.csv";
    private static final String REPORT_DIR = "report";

    private String modelName;
    private HashMap<String, String> envStats;
    private HashMap<String, String> actStats;
    private HashMap<String, String> envCondStats;
    private HashMap<String, String> actCondStats;
    private HashMap<String, String> actCondEnvStats;
    private String prevEnv;
    private String prevAct;
    private ArrayList<HashMap<String,String>> trajectory;

    public Stats(String modelName) {
        this.modelName = modelName;
        this.envStats = new HashMap<String, String>();
        this.actStats = new HashMap<String, String>();
        this.envCondStats = new HashMap<String, String>();
        this.actCondStats = new HashMap<String, String>();
        this.actCondEnvStats = new HashMap<String, String>();
        this.prevEnv = null;
        this.prevAct = null;
        this.trajectory = new ArrayList<HashMap<String, String>>();

        // Add an extra field to identify the model used to each stat file.
        this.envStats.put(Stats.MODEL, this.modelName);
        this.actStats.put(Stats.MODEL, this.modelName);
        this.envCondStats.put(Stats.MODEL, this.modelName);
        this.actCondStats.put(Stats.MODEL, this.modelName);
        this.actCondEnvStats.put(Stats.MODEL, this.modelName);
    }

    public void logEnv(String env){
        //TODO
	this.incrementFrequency(this.envStats, env);

	if (this.prevEnv != null)
	    this.incrementCondFrequency(this.envCondStats,
					this.prevEnv,
					env);

	this.prevEnv = env;
    }

    public void logAct(String act, String supAct){
	this.incrementFrequency(this.actStats, act);

	this.incrementCondFrequency(this.actCondEnvStats,
				    this.prevEnv,
				    act);

	if (this.prevAct != null)
	    this.incrementCondFrequency(this.actCondStats,
					this.prevAct,
					act);

	this.prevAct = act;

	this.appendToTrajectory(this.prevEnv, this.prevAct, supAct);
    }

    public void incrementFrequency(HashMap<String, String> stats, String key) {
        if (stats.containsKey(key)) {
	    int temp = Integer.parseInt(stats.get(key)) + 1;
            stats.put(key, "" + temp);
	}
        else {
            stats.put(key, "" + 1);
	}
    }

    private void incrementCondFrequency(HashMap<String, String> condStats, String prev_key, String key) {
        String condKey = String.format(Stats.COND_FMT, key, prev_key);
        this.incrementFrequency(condStats, condKey);
    }

    private void appendToTrajectory(String env, String act, String supAct) {
        HashMap<String, String> temp = new HashMap<String, String>();
        temp.put("state", env);
        temp.put("action", act);
        temp.put("supervisor_action", supAct);
        trajectory.add(temp);
    }

    private static void writeToFile(ArrayList<HashMap<String, String>> entries,
				    HashMap<String, String> stats,
				    String statsPath) throws Exception {
        // Helper method to write to the file the new entry.
        // TODO
        entries.add(stats);

	// Get all the fields to make sure no field is excluded.
	Set<String> fields = new HashSet<String>();
	fields.addAll(stats.keySet());

	for (HashMap<String, String> entry : entries) {
	    fields.addAll(entry.keySet());
	}

	// Sort the fields to ensure all data is entered in the correct field.
	// This is important especially in cases where a field is missing in one
	// of the entries.
	ArrayList<String> sortedFields = new ArrayList<String>(fields);
	Collections.sort(sortedFields);

	FileWriter csvWriter = new FileWriter(statsPath);
	csvWriter.append(String.join(",", sortedFields));
	csvWriter.append("\n");

	for (HashMap<String, String> entry : entries) {

	    // Make sure to add a value in each field for each entry.
	    for (String field : sortedFields) {

		// If it does not have a value for a field then it means that the count is 0.
		if (entry.containsKey(field))
		    csvWriter.append(entry.get(field));
		else
		    csvWriter.append("" + 0);

		// If it is not the last field then we need to add commas.
		if (sortedFields.indexOf(field) < sortedFields.size() - 1)
		    csvWriter.append(",");
	    }
	    csvWriter.append("\n");
	}

	csvWriter.flush();
	csvWriter.close();

    }

    public void save() {
	System.out.println("Saving stats..");
	this.save(this.envStats, "environment");
	this.save(this.actStats, "action");
	this.save(this.envCondStats, "environment_cond");
	this.save(this.actCondStats, "action_cond");
	this.save(this.actCondEnvStats, "action_cond_environment");
	System.out.println("Saved stats successfully");
	this.saveTrajectory();
    }

    private void saveTrajectory() {
	String trajFileName = String.format(Stats.TRAJ_FILE_NAME_FMT, this.modelName);
	
        Path currentPath = Paths.get(System.getProperty("user.dir"));
        Path reportPath = Paths.get(currentPath.toString(), Stats.REPORT_DIR);

        File reportDir = new File(reportPath.toString());

	try {
	    // Create directories in the model path if they do not exist.
	    if (!reportDir.exists())
		reportDir.mkdirs();

	    Path trajPath = Paths.get(reportPath.toString(), trajFileName);

	    FileWriter csvWriter = new FileWriter(trajPath.toString());
	    csvWriter.append("state");
	    csvWriter.append(",");
	    csvWriter.append("action");
	    csvWriter.append(",");
	    csvWriter.append("supervisor_action");
	    csvWriter.append("\n");

	    for (HashMap<String, String> step : trajectory) {

		csvWriter.append(step.get("state"));
		csvWriter.append(",");
		csvWriter.append(step.get("action"));
		csvWriter.append(",");
		csvWriter.append(step.get("supervisor_action"));
		csvWriter.append("\n");
	    }

	    csvWriter.flush();
	    csvWriter.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    private void save(HashMap<String, String> stats, String type) {
        String statsFileName = String.format("%1$s_stats.csv", type);

        Path currentPath = Paths.get(System.getProperty("user.dir"));
        Path reportPath = Paths.get(currentPath.toString(), Stats.REPORT_DIR);

        File reportDir = new File(reportPath.toString());

        // Create directories in the model path if they do not exist.
        if (!reportDir.exists())
            reportDir.mkdirs();

        Path statsPath = Paths.get(reportPath.toString(), statsFileName);

	// The list of stats of each model in the stats file.
	ArrayList<HashMap<String, String>> entries = new ArrayList<HashMap<String, String>>();

	// Check if the file exists to not overwrite previous entries.
	File statsFile = new File(statsPath.toString());

	if (statsFile.exists()) {
	    // Lock the file before writing.
	    try (FileChannel channel = FileChannel.open(statsPath,
							StandardOpenOption.APPEND)) {
		entries = Stats.read(statsFile);

		// Filter the list to have no record of the current model.
		HashMap<String, String> entryToRemove = null;
		for (HashMap<String, String> entry : entries) {
		    if (entry.get(Stats.MODEL).equals(this.modelName)) 
			entryToRemove = entry;
		}
		entries.remove(entryToRemove);

		Stats.writeToFile(entries, stats, statsPath.toString());
	    } catch (Exception e) {
		System.out.println("File existed");
		e.printStackTrace();
	    }
	}
	else {
	    try {
		statsFile.createNewFile();
	    }
	    catch (Exception e) {
		e.printStackTrace();
	    }
	    // Lock the file before writing.
	    try (FileChannel channel = FileChannel.open(statsPath,
							StandardOpenOption.APPEND)) {
		Stats.writeToFile(entries, stats, statsPath.toString());
	    }  catch (Exception e) {
		System.out.println("File did not exist");
		e.printStackTrace();
	    }
	}
    }

    private static ArrayList<HashMap<String, String>> read(File statsFile) throws Exception {
	ArrayList<HashMap<String, String>> entries = new ArrayList<HashMap<String, String>>();

	if (!statsFile.isFile())
	    return entries;
	
	boolean firstLine = true;
	ArrayList<String> keys = new ArrayList<String>();
	BufferedReader csvReader = new BufferedReader(new FileReader(statsFile));

	String row;
	while ((row = csvReader.readLine()) != null) {
	    String[] data = row.split(",");
	    // do something with the data

	    // Get the field names of each stat in the csv.
	    if (firstLine) {
		firstLine = false;
		for (int i = 0; i < data.length; i++) {
		    keys.add(data[i].trim());
		}
	    }

	    else {
		HashMap<String, String> stats = new HashMap<String, String>();
		for (int i = 0; i < data.length; i++) {
		    stats.put(keys.get(i), data[i].trim());
		}
		entries.add(stats);
	    }
	    
	}
	csvReader.close();
	
	return entries;
    }

}
