package com.panaderia.erp.core.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Convierte un rango de {@link LocalDate} (como llegan de un query param "desde"/"hasta") al
 * rango de {@link Instant} equivalente, usando la zona horaria del servidor. "hasta" es exclusivo
 * (incluye todo el día indicado).
 */
public final class RangoFechas {

    private RangoFechas() {
    }

    public static Instant inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    public static Instant finDelDia(LocalDate fecha) {
        return fecha.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
