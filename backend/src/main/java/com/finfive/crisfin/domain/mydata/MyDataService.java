package com.finfive.crisfin.domain.mydata;

import com.finfive.crisfin.domain.mydata.dto.MyDataFilterRequest;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyDataService {

    private final MockMyDataRepository mockMyDataRepository;

    /**
     * Returns the full mock financial data for the given persona string.
     *
     * @param personaStr raw persona name (e.g. "OFFICE_WORKER")
     * @return the financialData map stored for that persona
     * @throws CrisfinException PERSONA_NOT_FOUND if the string is not a valid
     *                          PersonaType or no profile row exists in the DB
     */
    public Map<String, Object> getMockData(String personaStr) {
        PersonaType personaType = parsePersonaType(personaStr);

        MockMyDataProfile profile = mockMyDataRepository.findByPersona(personaType)
                .orElseThrow(() -> new CrisfinException(ErrorCode.PERSONA_NOT_FOUND));

        return profile.getFinancialData();
    }

    /**
     * Returns only the requested fields from the mock financial data.
     *
     * @param req filter request carrying persona and the list of keys to retain
     * @return a map containing only the selected fields (absent keys are skipped)
     */
    public Map<String, Object> filterData(MyDataFilterRequest req) {
        Map<String, Object> fullData = getMockData(req.getPersona());

        Map<String, Object> filtered = new LinkedHashMap<>();
        for (String field : req.getSelectedFields()) {
            if (fullData.containsKey(field)) {
                filtered.put(field, fullData.get(field));
            }
        }
        return filtered;
    }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    private PersonaType parsePersonaType(String personaStr) {
        try {
            return PersonaType.valueOf(personaStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CrisfinException(ErrorCode.PERSONA_NOT_FOUND);
        }
    }
}
