package com.ruoyi.system.api.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Locale;
import java.util.TimeZone;

@Getter
@Setter
public class LangVo {

    public LangVo(Locale lang, TimeZone timezone){
        this.lang = lang;
        this.timezone = timezone;
    }

    Locale lang;
    TimeZone timezone;
}
