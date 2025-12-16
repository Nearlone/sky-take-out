package com.sky.handler;

import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 处理SQL异常: 数据库字段唯一性约束
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        String message = ex.getMessage();

        // 获取重复字段类型
        if (message.contains("Duplicate entry")) {
            // 用户名已存在
            if (message.contains("username")){
                log.error("用户名已存在");
                return Result.error("用户名已存在");

            // TODO 其他字段重复异常
            }else {
                log.error("异常信息：{}", ex.getMessage());
                return Result.error("服务器异常");
            }
//            String[] split = message.split(" "); // Duplicate entry '乜倩' for key 'employee.idx_username'
//            String fieldName = split[6];  // 'employee.idx_username'
//            String msg = fieldName + "已存在";
//            log.error(msg);
//            return Result.error(msg);
        }else{
            log.error("异常信息：{}", ex.getMessage());
            return Result.error("服务器异常");
        }
    }

}
