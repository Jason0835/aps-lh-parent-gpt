package com.zlt.aps.controller.gdyy;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.gdyy.api.domain.entity.GdyyDispatcherLog;
import com.zlt.aps.gdyy.api.service.IGdyyDispatcherLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 钢带压延调度员排程操作日志Controller
 * @author Gim
 * @date 2022-02-25
 */
@Api(tags = "钢带压延调度员排程操作日志")
@Controller
@RequestMapping("/gdyy/dispatcherLog")
public class GdyyDispatcherLogController extends BaseController {

    @Autowired
    private IGdyyDispatcherLogService dispatcherLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    private final String prefix = "gdyy/dispatcherLog";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("gdyy:dispatcherLog:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/log";
    }


    /**
     * 根据条件查询钢带压延调度员排程操作日志列表
     */
    @ApiOperation("根据条件查询钢带压延调度员排程操作日志列表")
    @RequiresPermissions("gdyy:dispatcherLog:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GdyyDispatcherLog entity) {
        return dispatcherLogService.list(entity);
    }

    /**
     * 导出调度员排程操作日志列表
     */
    @ApiOperation("导出调度员排程操作日志列表")
    @RequiresPermissions("gdyy:dispatcherLog:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GdyyDispatcherLog dispatcherLog) throws IOException {
        Map<String, String> operationTypeDictMap = iSysDictDataCacheService.getType("DISPATCHER_OPER_TYPE").stream()
                .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        dispatcherLog.setOperationTypeDictMap(operationTypeDictMap);
        byte[] data = dispatcherLogService.export(dispatcherLog);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.data.column.gdyy.dispatcherlog.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, dispatcherLog.toString(), ApsConstant.PROCEDURE_CODE_GDYY);
        iExportLogService.add(exportLog);
    }
}
