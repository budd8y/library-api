package br.com.nerdslab.libraryapi.api.resource;

import br.com.nerdslab.libraryapi.api.dto.LoanDTO;
import br.com.nerdslab.libraryapi.api.dto.LoanFilterDTO;
import br.com.nerdslab.libraryapi.api.dto.ReturnedLoanDTO;
import br.com.nerdslab.libraryapi.exception.BusinessException;
import br.com.nerdslab.libraryapi.model.entity.Book;
import br.com.nerdslab.libraryapi.model.entity.Loan;
import br.com.nerdslab.libraryapi.service.BookService;
import br.com.nerdslab.libraryapi.service.LoanService;
import br.com.nerdslab.libraryapi.service.LoanServiceTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@WebMvcTest(controllers = LoanController.class)
public class LoanControllerTest {

    private static final String LOAN_API = "/api/loans";
    @Autowired
    MockMvc mvc;

    @MockBean
    private BookService bookService;

    @MockBean
    private LoanService loanService;

    @Test
    @DisplayName("Deve realizar um empréstimo.")
    public  void createLoanTest() throws Exception {
        // cenário
        Long id = 1l;
        String isbn = "123";
        LoanDTO dto = LoanDTO.builder().isbn(isbn).email("customer@email.com").customer("Fulano").build();
        String json = new ObjectMapper().writeValueAsString(dto);

        Book book = Book.builder().id(id).isbn(isbn).build();
        BDDMockito.given(bookService.getBookByIsbn(isbn))
                .willReturn(Optional.of(book));

        Loan loan = Loan.builder().id(id).customer("Fulano").book(book).loanDate(LocalDate.now()).build();
        BDDMockito.given(loanService.save(Mockito.any(Loan.class))).willReturn(loan);
        
        // execução
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(LOAN_API)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);

        // verificação
        mvc
                .perform(request)
                .andExpect(status().isCreated())
                .andExpect(content().string("1"));

    }

    @Test
    @DisplayName("Deve retornar erro ao tentar fazer empréstimo de um livro inexistente.")
    public void invalidIsbnCreateLoanTest() throws Exception {
        // cenário
        String isbn = "123";
        LoanDTO dto = LoanDTO.builder().isbn(isbn).customer("Fulano").build();
        String json = new ObjectMapper().writeValueAsString(dto);

        BDDMockito.given(bookService.getBookByIsbn(isbn))
                .willReturn(Optional.empty());

        // execução
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(LOAN_API)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);

        // verificação
        mvc
                .perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors", Matchers.hasSize(1)))
                .andExpect(jsonPath("errors[0]").value("Book not found for passed isbn"));


    }

    @Test
    @DisplayName("Deve retornar erro ao tentar fazer empréstimo de um livro inexistente.")
    public void loanedBookErrorOnCreateLoanTest() throws Exception {
        // cenário
        Long id = 1l;
        String isbn = "123";
        LoanDTO dto = LoanDTO.builder().isbn(isbn).customer("Fulano").build();
        String json = new ObjectMapper().writeValueAsString(dto);


        Book book = Book.builder().id(id).isbn(isbn).build();
        BDDMockito.given(bookService.getBookByIsbn(isbn))
                .willReturn(Optional.of(book));

        BDDMockito.given(loanService.save(Mockito.any(Loan.class)))
                .willThrow(new BusinessException("Book already loaded"));

        // execução
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(LOAN_API)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);

        // verificação
        mvc
                .perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors", Matchers.hasSize(1)))
                .andExpect(jsonPath("errors[0]").value("Book already loaded"));


    }

    @Test
    @DisplayName("Deve retornar um livro.")
    public void returnBookTest() throws Exception {
        // cenário
        ReturnedLoanDTO dto = ReturnedLoanDTO.builder().returned(true).build();
        Loan loan = Loan.builder().id(1l).build();
        BDDMockito.given(loanService.getById(Mockito.anyLong()))
                .willReturn(Optional.of(loan));

        String json = new ObjectMapper().writeValueAsString(dto);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.patch(LOAN_API.concat("/1"))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);

        // execução e verificação
        mvc
                .perform(request)
                .andExpect(status().isOk());

        Mockito.verify(loanService, Mockito.times(1)).update(loan);

    }

    @Test
    @DisplayName("Deve retornar 404 ao tentar devolver um livro inexistent.")
    public void returnInexistentBookTest() throws Exception {
        // cenário
        ReturnedLoanDTO dto = ReturnedLoanDTO.builder().returned(true).build();
        String json = new ObjectMapper().writeValueAsString(dto);

        BDDMockito.given(loanService.getById(Mockito.anyLong()))
                .willReturn(Optional.empty());


        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.patch(LOAN_API.concat("/1"))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);

        // execução e verificação
        mvc
                .perform(request)
                .andExpect(status().isNotFound());


    }

    @Test
    @DisplayName("Deve filtrar empréstimos.")
    public void findLoansTest() throws Exception {
        // cenário
        Long id = 1l;

        Book book = Book.builder().id(id).isbn("321").build();
        Loan loan = LoanServiceTest.createLoan();
        loan.setId(id);
        loan.setBook(book);

        BDDMockito.given(
                loanService.find(
                        Mockito.any(LoanFilterDTO.class),
                        Mockito.any(Pageable.class)
                )
        ).willReturn(
                new PageImpl<Loan>(
                        Arrays.asList(loan),
                        PageRequest.of(0, 10),
                        1
                )
        );

        // execução
        String queryString = String.format("?isbn=%s&customer=%s&page=0&size=10",
                book.getIsbn(), loan.getCustomer());

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get(LOAN_API.concat(queryString))
                .accept(MediaType.APPLICATION_JSON);

        // verificação
        mvc
                .perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(1)))
                .andExpect(jsonPath("totalElements").value(1))
                .andExpect(jsonPath("pageable.pageSize").value(10))
                .andExpect(jsonPath("pageable.pageNumber").value(0));


    }

}
