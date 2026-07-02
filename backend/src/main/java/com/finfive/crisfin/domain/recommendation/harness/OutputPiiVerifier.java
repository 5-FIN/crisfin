package com.finfive.crisfin.domain.recommendation.harness;

import com.finfive.crisfin.global.filter.PiiMaskingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects personal/financial data re-exposed in the LLM's <em>output</em>.
 *
 * <p>Input is masked before the model sees it, but the model can still echo back a card,
 * resident-registration, or account number (e.g. copied from the situation text or
 * hallucinated). The security spec requires a "PII 재노출 검사" on the output side, so this
 * verifier re-applies {@link PiiMaskingService} masking to every free-text field: if masking
 * changes the value, PII was present — a HARD violation, redacted on sanitize.</p>
 */
@Component
@RequiredArgsConstructor
public class OutputPiiVerifier implements OutputVerifier {

    private final PiiMaskingService piiMaskingService;

    @Override
    public List<HarnessFlag> inspect(Map<String, Object> resultMap, HarnessContext ctx, boolean sanitize) {
        List<HarnessFlag> flags = new ArrayList<>();

        TextTraversal.forEachFreeText(resultMap, (path, key, value, replacer) -> {
            String masked = piiMaskingService.maskString(value);
            if (masked != null && !masked.equals(value)) {
                flags.add(new HarnessFlag(path, "OUTPUT_PII", Severity.HARD,
                        "응답에 계좌/카드/주민번호 등 개인정보로 보이는 값이 감지되어 마스킹했습니다.",
                        sanitize ? "STRIPPED" : "FLAGGED"));
                if (sanitize) {
                    replacer.accept(masked);
                }
            }
        });

        return flags;
    }
}
