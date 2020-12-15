import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;

public class OciHandler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    private ArrayList<Customer> customers = new ArrayList<Customer>();
    private Gson g = new GsonBuilder().setPrettyPrinting().create();

    public OciHandler() {
        Customer c1 = new Customer("John", "Doe", "1970-01-01", "john.doe@heeki.cloud", "+15551234567", true);
        Customer c2 = new Customer("Jane", "Doe", "1970-01-01", "jane.doe@heeki.cloud", "+15551234567", true);
        customers.add(c1);
        customers.add(c2);
    }

    @Override
    public APIGatewayV2ProxyResponseEvent handleRequest(APIGatewayV2ProxyRequestEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log(g.toJson(context) + "\n");
        logger.log(g.toJson(event) + "\n");
        String method = event.getRequestContext().getRouteKey().split(" ", 2)[0];
        String response = "";
        switch (method) {
            case "GET":
                response = getCustomers(event, context);
                break;
            case "POST":
                response = createCustomer(event, context);
                break;
            case "DELETE":
                deleteCustomer(event, context);
                break;
        }
        return buildResponse(200, response);
    }

    private String getCustomers(APIGatewayV2ProxyRequestEvent event, Context context) {
        return this.customers.toString();
    }

    private String createCustomer(APIGatewayV2ProxyRequestEvent event, Context context) {
        String body = getDecodedBody(event);
        Customer c = new Customer(body);
        customers.add(c);
        return c.uuid.toString();
    }

    private void deleteCustomer(APIGatewayV2ProxyRequestEvent event, Context context) {
        String id = event.getPathParameters().get("proxy");
        customers.removeIf(c -> c.uuid.toString().equals(id));
    }

    private String getDecodedBody(APIGatewayV2ProxyRequestEvent event) {
        String body = "";
        if (event.isIsBase64Encoded()) body = new String(Base64.getDecoder().decode(event.getBody()));
        else body = event.getBody();
        return body;
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
}