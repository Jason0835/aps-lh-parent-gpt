package com.zlt.aps.factory.domain.vo;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.tlt.aps.enums.*;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMonthPlanInit;
import com.zlt.aps.monthplan.api.domain.vo.ProductSpecInfoVo;
import com.zlt.common.utils.PubUtil;
import com.zlt.common.utils.SafeCompute;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分厂月制造需求计划对象
 *
 * @author ZLT
 * @date 20250327
 */
@Data
public class MonthPlanManufacturingRequirementVo extends ProductionMonthPlanInit {
    /**
     * 库位列表排序值
     */
    @TableField(exist = false)
    private Integer locationSortValue;
    /**
     * 轮胎类型
     */
    private String tireType;
    /**
     * 胎体布层级数 1 表示单层 2 表示多层(即2,3等)单层可使用多层，多层不能使用单层 即1可变成2,2不能变成1
     */
    private Integer tireFabricNumber;
    /**
     * SAP-施工本身的胎体层级
     */
    private Integer originalTireFabricNumber;
    /**
     * 是否有交期
     */
    private Integer hasDeliveryDate;
    /**
     * 不排产数量
     */
    private Long noProductionQty;
    /**
     * 施工代号，可转换成施工阶段
     */
    private String constructionCode;
    /**
     * 施工阶段枚举实例
     */
    private ConstructionStageEnum constructionStageType;
    /**
     * 续作模具预占量--预占模具产能计算时使用
     */
    private Long continueMouldPreemptQty;
    /**
     * 预占模具集合
     */
    private Set<String> preemptMouldCodeSet;
    /**
     * 是否出现因规格数限制及日产能限制
     */
    private Boolean isCapacityLimit;
    /**
     * 是否出现过新增规格数限制
     */
    private Boolean isAddedProductLimit;
    /**
     * 汇总备货制造需求量--原始的
     */
    private Long summaryStockUpDemandQty;
    /**
     * 计划规格总的排产量--拼模时使用(主要)
     */
    private Long summaryProductionQty;
    /**
     * 计划规格总的净需求量--拼模时使用
     */
    private Long summaryNetDemandQty;
    /**
     * 合模压力--拼模时使用
     */
    private BigDecimal mouldClampingPressure;
    /**
     * 模具行腔--拼模时使用
     */
    private String moldCavity;
    /**
     * 需求量差值--拼模时使用
     */
    private Long diffValue;
    /**
     * 拼模前的信息
     */
    private PlanAssemblingMouldChangeInfoVo beforeAssemblingMouldInfo;
    /**
     * 可拼模排产的模具信息--拼模时后规格使用
     */
    private List<MouldInfoVO> enableAssemblingMouldList;
    /**
     * 计划所处的分组
     */
    private ProductionFirstSortOptionsEnum groupType;
    /**
     * 20251026 ZLT 配置的模具
     */
    private String mouldNoInfo;
    /**
     * 排产量？
     */
    private Long productionQty;
    /**
     *
     */
    private Long mouldFullQty;

    private Long prodReqPlan;

    private Long productionSequence;

    public Long getProductionSequence() {
        return null;
    }

    public Long getProdReqPlan(){
        return null;
    }

    public String getIsStockUp(){
        return "";
    }

    /**
     * 判断是否为有交期计划
     *
     * @return
     */
    public Integer getHasDeliveryDate() {
        if (null == getDeliveryDateDue()) {
            return YesOrNoEnum.NO.getValue();
        }
        return YesOrNoEnum.YES.getValue();
    }

    /**
     * 获取寸口产能分组key
     * 寸口|*|工装类别|*|成型法|*|胎体布层级
     *
     * @return
     */
    public String getSizeCapacityGroupKey() {
        String groupKey = "%s|*|%s|*|%s|*|%s";
        return String.format(groupKey, getProSize(), getWorkWearTypeValue(), getMouldMethod(), tireFabricNumber);
    }

    /**
     * 忽略胎体层级--因一次法都是多层产能
     * 获取寸口产能分组key
     * 寸口|*|工装类别|*|成型法
     *
     * @return
     */
    public String getSizeCapacityGroupKeyNoTireFabric() {
        String groupKey = "%s|*|%s|*|%s";
        return String.format(groupKey, getProSize(), getWorkWearTypeValue(), getMouldMethod());
    }

    /**
     * 获取对应多层胎体布Key
     * 寸口|*|工装类别|*|成型法|*|胎体层级
     *
     * @return
     */
    public String getMultilayerTireFabricMultilayerKey(Integer changeTireFabricNumber) {
        String groupKey = "%s|*|%s|*|%s|*|%s";
        return String.format(groupKey, getProSize(), getWorkWearTypeValue(), getMouldMethod(), changeTireFabricNumber);
    }

    /**
     * 换成多层胎体布
     */
    public void changedMultilayerTireFabric() {
        if (ProductionConstant.MULTILAYER_TIRE_FABRIC.equals(originalTireFabricNumber)) {
            return;
        }
        tireFabricNumber = ProductionConstant.MULTILAYER_TIRE_FABRIC;
    }

    /**
     * 轮胎类型产能控制
     * 轮胎类型|*|寸口
     *
     * @return
     */
    public String getTireCapacityGroupKey() {
        String groupKey = "%s|*|%s";
        return String.format(groupKey, getTireType(), getProSize());
    }

    /**
     * 更换成型法，需要更换对应的生胎，硫化代号
     *
     * @param change
     */
    public void changeSpecCode(FormingMethodTypeEnum change) {
        List<ProductSpecInfoVo> productSpecInfoList = getProductSpecInfos();
        if (CollectionUtils.isEmpty(productSpecInfoList)) {
            return;
        }
        for (ProductSpecInfoVo productSpecInfo : productSpecInfoList) {
            //匹配到
            if (change.getMethodValue().equals(productSpecInfo.getMouldMethod())) {
                setEmbryoCode(productSpecInfo.getEmbryoCode());
                setSpecCode(productSpecInfo.getSpecCode());
                setMouldMethod(productSpecInfo.getMouldMethod());
                break;
            }
        }
    }

    /**
     * 获取工装类别
     *
     * @return
     */
    public String getWorkWearTypeValue() {
//        WorkWearTypeEnum type = WorkWearTypeEnum.getInstance(getProSize(), getMouldMethod(), getSpecifications());
        return "";
    }

    public void setIsProduction(Integer isProduction){

    }

    /**
     * 是否可以切换规格
     *
     * @return
     */
    public Boolean hasChangeSpecCode() {
        List<ProductSpecInfoVo> productSpecInfoList = getProductSpecInfos();
        if (CollectionUtils.isEmpty(productSpecInfoList)) {
            return false;
        }
        Set<String> mouldMethodSet = productSpecInfoList.stream().map(ProductSpecInfoVo::getMouldMethod).collect(Collectors.toSet());
        return mouldMethodSet.size() > BigDecimal.ONE.intValue();
    }

    /**
     * 保存拼模前的信息
     * 可排产量
     * 不排产原因
     * 不排产数量
     */
    public final void saveBeforeAssemblingMouldPlanInfo() {
        beforeAssemblingMouldInfo = new PlanAssemblingMouldChangeInfoVo(getProductionQty(), getNoProductionReason(), noProductionQty, getRemark());
    }

    /**
     * 还原拼模前的计划数据
     */
    public final void resetBeforeAssemblingMouldPlanInfo() {
        if (null == beforeAssemblingMouldInfo) {
            return;
        }
        setProductionQty(beforeAssemblingMouldInfo.getBeforeProductionQty());
        setNoProductionReason(beforeAssemblingMouldInfo.getBeforeNoProductionReason());
        setNoProductionQty(beforeAssemblingMouldInfo.getBeforeNoProductionQty());
        setRemark(beforeAssemblingMouldInfo.getBeforeRemark());
    }

    /**
     * 是否只是一次法
     *
     * @return
     */
    public boolean isOnlyFirstMethod() {
        if (hasChangeSpecCode()) {
            return false;
        }
        return FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(getMouldMethod());
    }

    /**
     * 20250524 ZLT 增加不排数量及不排原因
     *
     * @param addNoProductionReason
     * @param addNoProductionQty
     */
    public void addNoProductionReasonAndQty(String addNoProductionReason, Long addNoProductionQty) {
        addNoProductionReason(addNoProductionReason);
        addNoProductionQty(addNoProductionQty);
    }

    /**
     * 是否为有效计划
     * 需要判断 施工阶段 生胎代码
     * 硫化时间 寸口 有无模具 分厂是否排产
     * 需排产量 规格代号
     *
     * @return
     */
    public boolean isEffectivePlan() {
        if (!isCapacityControlPlan()) {
            return false;
        }
        //有无模具
        if (null == getMouldQty() || getMouldQty().intValue() == 0) {
            return false;
        }
        return true;
    }

    /**
     * 是否为产能控制计划
     * true表示是
     * false表示否
     *
     * @return
     */
    public boolean isCapacityControlPlan() {
        //施工阶段
        if (null == getConstructionStageType() || ConstructionStageEnum.FORMAL_PRODUCTION != getConstructionStageType()) {
            return false;
        }
        //硫化时间
        if (null == getCuringTime() || SafeCompute.compareToZero(getCuringTime()) == 0) {
            return false;
        }
        //寸口
        if (null == getProSize()) {
            return false;
        }
        //分厂是否排产
        if (PubUtil.isTrue(getIsFactoryProduction())) {
            return false;
        }
        //生胎代码
        if (StringUtils.isBlank(getEmbryoCode())) {
            return false;
        }
        //规格代号(硫化)
        if (StringUtils.isBlank(getSpecCode())) {
            return false;
        }
        return true;
    }

    /**
     * 增加拼模备注
     *
     * @param assemblingRemark
     */
    public void addAssemblingRemark(String assemblingRemark) {
        String content = getRemark();
        if (StringUtils.isBlank(content)) {
            setRemark(assemblingRemark);
            return;
        }
        setRemark(String.format("%s;%s", content, assemblingRemark));
    }

    /**
     * 获取对应的硫化规格信息
     * 包含成型法
     *
     * @return
     */
    private List<ProductSpecInfoVo> getProductSpecInfos() {
        String specCodeInfo = getSpecCodeInfo();
        if (StringUtils.isBlank(specCodeInfo)) {
            return Collections.emptyList();
        }
        return JSON.parseArray(specCodeInfo, ProductSpecInfoVo.class);
    }

    /**
     * 增加不排原因
     *
     * @param addNoProductionReason
     */
    private void addNoProductionReason(String addNoProductionReason) {
        if (StringUtils.isBlank(addNoProductionReason)) {
            return;
        }
        String noProductionReason = getNoProductionReason();
        if (PubUtil.isEmpty(noProductionReason)) {
            setNoProductionReason(addNoProductionReason);
        } else {
            setNoProductionReason(String.format("%s,%s", noProductionReason, addNoProductionReason));
        }
    }

    /**
     * 增加不排数量
     *
     * @param addNoProductionQty
     */
    private void addNoProductionQty(Long addNoProductionQty) {
        if (null == addNoProductionQty || addNoProductionQty == BigDecimal.ZERO.longValue()) {
            return;
        }
        Long sumNoProductionQty = BigDecimal.ZERO.longValue();
        if (null != noProductionQty) {
            sumNoProductionQty = sumNoProductionQty + noProductionQty;
        }
        sumNoProductionQty = sumNoProductionQty + addNoProductionQty;
        if (sumNoProductionQty > getFactProdReqQty()) {
            sumNoProductionQty = getFactProdReqQty();
        }
        setNoProductionQty(sumNoProductionQty);
    }

}
