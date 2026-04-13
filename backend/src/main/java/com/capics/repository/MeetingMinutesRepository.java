package com.capics.repository;

import com.capics.entity.MeetingMinutes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingMinutesRepository extends JpaRepository<MeetingMinutes, Long> {

    List<MeetingMinutes> findByMpsVersionOrderByItemNoAsc(String mpsVersion);
}
