package com.jooks.minigate.router;

import java.util.List;

public interface HttpEndpointRouter {
    
    String route(List<String> endpoints);
}
