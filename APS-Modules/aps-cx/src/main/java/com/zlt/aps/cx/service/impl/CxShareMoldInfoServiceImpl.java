package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.CxShareMoldInfo;
import com.zlt.aps.cx.mapper.CxShareMoldInfoMapper;
import com.zlt.aps.cx.service.CxShareMoldInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 成型胎胚共用模具信息Service业务层处理
 *
 * @author chen
 * @date 2022-03-22
 */
@Service
public class CxShareMoldInfoServiceImpl implements CxShareMoldInfoService {
    @Autowired
    private CxShareMoldInfoMapper cxShareMoldInfoMapper;

    /**
     * 查询成型胎胚共用模具信息
     *
     * @param id 成型胎胚共用模具信息ID
     * @return 成型胎胚共用模具信息
     */
    @Override
    public CxShareMoldInfo selectCxShareMoldInfoById(Long id) {
        return cxShareMoldInfoMapper.selectCxShareMoldInfoById(id);
    }

    /**
     * 查询成型胎胚共用模具信息列表
     *
     * @param cxShareMoldInfo 成型胎胚共用模具信息
     * @return 成型胎胚共用模具信息
     */
    @Override
    public List<CxShareMoldInfo> selectCxShareMoldInfoList(CxShareMoldInfo cxShareMoldInfo) {
        return cxShareMoldInfoMapper.selectCxShareMoldInfoList(cxShareMoldInfo);
    }

    /**
     * 新增成型胎胚共用模具信息
     *
     * @param cxShareMoldInfo 成型胎胚共用模具信息
     * @return 结果
     */
    @Override
    public int insertCxShareMoldInfo(CxShareMoldInfo cxShareMoldInfo) {
        cxShareMoldInfo.setBaseVale(null);
        return cxShareMoldInfoMapper.insertCxShareMoldInfo(cxShareMoldInfo);
    }

    /**
     * 修改成型胎胚共用模具信息
     *
     * @param cxShareMoldInfo 成型胎胚共用模具信息
     * @return 结果
     */
    @Override
    public int updateCxShareMoldInfo(CxShareMoldInfo cxShareMoldInfo) {
        cxShareMoldInfo.setBaseVale(cxShareMoldInfo.getId());
        return cxShareMoldInfoMapper.updateCxShareMoldInfo(cxShareMoldInfo);
    }

    /**
     * 批量删除成型胎胚共用模具信息
     *
     * @param ids 需要删除的成型胎胚共用模具信息ID
     * @return 结果
     */
    @Override
    public int deleteCxShareMoldInfoByIds(Long[] ids) {
        return cxShareMoldInfoMapper.deleteCxShareMoldInfoByIds(ids);
    }

    /**
     * 删除成型胎胚共用模具信息信息
     *
     * @param id 成型胎胚共用模具信息ID
     * @return 结果
     */
    @Override
    public int deleteCxShareMoldInfoById(Long id) {
        return cxShareMoldInfoMapper.deleteCxShareMoldInfoById(id);
    }

    /**
     * 校验成型胎胚共用模具信息唯一性
     */
    @Override
    public String checkCxShareMoldInfoUnique(CxShareMoldInfo cxShareMoldInfo) {
        if (cxShareMoldInfo == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int num = cxShareMoldInfoMapper.checkUnique(cxShareMoldInfo);
        if (num > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入成型胎胚共用模具信息数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<CxShareMoldInfo> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxShareMoldInfo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(item -> item.getGroupName() + item.getEmbryoCode(), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxShareMoldInfo cxShareMoldInfo = list.get(i);
            //重复记录校验
            Long hasValue = groupMap.get(cxShareMoldInfo.getGroupName() + cxShareMoldInfo.getEmbryoCode());
            if (hasValue > 1) {
                failureNum++;
                cxShareMoldInfo.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName1 = I18nUtil.getMessage("ui.data.column.shareMoldInfo.groupName");
                String columnName2 = I18nUtil.getMessage("ui.construction.embryoCode");
                message=String.format(message, columnName1 + "," + columnName2);
                addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, cxShareMoldInfo);
            if (CollectionUtils.isNotEmpty(validated)) {
                cxShareMoldInfo.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                cxShareMoldInfo.setBaseVale(null);
                importList.add(cxShareMoldInfo);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                cxShareMoldInfoMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    CxShareMoldInfo cxShareMoldInfo = list.get(i);
                    // 错误记录跳过
                    if (cxShareMoldInfo.getId() != null && cxShareMoldInfo.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkCxShareMoldInfoUnique(cxShareMoldInfo);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertCxShareMoldInfo(cxShareMoldInfo);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("mes.error.message.cxShareMoldInfo.exist"), importErrorLogs);
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

    /**
     * 根据胎胚代码查询所属组别对应的所有胎胚代码信息（排除传入胎胚）
     *
     * @param embryoCode 胎胚代码
     * @return 查询到的共用模具信息
     */
    @Override
    public List<CxShareMoldInfo> selectShareMoldInfoListByEmbryoCode(String embryoCode) {
        return cxShareMoldInfoMapper.selectShareMoldInfoListByEmbryoCode(embryoCode);
    }

    /**
     * 根据sap品号查询所属组别对应的所有sap品号信息（排除传入sap品号）
     * @param sapCode sap品号
     * @return 查询到的共用模具信息
     */
    @Override
    public List<CxShareMoldInfo> selectShareMoldInfoListBySapCode(String sapCode) {
        return cxShareMoldInfoMapper.selectShareMoldInfoListBySapCode(sapCode);
    }

    /**
     * 查询共用模具信息，并根据sap品号及胎胚关联硫化外胎施工表，获取规格规格信息
     *
     * @return 查询到的集合数据（sap品号、胎胚代码、规格型号）
     */
    @Override
    public List<CxShareMoldInfo> selectShareMoldInfoList() {
        return cxShareMoldInfoMapper.selectShareMoldInfoList();
    }
}
