package com.finfive.crisfin.domain.mydata.dto;

import com.finfive.crisfin.domain.mydata.PersonaType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class MyDataResponse {

    private final PersonaType persona;
    private final Map<String, Object> data;
}
