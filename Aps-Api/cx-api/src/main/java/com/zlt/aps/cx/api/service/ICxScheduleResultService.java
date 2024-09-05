package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxChangeLhMachine;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.Gante;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 成型排程结果Service接口
 *
 * @author zlt
 * @date 2021-07-12
 */
@FeignClient(contextId = "iCxScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxScheduleResultService {


    /**
     * 查询成型排程结果列表
     */
    @PostMapping("/cxScheduleResult/list")
    TableDataInfo list(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 查询成型排程结果列表
     */
    @PostMapping("/cxScheduleResult/finishedList")
    TableDataInfo finishedList(@RequestBody CxScheduleResult cxScheduleResult);


    /**
     * 插单校验
     */
    @PostMapping("/cxScheduleResult/validateAdd")
    AjaxResult validateAdd(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 转机台校验校验
     */
    @PostMapping("/cxScheduleResult/validateChangeMachine")
    AjaxResult validateChangeMachine(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 成型插单
     */
    @PostMapping("/cxScheduleResult/add")
    AjaxResult add(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 计算半部件调量参考值
     */
    @PostMapping("/cxScheduleResult/qtyReference")
    AjaxResult qtyReference(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 转机台
     */
    @PostMapping("/cxScheduleResult/changeMachine")
    AjaxResult changeMachine(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 修改成型排程结果
     */
    @PostMapping("/cxScheduleResult/edit")
    AjaxResult edit(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 修改成型排程结果
     */
    @PostMapping("/cxScheduleResult/modifyStatus")
    AjaxResult modifyStatus(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 修改成型排程结果
     */
    @PostMapping("/cxScheduleResult/updateCxScheduleResult")
    AjaxResult updateCxScheduleResult(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 调量更新
     */
    @PostMapping("/cxScheduleResult/changeQty")
    AjaxResult changeQty(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 修改施工版本
     */
    @PostMapping("/cxScheduleResult/changeBomDataVersion")
    AjaxResult changeBomDataVersion(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 删除成型排程结果
     */
    @DeleteMapping("/cxScheduleResult/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 收尾成型排程结果
     */
    @GetMapping("/cxScheduleResult/manualClose/{ids}")
    AjaxResult manualClose(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/cxScheduleResult/{id}")
    CxScheduleResult getInfo(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/cxScheduleResult/getInfoForQty/{id}")
    CxScheduleResult getInfoForQty(@PathVariable("id") Long id);


    /**
     * 成型排程结果列表
     */
    @PostMapping("/cxScheduleResult/getList")
    List<CxScheduleResult> getList(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 查询非本id的且包含该硫化机的记录
     */
    @PostMapping("/cxScheduleResult/getListByLhMachineCode")
    List<CxScheduleResult> getListByLhMachineCode(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 硫化自动排程校验
     */
    @PostMapping("/cxScheduleResult/getLhList")
    List<CxScheduleResult> getLhList(@RequestBody CxScheduleResult cxScheduleResult);


    /**
     * 导出列表
     */
    @PostMapping("/cxScheduleResult/export")
    byte[] export(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 自动排程
     */
    @PostMapping("/cxScheduleResult/autoPlan")
    AjaxResult autoPlan(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 硫化自动排程
     */
    @PostMapping("/cxScheduleResult/lhAutoPlan")
    AjaxResult lhAutoPlan(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 生成模具变动单校验
     */
    @PostMapping("/cxScheduleResult/modelChangeValidate")
    AjaxResult modelChangeValidate(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 生成模具变动单
     */
    @PostMapping("/cxScheduleResult/modelChange")
    AjaxResult modelChange(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 生成模具调整计划
     */
    @PostMapping("/cxScheduleResult/modelAdjustPlan")
    AjaxResult modelAdjustPlan(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 排程发布校验
     */
    @PostMapping("/cxScheduleResult/publishValidate")
    AjaxResult publishValidate(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 排程发布
     */
    @PostMapping("/cxScheduleResult/publish")
    AjaxResult publish(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/cxScheduleResult/isCxPublish")
    Boolean isCxPublish(@RequestBody CxScheduleResult entity);

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/cxScheduleResult/isLhPublish")
    Boolean isLhPublish(@RequestBody CxScheduleResult entity);

    /**
     * 导入数据
     */
    @PostMapping("/cxScheduleResult/importData")
    public AjaxResult importData(@RequestBody List<CxScheduleResult> list, @RequestParam("importLogId") Long importLogId,@RequestParam("scheduleDate")String scheduleDate);

    /**
     * 获取-使用模数
     */
    @PostMapping("/cxScheduleResult/getMolds")
    CxScheduleResult getMolds(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 校验-使用模数
     */
    @PostMapping("/cxScheduleResult/modifyMoldsValidate")
    AjaxResult modifyMoldsValidate(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 修改-使用模数
     */
    @PostMapping("/cxScheduleResult/modifyMolds")
    AjaxResult modifyMolds(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 在产下发MPS
     */
    @PostMapping("/cxScheduleResult/producingIssue")
    public AjaxResult producingIssue(@RequestBody CxScheduleResult entity);

    /**
     * 单机自动排程校验
     */
    @PostMapping("/cxScheduleResult/singleMachinAutoPlanValidate")
    List<CxScheduleResult> singleMachinAutoPlanValidate(@RequestBody CxScheduleResult cxScheduleResult);

    @PostMapping("/cxScheduleResult/checkScheduleResultUnique")
    public List<CxScheduleResult> checkScheduleResultUnique(CxScheduleResult cxScheduleResult);

    /**
     * 增补计划校验
     * @param entity 校验日期
     * @return 结果
     */
    @PostMapping("/cxScheduleResult/autoScheduleValidateSupplePlanByScheduleDate")
    public AjaxResult autoScheduleValidateSupplePlanByScheduleDate(@RequestBody CxScheduleResult entity);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录(成型排程结果)
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/cxScheduleResult/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody CxScheduleResult scheduleResult);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录(硫化排程结果)
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/cxScheduleResult/lhIsReleasingOrTimeoutByDate")
    public int lhIsReleasingOrTimeoutByDate(@RequestBody CxScheduleResult scheduleResult);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param ids id
     * @return 查询到的记录数
     */
    @PostMapping("/cxScheduleResult/isReleasingOrTimeoutByIds/{ids}")
    public int isReleasingOrTimeoutByIds(@PathVariable("ids") Long[] ids);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @PostMapping("/cxScheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody CxScheduleResult entity);

    /**
     * 验证选中的排程信息进行施工信息
     */
    @PostMapping("/cxScheduleResult/validateConstructionByIds")
    AjaxResult validateConstructionByIds(@RequestParam("ids") Long[] ids);

    /**
     * 查询成型排程最新排程日期
     * @return 最新排程日期
     */
    @PostMapping("/cxScheduleResult/selectMaxScheduleDate")
    public String selectMaxScheduleDate();

    /**
     * 查询成型排程硫化机台更换类型集合
     * @param cxChangeLhMachine 参数：工单号
     */
    @PostMapping("/cxScheduleResult/listCxChangeLhMachine")
    public TableDataInfo listCxChangeLhMachine(@RequestBody CxChangeLhMachine cxChangeLhMachine);

    /**
     * 调量校验
     */
    @PostMapping("/cxScheduleResult/validateChangeQty")
    public AjaxResult validateChangeQty(@RequestBody CxScheduleResult entity);

    /**
     * 查询成型排程机台甘特图数据
     */
    @PostMapping("/cxScheduleResult/getCxGanteData")
    public List<Gante> getCxGanteData(@RequestBody Gante gante);
}
