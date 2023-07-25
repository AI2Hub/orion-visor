package com.orion.ops.module.infra.framework.service.impl;

import com.orion.lang.utils.time.Dates;
import com.orion.ops.framework.common.constant.ErrorCode;
import com.orion.ops.framework.common.security.LoginUser;
import com.orion.ops.framework.security.core.service.SecurityFrameworkService;
import com.orion.ops.module.infra.entity.dto.LoginTokenDTO;
import com.orion.ops.module.infra.enums.LoginTokenStatusEnum;
import com.orion.ops.module.infra.enums.UserStatusEnum;
import com.orion.ops.module.infra.service.AuthenticationService;
import com.orion.ops.module.infra.service.PermissionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 安全包 实现类
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2023/7/7 10:57
 */
@Service
public class SecurityFrameworkServiceImpl implements SecurityFrameworkService {

    @Resource
    private AuthenticationService authenticationService;

    @Resource
    private PermissionService permissionService;

    @Override
    public boolean hasPermission(String permission) {
        // 检查是否有权限
        return permissionService.hasPermission(permission);
    }

    @Override
    public boolean hasRole(String role) {
        // 检查是否有角色
        return permissionService.hasRole(role);
    }

    @Override
    public LoginUser getUserByToken(String token) {
        // 获取 token 信息
        LoginTokenDTO tokenInfo = authenticationService.getLoginTokenInfo(token, true);
        if (tokenInfo == null) {
            return null;
        }
        // 检查 token 状态
        this.checkTokenStatus(tokenInfo);
        // 获取登陆信息
        LoginUser user = authenticationService.getLoginUser(tokenInfo.getId());
        // 检查用户状态
        UserStatusEnum.checkUserStatus(user.getStatus());
        return user;
    }

    /**
     * 检查 token 状态
     *
     * @param loginToken loginToken
     */
    private void checkTokenStatus(LoginTokenDTO loginToken) {
        Integer tokenStatus = loginToken.getTokenStatus();
        // 正常状态
        if (LoginTokenStatusEnum.OK.getStatus().equals(tokenStatus)) {
            return;
        }
        // 其他设备登陆
        if (LoginTokenStatusEnum.OTHER_DEVICE.getStatus().equals(tokenStatus)) {
            throw ErrorCode.OTHER_DEVICE_LOGIN.exception(
                    Dates.format(new Date(loginToken.getLoginTime()), Dates.MD_HM),
                    loginToken.getIp(),
                    loginToken.getLocation());
        }
    }

}
