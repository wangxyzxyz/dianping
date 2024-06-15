package com.dianping.dto;

import lombok.Data;

import java.util.List;

/**
 * @description: 推送到粉丝收件箱的笔记数据 结果
 * @author Wangyw
 * @date 2024/6/6 14:49
 * @version 1.0
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
