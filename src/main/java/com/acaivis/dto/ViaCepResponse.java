package com.acaivis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ViaCepResponse(
        String cep,
        String logradouro,
        String bairro,
        String localidade,
        String uf,
        String ibge,
        @JsonProperty("erro") Boolean erro
) {}
