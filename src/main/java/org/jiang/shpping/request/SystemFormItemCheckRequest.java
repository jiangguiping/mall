package org.jiang.shpping.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="SystemFormItemCheckRequest对象", description="表单字段明细")
public class SystemFormItemCheckRequest implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "字段名称", required = true)
    private String name;

    @ApiModelProperty(value = "字段值", required = true)
    private String value;

    @ApiModelProperty(value = "字段显示文字", required = true)
    private String title;

}
