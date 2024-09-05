package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.SysPost;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 岗位管理对外暴露接口
 */
@FeignClient(contextId = "iSysPostService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysPostService {

    /**
     * 获取岗位列表
     * @param post
     * @return
     */
    @GetMapping("/post/list")
    TableDataInfo list(@SpringQueryMap SysPost post);

    @GetMapping("/post/totalList")
    List<SysPost> totalList(@SpringQueryMap SysPost role);

    /*@PostMapping("/post/export")
    void export(HttpServletResponse response, SysPost post) throws IOException;*/

    /**
     * 根据岗位编号获取详细信息
     * @param postId
     * @return
     */
    @GetMapping(value = "/post/{postId}")
    AjaxResult getInfo(@PathVariable("postId") Long postId);

    /**
     * 新增岗位
     * @param post
     * @return
     */
    @PostMapping("/post")
    AjaxResult add(@Validated @RequestBody SysPost post);

    /**
     * 修改岗位
     * @param post
     * @return
     */
    @PutMapping("/post")
    AjaxResult edit(@Validated @RequestBody SysPost post);

    /**
     * 删除岗位
     * @param postIds
     * @return
     */
    @DeleteMapping("/post/{postIds}")
    AjaxResult remove(@PathVariable("postIds") Long[] postIds);

    /**
     * 获取岗位选择框列表
     * @return
     */
    @GetMapping("/post/optionselect")
    AjaxResult optionselect();

    /**
     * 根据岗位ID获取岗位信息
     * @param postId
     * @return
     */
    @PostMapping("/post/selectPostById")
    SysPost selectPostById(@RequestParam("postId")Long postId);

    /**
     * 校验岗位名称
     */
    @PostMapping("/post/checkPostNameUnique")
    String checkPostNameUnique(@RequestBody SysPost post);

    /**
     * 校验岗位编码
     * @param post
     * @return
     */
    @PostMapping("/post/checkPostCodeUnique")
    String checkPostCodeUnique(@RequestBody SysPost post);

    /**
     * 根据用户id获取岗位列表
     * @param userId
     * @return
     */
    @PostMapping("/post/selectPostsByUserId")
    public List<SysPost> selectPostsByUserId(@RequestParam("userId") Long userId);
}
