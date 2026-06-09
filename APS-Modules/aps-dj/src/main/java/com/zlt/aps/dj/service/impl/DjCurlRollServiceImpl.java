package com.zlt.aps.dj.service.impl;


import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.dj.api.domain.dto.DjCurlRollDto;
import com.zlt.aps.dj.api.domain.entity.DjCurlRoll;
import com.zlt.aps.dj.api.domain.entity.DjParams;
import com.zlt.aps.dj.mapper.DjCurlRollMapper;
import com.zlt.aps.dj.mapper.DjParamsMapper;
import com.zlt.aps.dj.service.DjCurlRollService;

/**
 * <p>
 * 90度裁断胎侧卷曲信息表 服务实现类
 * </p>
 *
 * @author zlt
 * @since 2023-09-07
 */
@Service
public class DjCurlRollServiceImpl extends ServiceImpl<DjCurlRollMapper, DjCurlRoll> implements DjCurlRollService {

    @Resource
    private DjCurlRollMapper ncCurlRollMapper;

    /**
     * 根据条件查询胎侧卷曲信息列表
     *
     * @return
     */
    public List<DjCurlRoll> listCurlRoll(DjCurlRoll dto) {
        return ncCurlRollMapper.listCurlRoll(dto);
    }

    /**
     * 保存胎侧卷曲信息信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveCurlRoll(DjCurlRoll entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        this.saveOrUpdate(entity);
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteCurlRoll(Long[] ids) {
        LambdaUpdateWrapper<DjCurlRoll> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(DjCurlRoll::getId, Arrays.asList(ids));
        wrapper.set(DjCurlRoll::getIsDelete, null);
        wrapper.set(DjCurlRoll::getUpdateBy, SecurityUtils.getUsername());
        wrapper.set(DjCurlRoll::getUpdateTime, new Date());
        super.getBaseMapper().update(null, wrapper);
    }

    /**
     * 根据code判断胎侧卷曲是否已经存在
     */
    public String checkCurlRollCodeUnique(DjCurlRoll dto) {
        if (dto == null || StringUtils.isBlank(dto.getLiningCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<DjCurlRoll> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("LINING_CODE", dto.getLiningCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<DjCurlRoll> list = ncCurlRollMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    private static final Integer DEFAULT_CURL_LENGTH = 82;
    @Autowired
    private DjParamsMapper paramsMapper;

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<DjCurlRollDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<DjCurlRoll> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(DjCurlRollDto::getLiningCode, Collectors.counting()));
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int rowNo = i + 2; // excel里的行号
            DjCurlRollDto sourceEntity = list.get(i);
			// excel内业务主键唯一校验
			if (groupMap.get(sourceEntity.getLiningCode()) > 1) {
				String columnName = I18nUtil.getMessage("ui.data.column.quota.liningCode");
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
            		DjCurlRoll importEntity = new DjCurlRoll();
            		importEntity.setLiningCode(sourceEntity.getLiningCode());
            		importEntity.setCurlLength(new BigDecimal(sourceEntity.getCurlLength()));
            		importEntity.setRemark(sourceEntity.getRemark());
                    List<ImportErrorLog> importValidated = ImportUtil.validated(importLogId, rowNo, importEntity);
                    if (CollectionUtils.isNotEmpty(importValidated)) { // 数字字段校验失败
                        sourceEntity.setId(-999L);
                        failureNum++;
                        importErrorLogs.addAll(importValidated);
                    } else { // 全部校验通过
                        DjCurlRoll targetEntity = new DjCurlRoll();
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
                    ncCurlRollMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        DjCurlRollDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        DjCurlRoll newItem = new DjCurlRoll();
                        newItem.setLiningCode(dto.getLiningCode());
                        newItem.setCurlLength(new BigDecimal(dto.getCurlLength()));
                        newItem.setRemark(dto.getRemark());
                        newItem.setBaseVale(null);

                        QueryWrapper<DjCurlRoll> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("LINING_CODE", newItem.getLiningCode());
                        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
                        List<DjCurlRoll> alreadyExistList = ncCurlRollMapper.selectList(queryWrapper);
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

    /**
     * 根据code查询卷曲长度
     *
     * @param curlRoll 查询条件
     * @return 结果
     */
    @Override
    public AjaxResult selectCurlLengthByCode(DjCurlRoll curlRoll) {
        LambdaQueryWrapper<DjCurlRoll> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DjCurlRoll::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(DjCurlRoll::getLiningCode, curlRoll.getQueryCode());
        DjCurlRoll data = ncCurlRollMapper.selectOne(queryWrapper);
        LambdaQueryWrapper<DjParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApsBaseEntity::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(DjParams::getParamCode, "STANDARD_CRIMP_LENGTH");
        DjParams params = paramsMapper.selectOne(wrapper);
        if (data == null) {
            data = new DjCurlRoll();
            data.setLiningCode(curlRoll.getQueryCode());
            data.setCurlLength(params == null ? new BigDecimal(DEFAULT_CURL_LENGTH) : new BigDecimal(params.getParamValue()));
        }
        return AjaxResult.success(data);
    }
}
