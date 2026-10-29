package com.zlt.aps.lh.controller;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import com.zlt.aps.lh.api.domain.vo.LhScheduleCheckReportVO;
import com.zlt.aps.lh.service.ILhScheduleCheckService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 硫化排程结果合理性校验控制器（服务侧）。
 *
 * <p>返回值均为原始类型对象，与 Feign 类型化返回对齐；由 BootUI 侧包装统一响应体。</p>
 *
 * @author APS
 * @date 2026/10/27
 */
@RestController
@RequestMapping("/scheduleCheck")
@Api(tags = "硫化排程校验")
public class LhScheduleCheckController {

    /** 参数缺失提示多语言 key */
    private static final String PARAM_MISSING_KEY = "ui.data.alert.lhScheduleCheck.paramMissing";

    /** 日期格式 */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /** 校验服务 */
    @Autowired
    private ILhScheduleCheckService lhScheduleCheckService;

    /**
     * 查询校验报告
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（yyyy-MM-dd）
     * @return 校验报告
     */
    @ApiOperation("查询硫化排程校验报告")
    @GetMapping("/result")
    public LhScheduleCheckReportVO result(@RequestParam(value = "factoryCode", required = false) String factoryCode,
                                          @RequestParam(value = "scheduleDate", required = false) String scheduleDate) {
        Date parsedDate = this.parseScheduleDate(factoryCode, scheduleDate);
        return lhScheduleCheckService.getReport(factoryCode, parsedDate);
    }

    /**
     * 查询是否存在未确认异常
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（yyyy-MM-dd）
     * @return true-存在未确认异常
     */
    @ApiOperation("查询是否存在未确认校验异常")
    @GetMapping("/pending")
    public Boolean pending(@RequestParam(value = "factoryCode", required = false) String factoryCode,
                           @RequestParam(value = "scheduleDate", required = false) String scheduleDate) {
        Date parsedDate = this.parseScheduleDate(factoryCode, scheduleDate);
        return lhScheduleCheckService.hasPending(factoryCode, parsedDate);
    }

    /**
     * 确认（已了解）某工厂某业务日的校验异常
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（yyyy-MM-dd）
     * @return 操作结果
     */
    @ApiOperation("确认硫化排程校验异常")
    @PostMapping("/acknowledge")
    public AjaxResult acknowledge(@RequestParam(value = "factoryCode", required = false) String factoryCode,
                                  @RequestParam(value = "scheduleDate", required = false) String scheduleDate) {
        Date parsedDate = this.parseScheduleDate(factoryCode, scheduleDate);
        lhScheduleCheckService.acknowledge(factoryCode, parsedDate);
        return AjaxResult.success();
    }

    /**
     * 手动重新执行校验
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（yyyy-MM-dd）
     * @return 重新校验后的报告
     */
    @ApiOperation("重新执行硫化排程校验")
    @PostMapping("/rerun")
    public LhScheduleCheckReportVO rerun(@RequestParam(value = "factoryCode", required = false) String factoryCode,
                                         @RequestParam(value = "scheduleDate", required = false) String scheduleDate) {
        Date parsedDate = this.parseScheduleDate(factoryCode, scheduleDate);
        return lhScheduleCheckService.execute(
                factoryCode, parsedDate, LhScheduleCheckResult.TRIGGER_TYPE_MANUAL);
    }

    /**
     * 解析并校验排程日参数
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日字符串
     * @return 业务排程日
     */
    private Date parseScheduleDate(String factoryCode, String scheduleDate) {
        if (StringUtils.isBlank(factoryCode) || StringUtils.isBlank(scheduleDate)) {
            throw new ServiceException(I18nUtil.getMessage(PARAM_MISSING_KEY));
        }
        return DateUtil.parse(scheduleDate, DATE_PATTERN);
    }
}
