package com.zlt.aps.monthplan.mdm.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.factory.mapper.FactoryEngineProductionVersionMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuLhCapacityEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuStructureRefEntityMapper;
import com.zlt.aps.maindata.mapper.MpMonthPlanMonitorEntityMapper;
import com.zlt.aps.maindata.mapper.MpTrialPlanEntityMapper;
import com.zlt.aps.maindata.mapper.RawSpecialMaterialRecordEntityMapper;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuLhCapacity;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuStructureRef;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.api.domain.entity.MpTrialPlan;
import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialRecord;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.demand.mapper.SalesOrderPoolEntityMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.monthplan.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.monthplan.mdm.dto.DataDTO;
import com.zlt.common.utils.PubUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据管理器
 * @author wengpc
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataManager {

    protected final FactoryEngineProductionVersionMapper factoryProductionVersionMapper;
    protected final FactoryMonthPlanProductionFinalResultEntityMapper factoryMonthPlanProdFinalMapper;
    protected final SalesOrderPoolEntityMapper salesOrderPoolEntityMapper;
    protected final MpTrialPlanEntityMapper mpTrialPlanEntityMapper;
    protected final MdmSkuLhCapacityEntityMapper mdmSkuLhCapacityEntityMapper;
    protected final MdmSkuConstructionRefEntityMapper mdmSkuConstructionRefEntityMapper;
    protected final MdmSkuStructureRefEntityMapper mdmSkuStructureRefEntityMapper;
    protected final MpStructureAllocationEntityMapper mpStructureAllocationEntityMapper;
    protected final MpMonthPlanMonitorEntityMapper mpMonthPlanMonitorEntityMapper;
    protected final MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;
    protected final RawSpecialMaterialRecordEntityMapper rawSpecialMaterialRecordMapper;
    protected final MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;


    /**
     * 获取排产版本列表
     * @param dataDTO
     */
    @Cacheable(
            cacheNames = "factory:production:version",
            key = "#dataDTO.cacheKey",
            condition = "#dataDTO.isQueryCache and @caffeineCacheProperties.cacheEnabled"
    )
    public List<MpFactoryProductionVersion> listVersions(DataDTO<MpFactoryProductionVersion> dataDTO) {
        LambdaQueryWrapper<MpFactoryProductionVersion> wrapper = new LambdaQueryWrapper<>();
        buildVersionCondition(wrapper, dataDTO.getQueryObject());
        return factoryProductionVersionMapper.selectList(wrapper);
    }

    /**
     * 构建排产版本条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public void buildVersionCondition(LambdaQueryWrapper<MpFactoryProductionVersion> queryWrapper, MpFactoryProductionVersion queryVO) {
        queryWrapper.eq(MpFactoryProductionVersion::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MpFactoryProductionVersion::getYear, queryVO.getYear());
        queryWrapper.eq(MpFactoryProductionVersion::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpFactoryProductionVersion::getPlanType, queryVO.getPlanType());
        queryWrapper.eq(queryVO.getIsFinal() != null, MpFactoryProductionVersion::getIsFinal, queryVO.getIsFinal());
        queryWrapper.eq(MpFactoryProductionVersion::getIsDelete, YesOrNoEnum.NO.getValue());
    }


    /**
     * 获取月度生产计划列表
     * @param dataDTO
     */
    @Cacheable(
            cacheNames = "factory:monthplan:production",
            key = "#dataDTO.cacheKey",
            condition = "#dataDTO.isQueryCache and @caffeineCacheProperties.cacheEnabled"
    )
    public List<FactoryMonthPlanProductionFinalResult> listMonthPlans(DataDTO<FactoryMonthPlanProductionFinalResult> dataDTO) {
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaQueryWrapper<>();
        buildMonthPlanCondition(wrapper, dataDTO.getQueryObject());
        return factoryMonthPlanProdFinalMapper.selectList(wrapper);
    }


    /**
     * 构建月度生产计划条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public void buildMonthPlanCondition(LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper, FactoryMonthPlanProductionFinalResult queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), FactoryMonthPlanProductionFinalResult::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(queryVO.getYearMonth() != null, FactoryMonthPlanProductionFinalResult::getYearMonth, queryVO.getYearMonth());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getMonthPlanVersion()), FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, queryVO.getMonthPlanVersion());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getProductionVersion()), FactoryMonthPlanProductionFinalResult::getProductionVersion, queryVO.getProductionVersion());
        queryWrapper.eq(FactoryMonthPlanProductionFinalResult::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(queryVO.getYear() != null, FactoryMonthPlanProductionFinalResult::getYear, queryVO.getYear());
        queryWrapper.eq(queryVO.getMonth() != null, FactoryMonthPlanProductionFinalResult::getMonth, queryVO.getMonth());
    }


    /**
     * 获取销售订单池列表
     * @param dataDTO
     */
    @Cacheable(
            cacheNames = "sales:order:pool",
            key = "#dataDTO.cacheKey",
            condition = "#dataDTO.isQueryCache and @caffeineCacheProperties.cacheEnabled"
    )
    public List<SalesOrderPool> listSalesOrderPools(DataDTO<SalesOrderPool> dataDTO) {
        LambdaQueryWrapper<SalesOrderPool> wrapper = new LambdaQueryWrapper<>();
        buildSaleOrderPoolCondition(wrapper, dataDTO.getQueryObject());
        return salesOrderPoolEntityMapper.selectList(wrapper);
    }


    /**
     * 构建销售订单池条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public void buildSaleOrderPoolCondition(LambdaQueryWrapper<SalesOrderPool> queryWrapper, SalesOrderPool queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), SalesOrderPool::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getOrderStatus()), SalesOrderPool::getOrderStatus, queryVO.getOrderStatus());
        queryWrapper.eq(SalesOrderPool::getIsDelete, YesOrNoEnum.NO.getValue());
    }


    /**
     * 获取试制量试计划列表
     * @param dataDTO
     */
    @Cacheable(
            cacheNames = "trial:plan",
            key = "#dataDTO.cacheKey",
            condition = "#dataDTO.isQueryCache and @caffeineCacheProperties.cacheEnabled"
    )
    public List<MpTrialPlan> listTrialPlans(DataDTO<MpTrialPlan> dataDTO) {
        LambdaQueryWrapper<MpTrialPlan> wrapper = new LambdaQueryWrapper<>();
        buildTrialPlanCondition(wrapper, dataDTO.getQueryObject());
        return mpTrialPlanEntityMapper.selectList(wrapper);
    }


    /**
     * 构建试制量试计划条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public void buildTrialPlanCondition(LambdaQueryWrapper<MpTrialPlan> queryWrapper, MpTrialPlan queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), MpTrialPlan::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MpTrialPlan::getYear, queryVO.getYear());
        queryWrapper.eq(MpTrialPlan::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpTrialPlan::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.isNull(MpTrialPlan::getProductionDate);
    }

    /**
     * 获取sku日硫化产能列表
     * @param dataDTO
     */
    @Cacheable(
            cacheNames = "sku:lh:capacity",
            key = "#dataDTO.cacheKey",
            condition = "#dataDTO.isQueryCache and @caffeineCacheProperties.cacheEnabled"
    )
    public List<MdmSkuLhCapacity> listSkuLhCapacitys(DataDTO<MdmSkuLhCapacity> dataDTO) {
        LambdaQueryWrapper<MdmSkuLhCapacity> wrapper = new LambdaQueryWrapper<>();
        buildSkuLhCapacityCondition(wrapper, dataDTO.getQueryObject());
        return mdmSkuLhCapacityEntityMapper.selectList(wrapper);
    }

    /**
     * 构建sku日硫化产能条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public void buildSkuLhCapacityCondition(LambdaQueryWrapper<MdmSkuLhCapacity> queryWrapper, MdmSkuLhCapacity queryVO) {
        queryWrapper.eq(MdmSkuLhCapacity::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmSkuLhCapacity::getIsDelete, YesOrNoEnum.NO.getValue());
    }


    /**
     * 获取SKU与施工（示方书）关系列表
     * @param dataDTO
     */
    @Cacheable(
            cacheNames = "sku:construction:ref",
            key = "#dataDTO.cacheKey",
            condition = "#dataDTO.isQueryCache and @caffeineCacheProperties.cacheEnabled"
    )
    public List<MdmSkuConstructionRef> listSkuConstructionRefs(DataDTO<MdmSkuConstructionRef> dataDTO) {
        LambdaQueryWrapper<MdmSkuConstructionRef> wrapper = new LambdaQueryWrapper<>();
        buildSkuConstructionRefCondition(wrapper, dataDTO.getQueryObject());
        return mdmSkuConstructionRefEntityMapper.selectList(wrapper);
    }

    /**
     * 构建SKU与施工（示方书）关系条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public void buildSkuConstructionRefCondition(LambdaQueryWrapper<MdmSkuConstructionRef> queryWrapper, MdmSkuConstructionRef queryVO) {
        queryWrapper.eq(MdmSkuConstructionRef::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmSkuConstructionRef::getIsDelete, YesOrNoEnum.NO.getValue());
    }


    /**
     * 获取sku与结构关系列表
     * @param dataDTO
     */
    @Cacheable(
            cacheNames = "sku:structure:ref",
            key = "#dataDTO.cacheKey",
            condition = "#dataDTO.isQueryCache and @caffeineCacheProperties.cacheEnabled"
    )
    public List<MdmSkuStructureRef> listSkuStructureRefs(DataDTO<MdmSkuStructureRef> dataDTO) {
        LambdaQueryWrapper<MdmSkuStructureRef> wrapper = new LambdaQueryWrapper<>();
        buildSkuStructureRefCondition(wrapper, dataDTO.getQueryObject());
        return mdmSkuStructureRefEntityMapper.selectList(wrapper);
    }



    /**
     * 构建sku与结构关系条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public void buildSkuStructureRefCondition(LambdaQueryWrapper<MdmSkuStructureRef> queryWrapper, MdmSkuStructureRef queryVO) {
        queryWrapper.eq(MdmSkuStructureRef::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmSkuStructureRef::getIsDelete, YesOrNoEnum.NO.getValue());
    }



    /**
     * 获取月计划结构转产列表
     * @param dataDTO
     */
    @Cacheable(
            cacheNames = "structure:allocation",
            key = "#dataDTO.cacheKey",
            condition = "#dataDTO.isQueryCache and @caffeineCacheProperties.cacheEnabled"
    )
    public List<MpStructureAllocation> listStructureAllocations(DataDTO<MpStructureAllocation> dataDTO) {
        LambdaQueryWrapper<MpStructureAllocation> wrapper = new LambdaQueryWrapper<>();
        buildStructureAllocationCondition(wrapper, dataDTO.getQueryObject());
        return mpStructureAllocationEntityMapper.selectList(wrapper);
    }


    /**
     * 构建月计划结构转产条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public void buildStructureAllocationCondition(LambdaQueryWrapper<MpStructureAllocation> queryWrapper, MpStructureAllocation queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), MpStructureAllocation::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MpStructureAllocation::getYear, queryVO.getYear());
        queryWrapper.eq(MpStructureAllocation::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpStructureAllocation::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getProductionVersion()), MpStructureAllocation::getProductionVersion, queryVO.getProductionVersion());
    }


    /**
     * 获取月度硫化监控列表
     * @param dataDTO
     */
    @Cacheable(
            cacheNames = "plan:monitor",
            key = "#dataDTO.cacheKey",
            condition = "#dataDTO.isQueryCache and @caffeineCacheProperties.cacheEnabled"
    )
    public List<MpMonthPlanMonitor> listPlanMonitors(DataDTO<MpMonthPlanMonitor> dataDTO) {
        LambdaQueryWrapper<MpMonthPlanMonitor> wrapper = new LambdaQueryWrapper<>();
        buildPlanMonitorCondition(wrapper, dataDTO.getQueryObject());
        return mpMonthPlanMonitorEntityMapper.selectList(wrapper);
    }


    /**
     * 构建月度硫化监控条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public void buildPlanMonitorCondition(LambdaQueryWrapper<MpMonthPlanMonitor> queryWrapper, MpMonthPlanMonitor queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), MpMonthPlanMonitor::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MpMonthPlanMonitor::getYear, queryVO.getYear());
        queryWrapper.eq(MpMonthPlanMonitor::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpMonthPlanMonitor::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getMonthPlanVersion()), MpMonthPlanMonitor::getMonthPlanVersion, queryVO.getMonthPlanVersion());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getProductionVersion()), MpMonthPlanMonitor::getProductionVersion, queryVO.getProductionVersion());
    }


    /**
     * 获取物料信息列表
     * @param dataDTO
     */
    @Cacheable(
            cacheNames = "material:info",
            key = "#dataDTO.cacheKey",
            condition = "#dataDTO.isQueryCache and @caffeineCacheProperties.cacheEnabled"
    )
    public List<MdmMaterialInfo> listMaterialInfos(DataDTO<MdmMaterialInfo> dataDTO) {
        LambdaQueryWrapper<MdmMaterialInfo> wrapper = new LambdaQueryWrapper<>();
        buildMaterialInfoCondition(wrapper, dataDTO.getQueryObject());
        return mdmMaterialInfoEntityMapper.selectList(wrapper);
    }


    /**
     * 构建物料信息条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public void buildMaterialInfoCondition(LambdaQueryWrapper<MdmMaterialInfo> queryWrapper, MdmMaterialInfo queryVO) {
        queryWrapper.eq(MdmMaterialInfo::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmMaterialInfo::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 获取特殊材料列表
     * @param dataDTO
     */
    @Cacheable(
            cacheNames = "special:material:record",
            key = "#dataDTO.cacheKey",
            condition = "#dataDTO.isQueryCache and @caffeineCacheProperties.cacheEnabled"
    )
    public List<RawSpecialMaterialRecord> listSpecialMaterials(DataDTO<RawSpecialMaterialRecord> dataDTO) {
        LambdaQueryWrapper<RawSpecialMaterialRecord> wrapper = new LambdaQueryWrapper<>();
        buildSpecialMaterialCondition(wrapper, dataDTO.getQueryObject());
        return rawSpecialMaterialRecordMapper.selectList(wrapper);
    }



    /**
     * 构建特殊材料条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public void buildSpecialMaterialCondition(LambdaQueryWrapper<RawSpecialMaterialRecord> queryWrapper, RawSpecialMaterialRecord queryVO) {
        queryWrapper.eq(RawSpecialMaterialRecord::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(RawSpecialMaterialRecord::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 获取BOM物料消耗明细列表
     * @param dataDTO
     */
    @Cacheable(
            cacheNames = "material:consume:detail",
            key = "#dataDTO.cacheKey",
            condition = "#dataDTO.isQueryCache and @caffeineCacheProperties.cacheEnabled"
    )
    public List<MdmMaterialConsumeDetail> listMaterialConsumeDetails(DataDTO<MdmMaterialConsumeDetail> dataDTO) {
        LambdaQueryWrapper<MdmMaterialConsumeDetail> wrapper = new LambdaQueryWrapper<>();
        buildMaterialConsumeDetailCondition(wrapper, dataDTO.getQueryObject());
        return mdmMaterialConsumeDetailMapper.selectList(wrapper);
    }

    /**
     * 构建BOM物料消耗明细条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public void buildMaterialConsumeDetailCondition(LambdaQueryWrapper<MdmMaterialConsumeDetail> queryWrapper, MdmMaterialConsumeDetail queryVO) {
        queryWrapper.eq(MdmMaterialConsumeDetail::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmMaterialConsumeDetail::getIsDelete, YesOrNoEnum.NO.getValue());
    }


    /**
     * 清除排产版本缓存
     */
    @CacheEvict(cacheNames = "factory:production:version", allEntries = true)
    public void clearVersions() {
    }


    /**
     * 清除月度生产计划缓存
     */
    @CacheEvict(cacheNames = "factory:monthplan:production", allEntries = true)
    public void clearMonthPlans() {
    }

    /**
     * 清除销售订单池缓存
     */
    @CacheEvict(cacheNames = "sales:order:pool", allEntries = true)
    public void clearSalesOrderPools() {
    }

    /**
     * 清除试制量试计划缓存
     */
    @CacheEvict(cacheNames = "trial:plan", allEntries = true)
    public void clearTrialPlans() {
    }

    /**
     * 清除sku日硫化产能缓存
     */
    @CacheEvict(cacheNames = "sku:lh:capacity", allEntries = true)
    public void clearSkuLhCapacitys() {
    }

    /**
     * 清除SKU与施工（示方书）关系缓存
     */
    @CacheEvict(cacheNames = "sku:construction:ref", allEntries = true)
    public void clearSkuConstructionRefs() {
    }

    /**
     * 清除sku与结构关系缓存
     */
    @CacheEvict(cacheNames = "sku:structure:ref", allEntries = true)
    public void clearSkuStructureRefs() {
    }

    /**
     * 清除月计划结构转产缓存
     */
    @CacheEvict(cacheNames = "structure:allocation", allEntries = true)
    public void clearStructureAllocations() {
    }

    /**
     * 清除月度硫化监控缓存
     */
    @CacheEvict(cacheNames = "plan:monitor", allEntries = true)
    public void clearPlanMonitors() {
    }

    /**
     * 清除物料信息缓存
     */
    @CacheEvict(cacheNames = "material:info", allEntries = true)
    public void clearMaterialInfos() {
    }

    /**
     * 清除特殊材料缓存
     */
    @CacheEvict(cacheNames = "special:material:record", allEntries = true)
    public void clearSpecialMaterials() {
    }

    /**
     * 清除BOM物料消耗明细缓存
     */
    @CacheEvict(cacheNames = "material:consume:detail", allEntries = true)
    public void clearMaterialConsumeDetails() {
    }


    /**
     * 生成缓存Key
     * @param args 参数
     * @return String
     */
    public String generateCacheKey(Object... args) {
        StringBuilder stringBuilder = new StringBuilder();
        if (PubUtil.isNotEmpty(args)) {
            for (Object arg : args) {
                stringBuilder.append(arg)
                        .append(ApsConstant.SPLIT_CHAR);
            }
        }
        return stringBuilder.toString();
    }


    /**
     * 构建数据DTO
     * @param queryObject 查询对象
     * @param isQueryCache 是否查询缓存
     * @param args 参数
     * @return DataDTO
     */
    public DataDTO buildDataDTO(Object queryObject, boolean isQueryCache, Object... args) {
        return DataDTO.builder()
                .queryObject(queryObject)
                .cacheKey(generateCacheKey(args))
                .isQueryCache(isQueryCache)
                .build();
    }

    /**
     * 构建数据DTO
     * @param queryObject 查询对象
     * @param cacheKey 缓存Key
     * @param isQueryCache 是否查询缓存
     * @return DataDTO
     */
    public DataDTO buildDataDTO(Object queryObject, String cacheKey, boolean isQueryCache) {
        return DataDTO.builder()
                .queryObject(queryObject)
                .cacheKey(cacheKey)
                .isQueryCache(isQueryCache)
                .build();
    }

    /**
     * 构建数据DTO
     * @param queryObject 查询对象
     * @param isQueryCache 是否查询缓存
     * @return DataDTO
     */
    public DataDTO buildDataDTO(Object queryObject, boolean isQueryCache) {
        return buildDataDTO(queryObject, null, isQueryCache);
    }

    /**
     * 构建数据DTO
     * @param queryObject 查询对象
     * @return DataDTO
     */
    public DataDTO buildDataDTO(Object queryObject) {
        return buildDataDTO(queryObject, null, Boolean.FALSE);
    }



}
