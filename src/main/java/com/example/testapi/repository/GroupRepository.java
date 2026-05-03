package com.example.testapi.repository;

import com.example.testapi.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findDistinctByMembersId(Long userId);
}
