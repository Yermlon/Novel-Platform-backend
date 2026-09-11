package com.commence.novel.repository;

import com.commence.novel.entity.NovelsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NovelsTypeRepository extends JpaRepository<NovelsType, Integer> {
    List<NovelsType> findByStatusOrderByTypeIdAsc(Integer status);

    Optional<NovelsType> findByTypeIdAndStatus(Integer typeId, Integer status);

    Optional<NovelsType> findByTypeName(String typeName);
}
