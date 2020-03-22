package br.com.nerdslab.libraryapi.service;

import br.com.nerdslab.libraryapi.api.dto.LoanFilterDTO;
import br.com.nerdslab.libraryapi.api.resource.BookController;
import br.com.nerdslab.libraryapi.model.entity.Book;
import br.com.nerdslab.libraryapi.model.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface LoanService {
    Loan save(Loan any);

    Optional<Loan> getById(Long id);

    Loan update(Loan loan);

    Page<Loan> find(LoanFilterDTO filterDTO, Pageable pageable);

    Page<Loan> getLoansByBook(Book book, Pageable pageable);

    List<Loan> getAllLateLoans();
}
