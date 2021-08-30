package com.gs.api.repository;

import com.gs.api.model.entity.Book;
import com.gs.api.model.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Query(value = "select case when ( count(l.id) > 0 ) then true else false end " +
            " from Loan l where l.book = :book and returned is not true")
    boolean existsByBookIdAndReturnedIsFalse(@Param("book") Book book);

    @Query( value = "select l from Loan l join l.book as b where b.isbn = :isbn or l.customer = :customer")
    Page<Loan> findByBookIsbnOrCustumer(
            @Param("isbn") String isbn,
            @Param("customer") String customer,
            Pageable pageable);

    @Query( value = "select l from Loan l where l.book = :book ")
    Page<Loan> getLoansByBook(@Param("book") Book book, Pageable pageable);
}
