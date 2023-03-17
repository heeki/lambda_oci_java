package cloud.heeki.oci;

import cloud.heeki.oci.lib.Customer;
import cloud.heeki.oci.lib.DynamoAdapter;
import cloud.heeki.oci.lib.PropertiesLoader;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;

public class OciHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private Properties props = PropertiesLoader.loadProperties("application.properties");
    private Map<String, String> envvars = System.getenv();
    private Gson g = new Gson();
    private DynamoAdapter da;

    public OciHandler() {
        // initialization: client
        // System.out.println(g.toJson(this.envvars));
        // System.out.println(g.toJson(this.props));
        // this.da = new DynamoAdapter(props.getProperty("table.name"));
        this.da = new DynamoAdapter(envvars.get("TABLE_NAME"));
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        // logger.log(g.toJson(context));
        // logger.log(g.toJson(event));
        String method = event.getHttpMethod();
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

    private String getCustomers(APIGatewayProxyRequestEvent event, Context context) {
        ArrayList<Customer> customers = new ArrayList<Customer>();
        for (ScanResponse page : da.scan()) {
            for (Map<String, AttributeValue> item : page.items()) {
                Customer c = new Customer(item);
                customers.add(c);
            }
        }
        return customers.toString();
    }

    private String createCustomer(APIGatewayProxyRequestEvent event, Context context) {
        String body = getDecodedBody(event);
        Customer c = new Customer(body);
        this.da.put(c);
        return c.toString();
    }

    private void deleteCustomer(APIGatewayProxyRequestEvent event, Context context) {
        String id = event.getPathParameters().get("proxy");
        this.da.delete(id);
        // customers.removeIf(c -> c.uuid.toString().equals(id));
    }

    private String getDecodedBody(APIGatewayProxyRequestEvent event) {
        String body = "";
        if (event.getIsBase64Encoded()) body = new String(Base64.getDecoder().decode(event.getBody()));
        else body = event.getBody();
        return body;
    }

    private APIGatewayProxyResponseEvent buildResponse(int code, String body) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
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