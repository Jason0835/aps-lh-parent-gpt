package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：HalfYcImportBak.java
 * 描    述：线下计划导入对象 half_yc_import_bak
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-26
 */
@ApiModel(value = "线下计划导入对象", description = "线下计划导入对象")
@Data
@TableName(value = "HALF_YC_IMPORT_BAK")
public class HalfYcImportBak extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableField(exist = false)
    private Long id;
    @TableField(exist = false)
    private String searchValue;
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private Date createTime;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private Date updateTime;
    @TableField(exist = false)
    private String remark;
    @TableField(exist = false)
    private Integer isDelete = 0;
    @TableField(exist = false)
    private Map<String, Object> params;
    @TableField(exist = false)
    private RowStateEnum rowState;

    /**
     * 序号
     */
    @Excel(name = "ui.data.column.halfYcImportBak.cx1")
    @ApiModelProperty(value = "序号", name = "cx1")
    @TableField(value = "CX1")
    private Integer cx1;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.halfYcImportBak.cx2")
    @ApiModelProperty(value = "规格", name = "cx2")
    @TableField(value = "CX2")
    private String cx2;

    /**
     * 规格代码
     */
    @Excel(name = "ui.data.column.halfYcImportBak.cx3")
    @ApiModelProperty(value = "规格代码", name = "cx3")
    @TableField(value = "CX3")
    private String cx3;

    /**
     * 施工表
     */
    @Excel(name = "ui.data.column.halfYcImportBak.cx4")
    @ApiModelProperty(value = "施工表", name = "cx4")
    @TableField(value = "CX4")
    private String cx4;

    /**
     * 胶种
     */
    @Excel(name = "ui.data.column.halfYcImportBak.cx5")
    @ApiModelProperty(value = "胶种", name = "cx5")
    @TableField(value = "CX5")
    private String cx5;

    /**
     * 成型早班
     */
    @Excel(name = "ui.data.column.halfYcImportBak.cx6")
    @ApiModelProperty(value = "成型早班", name = "cx6")
    @TableField(value = "CX6")
    private Integer cx6;

    /**
     * 成型夜班
     */
    @Excel(name = "ui.data.column.halfYcImportBak.cx7")
    @ApiModelProperty(value = "成型夜班", name = "cx7")
    @TableField(value = "CX7")
    private Integer cx7;

    /**
     * 成型计划合计
     */
    @Excel(name = "ui.data.column.halfYcImportBak.cx8")
    @ApiModelProperty(value = "成型计划合计", name = "cx8")
    @TableField(value = "CX8")
    private Integer cx8;

    /**
     * 成型完成合计
     */
    @Excel(name = "ui.data.column.halfYcImportBak.cx9")
    @ApiModelProperty(value = "成型完成合计", name = "cx9")
    @TableField(value = "CX9")
    private Double cx9;

    /**
     * 胎面口型
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm1")
    @ApiModelProperty(value = "胎面口型", name = "tm1")
    @TableField(value = "TM1")
    private String tm1;

    /**
     * 条米
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm2")
    @ApiModelProperty(value = "条米", name = "tm2")
    @TableField(value = "TM2")
    private Double tm2;

    /**
     * 卷长
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm3")
    @ApiModelProperty(value = "卷长", name = "tm3")
    @TableField(value = "TM3")
    private Double tm3;

    /**
     * 理论库存
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm4")
    @ApiModelProperty(value = "理论库存", name = "tm4")
    @TableField(value = "TM4")
    private Double tm4;

    /**
     * 实际库存
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm5")
    @ApiModelProperty(value = "实际库存", name = "tm5")
    @TableField(value = "TM5")
    private Double tm5;

    /**
     * 计划用量
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm6")
    @ApiModelProperty(value = "计划用量", name = "tm6")
    @TableField(value = "TM6")
    private Double tm6;

    /**
     * 完成用量
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm7")
    @ApiModelProperty(value = "完成用量", name = "tm7")
    @TableField(value = "TM7")
    private Double tm7;

    /**
     * 3号线早班顺位
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm8")
    @ApiModelProperty(value = "3号线早班顺位", name = "tm8")
    @TableField(value = "TM8")
    private Integer tm8;

    /**
     * 3号线早班计划
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm9")
    @ApiModelProperty(value = "3号线早班计划", name = "tm9")
    @TableField(value = "TM9")
    private Double tm9;

    /**
     * 3号线夜班顺位
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm10")
    @ApiModelProperty(value = "3号线夜班顺位", name = "tm10")
    @TableField(value = "TM10")
    private Integer tm10;

    /**
     * 3号线夜班计划
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm11")
    @ApiModelProperty(value = "3号线夜班计划", name = "tm11")
    @TableField(value = "TM11")
    private Double tm11;

    /**
     * 2号线早班顺位
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm12")
    @ApiModelProperty(value = "2号线早班顺位", name = "tm12")
    @TableField(value = "TM12")
    private Integer tm12;

    /**
     * 2号线早班计划
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm13")
    @ApiModelProperty(value = "2号线早班计划", name = "tm13")
    @TableField(value = "TM13")
    private Double tm13;

    /**
     * 2号线夜班顺位
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm14")
    @ApiModelProperty(value = "2号线夜班顺位", name = "tm14")
    @TableField(value = "TM14")
    private Integer tm14;

    /**
     * 2号线夜班计划
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm15")
    @ApiModelProperty(value = "2号线夜班计划", name = "tm15")
    @TableField(value = "TM15")
    private Double tm15;

    /**
     * 4号线早班顺位
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm16")
    @ApiModelProperty(value = "4号线早班顺位", name = "tm16")
    @TableField(value = "TM16")
    private Integer tm16;

    /**
     * 4号线早班计划
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm17")
    @ApiModelProperty(value = "4号线早班计划", name = "tm17")
    @TableField(value = "TM17")
    private Double tm17;

    /**
     * 4号线夜班顺位
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm18")
    @ApiModelProperty(value = "4号线夜班顺位", name = "tm18")
    @TableField(value = "TM18")
    private Integer tm18;

    /**
     * 4号线夜班计划
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm19")
    @ApiModelProperty(value = "4号线夜班计划", name = "tm19")
    @TableField(value = "TM19")
    private Double tm19;

    /**
     * 3#早班完成
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm20")
    @ApiModelProperty(value = "3#早班完成", name = "tm20")
    @TableField(value = "TM20")
    private Double tm20;

    /**
     * 3#夜班完成
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm21")
    @ApiModelProperty(value = "3#夜班完成", name = "tm21")
    @TableField(value = "TM21")
    private Double tm21;

    /**
     * 2#早班完成
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm22")
    @ApiModelProperty(value = "2#早班完成", name = "tm22")
    @TableField(value = "TM22")
    private Double tm22;

    /**
     * 2#夜班完成
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm23")
    @ApiModelProperty(value = "2#夜班完成", name = "tm23")
    @TableField(value = "TM23")
    private Double tm23;

    /**
     * 4#早班完成
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm24")
    @ApiModelProperty(value = "4#早班完成", name = "tm24")
    @TableField(value = "TM24")
    private Double tm24;

    /**
     * 4#夜班完成
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm25")
    @ApiModelProperty(value = "4#夜班完成", name = "tm25")
    @TableField(value = "TM25")
    private Double tm25;

    /**
     * 理论交班库存
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm26")
    @ApiModelProperty(value = "理论交班库存", name = "tm26")
    @TableField(value = "TM26")
    private Double tm26;

    /**
     * 实际交班库存
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tm27")
    @ApiModelProperty(value = "实际交班库存", name = "tm27")
    @TableField(value = "TM27")
    private Double tm27;

    /**
     * 胎侧口型
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc1")
    @ApiModelProperty(value = "胎侧口型", name = "tc1")
    @TableField(value = "TC1")
    private String tc1;

    /**
     * 条米
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc2")
    @ApiModelProperty(value = "条米", name = "tc2")
    @TableField(value = "TC2")
    private Double tc2;

    /**
     * 卷长
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc3")
    @ApiModelProperty(value = "卷长", name = "tc3")
    @TableField(value = "TC3")
    private Double tc3;

    /**
     * 理论库存
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc4")
    @ApiModelProperty(value = "理论库存", name = "tc4")
    @TableField(value = "TC4")
    private Double tc4;

    /**
     * 实际库存
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc5")
    @ApiModelProperty(value = "实际库存", name = "tc5")
    @TableField(value = "TC5")
    private Double tc5;

    /**
     * 计划用量
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc6")
    @ApiModelProperty(value = "计划用量", name = "tc6")
    @TableField(value = "TC6")
    private Double tc6;

    /**
     * 完成用量
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc7")
    @ApiModelProperty(value = "完成用量", name = "tc7")
    @TableField(value = "TC7")
    private Double tc7;

    /**
     * 1号线早班顺位
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc8")
    @ApiModelProperty(value = "1号线早班顺位", name = "tc8")
    @TableField(value = "TC8")
    private Integer tc8;

    /**
     * 1号线早班计划
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc9")
    @ApiModelProperty(value = "1号线早班计划", name = "tc9")
    @TableField(value = "TC9")
    private Double tc9;

    /**
     * 1号线夜班顺位
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc10")
    @ApiModelProperty(value = "1号线夜班顺位", name = "tc10")
    @TableField(value = "TC10")
    private Integer tc10;

    /**
     * 1号线夜班计划
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc11")
    @ApiModelProperty(value = "1号线夜班计划", name = "tc11")
    @TableField(value = "TC11")
    private Double tc11;

    /**
     * 2号线早班顺位
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc12")
    @ApiModelProperty(value = "2号线早班顺位", name = "tc12")
    @TableField(value = "TC12")
    private Integer tc12;

    /**
     * 2号线早班计划
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc13")
    @ApiModelProperty(value = "2号线早班计划", name = "tc13")
    @TableField(value = "TC13")
    private Double tc13;

    /**
     * 2号线夜班顺位
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc14")
    @ApiModelProperty(value = "2号线夜班顺位", name = "tc14")
    @TableField(value = "TC14")
    private Integer tc14;

    /**
     * 2号线夜班计划
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc15")
    @ApiModelProperty(value = "2号线夜班计划", name = "tc15")
    @TableField(value = "TC15")
    private Double tc15;

    /**
     * 1#早班完成
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc16")
    @ApiModelProperty(value = "1#早班完成", name = "tc16")
    @TableField(value = "TC16")
    private Double tc16;

    /**
     * 1#夜班完成
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc17")
    @ApiModelProperty(value = "1#夜班完成", name = "tc17")
    @TableField(value = "TC17")
    private Double tc17;

    /**
     * 2#早班完成
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc18")
    @ApiModelProperty(value = "2#早班完成", name = "tc18")
    @TableField(value = "TC18")
    private Double tc18;

    /**
     * 2#夜班完成
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc19")
    @ApiModelProperty(value = "2#夜班完成", name = "tc19")
    @TableField(value = "TC19")
    private Double tc19;

    /**
     * 理论交班库存
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc20")
    @ApiModelProperty(value = "理论交班库存", name = "tc20")
    @TableField(value = "TC20")
    private Double tc20;

    /**
     * 实际交班库存
     */
    @Excel(name = "ui.data.column.halfYcImportBak.tc21")
    @ApiModelProperty(value = "实际交班库存", name = "tc21")
    @TableField(value = "TC21")
    private Double tc21;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.halfYcImportBak.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;


}
