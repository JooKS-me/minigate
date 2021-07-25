package com.jooks.minigate.router;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RouterRegistry {

    private static RouterRegistry routerRegistry;

    private static final String JSON_PATH = "router.json";

    private static Map<String, List<String>> map;

    private RouterRegistry() {
    }

    public static RouterRegistry getInstance() {
        if (routerRegistry == null) {
            return new RouterRegistry();
        }
        return routerRegistry;
    }

    public void put(String key, List<String> value) {
        map.put(key, value);
    }

    public List<String> get(String key) {
        return map.get(key);
    }

    public void setup() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(RouterRegistry.class.getClassLoader().getResourceAsStream(JSON_PATH))));
            String line = br.readLine();
            StringBuilder sb = new StringBuilder();
            while (line != null) {
                sb.append(line).append("\r\n");
                line = br.readLine();
            }
            Gson gson = new Gson();
            map = gson.fromJson(sb.toString(), new TypeToken<Map<String, List<String>>>() {}.getType());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
