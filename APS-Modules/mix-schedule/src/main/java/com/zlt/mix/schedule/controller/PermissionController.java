package com.zlt.mix.schedule.controller;

import com.zlt.mix.schedule.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户资源权限Controller
 *
 * @author Liam
 * @date 2022-07-12
 */
@RestController
@RequestMapping("/permission")
public class PermissionController {
    @Autowired
    private PermissionService permissionService;

    @PostMapping("/haveMixAreaPermission")
    public List<String> haveMixAreaPermission() {
        return permissionService.haveMixAreaPermission();
    }
}
