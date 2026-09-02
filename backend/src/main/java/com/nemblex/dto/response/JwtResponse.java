package com.nemblex.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {

    private String token;
    private String email;
    private String type = "Bearer";

    public JwtResponse(String token, String email) {
        this.token = token;
        this.email = email;
    }
}
