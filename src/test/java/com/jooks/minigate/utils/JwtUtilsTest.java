package com.jooks.minigate.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import java.util.Date;

final public class JwtUtilsTest {

    @Test
    public void generateJwt() {
        Date nowDate= new Date();
        Date expireDate = new Date(nowDate.getTime() + 88888 * 1000);

        String jwt =  Jwts.builder()
                .setHeaderParam("typ", "jwt")
                .setId(String.valueOf(123))
                .setIssuedAt(nowDate)
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS512, "dsaca546cdsv")
                .compact();
        System.out.println(jwt);
    }
}
