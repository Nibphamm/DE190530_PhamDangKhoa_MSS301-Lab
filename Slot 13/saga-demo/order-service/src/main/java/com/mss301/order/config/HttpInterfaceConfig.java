package com.mss301.order.config;

import com.mss301.order.client.api.*;
import org.springframework.context.annotation.*;
import org.springframework.web.service.registry.ImportHttpServices;

@Configuration(proxyBeanMethods = false)
@Profile("http-interface")
@ImportHttpServices(group = "inventory", types = InventoryApi.class)
@ImportHttpServices(group = "payment", types = PaymentApi.class)
public class HttpInterfaceConfig { }
