package com.acaivis.repository;

import com.acaivis.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByAvailableTrue();

    @Query("""
        select p from Product p
        join p.category c
        where (:search = ''
               or lower(p.name) like lower(concat('%', :search, '%'))
               or lower(p.size) like lower(concat('%', :search, '%'))
               or lower(p.description) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.badge, '')) like lower(concat('%', :search, '%'))
               or lower(c.name) like lower(concat('%', :search, '%')))
          and (:categoryId is null or c.id = :categoryId)
          and (:available is null or p.available = :available)
        order by p.id desc
        """)
    Page<Product> findAdminProducts(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("available") Boolean available,
            Pageable pageable
    );
}
