package com.zlt.aps.factory.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.tlt.aps.enums.ConstructionStageEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 物料最新施工信息
 *
 * @author ZLT
 * @daate 20250414
 */
@Data
public class ProductConstructionInfoVo implements Serializable {

    /**
     * 物料编码
     */
    private String productCode;

    /**
     * 硫化规格代号
     */
    private String specCode;

    /**
     * 施工代号，可转换成施工阶段
     */
    private String constructionCode;
    /**
     * 生胎代号
     */
    private String embryoCode;
    /**
     * 成型法: MACHINE_TYPE
     * 1-1次法
     * 2-2次法
     */
    private String mouldMethod;

    /**
     * 夏季硫化时间--单位秒
     */
    private Integer summerCuringTime;

    /**
     * 冬季硫化时间--单位秒
     */
    private Integer winterCuringTime;

    /**
     * 对应-施工阶段 biz_construction_stage
     */
    private ConstructionStageEnum constructionStage;
    /**
     * 合模压力
     */
    private BigDecimal mouldClampingPressure;
    /**
     * 模具行腔
     */
    private String moldCavity;
}
