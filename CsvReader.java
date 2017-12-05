
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class CsvReader {
	public static ArrayList<String> getSubscriptions(String csvFile) throws IOException{
		if (csvFile == null)
			csvFile = PropertyFetcher.getValue("csvFileName");;
		String commonExtenstion = "Simulation Examples.Functions.";
        String line = "";
        
        ArrayList<String> subscriptionList = new ArrayList<String>();
        
        BufferedReader br = new BufferedReader(new FileReader(csvFile));
        
        while ((line = br.readLine()) != null) {
            	String[] parts = line.split("\\,");
            	subscriptionList.add(commonExtenstion+parts[0].replaceAll("^\\\"|\\\"$", ""));               

            }
        
        return subscriptionList;
	}

}
