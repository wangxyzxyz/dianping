package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.entity.ShopType;
import com.dianping.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: 商铺类型 Controller
 * @author Wangyw
 * @date 2024/5/25 16:50
 * @version 1.0
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;


    /**
     * @description: 查询商铺类型列表
     * @author Wangyw
     * @date: 2024/6/3 0:36
     */
    @GetMapping("list")
    public Result queryTypeList() {
        List<ShopType> typeList = typeService
                .query().orderByAsc("sort").list();
        return Result.ok(typeList);
    }
}
