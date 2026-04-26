package com.capics.repository;

import com.capics.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {
    Optional<SysRole> findByRoleCode(String roleCode);
    List<SysRole> findByIdIn(Collection<Long> ids);
    List<SysRole> findByRoleCodeIn(Collection<String> roleCodes);
}
