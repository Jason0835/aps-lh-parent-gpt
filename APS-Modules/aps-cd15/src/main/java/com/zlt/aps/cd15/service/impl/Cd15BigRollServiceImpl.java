package com.zlt.aps.cd15.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.dto.Cd15BigRollDto;
import com.zlt.aps.cd15.entity.Cd15BigRoll;
import com.zlt.aps.cd15.mapper.Cd15BigRollMapper;
import com.zlt.aps.cd15.service.Cd15BigRollService;
import com.zlt.aps.cd15.service.Cd15MachineInfoService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.core.utils.ImportUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 15度裁断钢压大卷信息表 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Service
public class Cd15BigRollServiceImpl extends ServiceImpl<Cd15BigRollMapper, Cd15BigRoll> implements Cd15BigRollService {

    @Resource
    private Cd15BigRollMapper cd15BigRollMapper;
    @Autowired
    private Cd15MachineInfoService cd15MachineInfoService;

    /**
     * 根据条件查询钢压大卷信息列表
     *
     * @return
     */
    public List<Cd15BigRollDto> listBigRoll(Cd15BigRollDto dto) {
        return cd15BigRollMapper.listBigRoll(dto);
    }

    /**
     * 保存钢压大卷信息信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveBigRoll(Cd15BigRoll entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        this.saveOrUpdate(entity);
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteBigRoll(Long[] ids) {
        LambdaUpdateWrapper<Cd15BigRoll> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApsBaseEntity::getId, Arrays.asList(ids));
        wrapper.set(ApsBaseEntity::getDelFlag, null);
        wrapper.set(ApsBaseEntity::getUpdateBy, SecurityUtils.getUsername());
        wrapper.set(ApsBaseEntity::getUpdateTime, new Date());
        super.getBaseMapper().update(null, wrapper);
    }

    /**
     * 根据code判断钢压大卷是否已经存在
     */
    public String checkBigRollCodeUnique(Cd15BigRollDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getBigRollCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<Cd15BigRoll> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("BIG_ROLL_CODE", dto.getBigRollCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<Cd15BigRoll> list = cd15BigRollMapper.selectList(queryWrapper);
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
    public AjaxResult importData(List<Cd15BigRollDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<Cd15BigRollDto> importList = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getBigRollCode(), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            Cd15BigRollDto bigRollDto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(bigRollDto.getBigRollCode());
            if (hasValue > 1) {
                failureNum++;
                bigRollDto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.common.column.gy.bigRollCode");
                message=String.format(message,columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, bigRollDto);
            if (CollectionUtils.isEmpty(validated)) {
                bigRollDto.setBaseVale(null);
                importList.add(bigRollDto);
            } else {
                failureNum++;
                // 添加错误标识
                bigRollDto.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用merge即可
                if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    successNum = importList.size();
                    cd15BigRollMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        Cd15BigRollDto excelItem = list.get(i);
                        if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                            continue;
                        }
                        // 唯一性校验
                        String unique = checkBigRollCodeUnique(excelItem);
                        if (UserConstants.UNIQUE.equals(unique)) {
                            //不存在插入
                            successNum++;
                            Cd15BigRoll cd15BigRoll = new Cd15BigRoll();
                            BeanUtils.copyProperties(excelItem, cd15BigRoll);
                            cd15BigRollMapper.insert(cd15BigRoll);
                        } else {
                            // 存在，插入错误详细日志
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2,
                                    I18nUtil.getMessage("ui.bigRoll.message.unique"), importErrorLogs);
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
}
