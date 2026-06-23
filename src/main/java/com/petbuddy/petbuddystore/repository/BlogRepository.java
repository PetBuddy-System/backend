package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogRepository extends JpaRepository<Blog, String> {
    boolean existsByTitle(String title);

    @Query("""
    SELECT b
    FROM Blog b
    WHERE (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:label IS NULL OR LOWER(b.label) = LOWER(:label))
    """)
    Page<Blog> findBlogs(@Param("keyword") String keyword, @Param("label") String label, Pageable pageable);
}
