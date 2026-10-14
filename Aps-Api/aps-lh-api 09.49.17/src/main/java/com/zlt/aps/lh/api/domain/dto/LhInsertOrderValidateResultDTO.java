package com.zlt.aps.lh.api.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 硫化插单校验结果DTO
 * <p>包含阻断性错误和非阻断性提示信息</p>
 *
 * @author APS
 */
@Data
public class LhInsertOrderValidateResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 校验是否通过（无阻断性错误）
     */
    @ApiModelProperty(value = "校验是否通过", name = "valid")
    private boolean valid;

    /**
     * 阻断性错误信息列表
     */
    @ApiModelProperty(value = "阻断性错误信息列表", name = "errorMessages")
    private List<String> errorMessages = new ArrayList<>();

    /**
     * 提示性信息列表（非阻断，如产能提示、超产提示、模具可用提示）
     */
    @ApiModelProperty(value = "提示性信息列表", name = "warningMessages")
    private List<String> warningMessages = new ArrayList<>();

    /**
     * 机台班产
     */
    @ApiModelProperty(value = "机台班产", name = "machineShiftCapacity")
    private Integer machineShiftCapacity;

    /**
     * 各班次剩余产能（key: 班次索引1-8, value: 剩余产能）
     */
    @ApiModelProperty(value = "各班次剩余产能", name = "remainingCapacityByShift")
    private List<ShiftCapacityInfo> remainingCapacityByShift = new ArrayList<>();

    /**
     * 硫化余量
     */
    @ApiModelProperty(value = "硫化余量", name = "mouldSurplusQty")
    private Integer mouldSurplusQty;

    /**
     * 胎胚库存
     */
    @ApiModelProperty(value = "胎胚库存", name = "embryoStock")
    private Integer embryoStock;

    /**
     * 左右模标识（L-左模，R-右模）
     */
    @ApiModelProperty(value = "左右模标识", name = "leftRightMould")
    private String leftRightMould;

    /**
     * 示方类型（字典 lh_trial_status：S-正规示方，T-量试示方，X-试验示方）
     */
    @ApiModelProperty(value = "示方类型", name = "trialStatus")
    private String trialStatus;

    /**
     * 单模硫化班产（单模机台=双模班产/2向下取整，双模机台=双模班产）
     */
    @ApiModelProperty(value = "单模硫化班产", name = "singleMouldShiftQty")
    private Integer singleMouldShiftQty;

    /**
     * 胎胚代码
     */
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    private String embryoCode;

    /**
     * 胎胚描述（主物料描述）
     */
    @ApiModelProperty(value = "胎胚描述", name = "mainMaterialDesc")
    private String mainMaterialDesc;

    /**
     * 需求计划版本号
     */
    @ApiModelProperty(value = "需求计划版本号", name = "monthPlanVersion")
    private String monthPlanVersion;

    /**
     * 排产版本号
     */
    @ApiModelProperty(value = "排产版本号", name = "productionVersion")
    private String productionVersion;

    /**
     * 规格
     */
    @ApiModelProperty(value = "规格", name = "specCode")
    private String specCode;

    /**
     * 产品结构
     */
    @ApiModelProperty(value = "产品结构", name = "structureName")
    private String structureName;

    /**
     * 模具号
     */
    @ApiModelProperty(value = "模具号", name = "mouldCode")
    private String mouldCode;

    /**
     * 班次产能信息
     */
    @Data
    public static class ShiftCapacityInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "班次索引", name = "shiftIndex")
        private Integer shiftIndex;

        @ApiModelProperty(value = "班次名称", name = "shiftName")
        private String shiftName;

        @ApiModelProperty(value = "机台班产", name = "shiftCapacity")
        private Integer shiftCapacity;

        @ApiModelProperty(value = "已排计划量", name = "scheduledQty")
        private Integer scheduledQty;

        @ApiModelProperty(value = "剩余产能", name = "remainingCapacity")
        private Integer remainingCapacity;

        public ShiftCapacityInfo() {
        }

        public ShiftCapacityInfo(Integer shiftIndex, String shiftName, Integer shiftCapacity,
                                 Integer scheduledQty, Integer remainingCapacity) {
            this.shiftIndex = shiftIndex;
            this.shiftName = shiftName;
            this.shiftCapacity = shiftCapacity;
            this.scheduledQty = scheduledQty;
            this.remainingCapacity = remainingCapacity;
        }
    }

    /**
     * 添加阻断性错误
     *
     * @param error 错误信息
     */
    public void addError(String error) {
        this.errorMessages.add(error);
        this.valid = false;
    }

    /**
     * 添加提示性信息
     *
     * @param warning 提示信息
     */
    public void addWarning(String warning) {
        this.warningMessages.add(warning);
    }
}
