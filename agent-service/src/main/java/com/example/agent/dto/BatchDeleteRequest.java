package com.example.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "批量删除请求DTO")
public class BatchDeleteRequest {

    @Schema(description = "要删除的代理userId列表")
    @NotEmpty(message = "删除ID列表不能为空")
    private List<Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}

