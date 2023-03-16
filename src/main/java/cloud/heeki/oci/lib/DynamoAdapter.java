package cloud.heeki.oci.lib;

import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;

public class DynamoAdapter {
    private AwsCredentialsProvider credentials;
    private Region region;
    private DynamoDbClient client;
    private String table;
    private Gson g;
    
    public DynamoAdapter(String table) {
        this.g = new Gson();
        this.table = table;
        // For SnapStart, can't use ProfileCredentialsProvider, have to use DefaultCredentialsProvider or ContainerCredentialsProvider
        // Remember the DefaultCredentialsProvider will search the chain while ContainerCredentialsProvider will be used directly
        this.credentials = (System.getenv("AWS_ACCESS_KEY_ID") != null && System.getenv("AWS_SECRET_ACCESS_KEY") != null) ? EnvironmentVariableCredentialsProvider.create() : DefaultCredentialsProvider.create();
        this.region = System.getenv("AWS_REGION") != null ? Region.of(System.getenv("AWS_REGION")) : Region.US_EAST_1;
        this.client = DynamoDbClient.builder()
            .credentialsProvider(credentials)
            .region(region)
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();
    }

    public ScanIterable scan() {
        ScanRequest request = ScanRequest.builder()
            .tableName(this.table)
            .build();
        ScanIterable response = client.scanPaginator(request);
        return response;
    }

    public String put(Customer c) {
        PutItemRequest request = PutItemRequest.builder()
            .tableName(this.table)
            .item(c.toDynamoMap())
            .build();
        Map<String, String> payload = new HashMap<>();
        payload.put("table", this.table);
        try {
            PutItemResponse response = client.putItem(request);
            payload.put("request_id", response.responseMetadata().requestId());
            System.out.println(this.g.toJson(payload));
        } catch (ResourceNotFoundException e) {
            payload.put("error", "table not found");
            System.err.println(this.g.toJson(payload));
            System.exit(1);
        } catch (DynamoDbException e) {
            payload.put("error", e.getMessage());
            System.err.println(this.g.toJson(payload));
            System.exit(1);
        }
        return this.g.toJson(payload);
    }

    public String delete(String uuid) {
        Map<String, AttributeValue> keymap = new HashMap<>();
        keymap.put("uuid", AttributeValue.builder().s(uuid).build());
        DeleteItemRequest request = DeleteItemRequest.builder()
            .tableName(this.table)
            .key(keymap)
            .build();
        Map<String, String> payload = new HashMap<>();
        payload.put("table", this.table);
        payload.put("uuid", uuid);
        try {
            client.deleteItem(request);
            payload.put("status", "deleted");
        } catch (ResourceNotFoundException e) {
            payload.put("error", "table not found");
            System.err.println(this.g.toJson(payload));
            System.exit(1);
        } catch (DynamoDbException e) {
            payload.put("error", e.getMessage());
            System.err.println(this.g.toJson(payload));
            System.exit(1);
        }
        return this.g.toJson(payload);
    }
}