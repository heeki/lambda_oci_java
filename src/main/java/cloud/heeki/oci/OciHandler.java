package cloud.heeki.oci;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import cloud.heeki.oci.lib.Customer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;

public class OciHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private ArrayList<Customer> customers = new ArrayList<Customer>();
    // private Gson g = new GsonBuilder().setPrettyPrinting().create();
    private Gson g = new Gson();

    public OciHandler() {
        Customer c1 = new Customer("John", "Doe", "1970-01-01", "john.doe@heeki.cloud", "+15551234567", true);
        Customer c2 = new Customer("Jane", "Doe", "1970-01-01", "jane.doe@heeki.cloud", "+15551234567", true);
        customers.add(c1);
        customers.add(c2);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log(g.toJson(context));
        logger.log(g.toJson(event));
        String method = event.getRequestContext().getHttp().getMethod();
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

    private String getCustomers(APIGatewayV2HTTPEvent event, Context context) {
        return this.customers.toString();
    }

    private String createCustomer(APIGatewayV2HTTPEvent event, Context context) {
        String body = getDecodedBody(event);
        Customer c = new Customer(body);
        customers.add(c);
        return c.uuid.toString();
    }

    private void deleteCustomer(APIGatewayV2HTTPEvent event, Context context) {
        String id = event.getPathParameters().get("proxy");
        customers.removeIf(c -> c.uuid.toString().equals(id));
    }

    private String getDecodedBody(APIGatewayV2HTTPEvent event) {
        String body = "";
        if (event.getIsBase64Encoded()) body = new String(Base64.getDecoder().decode(event.getBody()));
        else body = event.getBody();
        return body;
    }

    private APIGatewayV2HTTPResponse buildResponse(int code, String body) {
        APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
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