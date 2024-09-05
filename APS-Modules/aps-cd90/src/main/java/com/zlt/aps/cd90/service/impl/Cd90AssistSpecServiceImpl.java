package com.zlt.aps.cd90.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90AssistSpec;
import com.zlt.aps.cd90.mapper.Cd90AssistSpecMapper;
import com.zlt.aps.cd90.service.Cd90AssistSpecService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import org.apache.commons.collections4.CollectionUtils;
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
 * 90度裁断外协规格管理表 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Service
public class Cd90AssistSpecServiceImpl extends ServiceImpl<Cd90AssistSpecMapper, Cd90AssistSpec> implements Cd90AssistSpecService {

    @Resource
    private Cd90AssistSpecMapper cd90AssistSpecMapper;

    /**
     * 根据条件查询外协规格管理列表
     *
     * @return
     */
    public List<Cd90AssistSpec> listAssistSpec(Cd90AssistSpec dto) {
        return cd90AssistSpecMapper.listAssistSpec(dto);
    }

    /**
     * 保存外协规格管理信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveAssistSpec(Cd90AssistSpec entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        this.saveOrUpdate(entity);
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteAssistSpec(Long[] ids) {
        for (int i = 0; i < ids.length; i++) {
            Cd90AssistSpec entity = new Cd90AssistSpec();
            entity.setId(ids[i]);
            entity.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            entity.setUpdateTime(new Date());
            this.updateById(entity);
        }
    }

    /**
     * 根据code判断是否已经存在
     */
    public String checkAssistSpecCodeUnique(Cd90AssistSpec dto) {
        if (dto == null || StringUtils.isBlank(dto.getMaterialCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<Cd90AssistSpec> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("MATERIAL_CODE", dto.getMaterialCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<Cd90AssistSpec> list = cd90AssistSpecMapper.selectList(queryWrapper);
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
    public AjaxResult importData(List<Cd90AssistSpec> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<Cd90AssistSpec> importList = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(Cd90AssistSpec::getMaterialCode, Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            Cd90AssistSpec bigRoll = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(bigRoll.getMaterialCode());
            if (hasValue > 1) {
                failureNum++;
                bigRoll.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.common.column.assist.cd90.materialCode");
                message=String.format(message,columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, bigRoll);
            if (CollectionUtils.isEmpty(validated)) {
                bigRoll.setBaseVale(null);
                importList.add(bigRoll);
            } else {
                failureNum++;
                // 添加错误标识
                bigRoll.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用merge即可
                if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    successNum = importList.size();
                    cd90AssistSpecMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        Cd90AssistSpec excelItem = list.get(i);
                        if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                            continue;
                        }
                        // 唯一性校验
                        String unique = checkAssistSpecCodeUnique(excelItem);
                        if (UserConstants.UNIQUE.equals(unique)) {
                            //不存在插入
                            successNum++;
                            Cd90AssistSpec cd90AssistSpec = new Cd90AssistSpec();
                            BeanUtils.copyProperties(excelItem, cd90AssistSpec);
                            cd90AssistSpecMapper.insert(cd90AssistSpec);
                        } else {
                            // 存在，插入错误详细日志
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2,
                                    I18nUtil.getMessage("ui.cd90.assistSpec.alter.isAssistSpecExist"), importErrorLogs);
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
