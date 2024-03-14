package com.example.demo;

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
    private static final HashMap<String, String> createDocumentHeaders = new HashMap<>() {
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

    public HttpRequestResult createDocument(Document document)
            throws URISyntaxException, IOException, InterruptedException {
        //todo toString() -> toJson()
        byte[] data = document.toString().getBytes();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(HttpUtils.getUri(apiOptions.baseUrl, apiOptions.createDocumentUrn))
                .POST(HttpRequest.BodyPublishers.ofByteArray(data));
        HttpUtils.withHeaders(requestBuilder, createDocumentHeaders);
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

        public ApiOptions(String baseUrl, String createDocumentUrn) {
            this.baseUrl = baseUrl;
            this.createDocumentUrn = createDocumentUrn;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public String getCreateDocumentUrn() {
            return createDocumentUrn;
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
        private HashMap<String, String> description;

        public void setDoc_id(String doc_id) {
            this.doc_id = doc_id;
        }

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

    public class Product {
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
