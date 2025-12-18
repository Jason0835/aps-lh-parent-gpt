package com.zlt.aps.itf.mes.service.impl;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.itf.mes.mapper.MesItfMapper;
import com.zlt.aps.itf.mes.service.MesItfService;
import com.zlt.aps.maindata.mapper.MdmModelInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductModelRelationEntityMapper;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.MdmModelInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuMouldRel;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Chen
 * @since 2025/12/16
 */
@Slf4j
@Service
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
     * @param mdmSkuMouldRel SAP与模具关系
     * @return 结果
     */
    @Override
    public AjaxResult syncProductModRelation(MdmSkuMouldRel mdmSkuMouldRel) {
        // 查询中间表
        List<MdmSkuMouldRel> list = getMdmSkuMouldRelList(mdmSkuMouldRel);
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
     * @param modelInfo 模具台账
     * @return 结果
     */
    @Override
    public AjaxResult syncModelInfo(MdmModelInfo modelInfo) {
        // 查询中间表
        List<MdmModelInfo> list = getMdmModelInfoList(modelInfo);
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
}
