package com.deadlock.hellocs.gateway.config;

import io.micrometer.common.KeyValue;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;

@Component
public class GatewayObservationConvention extends DefaultServerRequestObservationConvention {

    @Override
    protected KeyValue uri(ServerRequestObservationContext context) {
        Object routeAttr = context.getAttributes().get(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (routeAttr instanceof Route route) {
            return KeyValue.of("uri", "/" + route.getId());
        }
        return super.uri(context);
    }
}
