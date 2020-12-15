import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class OciHandler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    private ArrayList<Customer> customers = new ArrayList<Customer>();
    private Gson g = new Gson();

    public OciHandler() {
        Customer c1 = new Customer("John", "Doe", "1970-01-01", "john.doe@heeki.cloud", "+15551234567", true);
        Customer c2 = new Customer("Jane", "Doe", "1970-01-01", "jane.doe@heeki.cloud", "+15551234567", true);
        customers.add(c1);
        customers.add(c2);
    }

    private APIGatewayV2ProxyResponseEvent buildResponse(int code, String body) {
        APIGatewayV2ProxyResponseEvent response = new APIGatewayV2ProxyResponseEvent();
        response.setIsBase64Encoded(false);
        response.setStatusCode(code);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Access-Control-Allow-Origin", "amazonaws.com");
        headers.put("Access-Control-Allow-Credentials", "true");
        response.setHeaders(headers);
        response.setBody(body);
        return response;
    }

    @Override
    public APIGatewayV2ProxyResponseEvent handleRequest(APIGatewayV2ProxyRequestEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        APIGatewayV2ProxyResponseEvent response = buildResponse(200, this.customers.toString());
        return response;
    }
}