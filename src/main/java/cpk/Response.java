package cpk;

public class Response {

    public String content;
    
    public int statusCode;
    
    public Response(String result, int status) {
        this.content = result;
        this.statusCode = status;
    }
    
}
