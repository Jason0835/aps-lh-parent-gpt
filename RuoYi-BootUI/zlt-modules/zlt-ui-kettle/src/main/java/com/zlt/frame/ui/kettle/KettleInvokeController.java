package com.zlt.frame.ui.kettle;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.kettle.api.domain.JobRecord;
import com.zlt.kettle.api.domain.TaskInfo;
import com.zlt.kettle.api.domain.TransRecord;
import com.zlt.kettle.api.service.IKettleProxyService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/kettle")
public class KettleInvokeController extends BaseController {

    private String prefix = "kettle";

    @Autowired
    IKettleProxyService iKettleProxyService;

    @GetMapping("/list")
    public String config()
    {
        return prefix + "/kettle";
    }
    @GetMapping("/jobLogList")
    public String jobLogList()
    {
        return prefix + "/jobLog";
    }
    @GetMapping("/transLogList")
    public String transLogList()
    {
        return prefix + "/transLog";
    }

    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TaskInfo taskInfo)
    {
        return iKettleProxyService.getTaskInfo(taskInfo);
    }

    @PostMapping("/jobLogList")
    @ResponseBody
    public TableDataInfo jobLog(JobRecord jobRecord)
    {
        return iKettleProxyService.getJobLogs(jobRecord);
    }

    @PostMapping("/transLogList")
    @ResponseBody
    public TableDataInfo transLog(TransRecord transRecord)
    {
        return iKettleProxyService.getTransLogs(transRecord);
    }

    @GetMapping({"/start/{taskType}/{id}"})
    @RequiresPermissions("kettle:task:start")
    @ResponseBody
    AjaxResult startTrans(@PathVariable("taskType") String taskType, @PathVariable("id") Integer id){
        return iKettleProxyService.startTrans(taskType, id);
    }

}
