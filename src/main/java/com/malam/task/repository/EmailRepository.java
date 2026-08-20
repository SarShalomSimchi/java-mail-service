package com.malam.task.repository;

import com.malam.task.model.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    // Custom query to search emails by sender, recipient, or subject
    @Query("SELECT e FROM Email e WHERE " +
           "(:sender IS NULL OR e.sender LIKE %:sender%) AND " +
           "(:recipient IS NULL OR e.recipient LIKE %:recipient%) AND " +
           "(:subject IS NULL OR e.subject LIKE %:subject%)")
    List<Email> searchEmails(
            @Param("sender") String sender,
            @Param("recipient") String recipient,
            @Param("subject") String subject
    );

    // Find all emails (for "sent" list)
    List<Email> findAll();
}