package com.zlt.aps.mps.service.impl;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.mps.domain.TMesCdBaseEntity;
import com.zlt.aps.mps.domain.TMesStockBaseEntity;
import com.zlt.aps.mps.domain.BaseStock;
import com.zlt.aps.mps.mapper.TMesStockBaseMapper;
import com.zlt.aps.mps.service.MesHalfPartService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class MesHalfPartServiceImpl implements MesHalfPartService {

    @Resource
    private TMesStockBaseMapper stockMapper;

    /**
     * 胎面库存
     * @param dataVersion 同步版本
     */
    @Override
    public AjaxResult mergeTm(String dataVersion) {
        // 获取MES库存数据
        List<TMesStockBaseEntity> mesList = stockMapper.getTmByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error("数据为空");
        }
        List<BaseStock> list = new ArrayList<>();
        buildBaseStockList(mesList, list);
        stockMapper.mergeTmSql(list);
        return AjaxResult.success();
    }

    /**
     * 胎侧库存
     * @param dataVersion 同步版本
     */
    @Override
    public AjaxResult mergeTc(String dataVersion) {
        List<TMesStockBaseEntity> mesList = stockMapper.getTcByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error("数据为空");
        }
        List<BaseStock> list = new ArrayList<>();
        buildBaseStockList(mesList, list);
        stockMapper.mergeTcSql(list);
        return AjaxResult.success();
    }

    /**
     * 内衬库存
     * @param dataVersion 同步版本
     */
    @Override
    public AjaxResult mergeNc(String dataVersion) {
        List<TMesStockBaseEntity> mesList = stockMapper.getNcByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error("数据为空");
        }
        List<BaseStock> list = new ArrayList<>();
        buildBaseStockList(mesList, list);
        stockMapper.mergeNcSql(list);
        return AjaxResult.success();
    }

    /**
     * 胎圈库存
     * @param dataVersion 同步版本
     */
    @Override
    public AjaxResult mergeTq(String dataVersion) {
        List<TMesStockBaseEntity> mesList = stockMapper.getTqByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error("数据为空");
        }
        List<BaseStock> list = new ArrayList<>();
        buildBaseStockList(mesList, list);
        stockMapper.mergeTqSql(list);
        return AjaxResult.success();
    }

    /**
     * 钢丝圈库存
     * @param dataVersion 同步版本
     */
    @Override
    public AjaxResult mergeGsq(String dataVersion) {
        List<TMesStockBaseEntity> mesList = stockMapper.getGsqByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error("数据为空");
        }
        List<BaseStock> list = new ArrayList<>();
        buildBaseStockList(mesList, list);
        stockMapper.mergeGsqSql(list);
        return AjaxResult.success();
    }

    /**
     * 钢带压延库存
     * @param dataVersion 同步版本
     */
    @Override
    public AjaxResult mergeGdyy(String dataVersion) {
        List<TMesStockBaseEntity> mesList = stockMapper.getGdyyByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error("数据为空");
        }
        List<BaseStock> list = new ArrayList<>();
        buildBaseStockList(mesList, list);
        stockMapper.mergeGdyySql(list);
        return AjaxResult.success();
    }

    /**
     * 纤维压延库存
     * @param dataVersion 同步版本
     */
    @Override
    public AjaxResult mergeXwyy(String dataVersion) {
        List<TMesStockBaseEntity> mesList = stockMapper.getXwyyByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error("数据为空");
        }
        List<BaseStock> list = new ArrayList<>();
        buildBaseStockList(mesList, list);
        stockMapper.mergeXwyySql(list);
        return AjaxResult.success();
    }

    /**
     * 15度裁断库存
     * @param dataVersion 同步版本
     */
    @Override
    public AjaxResult mergeCd15(String dataVersion) {
        List<TMesCdBaseEntity> mesList = stockMapper.getCd15ByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error("数据为空");
        }
        List<BaseStock> list = new ArrayList<>();
        buildBaseCdStockList(mesList, list);
        stockMapper.mergeCd15Sql(list);
        return AjaxResult.success();
    }
    
    /**
     * 15度裁断线边库库存
     */
    @Override
    public AjaxResult mergeCd15LineSide(String dataVersion) {
        List<TMesCdBaseEntity> mesList = stockMapper.getCd15LineSideByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) { // 查不到数据，1、真的没数据，2、当天已经同步过了，只能通过页面的同步按钮进行同步
            return AjaxResult.success();
        }
        stockMapper.mergeCd15LineSideSql(mesList);
        return AjaxResult.success();
    }

    /**
     * 90度裁断库存
     * @param dataVersion 同步版本
     */
    @Override
    public AjaxResult mergeCd90(String dataVersion) {
        List<TMesCdBaseEntity> mesList = stockMapper.getCd90ByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) {
            return AjaxResult.error("数据为空");
        }
        List<BaseStock> list = new ArrayList<>();
        buildBaseCdStockList(mesList, list);
        stockMapper.mergeCd90Sql(list);
        return AjaxResult.success();
    }
    
    /**
     * 90度裁断线边库库存
     */
    @Override
    public AjaxResult mergeCd90LineSide(String dataVersion) {
        List<TMesCdBaseEntity> mesList = stockMapper.getCd90LineSideByDataVersion(dataVersion);
        if (CollectionUtil.isEmpty(mesList)) { // 查不到数据，1、真的没数据，2、当天已经同步过了，只能通过页面的同步按钮进行同步
        	return AjaxResult.success();
        }
        stockMapper.mergeCd90LineSideSql(mesList);
        return AjaxResult.success();
    }

    /**
     * 拼装库存实体list
      */
    private void buildBaseStockList(List<TMesStockBaseEntity> mesList, List<BaseStock> list) {
        HashMap<String, List<TMesStockBaseEntity>> mesMap = CollectionUtil.toMapList(mesList, obj -> obj.getStockDate() + "+" + obj.getMaterialCode());
        for (String code : mesMap.keySet()) {
            List<TMesStockBaseEntity> entityList = mesMap.get(code);
            BaseStock base = new BaseStock();
            base.setStockDate(entityList.get(0).getStockDate());
            base.setMaterialCode(entityList.get(0).getMaterialCode());
            BigDecimal stockNum = BigDecimal.ZERO;
            for (TMesStockBaseEntity mes : entityList) {
                if (mes.getAvailableStock() != null) {
                    // 库存量，累计可用库存量
                    stockNum = stockNum.add(mes.getAvailableStock());
                }
            }
            base.setStockNum(stockNum.setScale(3, RoundingMode.UP));
            base.setBadNum(BigDecimal.ZERO);
            base.setModifyNum(BigDecimal.ZERO);
            base.setBaseVale(null);
            list.add(base);
        }
    }

    /**
     * 拼装裁断实体list
     * @param mesList
     * @param list
     */
    private void buildBaseCdStockList(List<TMesCdBaseEntity> mesList, List<BaseStock> list) {
        HashMap<String, List<TMesCdBaseEntity>> mesMap = CollectionUtil.toMapList(mesList, obj -> obj.getStockDate() + "+" + obj.getMaterialCode());
        for (String code : mesMap.keySet()) {
            List<TMesCdBaseEntity> cdBaseEntityList = mesMap.get(code);
            BaseStock base = new BaseStock();
            base.setStockDate(cdBaseEntityList.get(0).getStockDate());
            base.setMaterialCode(cdBaseEntityList.get(0).getMaterialCode());
            BigDecimal stockNum = BigDecimal.ZERO;
            for (TMesCdBaseEntity mes : cdBaseEntityList) {
                // 裁断直接放库存
                stockNum = stockNum.add(mes.getAvailableStock());
            }
            base.setStockNum(stockNum);
            base.setBadNum(BigDecimal.ZERO);
            base.setModifyNum(BigDecimal.ZERO);
            this.setBaseSysValue(base);
            list.add(base);
        }

    }


    /**
     * 设置默认值
     * @param entity
     * @param <K>
     */
    private <K extends ApsBaseEntity> void  setBaseSysValue(K entity) {
        try {
            entity.setBaseVale(null);
        } catch (Exception e) {
            entity.setDelFlag("0");
            entity.setCreateBy("system");
            entity.setUpdateBy("system");
            entity.setCreateTime(new Date());
            entity.setUpdateTime(new Date());
        }
    }
}
