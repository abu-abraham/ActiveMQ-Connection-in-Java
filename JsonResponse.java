
import com.google.gson.Gson;

public class JsonResponse implements JsonType{
	private String id;
	private String value;
	private boolean q;
	private long t;
	
	
	public String toJson() {
		Gson gson = new Gson();
	    return gson.toJson(this);
	}
	 
	public JsonResponse(String id, String value, boolean q, long t) {
		this.id = id;
		this.value = value;
		this.q = q;
		this.t= t;		 
	}
}
