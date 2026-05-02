package com.capics.repository;

import com.capics.entity.NotebookNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotebookNoteRepository extends JpaRepository<NotebookNote, Long> {
}
