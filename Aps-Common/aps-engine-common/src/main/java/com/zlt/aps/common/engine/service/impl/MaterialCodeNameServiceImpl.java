package com.zlt.aps.common.engine.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.common.engine.mapper.MdmMaterialCodeNameCommonMapper;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.common.engine.service.MaterialCodeNameService;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialCodeName;
import com.zlt.aps.mdm.api.enums.MaterialCodeTypeEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * 物料代号名称对照公共服务实现。
 * <p>
 * 数据源统一为 T_MDM_MATERIAL_CODE_NAME：原先垫胶在模块内自建查询、内衬直接读施工信息表，
 * 两者口径不一致；改为按物料类型（07=垫胶、04=内衬）从对照表取数后，各工序下拉的数据来源与维护入口统一。
 * </p>
 */
@Service
@Slf4j
public class MaterialCodeNameServiceImpl implements MaterialCodeNameService {

    @Resource
    private MdmMaterialCodeNameCommonMapper mdmMaterialCodeNameCommonMapper;

    @Resource
    private FactoryService factoryService;

    /**
     * 获取指定物料类型的部件下拉列表。
     *
     * @param materialType 物料类型枚举
     * @return 下拉选项列表，元素为 {value: 物料代号, label: 物料名称}
     */
    @Override
    public List<Map<String, String>> getMaterialDistList(MaterialCodeTypeEnum materialType) {
        if (materialType == null) {
            log.warn("部件下拉取数：物料类型为空，直接返回空列表");
            return new ArrayList<>();
        }
        // 当前所属工厂，取不到时不按工厂过滤（与既有垫胶下拉口径一致，避免工厂配置缺失导致整个下拉为空）
        String factoryCode = factoryService.getFactoryCode();
        LambdaQueryWrapper<MdmMaterialCodeName> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(MdmMaterialCodeName::getMaterialCode, MdmMaterialCodeName::getMaterialName)
                .eq(StringUtils.isNotEmpty(factoryCode), MdmMaterialCodeName::getFactoryCode, factoryCode)
                .eq(MdmMaterialCodeName::getMaterialType, materialType.getCode())
                .orderByAsc(MdmMaterialCodeName::getMaterialName);
        List<MdmMaterialCodeName> list = mdmMaterialCodeNameCommonMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            log.warn("部件下拉取数：物料代号名称对照表无数据，factoryCode={}，materialType={}({})",
                    factoryCode, materialType.getCode(), materialType.getName());
            return new ArrayList<>();
        }
        // 同一物料代号可能重复维护，按代号去重（保留按名称排序后的首条）
        Map<String, String> codeNameMap = list.stream()
                .filter(item -> StringUtils.isNotBlank(item.getMaterialCode()))
                .collect(Collectors.toMap(
                        item -> StringUtils.trim(item.getMaterialCode()),
                        item -> StringUtils.defaultString(item.getMaterialName()),
                        (first, second) -> first,
                        LinkedHashMap::new));
        List<Map<String, String>> result = new ArrayList<>(codeNameMap.size());
        codeNameMap.forEach((code, name) -> {
            Map<String, String> option = new HashMap<>(2);
            option.put("value", code);
            option.put("label", name);
            result.add(option);
        });
        return result;
    }
}
