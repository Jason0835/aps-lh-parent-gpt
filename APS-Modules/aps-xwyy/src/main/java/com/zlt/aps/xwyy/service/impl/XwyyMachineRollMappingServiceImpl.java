package com.zlt.aps.xwyy.service.impl;


import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.xwyy.api.domain.dto.XwyyMachineRollMappingDto;
import com.zlt.aps.xwyy.api.domain.entity.XwyyMachineInfo;
import com.zlt.aps.xwyy.entity.XwyyMachineRollMapping;
import com.zlt.aps.xwyy.mapper.XwyyMachineRollMappingMapper;
import com.zlt.aps.xwyy.service.XwyyMachineInfoService;
import com.zlt.aps.xwyy.service.XwyyMachineRollMappingService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 纤维压延帘布大卷与机台的映射表 服务实现类
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
@Service
public class XwyyMachineRollMappingServiceImpl extends ServiceImpl<XwyyMachineRollMappingMapper, XwyyMachineRollMapping> implements XwyyMachineRollMappingService {

    @Resource
    private XwyyMachineRollMappingMapper xwyyMachineRollMappingMapper;

    @Autowired
    private XwyyMachineInfoService xwyyMachineInfoService;

    /**
     * 根据条件大卷颜色提示列表
     *
     * @return
     */
    public List<XwyyMachineRollMappingDto> listXwyyMachineRollMapping(XwyyMachineRollMappingDto dto) {
        return xwyyMachineRollMappingMapper.listXwyyMachineRollMapping(dto);
    }

    /**
     * 保存大卷颜色提示信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveXwyyMachineRollMapping(XwyyMachineRollMapping entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        if (!isRollMappingUnique(entity)) {
            //根据物料号+机台信息验证
            throw new RuntimeException(I18nUtil.getMessage("ui.xwyy.machineRollMapping.unique"));
        }
        this.saveOrUpdate(entity);
    }

    /**
     * 帘布大卷编号+生产线验证唯一性
     */
    private boolean isRollMappingUnique(XwyyMachineRollMapping entity) {
        QueryWrapper<XwyyMachineRollMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("BIG_ROLL_CODE", entity.getBigRollCode());
        queryWrapper.eq("MACHINE_ID", entity.getMachineId());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (entity.getId() != null) {
            queryWrapper.ne("ID", entity.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<XwyyMachineRollMapping> list = xwyyMachineRollMappingMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return false;
        }
        return true;
    }


    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteXwyyMachineRollMapping(Long[] ids) {
        for (int i = 0; i < ids.length; i++) {
            XwyyMachineRollMapping entity = new XwyyMachineRollMapping();
            entity.setId(ids[i]);
            entity.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            entity.setUpdateTime(new Date());
            this.updateById(entity);
        }
    }

    /**
     * 根据大卷编号判断是否已经存在
     */
    public String checkXwyyMachineRollMapping(XwyyMachineRollMappingDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getBigRollCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<XwyyMachineRollMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("BIG_ROLL_CODE", dto.getBigRollCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<XwyyMachineRollMapping> list = xwyyMachineRollMappingMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<XwyyMachineRollMappingDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<XwyyMachineRollMapping> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<XwyyMachineInfo> machineInfoList = xwyyMachineInfoService.selectMachineInfoList(new XwyyMachineInfo());
        Map<String, Long> machineCodeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineInfoList)) {
            machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineCode(), a.getId()));
        }
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(v -> (v.getBigRollCode() + v.getMachineName()), Collectors.counting()));
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            XwyyMachineRollMappingDto dto = list.get(i);
			// excel内业务主键唯一校验
			if (groupMap.get(dto.getBigRollCode() + dto.getMachineName()) > 1) {
                dto.setId(-999L);
				String columnName1 = I18nUtil.getMessage("ui.bigRollColor.column.bigRollCode");
				String columnName2 = I18nUtil.getMessage("ui.data.column.machine.machineCode");
				addImportErrorLog(importLogId, i + 2,
						String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"),
								columnName1 + "+" + columnName2),
						importErrorLogs);
                failureNum++;
                continue;
			}
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            // 机台校验
            if (StringUtils.isNotEmpty(dto.getMachineName()) && machineCodeMap.get(dto.getMachineName()) == null) {
                addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.column.machineCodeNotExist"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                XwyyMachineRollMapping newEntity = new XwyyMachineRollMapping();
                BeanUtils.copyProperties(dto, newEntity);
                newEntity.setMachineId(machineCodeMap.get(dto.getMachineName()));
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
                    xwyyMachineRollMappingMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        XwyyMachineRollMappingDto dto = list.get(i);
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        XwyyMachineRollMapping newItem = new XwyyMachineRollMapping();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setMachineId(machineCodeMap.get(dto.getMachineName()));
                        newItem.setBaseVale(null);

                        if (isRollMappingUnique(newItem)) {
                            successNum++;
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
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

}
