package com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.text.Normalizer;
import java.util.Objects;
import java.util.regex.Pattern;

public class SafeTextValidator implements ConstraintValidator<SafeText, String> {

    private boolean allowNumbers;
    private boolean allowUnderscore;
    private boolean allowSpaces;
    private Pattern pattern;

    private String messageTemplate;

    @Override
    public void initialize(SafeText ann) {
        this.allowNumbers = ann.allowNumbers();
        this.allowUnderscore = ann.allowUnderscore();
        this.allowSpaces = ann.allowSpaces();
        String forbiddenWord = ann.forbiddenWord();
        this.messageTemplate = ann.message();

        // Construye clase de caracteres permitidos
        StringBuilder cls = new StringBuilder();
        cls.append("\\p{L}"); // letras Unicode (incluye tildes/ñ)
        if (allowNumbers) cls.append("\\p{N}");
        if (allowUnderscore) cls.append("_");
        if (allowSpaces) cls.append("\\s");

        // Negative lookahead que prohíbe la PALABRA forbiddenWord como palabra independiente (case-insensitive)
        // Usamos límites por letras Unicode: (?<!\p{L}) y (?!\p{L})
        // y escapamos la palabra para literal exacto
        String quotedForbidden = Pattern.quote(Objects.toString(forbiddenWord, "null"));

        String regex = "^(?!.*(?i)(?<!\\p{L})" + quotedForbidden + "(?!\\p{L}))"
                + "[" + cls + "]+$";

        this.pattern = Pattern.compile(regex);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        // Bean Validation: si es null, otra constraint (p.ej. @NotNull) debe manejarlo.
        if (value == null) return true;

        // Normaliza Unicode (NFC) y trim
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).trim();

        boolean ok = pattern.matcher(normalized).matches();

        if (!ok) {
            // Construimos mensaje dinámico según flags
            String digits = allowNumbers ? ", números" : "";
            String underscore = allowUnderscore ? ", '_'" : "";
            String spaces = allowSpaces ? ", espacios" : "";

            String msg = messageTemplate
                    .replace("{digits}", digits)
                    .replace("{underscore}", underscore)
                    .replace("{spaces}", spaces);

            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
        }

        return ok;
    }
}