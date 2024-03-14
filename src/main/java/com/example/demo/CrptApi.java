package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.concurrent.TimedSemaphore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private static CrptApi INSTANCE;
    private static TimedSemaphore semaphore;

    private CrptApi(long timePeriod, TimeUnit timeUnit, int requestLimit) {
        CrptApi.semaphore = new TimedSemaphore(timePeriod, timeUnit, requestLimit);
    }



    public static CrptApi getInstance(long timePeriod, TimeUnit timeUnit, int requestLimit) {
        if (INSTANCE == null) {
            INSTANCE = new CrptApi(timePeriod, timeUnit, requestLimit);
        }
        return INSTANCE;
    }

    public String createDocument(Document document) throws IOException {
        return Connection.createDocument(document, Connection.authorize());
    }

    private static class Connection {
        static final String ROOT_ADDRESS = "https://ismp.crpt.ru/";

        public static String authorize() {
            return "signature";
        }

        public static String createDocument(Document document, String signature) throws IOException {
            URL url = new URL(ROOT_ADDRESS + "api/v3/lk/documents/create");

            while (!semaphore.tryAcquire()) ;

            /*HttpURLConnection connection = getConnection(url, "POST");
            setAuthorization(connection, signature);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = document.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }*/

            StringBuilder response = new StringBuilder();
            /*try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }*/

            return response.toString();
        }

        private static HttpURLConnection getConnection(URL url, String method) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            return connection;
        }

        private static void setAuthorization(HttpURLConnection connection, String signature) {
            connection.setRequestProperty("Authorization", "Bearer " + signature);
        }

    }

    @Getter
    public enum DocType {

        LP_INTRODUCE_GOODS(109);

        private final int code;

        DocType(int code) {
            this.code = code;
        }

    }

    public static class Document {
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
