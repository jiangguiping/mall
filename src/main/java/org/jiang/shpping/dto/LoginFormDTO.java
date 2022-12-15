package org.jiang.shpping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.Pattern;


@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="LoginRequest对象", description="移动端手机密码登录请求对象")
public class LoginFormDTO {

//    @ApiModelProperty(value = "手机号", required = true, example = "18888888")
//    @JsonProperty(value = "account")
    private String phone;

    private String code;

    @ApiModelProperty(value = "密码", required = true, example = "1~[6,18]")
    private String password;
}
