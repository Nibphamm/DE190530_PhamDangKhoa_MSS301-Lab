package com.fudn.orderservice.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public final class InventoryStubs {

    private InventoryStubs() {
    }

    public static void stubInventoryCall(String skuCode, Integer quantity) {
        stubInventoryCall(skuCode, quantity, true);
    }

    public static void stubInventoryCall(String skuCode, Integer quantity, boolean inStock) {
        stubFor(get(urlPathEqualTo("/api/inventory"))
                .withQueryParam("skuCode", equalTo(skuCode))
                .withQueryParam("quantity", equalTo(quantity.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(Boolean.toString(inStock))));
    }
}
