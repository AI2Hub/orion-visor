/*
 * Copyright (c) 2023 - present Jiahang Li (visor.orionsec.cn ljh1553488six@139.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.visor.module.infra.service;

import org.dromara.visor.module.infra.entity.domain.SystemRoleDO;
import org.dromara.visor.module.infra.entity.dto.SystemMenuCacheDTO;
import org.dromara.visor.module.infra.entity.vo.SystemMenuVO;
import org.dromara.visor.module.infra.entity.vo.UserPermissionVO;

import java.util.List;
import java.util.Map;

/**
 * 用户权限服务
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2023/7/16 1:03
 */
public interface UserPermissionService {

    /**
     * 获取 角色缓存
     *
     * @return cache
     */
    Map<Long, SystemRoleDO> getRoleCache();

    /**
     * 获取 菜单缓存 以作角色权限直接引用
     *
     * @return cache
     */
    List<SystemMenuCacheDTO> getMenuCache();

    /**
     * 获取 角色菜单关联
     *
     * @return cache
     */
    Map<Long, List<SystemMenuCacheDTO>> getRoleMenuCache();

    /**
     * 初始化权限缓存
     */
    void initPermissionCache();

    /**
     * 检查当前用户是否含有此角色 (有效性判断)
     *
     * @param role role
     * @return 是否包含
     */
    boolean hasRole(String role);

    /**
     * 检查当前用户是否含有任意角色 (有效性判断)
     *
     * @param roles roles
     * @return 是否包含
     */
    boolean hasAnyRole(String... roles);

    /**
     * 检查当前用户是否含有此权限 (有效性判断)
     *
     * @param permission permission
     * @return 是否包含
     */
    boolean hasPermission(String permission);

    /**
     * 检查当前用户是否含任意权限 (有效性判断)
     *
     * @param permissions permissions
     * @return 是否包含
     */
    boolean hasAnyPermission(String... permissions);

    /**
     * 获取用户菜单
     *
     * @return 菜单
     */
    List<SystemMenuVO> getUserMenuList();

    /**
     * 获取用户权限
     *
     * @return 权限信息
     */
    UserPermissionVO getUserPermission();

}