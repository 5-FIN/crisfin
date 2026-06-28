package com.finfive.crisfin.global.filter;

import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects common prompt-injection patterns in user-supplied text.
 *
 * <p>All patterns are matched case-insensitively. Callers should invoke
 * {@link #validate(String)} before forwarding any user input to an LLM provider.</p>
 */
@Service
public class PromptInjectionDetector {

    private static final List<Pattern> PATTERNS = List.of(
            pattern("ignore previous instructions"),
            pattern("ignore all previous"),
            pattern("system prompt"),
            pattern("you are now"),
            pattern("forget your"),
            pattern("override your"),
            pattern("\\nHuman:"),
            pattern("\\nAssistant:")
    );

    private static Pattern pattern(String literal) {
        return Pattern.compile(Pattern.quote(literal), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Returns {@code true} if any injection pattern is found in {@code input}.
     *
     * @param input the raw user-supplied string to inspect
     */
    public boolean detect(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        for (Pattern p : PATTERNS) {
            if (p.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates that {@code input} does not contain prompt-injection patterns.
     *
     * @param input the raw user-supplied string to inspect
     * @throws CrisfinException with {@link ErrorCode#PROMPT_INJECTION_DETECTED} on detection
     */
    public void validate(String input) {
        if (detect(input)) {
            throw new CrisfinException(ErrorCode.PROMPT_INJECTION_DETECTED);
        }
    }
}
