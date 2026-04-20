package com.zlt.aps.common.core.utils;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExcelConfigHolder {
    public static Short HEAD_GROUND_COLOR;
    public static String FONT_NAME;
    public static Short FONT_HEIGHT_IN_POINTS;
    
    @Value("${export.system.headGroundColor:23}")
    private Short headGroundColor;
    
    @Value("${export.system.fontName:Arial}")
    private String fontName;
    
    @Value("${export.system.fontHeightInPoints:10}")
    private Short fontHeightInPoints;
    
    @PostConstruct
    public void init() {
        HEAD_GROUND_COLOR = this.headGroundColor;
        FONT_NAME = this.fontName;
        FONT_HEIGHT_IN_POINTS = this.fontHeightInPoints;
    }}
