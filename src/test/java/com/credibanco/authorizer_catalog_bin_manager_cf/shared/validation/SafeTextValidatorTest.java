package com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SafeTextValidatorTest {

    @Test
    void acceptsNullWithoutViolations() {
        SafeTextValidator validator = new SafeTextValidator();
        validator.initialize(mockAnnotation(true, false, true, "DROP", "msg"));
        ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class);

        assertTrue(validator.isValid(null, ctx));
        verifyNoInteractions(ctx);
    }

    @Test
    void validatesAgainstAllowedCharacterClasses() {
        SafeTextValidator validator = new SafeTextValidator();
        validator.initialize(mockAnnotation(true, false, false, "DROP", "msg"));
        ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class);

        assertTrue(validator.isValid("Árbol123", ctx));
        verifyNoInteractions(ctx);
    }

    @Test
    void rejectsForbiddenWordAndBuildsCustomMessage() {
        SafeTextValidator validator = new SafeTextValidator();
        SafeText annotation = mockAnnotation(false, true, false, "DROP", "Solo letras{underscore}{spaces}{digits}");
        validator.initialize(annotation);

        ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(ctx.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(ctx);

        boolean valid = validator.isValid("DROP_table", ctx);

        assertFalse(valid);
        verify(ctx).disableDefaultConstraintViolation();
        ArgumentCaptor<String> template = ArgumentCaptor.forClass(String.class);
        verify(ctx).buildConstraintViolationWithTemplate(template.capture());
        assertEquals("Solo letras, '_'", template.getValue());
        verify(builder).addConstraintViolation();
    }

    private SafeText mockAnnotation(boolean allowNumbers, boolean allowUnderscore, boolean allowSpaces, String forbidden, String message) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "allowNumbers" -> allowNumbers;
            case "allowUnderscore" -> allowUnderscore;
            case "allowSpaces" -> allowSpaces;
            case "forbiddenWord" -> forbidden;
            case "message" -> message;
            case "groups", "payload" -> method.getDefaultValue();
            case "annotationType" -> SafeText.class;
            default -> throw new IllegalStateException("Unexpected method: " + method.getName());
        };
        return (SafeText) Proxy.newProxyInstance(
                SafeText.class.getClassLoader(),
                new Class[]{SafeText.class},
                handler);
    }
}
