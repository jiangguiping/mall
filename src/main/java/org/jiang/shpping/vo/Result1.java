package org.jiang.shpping.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result1 {
    private Boolean success;
    private String errorMsg;
    private Object data;
    private Long total;

    public static Result1 ok(){
        return new Result1(true, null, null, null);
    }
    public static Result1 ok(Object data){
        return new Result1(true, null, data, null);
    }
    public static Result1 ok(List<?> data, Long total){
        return new Result1(true, null, data, total);
    }
    public static Result1 fail(String errorMsg){
        return new Result1(false, errorMsg, null, null);
    }
}
