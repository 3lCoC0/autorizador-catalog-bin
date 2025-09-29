package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception;

import org.springframework.http.HttpStatus;
import java.util.HashSet;
import java.util.Set;

/**
 * Catálogo central de errores de la API con códigos NUMÉRICOS.
 *
 * Convención de numeración:
 *   - BIN      : 01..??  (empezamos aquí)
 *   - SUBTYPE  : continuar desde el siguiente número disponible
 *   - PLAN     : continuar desde el siguiente número disponible
 *   - ...
 *   - INTERNAL : 99 (fallback de servidor)
 *
 * NOTA: el "código" es el que verá el consumidor en response.responseCode.
 */
public enum AppError {

    // =========================
    // BIN (01..)
    // =========================
    BIN_INVALID_DATA ("01", HttpStatus.BAD_REQUEST,           "Datos inválidos para BIN"),
    BIN_ALREADY_EXISTS("02", HttpStatus.CONFLICT,             "El BIN ya existe"),
    BIN_NOT_FOUND     ("03", HttpStatus.NOT_FOUND,            "BIN no encontrado"),


    BIN_EXT_DIGITS_INVALID            ("04", HttpStatus.BAD_REQUEST, "binExtDigits debe ser 1, 2 o 3 cuando usesBinExt='Y'"),
    BIN_EXT_TOTAL_LENGTH_OVERFLOW     ("05", HttpStatus.BAD_REQUEST, "bin + extensión no puede exceder 9 dígitos"),
    BIN_EXT_DIGITS_MUST_BE_NULL_ON_N  ("06", HttpStatus.BAD_REQUEST, "binExtDigits debe ser null cuando usesBinExt='N'"),


    SUBTYPE_INVALID_DATA ("07", HttpStatus.BAD_REQUEST, "Datos inválidos para SUBTYPE"),
    SUBTYPE_ALREADY_EXISTS("08", HttpStatus.CONFLICT,   "El SUBTYPE ya existe"),
    SUBTYPE_NOT_FOUND     ("09", HttpStatus.NOT_FOUND,  "SUBTYPE no encontrado"),

    AGENCY_INVALID_DATA   ("10", HttpStatus.BAD_REQUEST, "Datos inválidos para AGENCY"),
    AGENCY_ALREADY_EXISTS ("11", HttpStatus.CONFLICT,    "La AGENCY ya existe"),
    AGENCY_NOT_FOUND      ("12", HttpStatus.NOT_FOUND,   "AGENCY no encontrada"),
    AGENCY_CONFLICT_RULE  ("13", HttpStatus.CONFLICT,    "Regla de negocio de AGENCY violada"),
    SUBTYPE_ACTIVATE_REQUIRES_AGENCY("14", HttpStatus.CONFLICT, "Para activar SUBTYPE se requiere al menos una AGENCY activa"),


    RULES_VALIDATION_INVALID_DATA ("15", HttpStatus.BAD_REQUEST, "Datos inválidos para VALIDATION"),
    RULES_VALIDATION_ALREADY_EXISTS("16", HttpStatus.CONFLICT,   "La VALIDATION ya existe"),
    RULES_VALIDATION_NOT_FOUND     ("17", HttpStatus.NOT_FOUND,  "VALIDATION no encontrada"),

    RULES_MAP_INVALID_DATA         ("18", HttpStatus.BAD_REQUEST, "Datos inválidos para RULE mapping"),
    RULES_MAP_ALREADY_EXISTS       ("19", HttpStatus.CONFLICT,    "El RULE mapping ya existe"),
    RULES_MAP_NOT_FOUND            ("20", HttpStatus.NOT_FOUND,   "RULE mapping no encontrado"),
    RULES_MAP_CONFLICT             ("21", HttpStatus.CONFLICT,    "Regla de negocio de RULE mapping violada"),


    PLAN_INVALID_DATA            ("22", HttpStatus.BAD_REQUEST, "Datos inválidos para PLAN"),
    PLAN_ALREADY_EXISTS          ("23", HttpStatus.CONFLICT,    "Ya existe un plan con ese code"),
    PLAN_NOT_FOUND               ("24", HttpStatus.NOT_FOUND,   "Plan no encontrado"),

    PLAN_ITEM_INVALID_DATA       ("25", HttpStatus.BAD_REQUEST, "Datos inválidos para ítem de plan"),
    PLAN_ITEM_NOT_FOUND          ("26", HttpStatus.NOT_FOUND,   "Ítem de plan no encontrado"),

    PLAN_ASSIGNMENT_INVALID_DATA ("27", HttpStatus.BAD_REQUEST, "Datos inválidos para asignación de plan a SUBTYPE"),
    PLAN_ASSIGNMENT_CONFLICT     ("28", HttpStatus.CONFLICT,    "No se puede asignar plan al SUBTYPE"),


    INTERNAL          ("99", HttpStatus.INTERNAL_SERVER_ERROR,"Se produjo un error inesperado");

    public final String code;           // p.ej. "02"
    public final HttpStatus http;       // p.ej. HttpStatus.CONFLICT
    public final String defaultMessage; // p.ej. "El BIN ya existe"

    AppError(String code, HttpStatus http, String defaultMessage) {
        this.code = code;
        this.http = http;
        this.defaultMessage = defaultMessage;
    }

    /**
     * Verificación en arranque: todos los códigos deben ser únicos.
     */
    static {
        Set<String> seen = new HashSet<>();
        for (var e : AppError.values()) {
            if (!seen.add(e.code)) {
                throw new IllegalStateException("Código de error duplicado: " + e.code + " (" + e.name() + ")");
            }
        }
    }
}
