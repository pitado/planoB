package br.com.planob.domain;

import java.time.Instant;

public record MudancaStatus(StatusOcorrencia anterior, StatusOcorrencia atual, Instant alteradaEm) { }
