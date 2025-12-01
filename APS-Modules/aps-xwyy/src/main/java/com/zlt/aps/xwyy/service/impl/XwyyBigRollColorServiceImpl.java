package com.zlt.aps.xwyy.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
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
import com.zlt.aps.xwyy.api.domain.dto.XwyyBigRollColorDto;
import com.zlt.aps.xwyy.entity.XwyyBigRollColor;
import com.zlt.aps.xwyy.mapper.XwyyBigRollColorMapper;
import com.zlt.aps.xwyy.service.XwyyBigRollColorService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 帘布大卷颜色提示信息表 服务实现类
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-07
 */
@Service
public class XwyyBigRollColorServiceImpl extends ServiceImpl<XwyyBigRollColorMapper, XwyyBigRollColor> implements XwyyBigRollColorService {

    @Resource
    private XwyyBigRollColorMapper xwyyBigRollColorMapper;

    /**
     * 根据条件大卷颜色提示列表
     *
     * @return
     */
    public List<XwyyBigRollColorDto> listXwyyBigRollColor(XwyyBigRollColorDto dto) {
        return xwyyBigRollColorMapper.listXwyyBigRollColor(dto);
    }

    /**
     * 保存大卷颜色提示信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveXwyyBigRollColor(XwyyBigRollColor entity) {
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
    public void deleteXwyyBigRollColor(Long[] ids) {
        LambdaUpdateWrapper<XwyyBigRollColor> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApsBaseEntity::getId, Arrays.asList(ids));
        wrapper.set(ApsBaseEntity::getDelFlag, null);
        wrapper.set(ApsBaseEntity::getUpdateBy, SecurityUtils.getUsername());
        wrapper.set(ApsBaseEntity::getUpdateTime, new Date());
        super.getBaseMapper().update(null, wrapper);
    }

    /**
     * 根据大卷编号判断是否已经存在
     */
    public String checkXwyyBigRollColor(XwyyBigRollColorDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getBigRollCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<XwyyBigRollColor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("BIG_ROLL_CODE", dto.getBigRollCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<XwyyBigRollColor> list = xwyyBigRollColorMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<XwyyBigRollColorDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<XwyyBigRollColor> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(XwyyBigRollColorDto::getBigRollCode, Collectors.counting()));
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            XwyyBigRollColorDto dto = list.get(i);
			// excel内业务主键唯一校验
			Long hasValue = groupMap.get(dto.getBigRollCode());
			if (hasValue > 1) {
				dto.setId(-999L);
				String columnName = I18nUtil.getMessage("ui.bigRollColor.column.bigRollCode");
				addImportErrorLog(importLogId, i + 2,
						String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"), columnName),
						importErrorLogs);
				failureNum++;
				continue;
			}
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                XwyyBigRollColor newEntity = new XwyyBigRollColor();
                BeanUtils.copyProperties(dto, newEntity);
                newEntity.setBaseVale(null);
                if (StringUtils.isBlank(newEntity.getColorCode())) {
                    newEntity.setColorCode("#000000");
                }
                newList.add(newEntity);
            }
        }

        //新集合操作（更新或插入操作）
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用mergeOrInsert
                if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                    successNum = newList.size();
                    xwyyBigRollColorMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        XwyyBigRollColorDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        XwyyBigRollColor newItem = new XwyyBigRollColor();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setBaseVale(null);
                        if (StringUtils.isBlank(newItem.getColorCode())) {
                            newItem.setColorCode("#000000");
                        }
                        QueryWrapper<XwyyBigRollColor> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("BIG_ROLL_CODE", newItem.getBigRollCode());
                        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
                        List<XwyyBigRollColor> exist = xwyyBigRollColorMapper.selectList(queryWrapper);
                        if (CollectionUtils.isEmpty(exist)) {
                            successNum++;
                            this.save(newItem);
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
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
