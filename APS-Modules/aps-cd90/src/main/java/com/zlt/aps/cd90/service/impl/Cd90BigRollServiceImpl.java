package com.zlt.aps.cd90.service.impl;


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
import com.zlt.aps.cd90.api.domain.dto.Cd90BigRollDto;
import com.zlt.aps.cd90.entity.Cd90BigRoll;
import com.zlt.aps.cd90.entity.Cd90SpecifyMachine;
import com.zlt.aps.cd90.mapper.Cd90BigRollMapper;
import com.zlt.aps.cd90.service.Cd90BigRollService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.core.utils.ImportUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 90度裁断帘布大卷信息表 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Service
public class Cd90BigRollServiceImpl extends ServiceImpl<Cd90BigRollMapper, Cd90BigRoll> implements Cd90BigRollService {

    @Resource
    private Cd90BigRollMapper cd90BigRollMapper;

    /**
     * 根据条件查询帘布大卷信息列表
     *
     * @return
     */
    public List<Cd90BigRollDto> listBigRoll(Cd90BigRollDto dto) {
        return cd90BigRollMapper.listBigRoll(dto);
    }

    /**
     * 保存帘布大卷信息信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveBigRoll(Cd90BigRoll entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        this.saveOrUpdate(entity);
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteBigRoll(Long[] ids) {
        LambdaUpdateWrapper<Cd90BigRoll> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApsBaseEntity::getId, Arrays.asList(ids));
        wrapper.set(ApsBaseEntity::getDelFlag, null);
        wrapper.set(ApsBaseEntity::getUpdateBy, SecurityUtils.getUsername());
        wrapper.set(ApsBaseEntity::getUpdateTime, new Date());
        super.getBaseMapper().update(null, wrapper);
    }

    /**
     * 根据code判断帘布大卷是否已经存在
     */
    public String checkBigRollCodeUnique(Cd90BigRollDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getBigRollCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<Cd90BigRoll> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("BIG_ROLL_CODE", dto.getBigRollCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<Cd90BigRoll> list = cd90BigRollMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<Cd90BigRollDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<Cd90BigRoll> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(Cd90BigRollDto::getBigRollCode, Collectors.counting()));
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            Cd90BigRollDto sourceEntity = list.get(i);

			// excel内业务主键唯一校验
			if (groupMap.get(sourceEntity.getBigRollCode()) > 1) {
				String columnName = I18nUtil.getMessage("ui.common.column.lb.bigRollCode");
                sourceEntity.setId(-999L);
				addImportErrorLog(importLogId, i + 2,
						String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"), columnName),
						importErrorLogs);
                failureNum++;
				continue;
			}
            
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, sourceEntity);
            if (CollectionUtils.isNotEmpty(validated)) {
                sourceEntity.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                Cd90BigRoll targetEntity = new Cd90BigRoll();
                BeanUtils.copyProperties(sourceEntity, targetEntity);
                targetEntity.setBaseVale(null);
                newList.add(targetEntity);
            }
        }
        
        //新集合操作（更新或插入操作）
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用mergeOrInsert
                if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                    successNum = newList.size();
                    cd90BigRollMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        Cd90BigRollDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        Cd90BigRoll newItem = new Cd90BigRoll();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setBaseVale(null);

                        QueryWrapper<Cd90BigRoll> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("BIG_ROLL_CODE", newItem.getBigRollCode());
                        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
                        List<Cd90BigRoll> alreadyExistList = cd90BigRollMapper.selectList(queryWrapper);
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
