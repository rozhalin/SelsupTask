package com.example.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        CrptApi crptApi = CrptApi.getInstance(1, TimeUnit.SECONDS, 20);
        CrptApi.Product product = new CrptApi.Product();
        List<CrptApi.Product> products = new ArrayList<>();
        products.add(product);
        CrptApi.Document document = new CrptApi.Document();

        try {
            crptApi.createDocument(document);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
