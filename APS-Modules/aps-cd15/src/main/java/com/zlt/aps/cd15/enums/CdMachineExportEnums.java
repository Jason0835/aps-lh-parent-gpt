package com.zlt.aps.cd15.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Chen
 * @date 2025/6/3
 */
@Getter
public enum CdMachineExportEnums {

    /**
     * 1#直裁-夜班
     */
    CD_MACHINE_1_NIGHT("lb0-2", "lb15"),

    /**
     * 2#直裁-夜班
     */
    CD_MACHINE_2_NIGHT("lb1-2", "lb16"),

    /**
     * 3#直裁-夜班
     */
    CD_MACHINE_3_NIGHT("lb2-2", "lb17"),

    /**
     * 4#直裁-夜班
     */
    CD_MACHINE_4_NIGHT("lb3-2", "lb18"),

    /**
     * 1#直裁-2#-夜班
     */
    CD_MACHINE_2_1_NIGHT("lbt0-2", "lbt15"),

    /**
     * 2#直裁-2#-夜班
     */
    CD_MACHINE_2_2_NIGHT("lbt1-2", "lbt16"),

    /**
     * 3#直裁-2#-夜班
     */
    CD_MACHINE_2_3_NIGHT("lbt2-2", "lbt17"),

    /**
     * 4#直裁-2#-夜班
     */
    CD_MACHINE_2_4_NIGHT("lbt3-2", "lbt18"),

    /**
     * 1#NC-夜班
     */
    NC_MACHINE_NC_NIGHT("nc-1", "nc13"),

    /**
     * 1#机-夜班
     */
    CD_MACHINE_XD1_1_NIGHT("gd0-2", "gd15"),

    /**
     * 2#机-夜班
     */
    CD_MACHINE_XD1_2_NIGHT("gd1-2", "gd16"),

    /**
     * 3#机-夜班
     */
    CD_MACHINE_XD1_3_NIGHT("gd2-2", "gd17"),

    /**
     * 4#机-夜班
     */
    CD_MACHINE_XD1_4_NIGHT("gd3-2", "gd18"),

    /**
     * 1#机-夜班
     */
    CD_MACHINE_XD2_1_NIGHT("gdt0-2", "gdt15"),

    /**
     * 2#机-夜班
     */
    CD_MACHINE_XD2_2_NIGHT("gdt1-2", "gdt16"),

    /**
     * 3#机-夜班
     */
    CD_MACHINE_XD2_3_NIGHT("gdt2-2", "gdt17"),

    /**
     * 4#机-夜班
     */
    CD_MACHINE_XD2_4_NIGHT("gdt3-2", "gdt18"),

    /**
     * 1#机-夜班
     */
    CD_MACHINE_GSQ_1_NIGHT("gsq0-2", "gsq10"),

    /**
     * 2#机-夜班
     */
    CD_MACHINE_GSQ_3_NIGHT("gsq2-2", "gsq11"),

    /**
     * 4#机-夜班
     */
    CD_MACHINE_GSQ_4_NIGHT("gsq3-2", "gsq12"),

    /**
     * 1#机-夜班
     */
    CD_MACHINE_TQ_1_NIGHT("tq0-2", "tq19"),

    /**
     * 2#机-夜班
     */
    CD_MACHINE_TQ_2_NIGHT("tq1-2", "tq20"),

    /**
     * 3#机-夜班
     */
    CD_MACHINE_TQ_3_NIGHT("tq2-2", "tq21"),

    /**
     * 4#机-夜班
     */
    CD_MACHINE_TQ_4_NIGHT("tq3-2", "tq22"),

    /**
     * 5#机-夜班
     */
    CD_MACHINE_TQ_5_NIGHT("tq4-2", "tq23"),

    /**
     * 7#机-夜班
     */
    CD_MACHINE_TQ_7_NIGHT("tq5-2", "tq24"),

    /**
     * 8#机-夜班
     */
    CD_MACHINE_TQ_8_NIGHT("tq6-2", "tq25"),

    /**
     * 9#机-夜班
     */
    CD_MACHINE_TQ_9_NIGHT("tq7-2", "tq26"),

    /**
     * 12#机-夜班
     */
    CD_MACHINE_TQ_12_NIGHT("tq8-2", "tq27"),

    /**
     * 9#机-夜班
     */
    CD_MACHINE_TQ_13_NIGHT("tq9-2", "tq28"),


    /**
     * 1#直裁-早班
     */
    CD_MACHINE_1_DAY("lb0-3", "lb9"),

    /**
     * 2#直裁-早班
     */
    CD_MACHINE_2_DAY("lb1-3", "lb10"),

    /**
     * 3#直裁-早班
     */
    CD_MACHINE_3_DAY("lb2-3", "lb11"),

    /**
     * 4#直裁-早班
     */
    CD_MACHINE_4_DAY("lb3-3", "lb12"),

    /**
     * 1#直裁-2#-早班
     */
    CD_MACHINE_2_1_DAY("lbt0-3", "lbt9"),

    /**
     * 2#直裁-2#-早班
     */
    CD_MACHINE_2_2_DAY("lbt1-3", "lbt10"),

    /**
     * 3#直裁-2#-早班
     */
    CD_MACHINE_2_3_DAY("lbt2-3", "lbt11"),

    /**
     * 4#直裁-2#-早班
     */
    CD_MACHINE_2_4_DAY("lbt3-3", "lbt12"),

    /**
     * 1#NC-夜班
     */
    NC_MACHINE_NC_DAY("nc-2", "nc16"),

    /**
     * 1#机-早班
     */
    CD_MACHINE_XD1_1_DAY("gd0-3", "gd9"),

    /**
     * 2#机-早班
     */
    CD_MACHINE_XD1_2_DAY("gd1-3", "gd10"),

    /**
     * 3#机-早班
     */
    CD_MACHINE_XD1_3_DAY("gd2-3", "gd11"),

    /**
     * 4#机-早班
     */
    CD_MACHINE_XD1_4_DAY("gd3-3", "gd12"),

    /**
     * 1#机-早班
     */
    CD_MACHINE_XD2_1_DAY("gdt0-3", "gdt9"),

    /**
     * 2#机-早班
     */
    CD_MACHINE_XD2_2_DAY("gdt1-3", "gdt10"),

    /**
     * 3#机-早班
     */
    CD_MACHINE_XD2_3_DAY("gdt2-3", "gdt11"),

    /**
     * 4#机-早班
     */
    CD_MACHINE_XD2_4_DAY("gdt3-3", "gdt12"),

    /**
     * 1#机-早班
     */
    CD_MACHINE_GSQ_1_DAY("gsq0-3", "gsq6"),

    /**
     * 2#机-早班
     */
    CD_MACHINE_GSQ_3_DAY("gsq2-3", "gsq7"),

    /**
     * 4#机-早班
     */
    CD_MACHINE_GSQ_4_DAY("gsq3-3", "gsq8"),

    /**
     * 1#机-早班
     */
    CD_MACHINE_TQ_1_DAY("tq0-3", "tq8"),

    /**
     * 2#机-早班
     */
    CD_MACHINE_TQ_2_DAY("tq1-3", "tq9"),

    /**
     * 3#机-早班
     */
    CD_MACHINE_TQ_3_DAY("tq2-3", "tq10"),

    /**
     * 4#机-早班
     */
    CD_MACHINE_TQ_4_DAY("tq3-3", "tq11"),

    /**
     * 5#机-早班
     */
    CD_MACHINE_TQ_5_DAY("tq4-3", "tq12"),

    /**
     * 7#机-早班
     */
    CD_MACHINE_TQ_7_DAY("tq5-3", "tq13"),

    /**
     * 8#机-早班
     */
    CD_MACHINE_TQ_8_DAY("tq6-3", "tq14"),

    /**
     * 9#机-早班
     */
    CD_MACHINE_TQ_9_DAY("tq7-3", "tq15"),

    /**
     * 12#机-早班
     */
    CD_MACHINE_TQ_12_DAY("tq8-3", "tq16"),

    /**
     * 9#机-早班
     */
    CD_MACHINE_TQ_13_DAY("tq9-3", "tq17"),
    ;

    private final String code;

    private final String fieldName;

    CdMachineExportEnums(String code, String fieldName) {
        this.code = code;
        this.fieldName = fieldName;
    }

    /**
     * 根据机台名称，获取对应的字段名枚举
     *
     * @param code 机台名称
     * @return 结果
     */
    public static CdMachineExportEnums getInstance(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (CdMachineExportEnums fieldNameEnum : CdMachineExportEnums.values()) {
            if (fieldNameEnum.getCode().equals(code)) {
                return fieldNameEnum;
            }
        }
        return null;
    }
}
