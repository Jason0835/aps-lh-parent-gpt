package com.zlt.aps.tm.service.impl;


import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tm.api.domain.dto.TmCurlRollDto;
import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;
import com.zlt.aps.tm.mapper.TmCurlRollMapper;
import com.zlt.aps.tm.service.TmCurlRollService;

/**
 * <p>
 * 90度裁断胎面卷曲信息表 服务实现类
 * </p>
 *
 * @author zlt
 * @since 2023-09-07
 */
@Service
public class TmCurlRollServiceImpl extends ServiceImpl<TmCurlRollMapper, TmCurlRoll> implements TmCurlRollService {

    @Resource
    private TmCurlRollMapper tmCurlRollMapper;

    /**
     * 根据条件查询胎面卷曲信息列表
     *
     * @return
     */
    public List<TmCurlRoll> listCurlRoll(TmCurlRoll dto) {
        return tmCurlRollMapper.listCurlRoll(dto);
    }

    /**
     * 保存胎面卷曲信息信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveCurlRoll(TmCurlRoll entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        this.saveOrUpdate(entity);
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteCurlRoll(Long[] ids) {
        for (int i = 0; i < ids.length; i++) {
            TmCurlRoll entity = new TmCurlRoll();
            entity.setId(ids[i]);
            entity.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            entity.setUpdateTime(new Date());
            this.updateById(entity);
        }
    }

    /**
     * 根据code判断胎面卷曲是否已经存在
     */
    public String checkCurlRollCodeUnique(TmCurlRoll dto) {
        if (dto == null || StringUtils.isBlank(dto.getTreadCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<TmCurlRoll> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("TREAD_CODE", dto.getTreadCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<TmCurlRoll> list = tmCurlRollMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<TmCurlRollDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<TmCurlRoll> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(TmCurlRollDto::getTreadCode, Collectors.counting()));
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int rowNo = i + 2; // excel里的行号
            TmCurlRollDto sourceEntity = list.get(i);

			// excel内业务主键唯一校验
			if (groupMap.get(sourceEntity.getTreadCode()) > 1) {
				String columnName = I18nUtil.getMessage("ui.data.column.quota.treadCode");
                sourceEntity.setId(-999L);
				addImportErrorLog(importLogId, rowNo,
						String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"), columnName),
						importErrorLogs);
                failureNum++;
				continue;
			}
            
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, rowNo, sourceEntity);
            if (CollectionUtils.isNotEmpty(validated)) {
                sourceEntity.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
            	// 校验通过后单独对数字字段校验
            	String curlLength = sourceEntity.getCurlLength();
            	if (BigDecimalUtil.isDigits(curlLength)) {
            		TmCurlRoll importEntity = new TmCurlRoll();
            		importEntity.setTreadCode(sourceEntity.getTreadCode());
            		importEntity.setCurlLength(new BigDecimal(sourceEntity.getCurlLength()));
            		importEntity.setRemark(sourceEntity.getRemark());
                    List<ImportErrorLog> importValidated = ImportUtil.validated(importLogId, rowNo, importEntity);
                    if (CollectionUtils.isNotEmpty(importValidated)) { // 数字字段校验失败
                        sourceEntity.setId(-999L);
                        failureNum++;
                        importErrorLogs.addAll(importValidated);
                    } else { // 全部校验通过
                    	TmCurlRoll targetEntity = new TmCurlRoll();
                        BeanUtils.copyProperties(importEntity, targetEntity);
                        targetEntity.setBaseVale(null);
                        newList.add(targetEntity);
                    }
            	} else { // 卷曲长度不是数字类型
    				String columnName = I18nUtil.getMessage("ui.curlRoll.column.length");
                    sourceEntity.setId(-999L);
    				addImportErrorLog(importLogId, rowNo,
    						String.format(I18nUtil.getMessage("import.errorValueEnum.message.doubleValue"), rowNo, columnName),
    						importErrorLogs);
                    failureNum++;
            	}
            }
        }
        
        //新集合操作（更新或插入操作）
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用mergeOrInsert
                if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                    successNum = newList.size();
                    tmCurlRollMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        TmCurlRollDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        TmCurlRoll newItem = new TmCurlRoll();
                        newItem.setTreadCode(dto.getTreadCode());
                        newItem.setCurlLength(new BigDecimal(dto.getCurlLength()));
                        newItem.setRemark(dto.getRemark());
                        newItem.setBaseVale(null);

                        QueryWrapper<TmCurlRoll> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("TREAD_CODE", newItem.getTreadCode());
                        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
                        List<TmCurlRoll> alreadyExistList = tmCurlRollMapper.selectList(queryWrapper);
                        if (CollectionUtils.isNotEmpty(alreadyExistList)) {
                            failureNum++;
                            String message = I18nUtil.getMessage("ui.error.message.quota.unique");
                            addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                        } else {
                            successNum++;
                            this.saveOrUpdate(newItem);
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
