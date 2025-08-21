package com.example.lead.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Schema(description = "批量删除客资请求")
public class BatchDeleteRequest implements Serializable {
    @Schema(description = "客资ID列表", example = "[1, 2, 3]")
    @NotEmpty(message = "客资ID列表不能为空")
    private List<Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}
