

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyFetcher {
	public static String getValue(String property) {
		try {
			InputStream input = new FileInputStream("connection.properties");
			Properties prop = new Properties();
			prop.load(input);
			return prop.getProperty(property);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
		
	}
}
