package com.finfive.crisfin.domain.mydata.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class MyDataFilterRequest {

    @NotNull(message = "persona must not be null")
    private String persona;

    @NotEmpty(message = "selectedFields must not be empty")
    private List<String> selectedFields;
}
