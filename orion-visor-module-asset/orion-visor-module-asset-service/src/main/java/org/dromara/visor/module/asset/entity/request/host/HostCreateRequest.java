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
package org.dromara.visor.module.asset.entity.request.host;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 主机 创建请求对象
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2023-9-11 14:16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "HostCreateRequest", description = "主机 创建请求对象")
public class HostCreateRequest implements Serializable {

    @NotBlank
    @Schema(description = "主机类型")
    private String type;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "主机名称")
    private String name;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "主机编码")
    private String code;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "主机地址")
    private String address;

    @Min(value = 1)
    @Max(value = 65535)
    @Schema(description = "主机端口")
    private Integer port;

    @Schema(description = "主机分组")
    private List<Long> groupIdList;

    @Size(max = 5)
    @Schema(description = "tags")
    private List<Long> tags;

}