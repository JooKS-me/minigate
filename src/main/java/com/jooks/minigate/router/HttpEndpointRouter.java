package com.jooks.minigate.router;

import java.util.List;

public interface HttpEndpointRouter {
    
    String routeByRandom(List<String> endpoints);

    String routeByRoundRobin(List<String> urls);
}
