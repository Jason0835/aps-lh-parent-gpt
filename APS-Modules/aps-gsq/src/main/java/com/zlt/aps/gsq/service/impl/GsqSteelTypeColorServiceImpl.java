package com.zlt.aps.gsq.service.impl;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import cn.hutool.core.collection.CollUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gsq.api.domain.dto.GsqSteelTypeColorDto;
import com.zlt.aps.gsq.entity.GsqSteelTypeColor;
import com.zlt.aps.gsq.mapper.GsqSteelTypeColorMapper;
import com.zlt.aps.gsq.service.GsqSteelTypeColorService;
/**
 * @author Gim
 */
@Service
public class GsqSteelTypeColorServiceImpl  extends ServiceImpl<GsqSteelTypeColorMapper, GsqSteelTypeColor> implements GsqSteelTypeColorService {


    @Resource
    private GsqSteelTypeColorMapper gsqSteelTypeColorMapper;

    /**
     * 根据条件大卷颜色提示列表
     *
     * @return
     */
    public List<GsqSteelTypeColorDto> listGsqSteelTypeColor(GsqSteelTypeColorDto dto) {
        return gsqSteelTypeColorMapper.listGsqSteelTypeColor(dto);
    }

    /**
     * 保存大卷颜色提示信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveGsqSteelTypeColor(GsqSteelTypeColor entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        if (entity.getColorCode() == null || entity.getColorCode() == "") {
            entity.setColorCode("#000000");//无值设置默认黑色
        }
        this.saveOrUpdate(entity);
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteGsqSteelTypeColor(Long[] ids) {
        LambdaUpdateWrapper<GsqSteelTypeColor> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApsBaseEntity::getId, Arrays.asList(ids));
        wrapper.set(ApsBaseEntity::getDelFlag, null);
        wrapper.set(ApsBaseEntity::getUpdateBy, SecurityUtils.getUsername());
        wrapper.set(ApsBaseEntity::getUpdateTime, new Date());
        super.getBaseMapper().update(null, wrapper);
    }

    /**
     * 根据大卷编号判断是否已经存在
     */
    public String checkGsqSteelTypeColor(GsqSteelTypeColorDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getSteelType())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<GsqSteelTypeColor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("STEEL_TYPE", dto.getSteelType());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<GsqSteelTypeColor> list = gsqSteelTypeColorMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }


    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GsqSteelTypeColorDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqSteelTypeColorDto> importList = new ArrayList<>();
        // 按业务主键分组
        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(GsqSteelTypeColorDto::getSteelType, Collectors.counting()));
        for (int i = 0; i < list.size(); i++) {
            GsqSteelTypeColorDto entity = list.get(i);
            // excel内业务主键唯一校验
            Long hasValue = groupMap.get(entity.getSteelType());
            if (hasValue > 1) {
                entity.setId(-999L);
                String columnName = I18nUtil.getMessage("ui.data.column.scheduleResult.steelType");
                addImportErrorLog(importLogId, i + 2,
                        String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"), columnName),
                        importErrorLogs);
                failureNum++;
                continue;
            }
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, entity);
            if (CollectionUtils.isNotEmpty(validated)) {
                entity.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                if (StringUtils.isBlank(entity.getColorCode())) {
                    entity.setColorCode("#000000");
                }
                importList.add(entity);
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    // 勾选更新：批量预取已存在大卷颜色（按大卷编号匹配），存在则更新原记录，不存在则新增
                    Map<String, GsqSteelTypeColor> existingMap = this.loadExistingSteelTypeColorMap(importList);
                    for (GsqSteelTypeColorDto excelItem : importList) {
                        GsqSteelTypeColor color = new GsqSteelTypeColor();
                        BeanUtils.copyProperties(excelItem, color);
                        GsqSteelTypeColor existing = existingMap.get(excelItem.getSteelType());
                        if (existing != null) {
                            // 已存在：回填主键ID，清空新增审计字段避免覆盖原记录创建信息，setBaseVale补齐更新审计字段后更新
                            color.setId(existing.getId());
                            color.setCreateBy(null);
                            color.setCreateTime(null);
                            color.setBaseVale(existing.getId());
                            gsqSteelTypeColorMapper.updateById(color);
                        } else {
                            // 不存在：setBaseVale(null)自动补齐delFlag/createBy/createTime后插入
                            color.setBaseVale(null);
                            gsqSteelTypeColorMapper.insert(color);
                        }
                        successNum++;
                    }
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        GsqSteelTypeColorDto excelItem = list.get(i);
                        //过滤错误的记录
                        if (excelItem.getId() != null && excelItem.getId() == -999L) {
                            continue;
                        }
                        // 唯一性校验
                        String unic = checkGsqSteelTypeColor(excelItem);
                        if (unic.equals(UserConstants.UNIQUE)) {
                            //不存在插入
                            successNum++;
                            GsqSteelTypeColor color = new GsqSteelTypeColor();
                            BeanUtils.copyProperties(excelItem, color);
                            color.setBaseVale(null);
                            gsqSteelTypeColorMapper.insert(color);
                        } else {
                            // 存在，插入错误详细日志
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2,
                                    I18nUtil.getMessage("ui.color.message.unique.gsq"), importErrorLogs);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 执行sql失败，插入导入失败记录
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 批量预取已存在的大卷颜色数据（导入更新模式使用）
     * 按大卷编号批量查询数据库未删除的已有记录
     *
     * @param importList 导入数据列表
     * @return 大卷编号 -> 已存在大卷颜色记录 的映射
     */
    private Map<String, GsqSteelTypeColor> loadExistingSteelTypeColorMap(List<GsqSteelTypeColorDto> importList) {
        // 提取非空大卷编号并去重
        List<String> codeList = importList.stream()
                .map(GsqSteelTypeColorDto::getSteelType)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(codeList)) {
            return new HashMap<>();
        }
        // 按1000条一批查询（过滤逻辑删除），避免in条件超长；同编号多条时保留首条
        return CollUtil.split(codeList, 1000).stream()
                .flatMap(batch -> gsqSteelTypeColorMapper.selectList(new LambdaQueryWrapper<GsqSteelTypeColor>()
                        .eq(GsqSteelTypeColor::getDelFlag, ApsConstant.DEL_FLAG_NORMAL)
                        .in(GsqSteelTypeColor::getSteelType, batch)).stream())
                .collect(Collectors.toMap(GsqSteelTypeColor::getSteelType, Function.identity(), (v1, v2) -> v1));
    }
}
