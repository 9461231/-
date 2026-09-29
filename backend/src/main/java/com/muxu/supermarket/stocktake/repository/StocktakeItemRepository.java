package com.muxu.supermarket.stocktake.repository;

import com.muxu.supermarket.stocktake.entity.StocktakeItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StocktakeItemRepository extends JpaRepository<StocktakeItem, Long> {

    List<StocktakeItem> findByStocktakeId(Long stocktakeId);
}
