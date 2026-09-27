package com.fudn.orderservice.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;

public final class InventoryStubs {

    private InventoryStubs() {
    }

    public static void stubInventoryCall(
            WireMockServer server, String skuCode, Integer quantity) {
        stubInventoryCall(server, skuCode, quantity, true);
    }

    public static void stubInventoryCall(
            WireMockServer server, String skuCode, Integer quantity, boolean inStock) {
        server.stubFor(get(urlPathEqualTo("/api/inventory"))
                .withQueryParam("skuCode", equalTo(skuCode))
                .withQueryParam("quantity", equalTo(quantity.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(Boolean.toString(inStock))));
    }
}
