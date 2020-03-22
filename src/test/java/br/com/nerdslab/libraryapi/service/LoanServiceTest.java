package br.com.nerdslab.libraryapi.service;

import br.com.nerdslab.libraryapi.api.dto.LoanFilterDTO;
import br.com.nerdslab.libraryapi.exception.BusinessException;
import br.com.nerdslab.libraryapi.model.entity.Book;
import br.com.nerdslab.libraryapi.model.entity.Loan;
import br.com.nerdslab.libraryapi.model.repository.LoanRepository;
import br.com.nerdslab.libraryapi.service.impl.LoanServiceImpl;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class LoanServiceTest {

    private LoanService service;

    @MockBean
    private LoanRepository repository;

    @BeforeEach
    public void setUp() {
        this.service = new LoanServiceImpl(repository);
    }

    @Test
    @DisplayName("Deve salvar um empréstimo")
    public void saveLoanTest() {
        // cenário
        Book book = Book.builder().id(1l).build();
        String customer = "Fulano";
        Loan savingLoan = Loan.builder()
                .book(book)
                .customer(customer)
                .loanDate(LocalDate.now())
                .build();


        Loan savedLoan = Loan.builder()
                .id(1l)
                .loanDate(LocalDate.now())
                .customer(customer)
                .book(book)
                .build();

        Mockito.when(repository.existsByBookAndNotReturned(book))
                .thenReturn(false);
        Mockito.when(repository.save(savingLoan))
                .thenReturn(savedLoan);

        // execução
        Loan loan = service.save(savingLoan);

        // verificação
        assertThat(loan.getId()).isEqualTo(savedLoan.getId());
        assertThat(loan.getBook().getId()).isEqualTo(savedLoan.getBook().getId());
        assertThat(loan.getCustomer()).isEqualTo(savedLoan.getCustomer());
        assertThat(loan.getLoanDate()).isEqualTo(savedLoan.getLoanDate());
    }

    @Test
    @DisplayName("Deve lançar erro de negócio ao salvar um empréstimo com livro já emprestado")
    public void loanedBookSaveTest() {
        // cenário
        Book book = Book.builder().id(1l).build();
        String customer = "Fulano";
        Loan savingLoan = Loan.builder()
                .book(book)
                .customer(customer)
                .loanDate(LocalDate.now())
                .build();

        Mockito.when(repository.existsByBookAndNotReturned(book))
                .thenReturn(true);

        // execução
        Throwable exception = catchThrowable(() -> service.save(savingLoan));

        // verificação
        assertThat(exception)
                .isInstanceOf(BusinessException.class)
                .hasMessage("Book already loaned");

        Mockito.verify(repository, Mockito.never()).save(savingLoan);

    }

    @Test
    @DisplayName("Deve obter as informações de um empréstimo pelo ID")
    public void getLoanDetailsTest() {
        // cenário
        Long id = 1l;

        Loan loan = createLoan();
        loan.setId(id);

        Mockito.when(repository.findById(id)).thenReturn(Optional.of(loan));

        // execução
        Optional<Loan> result = service.getById(id);

        // verificação
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getId()).isEqualTo(id);
        assertThat(result.get().getCustomer()).isEqualTo(loan.getCustomer());
        assertThat(result.get().getBook()).isEqualTo(loan.getBook());
        assertThat(result.get().getLoanDate()).isEqualTo(loan.getLoanDate());

        Mockito.verify(repository, Mockito.times(1)).findById(id);

    }

    @Test
    @DisplayName("Deve atualizar um empréstimo")
    public void updatedLoanTest() {
        // cenário
        Loan loan = createLoan();
        loan.setId(1l);
        loan.setReturned(true);

        Mockito.when(repository.save(loan)).thenReturn(loan);

        // execução
        Loan updatedLoan = service.update(loan);

        // verificação
        assertThat(updatedLoan.getReturned()).isTrue();
        Mockito.verify(repository).save(loan);

    }

    @Test
    @DisplayName("Deve filtrar empréstimos pelas propriedades")
    public void findBookTest() {
        // cenário
        LoanFilterDTO loanFilterDTO = LoanFilterDTO.builder().customer("Fulano").isbn("321").build();

        Loan loan = createLoan();
        loan.setId(1l);

        PageRequest pageRequest = PageRequest.of(0, 10);

        List<Loan> lista = Arrays.asList(loan);
        Page<Loan> page = new PageImpl<Loan>(lista, pageRequest, lista.size());
        Mockito.when(repository.findByBookIsbnOrCustomer(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(PageRequest.class)))
                .thenReturn(page);

        // execução
        Page<Loan> result = service.find(loanFilterDTO, pageRequest);

        // verificação
        AssertionsForClassTypes.assertThat(result.getTotalElements()).isEqualTo(1);
        AssertionsForClassTypes.assertThat(result.getContent()).isEqualTo(lista);
        AssertionsForClassTypes.assertThat(result.getPageable().getPageNumber()).isEqualTo(0);
        AssertionsForClassTypes.assertThat(result.getPageable().getPageSize()).isEqualTo(10);

    }

    public static Loan createLoan() {
        Book book = Book.builder().id(1l).build();
        String customer = "Fulano";
        return Loan.builder()
                .book(book)
                .customer(customer)
                .loanDate(LocalDate.now())
                .build();
    }

}
