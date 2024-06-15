package com.dianping.service;

import com.dianping.dto.Result;
import com.dianping.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @description:  商铺信息 Service
 * @author Wangyw
 * @date 2024/5/17 19:32
 * @version 1.0
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);

    Result queryHotShopById(Long id);
}
