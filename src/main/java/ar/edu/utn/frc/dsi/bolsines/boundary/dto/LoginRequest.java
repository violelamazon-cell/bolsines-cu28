package ar.edu.utn.frc.dsi.bolsines.boundary.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record LoginRequest(
        @JsonAlias({"nombre", "username"})
        String usuario,
        String password
) {
}

