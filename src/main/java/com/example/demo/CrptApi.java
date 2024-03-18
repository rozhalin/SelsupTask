package com.example.demo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.concurrent.TimedSemaphore;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private static CrptApi INSTANCE;
    private TimedSemaphore semaphore;
    private Requestable httpClient;
    private ApiOptions apiOptions;
    private ObjectMapper mapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    private static final HashMap<String, String> REQUEST_HEADERS = new HashMap<>() {
        {
            put(Constants.Http.CONTENT_TYPE_HEADER_NAME, Constants.Http.JSON_MIME_TYPE_UTF8);
            put(Constants.Http.ACCEPT_HEADER_NAME, Constants.Http.JSON_MIME_TYPE);
        }
    };

    private static final class Constants {
        public static final class Http {
            public static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";
            public static final String ACCEPT_HEADER_NAME = "Accept";
            public static final String JSON_MIME_TYPE_UTF8 = "application/json;charset=UTF-8";
            public static final String JSON_MIME_TYPE = "application/json";
        }
    }

    private CrptApi(
            long timePeriod,
            TimeUnit timeUnit,
            int requestLimit,
            Requestable httpClient,
            ApiOptions apiOptions) {
        this.semaphore = new TimedSemaphore(timePeriod, timeUnit, requestLimit);
        this.httpClient = httpClient;
        this.apiOptions = apiOptions;
    }

    public synchronized static CrptApi getInstance(
            long timePeriod,
            TimeUnit timeUnit,
            int requestLimit,
            Requestable httpClient,
            ApiOptions apiOptions) {
        if (INSTANCE == null) {
            INSTANCE = new CrptApi(timePeriod, timeUnit, requestLimit, httpClient, apiOptions);
        }
        return INSTANCE;
    }

    public HttpRequestResult authorize() throws InterruptedException, URISyntaxException, IOException {
        semaphore.acquire();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(HttpUtils.getUri(apiOptions.baseUrl, apiOptions.authorizeUrn))
                .GET();
        HttpUtils.withHeaders(requestBuilder, REQUEST_HEADERS);

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.request(request);

        return new HttpRequestResult(response.body(), response.statusCode());
    }

    public HttpRequestResult createDocument(Document document, String signature)
            throws URISyntaxException, IOException, InterruptedException {
        semaphore.acquire();

        String body = mapper.writeValueAsString(document);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(HttpUtils.getUri(apiOptions.baseUrl, apiOptions.createDocumentUrn))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        HttpUtils.withHeaders(requestBuilder, REQUEST_HEADERS);
        requestBuilder.header("Authorization", signature);

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.request(request);

        return new HttpRequestResult(response.body(), response.statusCode());
    }

    public class HttpRequestResult {
        String body;
        int statusCode;

        public HttpRequestResult(String body, int statusCode) {
            this.body = body;
            this.statusCode = statusCode;
        }
    }

    public static class HttpUtils {
        public static URI getUri(String url, String urn) throws URISyntaxException {
            return new URI(url + "/" + urn);
        }

        public static HttpRequest.Builder withHeaders(HttpRequest.Builder builder, HashMap<String, String> headers) {
            headers.forEach(builder::header);
            return builder;
        }
    }

    public static class ApiOptions {
        private final String baseUrl;
        private final String createDocumentUrn;
        private final String authorizeUrn;

        public ApiOptions(String baseUrl, String createDocumentUrn, String authorizeUrn) {
            this.baseUrl = baseUrl;
            this.createDocumentUrn = createDocumentUrn;
            this.authorizeUrn = authorizeUrn;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public String getCreateDocumentUrn() {
            return createDocumentUrn;
        }

        public String getAuthorizeUrn() {
            return authorizeUrn;
        }
    }

    public class ApiClient implements Requestable {
        public HttpResponse<String> request(HttpRequest request) throws IOException, InterruptedException {
            return HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    public interface Requestable {
        HttpResponse<String> request(HttpRequest request) throws IOException, InterruptedException;
    }

    public enum DocType {

        LP_INTRODUCE_GOODS(109);

        private final int code;

        DocType(int code) {
            this.code = code;
        }

    }

    public static class Document {
        public Document(String doc_id, List<Product> products) {
            this.doc_id = doc_id;
            this.products = products;
        }

        private HashMap<String, String> description;
        private String doc_id;
        private String doc_status;
        private DocType docType;
        private boolean importRequest;
        private String ownerInn;
        private String participant_inn;
        private String producer_inn;
        private LocalDate production_date;
        private String production_type;
        private List<Product> products;
        private LocalDate reg_date;
        private String reg_number;
    }

    public static class Product {
        public Product(String uit_code) {
            this.uit_code = uit_code;
        }

        private String certificate_document;
        private LocalDate certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private LocalDate production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
}
