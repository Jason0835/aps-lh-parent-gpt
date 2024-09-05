package com.zlt.aps.cx.service.impl;


import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.cx.api.domain.dto.ConstructionInfoDto;
import com.zlt.aps.cx.entity.ConstructionInfo;
import com.zlt.aps.cx.mapper.ConstructionInfoMapper;
import com.zlt.aps.cx.service.ConstructionInfoService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 施工信息表 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Service
public class ConstructionInfoServiceImpl extends ServiceImpl<ConstructionInfoMapper, ConstructionInfo> implements ConstructionInfoService {

    @Resource
    private ConstructionInfoMapper constructionInfoMapper;

    @Resource
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;

    /**
     * 根据条件查询施工信息列表
     *
     * @return
     */
    public List<ConstructionInfoDto> listConstructionInfo(ConstructionInfoDto dto) {
        return constructionInfoMapper.listConstructionInfo(dto);
    }

    /**
     * 保存施工信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveConstructionInfo(ConstructionInfo entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        //Joran.Zhang 2021-07-01 清空引擎公共模块缓存数据，触发重新加载
        cxEngineQuotaCommonService.delCacheConstructionInfoMap();
        this.saveOrUpdate(entity);
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteConstructionInfo(Long[] ids) {
        for (int i = 0; i < ids.length; i++) {
            ConstructionInfo entity = new ConstructionInfo();
            entity.setId(ids[i]);
            entity.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            entity.setUpdateTime(new Date());
            this.updateById(entity);
        }
        //Joran.Zhang 2021-07-01 清空引擎公共模块缓存数据，触发重新加载
        cxEngineQuotaCommonService.delCacheConstructionInfoMap();
    }

    /**
     * 验证胚胎代码唯一性
     */
    public String checkEmbryoCodeUnique(ConstructionInfoDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getEmbryoCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<ConstructionInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("EMBRYO_CODE", dto.getEmbryoCode());
        queryWrapper.eq("EMBRYO_VERSION", dto.getEmbryoVersion());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<ConstructionInfo> list = constructionInfoMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<ConstructionInfoDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<ConstructionInfo> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getEmbryoCode(), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            ConstructionInfoDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getEmbryoCode());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.construction.embryoCode");
                message=String.format(message,columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                ConstructionInfo newEntity = new ConstructionInfo();
                dto.setTireFabric1Version(dto.getEmbryoVersion());
                dto.setTireFabric2Version(dto.getEmbryoVersion());
                dto.setTireFabric3Version(dto.getEmbryoVersion());
                dto.setCordVersion(dto.getEmbryoVersion());
                dto.setInsideVersion(dto.getEmbryoVersion());
                dto.setSidewallVersion(dto.getEmbryoVersion());
                dto.setBeadVersion(dto.getEmbryoVersion());
                dto.setTireRingVersion(dto.getEmbryoVersion());
                dto.setBelt1Version(dto.getEmbryoVersion());
                dto.setBelt2Version(dto.getEmbryoVersion());
                dto.setArticleCrownVersion(dto.getEmbryoVersion());
                dto.setTreadVersion(dto.getEmbryoVersion());
                BeanUtils.copyProperties(dto, newEntity);
                newEntity.setBaseVale(null);
                newList.add(newEntity);
            }
        }

        //新集合操作（更新或插入操作）
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用mergeOrInsert
                if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                    successNum = newList.size();
                    constructionInfoMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        ConstructionInfoDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        ConstructionInfo newItem = new ConstructionInfo();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setBaseVale(null);

                        QueryWrapper<ConstructionInfo> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("EMBRYO_CODE", newItem.getEmbryoCode());
                        queryWrapper.eq("EMBRYO_VERSION", newItem.getEmbryoVersion());
                        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
                        List<ConstructionInfo> exist = constructionInfoMapper.selectList(queryWrapper);
                        if (CollectionUtils.isEmpty(exist)) {
                            successNum++;
                            newItem.setTireFabric1Version(newItem.getEmbryoVersion());
                            newItem.setTireFabric2Version(newItem.getEmbryoVersion());
                            newItem.setTireFabric3Version(newItem.getEmbryoVersion());
                            newItem.setCordVersion(newItem.getEmbryoVersion());
                            newItem.setInsideVersion(newItem.getEmbryoVersion());
                            newItem.setSidewallVersion(newItem.getEmbryoVersion());
                            newItem.setBeadVersion(newItem.getEmbryoVersion());
                            newItem.setTireRingVersion(newItem.getEmbryoVersion());
                            newItem.setBelt1Version(newItem.getEmbryoVersion());
                            newItem.setBelt2Version(newItem.getEmbryoVersion());
                            newItem.setArticleCrownVersion(newItem.getEmbryoVersion());
                            newItem.setTreadVersion(newItem.getEmbryoVersion());
                            this.saveOrUpdate(newItem);
                        } else {
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        }

        //Joran.Zhang 2021-11-26 清空引擎公共模块缓存数据，触发重新加载
        cxEngineQuotaCommonService.delCacheConstructionInfoMap();
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

}
