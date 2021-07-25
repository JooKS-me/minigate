package com.jooks.minigate.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jooks.minigate.router.RouterRegistry;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

public class JwtUtils {

    private static volatile JwtUtils jwtUtils;

    private static volatile List<String> jwtList;

    private JwtUtils() {
    }

    public static JwtUtils getInstance() {
        if (jwtUtils != null) {
            return jwtUtils;
        }
        synchronized (JwtUtils.class) {
            if (jwtUtils == null) {
                return new JwtUtils();
            }
            return jwtUtils;
        }
    }

    /**
     * 验证token
     * @param token jwt
     * @param secret 密钥
     * @return claims
     */
    public Claims getClaimByToken(String token, String secret) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 得到需要jwt验证的path
     * @return path list
     */
    public List<String> getJwtPath() {
        BufferedReader br = null;
        if (jwtList != null) {
            return jwtList;
        }
        try {
            br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(RouterRegistry.class.getClassLoader().getResourceAsStream("jwt.json"))));
            String line = br.readLine();
            StringBuilder sb = new StringBuilder();
            while (line != null) {
                sb.append(line).append("\r\n");
                line = br.readLine();
            }
            Gson gson = new Gson();
            jwtList = gson.fromJson(sb.toString(), new TypeToken<List<String>>() {}.getType());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return jwtList;
    }
}