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
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CrptApiTest {

    @Test
    void createDocument() throws URISyntaxException, IOException, InterruptedException {
        //arrange
        CrptApi.Requestable httpClient = new TestApiClient();
        CrptApi.ApiOptions apiOptions = new CrptApi.ApiOptions("http://localhost.ru", "createDoc");
        CrptApi crptApi = CrptApi.getInstance(1, TimeUnit.SECONDS, 20, httpClient, apiOptions);
        //act
        crptApi.createDocument(new CrptApi.Document());
        //assert
        Assertions.assertEquals(1, TestApiClient.list.size());
    }

    class TestApiClient implements CrptApi.Requestable {

        public static CopyOnWriteArrayList<HttpRequest> list = new CopyOnWriteArrayList<>();
        @Override
        public HttpResponse<String> request(HttpRequest request) throws IOException, InterruptedException {
            list.add(request);
            //200
            return new HttpResponse<String>() {
                @Override
                public int statusCode() {
                    return 200;
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
                    return null;
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
            };
        }
    }

    //todo
    //количество запросов совпадают с количеством в листе
    //все ожидаемые присутствуют в актуальной коллекции
    //
}