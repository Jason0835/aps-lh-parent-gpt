//package com.zlt.aps.cx.controller;
//
//
//import com.ruoyi.common.core.web.controller.BaseController;
//import com.ruoyi.common.core.web.domain.AjaxResult;
//import com.zlt.aps.cxlh.cx.api.domain.dto.ScheduleRequestDTO;
//import com.zlt.aps.cxlh.cx.api.domain.dto.ScheduleResponseDTO;
//import com.zlt.aps.cxlh.cx.api.domain.vo.ScheduleVO;
//import lombok.RequiredArgsConstructor;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Date;
//import java.util.List;
//
///**
// * 成型排程控制器
// */
//@RestController
//@RequestMapping("/cx/schedule")
//@RequiredArgsConstructor
//public class CxScheduleController extends BaseController {
//
//    private final CxScheduleService cxScheduleService;
//
//    /**
//     * 执行成型排程
//     */
//    @PostMapping("/execute")
//    public AjaxResult executeSchedule(@RequestBody ScheduleRequestDTO request) {
//        ScheduleResponseDTO response = cxScheduleService.executeSchedule(request);
//        return AjaxResult.success(response);
//    }
//
//    /**
//     * 获取排程预览
//     */
//    @GetMapping("/preview")
//    public AjaxResult getSchedulePreview(
//            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate) {
//        List<ScheduleVO> list = cxScheduleService.getSchedulePreview(scheduleDate);
//        return AjaxResult.success(list);
//    }
//
//    /**
//     * 发布排程计划
//     */
//    @PostMapping("/publish")
//    public AjaxResult publishSchedule(
//            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate,
//            @RequestParam String versionNo) {
//        boolean result = cxScheduleService.publishSchedule(scheduleDate, versionNo);
//        return result ? AjaxResult.success() : AjaxResult.error("发布失败");
//    }
//
//    /**
//     * 调整排程计划
//     */
//    @PostMapping("/adjust")
//    public AjaxResult adjustSchedule(@RequestBody ScheduleRequestDTO request) {
//        ScheduleResponseDTO response = cxScheduleService.adjustSchedule(request);
//        return AjaxResult.success(response);
//    }
//
//    /**
//     * 检查数据完整性
//     */
//    @GetMapping("/check")
//    public AjaxResult checkDataIntegrity(
//            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate) {
//        DataCheckResult result = cxScheduleService.checkDataIntegrity(scheduleDate);
//        return AjaxResult.success(result);
//    }
//}
