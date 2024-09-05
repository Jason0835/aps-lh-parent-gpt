package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupList;
import com.zlt.aps.cx.mapper.CxMachineGroupListMapper;
import com.zlt.aps.cx.service.CxMachineGroupListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 组别机台列Service业务层处理
 *
 * @author zlt
 * @date 2021-12-16
 */
@Service
public class CxMachineGroupListServiceImpl implements CxMachineGroupListService {
    @Autowired
    private CxMachineGroupListMapper cxMachineGroupListMapper;

    /**
     * 查询组别机台列
     *
     * @param id 组别机台列ID
     * @return 组别机台列
     */
    @Override
    public CxMachineGroupList selectCxMachineGroupListById(Long id) {
        return cxMachineGroupListMapper.selectCxMachineGroupListById(id);
    }

    /**
     * 查询组别机台列列表
     *
     * @param cxMachineGroupList 组别机台列
     * @return 组别机台列
     */
    @Override
    public List<CxMachineGroupList> selectCxMachineGroupListList(CxMachineGroupList cxMachineGroupList) {
        return cxMachineGroupListMapper.selectCxMachineGroupListList(cxMachineGroupList);
    }

    public List<CxMachineGroupList> selectCxMachineGroupListList4MachineName(CxMachineGroupList cxMachineGroupList){
        return cxMachineGroupListMapper.selectCxMachineGroupListList4MachineName(cxMachineGroupList);
    }

    /**
     * 新增组别机台列
     *
     * @param cxMachineGroupList 组别机台列
     * @return 结果
     */
    @Override
    public int insertCxMachineGroupList(CxMachineGroupList cxMachineGroupList) {
        cxMachineGroupList.setBaseVale(null);
        return cxMachineGroupListMapper.insertCxMachineGroupList(cxMachineGroupList);
    }

    /**
     * 修改组别机台列
     *
     * @param cxMachineGroupList 组别机台列
     * @return 结果
     */
    @Override
    public int updateCxMachineGroupList(CxMachineGroupList cxMachineGroupList) {
        cxMachineGroupList.setBaseVale(cxMachineGroupList.getId());
        return cxMachineGroupListMapper.updateCxMachineGroupList(cxMachineGroupList);
    }

    /**
     * 批量删除组别机台列
     *
     * @param ids 需要删除的组别机台列ID
     * @return 结果
     */
    @Override
    public int deleteCxMachineGroupListByIds(Long[] ids) {
        return cxMachineGroupListMapper.deleteCxMachineGroupListByIds(ids);
    }

    /**
     * 删除组别机台列信息
     *
     * @param id 组别机台列ID
     * @return 结果
     */
    @Override
    public int deleteCxMachineGroupListById(Long id) {
        return cxMachineGroupListMapper.deleteCxMachineGroupListById(id);
    }

    /**
     * 校验组别机台列唯一性
     */
    @Override
    public List<CxMachineGroupList> checkCxMachineGroupListUnique(CxMachineGroupList cxMachineGroupList) {
        List<CxMachineGroupList> list = cxMachineGroupListMapper.checkCxMachineGroupListUnique(cxMachineGroupList);
        return list;
    }

    /**
     * 导入组别机台列数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<CxMachineGroupList> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxMachineGroupList> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxMachineGroupList cxMachineGroupList = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, cxMachineGroupList);
            if (CollectionUtils.isNotEmpty(validated)) {
                cxMachineGroupList.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                cxMachineGroupList.setBaseVale(null);
                importList.add(cxMachineGroupList);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                cxMachineGroupListMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    CxMachineGroupList cxMachineGroupList = list.get(i);
                    // 错误记录跳过
                    if (cxMachineGroupList.getId() != null && cxMachineGroupList.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = "this.checkCxMachineGroupListUnique(cxMachineGroupList)";
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertCxMachineGroupList(cxMachineGroupList);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("此处需手动填写唯一校验失败国际化信息"), importErrorLogs);
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
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
