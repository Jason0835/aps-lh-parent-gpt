package com.zlt.aps.lh.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.common.handle.LhSyncDataHandle;
import com.zlt.aps.lh.engine.service.LhEngineService;
import com.zlt.aps.lh.mapper.LhMoldChangePlanMapper;
import com.zlt.aps.lh.service.LhMachineInfoService;
import com.zlt.aps.lh.service.LhMoldChangePlanService;
import com.zlt.aps.lh.vo.MoldPlanPublishRecordVo;
import com.zlt.sync.povo.SyncParamsVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 模具变动单Service业务层处理
 *
 * @author zlt
 * @date 2021-06-17
 */
@Service
public class LhMoldChangePlanServiceImpl implements LhMoldChangePlanService {
    @Autowired
    private LhMoldChangePlanMapper lhMoldChangePlanMapper;

    @Autowired
    private LhMachineInfoService lhMachineInfoService;

    @Autowired
    private LhEngineService lhEngineService;

    @Resource
    private LhSyncDataHandle syncDataHandle;

    /**
     * 查询模具变动单
     *
     * @param id 模具变动单ID
     * @return 模具变动单
     */
    @Override
    public LhMoldChangePlan selectLhMoldChangePlanById(Long id) {
        return lhMoldChangePlanMapper.selectLhMoldChangePlanById(id);
    }

    /**
     * 查询模具变动单列表
     *
     * @param lhMoldChangePlan 模具变动单
     * @return 模具变动单
     */
    @Override
    public List<LhMoldChangePlan> selectLhMoldChangePlanList(LhMoldChangePlan lhMoldChangePlan) {
        return lhMoldChangePlanMapper.selectLhMoldChangePlanList(lhMoldChangePlan);
    }

    /**
     * 新增模具变动单
     *
     * @param lhMoldChangePlan 模具变动单
     * @return 结果
     */
    @Override
    public AjaxResult insertLhMoldChangePlan(LhMoldChangePlan lhMoldChangePlan) {
        lhMoldChangePlan.setCreateTime(DateUtils.getNowDate());
        ValidateResult validateResult=lhEngineService.insertChangePlanTask(lhMoldChangePlan);
        if (!validateResult.isSuccess()) {
            return AjaxResult.error(validateResult.getMsg());
        }
         lhMoldChangePlanMapper.insertLhMoldChangePlan(lhMoldChangePlan);
        return AjaxResult.success();
    }

    /**
     * 修改模具变动单
     *
     * @param lhMoldChangePlan 模具变动单
     * @return 结果
     */
    @Override
    public int updateLhMoldChangePlan(LhMoldChangePlan lhMoldChangePlan) {
        lhMoldChangePlan.setUpdateTime(DateUtils.getNowDate());
        return lhMoldChangePlanMapper.updateLhMoldChangePlan(lhMoldChangePlan);
    }

    /**
     * 批量删除模具变动单
     *
     * @param ids 需要删除的模具变动单ID
     * @return 结果
     */
    @Override
    public int deleteLhMoldChangePlanByIds(Long[] ids) {
        return lhMoldChangePlanMapper.deleteLhMoldChangePlanByIds(ids);
    }

    /**
     * 删除模具变动单信息
     *
     * @param id 模具变动单ID
     * @return 结果
     */
    @Override
    public int deleteLhMoldChangePlanById(Long id) {
        return lhMoldChangePlanMapper.deleteLhMoldChangePlanById(id);
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<LhMoldChangePlan> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhMoldChangePlan> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<LhMachineInfo> machineInfoList = lhMachineInfoService.selectMachineInfoList(new LhMachineInfo());
        Map<String, String> machineCodeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineInfoList)) {
            machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineCode(), a.getMachineCode()));
        }
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            LhMoldChangePlan dto = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                LhMoldChangePlan newEntity = new LhMoldChangePlan();
                BeanUtils.copyProperties(dto, newEntity);
                newEntity.setLhMachineCode(machineCodeMap.get(dto.getLhMachineName()));
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
                    lhMoldChangePlanMapper.mergeSql(newList);
                } else {
                    for (int i = 0; i < list.size(); i++) {
                        LhMoldChangePlan newItem = list.get(i);
                        if (newItem.getId() != null && newItem.getId() == -999L) {
                            continue;
                        }
                        newItem.setLhMachineCode(machineCodeMap.get(newItem.getLhMachineName()));
                        newItem.setBaseVale(null);
                        successNum++;
                        lhMoldChangePlanMapper.insertLhMoldChangePlan(newItem);
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
	 * 发布
	 * @param ids	待发布模具变动单id
	 * @param scheduleDate	排程日期
	 * @return
	 */
    @Override
    @Transactional
	public AjaxResult publish(long[] ids, Date scheduleDate) {
		// 获取数据版本号
		String dataVersion = syncDataHandle.getDataVersion(ApsConstant.MOULD_CHANGE_FBK);
		// 将数据发布至中间库
		lhMoldChangePlanMapper.deployMoldChangePlanToMes(dataVersion, ids);
		// 更新发布状态
		lhMoldChangePlanMapper.updateRelease(ids);
		// 记录发布日志
		MoldPlanPublishRecordVo record = new MoldPlanPublishRecordVo();
		record.setBaseVale(null);
		record.setPublishStatus(ApsConstant.IS_RELEASE);
		record.setPublishDate(DateUtils.getNowDate());
		lhMoldChangePlanMapper.insertPublishRecord(record);
		
		// 通知接口服务
		SyncParamsVO syncParamsVO = new SyncParamsVO();
		syncParamsVO.setSyncKey(ApsConstant.MOULD_CHANGE_FBK);
		syncParamsVO.setDataVersion(dataVersion);
        // 请求参数
        JSONObject params = new JSONObject();
        params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate));
        syncParamsVO.setParams(params);
		syncDataHandle.syncNotice(syncParamsVO);
		
		return AjaxResult.success(I18nUtil.getMessage("ui.lh.moldChange.successPublish"));
	}
}
