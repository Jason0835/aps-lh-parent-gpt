package com.zlt.aps.itf.mes.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.itf.mes.mapper.MesItfMapper;
import com.zlt.aps.itf.mes.service.MesItfService;
import com.zlt.aps.maindata.mapper.MdmModelInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductModelRelationEntityMapper;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.core.dao.basedao.BaseDao;
import com.zlt.sync.domain.AuxReqSyncDataLogs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Chen
 * @since 2025/12/16
 */
@Slf4j
@Service("mesItfService")
public class MesItfServiceImpl implements MesItfService {

    @Autowired
    private MesItfMapper mesItfMapper;
    @Autowired
    private MdmProductModelRelationEntityMapper productModelRelationEntityMapper;
    @Autowired
    private MdmModelInfoEntityMapper modelInfoEntityMapper;
    @Autowired
    private BaseDao baseDao;

    /**
     * 同步SAP与模具关系
     *
     * @param syncDataLogs SAP与模具关系
     * @return 结果
     */
    @Override
    public AjaxResult syncProductModRelation(AuxReqSyncDataLogs syncDataLogs) {
        // 查询中间表
        MdmSkuMouldRel mdmSkuMouldRel = new MdmSkuMouldRel();
        mdmSkuMouldRel.setDataVersion(syncDataLogs.getDataVersion());
        List<MdmSkuMouldRel> list = this.getMdmSkuMouldRelList(mdmSkuMouldRel);
        // 型腔模号+NC物料编码作为匹配条件，如果存在，则更新，不存在则插入
        List<List<MdmSkuMouldRel>> splitList = ScmListUtils.getSplitList(list, 1000);
        for (List<MdmSkuMouldRel> skuMouldRelList : splitList) {
            List<MdmSkuMouldRel> existsList = productModelRelationEntityMapper.selectByUniqueKeyList(skuMouldRelList);
            Map<String, MdmSkuMouldRel> existsMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(existsList)) {
                existsMap = existsList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMouldCode(), item.getMaterialCode()), Function.identity()));
            }
            for (MdmSkuMouldRel skuMouldRel : skuMouldRelList) {
                String mapKey = GenerageMapKeyUtils.createMapKey(skuMouldRel.getFactoryCode(), skuMouldRel.getMouldCode(), skuMouldRel.getMaterialCode());
                if (existsMap.containsKey(mapKey)) {
                    MdmSkuMouldRel existsData = existsMap.get(mapKey);
                    skuMouldRel.setId(existsData.getId());
                }
            }
            baseDao.saveBatch(skuMouldRelList);
        }
        return AjaxResult.success();
    }

    /**
     * 同步模具台账
     *
     * @param syncDataLogs 模具台账
     * @return 结果
     */
    @Override
    public AjaxResult syncModelInfo(AuxReqSyncDataLogs syncDataLogs) {
        // 查询中间表
        MdmModelInfo mdmSkuMouldRel = new MdmModelInfo();
        mdmSkuMouldRel.setDataVersion(syncDataLogs.getDataVersion());
        List<MdmModelInfo> list = getMdmModelInfoList(mdmSkuMouldRel);
        // 型腔模号+NC物料编码作为匹配条件，如果存在，则更新，不存在则插入
        List<List<MdmModelInfo>> splitList = ScmListUtils.getSplitList(list, 1000);
        for (List<MdmModelInfo> saveList : splitList) {
            List<MdmModelInfo> existsList = modelInfoEntityMapper.selectByUniqueKeyList(saveList);
            Map<String, MdmModelInfo> existsMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(existsList)) {
                existsMap = existsList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMouldCode()), Function.identity()));
            }
            for (MdmModelInfo entity : saveList) {
                String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getMouldCode());
                if (existsMap.containsKey(mapKey)) {
                    MdmModelInfo existsData = existsMap.get(mapKey);
                    entity.setId(existsData.getId());
                }
            }
            baseDao.saveBatch(saveList);
        }
        return AjaxResult.success();
    }

    /**
     * 获取模具台账List
     *
     * @param modelInfo 模具台账
     * @return 结果
     */
    @Override
    public List<MdmModelInfo> getMdmModelInfoList(MdmModelInfo modelInfo) {
        return mesItfMapper.selectModelInfoList(modelInfo);
    }

    /**
     * 获取AP与模具关系
     *
     * @param mdmSkuMouldRel SAP与模具关系
     * @return 结果
     */
    @Override
    public List<MdmSkuMouldRel> getMdmSkuMouldRelList(MdmSkuMouldRel mdmSkuMouldRel) {
        return mesItfMapper.selectSkuMouldRelList(mdmSkuMouldRel);
    }

    /**
     * 同步成品库存
     *
     * @param productStockMonth 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncProductStock(ProductStockMonth productStockMonth) {
        List<ProductStockMonth> productStockList = this.getProductStock(productStockMonth);
        // 先删后增，日期
        Calendar instance = Calendar.getInstance();
        instance.setTime(new Date());
        if (productStockMonth.getYear() == null) {
            productStockMonth.setYear(instance.get(Calendar.YEAR));
        }
        if (productStockMonth.getMonth() == null) {
            productStockMonth.setMonth(instance.get(Calendar.MONTH) + 1);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("YEAR", productStockMonth.getYear());
        map.put("MONTH", productStockMonth.getMonth());
        baseDao.deleteByMap(ProductStockMonth.class, map);
        List<List<ProductStockMonth>> splitList = ScmListUtils.getSplitList(productStockList, 1000);
        for (List<ProductStockMonth> importList : splitList) {
            baseDao.insertBatch(importList);
        }
        // steve's TODO 根据年周号计算是否超期胎
        return AjaxResult.success();
    }

    /**
     * 查询成品库存
     *
     * @param productStockMonth 参数
     * @return 结果
     */
    @Override
    public List<ProductStockMonth> getProductStock(ProductStockMonth productStockMonth) {
        // 查询视图
        return mesItfMapper.selectProductStock(productStockMonth);
    }

    /**
     * 同步不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncUnqualifiedStock(MdmUnqualifiedStock mdmUnqualifiedStock) throws ParseException {
        List<MdmUnqualifiedStock> productStockList = this.getUnqualifiedStock(mdmUnqualifiedStock);
        // 先删后增，日期
        Date stockDate = mdmUnqualifiedStock.getStockDate();
        if (stockDate == null) {
            stockDate = DateUtils.getNowDate("yyyy-MM-dd");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("STOCK_DATE", stockDate);
        baseDao.deleteByMap(ProductStockMonth.class, map);
        List<List<MdmUnqualifiedStock>> splitList = ScmListUtils.getSplitList(productStockList, 1000);
        for (List<MdmUnqualifiedStock> importList : splitList) {
            baseDao.insertBatch(importList);
        }
        return AjaxResult.success();
    }

    /**
     * 同步不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    @Override
    public List<MdmUnqualifiedStock> getUnqualifiedStock(MdmUnqualifiedStock mdmUnqualifiedStock) {
        // 查询视图
        return mesItfMapper.selectUnqualifiedStock(mdmUnqualifiedStock);
    }

    /**
     * 同步特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncRawSpecialMaterialStock(RawSpecialMaterialStock rawSpecialMaterialStock) throws ParseException {
        List<RawSpecialMaterialStock> productStockList = this.getRawSpecialMaterialStock(rawSpecialMaterialStock);
        // 先删后增，日期
        Date stockDate = rawSpecialMaterialStock.getStockDate();
        if (stockDate == null) {
            stockDate = DateUtils.getNowDate("yyyy-MM-dd");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("STOCK_DATE", stockDate);
        baseDao.deleteByMap(RawSpecialMaterialStock.class, map);
        List<List<RawSpecialMaterialStock>> splitList = ScmListUtils.getSplitList(productStockList, 1000);
        for (List<RawSpecialMaterialStock> importList : splitList) {
            baseDao.insertBatch(importList);
        }
        return AjaxResult.success();
    }

    /**
     * 查询特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    @Override
    public List<RawSpecialMaterialStock> getRawSpecialMaterialStock(RawSpecialMaterialStock rawSpecialMaterialStock) {
        // 查询视图
        return mesItfMapper.selectRawSpecialMaterialStock(rawSpecialMaterialStock);
    }


}
