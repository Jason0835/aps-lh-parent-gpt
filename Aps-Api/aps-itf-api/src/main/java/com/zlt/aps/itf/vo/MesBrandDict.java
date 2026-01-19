package com.zlt.aps.itf.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * MES品牌字典
 *
 * @author Chen
 * @since 2026/1/19
 */
@Data
public class MesBrandDict implements Serializable {

    private static final long serialVersionUID = 1L;

    private String objid;

    private String recordTime;

    private String deleteFlag;

    private String brandCode;

    private String brandName;

    private String brandShortName;

    private String otherName;

    private String seqIndex;

    private String remark;
}
