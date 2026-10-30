package com.shopease.inventory.repository;

import com.shopease.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    Optional<Inventory> findBySkuCode(String skuCode);

    // Single conditional UPDATE so two orders racing for the last units can't both succeed.
    // Returns the number of rows changed: 0 means unknown product or not enough stock.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :qty "
            + "WHERE i.productId = :productId AND i.quantity >= :qty")
    int decrementIfAvailable(@Param("productId") Long productId, @Param("qty") int qty);
}
