package com.ruoyi.system.controller;

import com.ruoyi.api.gateway.system.domain.SysPost;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 岗位信息操作处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/post")
public class SysPostController extends BaseController
{
    @Autowired
    private ISysPostService postService;

    /**
     * 获取岗位列表
     */
    @PreAuthorize(hasPermi = "system:post:list")
    @GetMapping("/list")
    public TableDataInfo list(SysPost post)
    {
        startPage();
        List<SysPost> list = postService.selectPostList(post);
        return getDataTable(list);
    }

    @PreAuthorize(hasPermi = "system:post:list")
    @GetMapping("/totalList")
    public List<SysPost> totalList(SysPost role)
    {
        List<SysPost> list = postService.selectPostList(role);
        return list;
    }

    @Log(title = "system.title.postmanage", businessType = BusinessType.EXPORT)
    @PreAuthorize(hasPermi = "system:post:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysPost post) throws IOException
    {
        List<SysPost> list = postService.selectPostList(post);
        ExcelUtil<SysPost> util = new ExcelUtil<SysPost>(SysPost.class);
        util.exportExcel(response, list, I18nUtil.getMessage("system.title.postdata"));
    }

    /**
     * 根据岗位编号获取详细信息
     */
    @PreAuthorize(hasPermi = "system:post:query")
    @GetMapping(value = "/{postId}")
    public AjaxResult getInfo(@PathVariable Long postId)
    {
        return AjaxResult.success(postService.selectPostById(postId));
    }

    /**
     * 新增岗位
     */
    @PreAuthorize(hasPermi = "system:post:add")
    @Log(title = "system.title.postmanage", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysPost post)
    {
        if (UserConstants.NOT_UNIQUE.equals(postService.checkPostNameUnique(post)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.post.exist.noadd") , post.getPostName());
            return AjaxResult.error(errMsg);
        }
        else if (UserConstants.NOT_UNIQUE.equals(postService.checkPostCodeUnique(post)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.post.exist.code.noadd"), post.getPostName());
            return AjaxResult.error(errMsg);
        }
        post.setCreateBy(SecurityUtils.getUsername());
        return toAjax(postService.insertPost(post));
    }

    /**
     * 修改岗位
     */
    @PreAuthorize(hasPermi = "system:post:edit")
    @Log(title = "system.title.postmanage", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysPost post)
    {
        if (UserConstants.NOT_UNIQUE.equals(postService.checkPostNameUnique(post)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.post.exist.noupdate"), post.getPostName());
            return AjaxResult.error(errMsg);
        }
        else if (UserConstants.NOT_UNIQUE.equals(postService.checkPostCodeUnique(post)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.post.exist.code.noupdate"), post.getPostName());
            return AjaxResult.error(errMsg);
        }
        post.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(postService.updatePost(post));
    }

    /**
     * 删除岗位
     */
    @PreAuthorize(hasPermi = "system:post:remove")
    @Log(title = "system.title.postmanage", businessType = BusinessType.DELETE)
    @DeleteMapping("/{postIds}")
    public AjaxResult remove(@PathVariable Long[] postIds)
    {
        return toAjax(postService.deletePostByIds(postIds));
    }

    /**
     * 获取岗位选择框列表
     */
    @GetMapping("/optionselect")
    public AjaxResult optionselect()
    {
        List<SysPost> posts = postService.selectPostAll();
        return AjaxResult.success(posts);
    }

    /**
     * 根据岗位ID获取岗位信息
     * @param postId
     * @return
     */
    @PostMapping("/selectPostById")
    public SysPost selectPostById(Long postId){
        return postService.selectPostById(postId);
    }

    /**
     * 校验岗位名称
     */
    @PostMapping("/checkPostNameUnique")
    public String checkPostNameUnique(@RequestBody SysPost post){
        return  postService.checkPostNameUnique(post);
    }

    /**
     * 校验岗位编码
     * @param post
     * @return
     */
    @PostMapping("/checkPostCodeUnique")
    public String checkPostCodeUnique(@RequestBody SysPost post){
        return postService.checkPostCodeUnique(post);
    }

    /**
     * 根据用户id获取岗位信息
     * @param userId
     * @return
     */
    @PostMapping("/selectPostsByUserId")
    public List<SysPost> selectPostsByUserId(Long userId){
        return postService.selectPostsByUserId(userId);
    }
}
