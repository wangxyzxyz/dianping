package com.dianping.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.dto.Result;
import com.dianping.entity.Shop;
import com.dianping.service.IShopService;
import com.dianping.utils.SystemConstants;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description: 商铺信息 Controller
 * @author Wangyw
 * @date 2024/5/17 19:31
 * @version 1.0
 */
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    public IShopService shopService;

    /**
     * @description: 根据商铺id查询商铺信息
     * @param: id   商铺id
     * @author Wangyw
     * @date: 2024/5/18 15:32
     * @note  由于在方法中使用了布隆过滤器，所以需要提前预热布隆过滤器
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.queryById(id);
    }


    /**
     * @description: 针对热门商铺信息的查询，查询热点key的处理方法(解决缓存击穿问题)
     * @param: id    商铺id
     * @author Wangyw
     * @date: 2024/5/18 17:14
     */
    @GetMapping("/hot/{id}")
    public Result queryHotShopById(@PathVariable("id") Long id) {
        return shopService.queryHotShopById(id);
    }


    /**
     * @description: 更新商铺信息，更新到数据库，同时要保证缓存的最终一致性。
     * @param: shop 商铺信息
     * @author Wangyw
     * @date: 2024/5/17 19:36
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        // 更新
        return shopService.update(shop);
    }


    /**
     * @description: 分类型查询商铺信息
     * @param: typeId   商铺类型
current 页数
     * @author Wangyw
     * @date: 2024/6/5 15:37
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .eq("type_id", typeId)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }


    /**
     * @description:  根据商铺名称关键字分页查询商铺信息
     * @param: name 商铺名称关键字
    current 页码
     * @author Wangyw
     * @date: 2024/6/3 0:33
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据名称分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }
}
