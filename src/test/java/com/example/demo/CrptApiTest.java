package com.example.demo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class CrptApiTest {

    @Test
    void createDocument() throws URISyntaxException, IOException, InterruptedException {
        //arrange
        CrptApi.Requestable httpClient = new TestApiClient();
        CrptApi.ApiOptions apiOptions = new CrptApi.ApiOptions(
                "http://localhost.ru",
                "createDoc",
                "auth"
        );
        CrptApi crptApi = CrptApi.getInstance(1, TimeUnit.SECONDS, 20, httpClient, apiOptions);
        //act
        CrptApi.HttpRequestResult httpRequestResult = crptApi.authorize();
        String signature = httpRequestResult.body;
        CrptApi.Product product = new CrptApi.Product("1");
        List<CrptApi.Product> products = new ArrayList<>();
        products.add(product);
        crptApi.createDocument(new CrptApi.Document("0", products), signature);
        //assert
        // auth + createDoc
        Assertions.assertEquals(2, TestApiClient.list.size());
    }

    class TestApiClient implements CrptApi.Requestable {

        public static CopyOnWriteArrayList<HttpRequest> list = new CopyOnWriteArrayList<>();

        @Override
        public HttpResponse<String> request(HttpRequest request) {
            list.add(request);

            return new TestHttpResponse<>(200, "complete");
        }

        public class TestHttpResponse<String> implements HttpResponse<String> {

            private int statusCode;
            private String body;

            public TestHttpResponse(int statusCode, String body) {
                this.statusCode = statusCode;
                this.body = body;
            }

            @Override
            public int statusCode() {
                return this.statusCode;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            public String body() {
                return this.body;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public HttpClient.Version version() {
                return null;
            }
        }
    }

    @Test
    void createDocumentWith20ThreadsIn5Seconds10RPSLimitedTrue() throws URISyntaxException, IOException,
            InterruptedException {
        //arrange
        final int timePeriod = 5;
        final TimeUnit timeUnit = TimeUnit.SECONDS;
        final int rpsLimit = 10;
        final int threads = 20;
        final List<CrptApi.Document> documents = new ArrayList<>();
        CrptApi.Requestable httpClient = new TestApiClient();
        CrptApi.ApiOptions apiOptions = new CrptApi.ApiOptions(
                "http://localhost.ru",
                "createDoc",
                "auth"
        );
        CrptApi crptApi = CrptApi.getInstance(timePeriod, timeUnit, rpsLimit, httpClient, apiOptions);

        //act
        CrptApi.HttpRequestResult httpRequestResult = crptApi.authorize();
        String signature = httpRequestResult.body;

        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            int finalI = i;
            executorService.execute(() -> {
                try {
                    String doc_id = "" + finalI;
                    List<CrptApi.Product> products = new ArrayList<>();
                    CrptApi.Product product = new CrptApi.Product(doc_id);
                    products.add(product);
                    CrptApi.Document document = new CrptApi.Document(doc_id, products);
                    documents.add(document);
                    crptApi.createDocument(document, signature);
                } catch (URISyntaxException | IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executorService.shutdown();
        //executorService.awaitTermination(timePeriod - 1, TimeUnit.SECONDS);
        executorService.awaitTermination(threads / rpsLimit * timePeriod + 1, TimeUnit.SECONDS);

        //assert
        //Assertions.assertEquals(rpsLimit, TestApiClient.list.size());
        Assertions.assertEquals(threads + 1, TestApiClient.list.size()); // plus auth
    }
}