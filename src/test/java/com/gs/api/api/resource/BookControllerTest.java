package com.gs.api.api.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gs.api.api.dto.BookDTO;
import com.gs.api.config.ModelMapperConfig;
import com.gs.api.exception.BusinessException;
import com.gs.api.model.entity.Book;
import com.gs.api.model.entity.Loan;
import com.gs.api.service.BookService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@WebMvcTest(controllers = BookController.class)
@AutoConfigureMockMvc
@Import(ModelMapperConfig.class)
public class BookControllerTest {

    static String BOOK_API = "/api/books";

    @Autowired
    MockMvc mvc;

    @MockBean
    BookService service;

    @Test
    @DisplayName("Should create a book with success")
    public void createBookTest() throws Exception {
        // given
        BookDTO dto = createNewBookDTO();
        Book savedBook = Book.builder().id(1L).author("Artur").title("As aventuras").isbn("001").build();
        given(service.save(Mockito.any(Book.class))).willReturn(savedBook);

        // when
        String json = new ObjectMapper().writeValueAsString(dto);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(BOOK_API)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json);

        // then
        mvc
                .perform(request)
                .andExpect( status().isCreated() )
                .andExpect(jsonPath("id").value(savedBook.getId()))
                .andExpect(jsonPath("title").value(savedBook.getTitle()))
                .andExpect(jsonPath("author").value(savedBook.getAuthor()))
                .andExpect(jsonPath("isbn").value(savedBook.getIsbn()));
    }

    @Test
    @DisplayName("Should throw exception when not enough attributes")
    public void createInvalidBookTest() throws Exception {
        String json = new ObjectMapper().writeValueAsString(new BookDTO());

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(BOOK_API)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json);

        mvc
                .perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors", Matchers.hasSize(3)));
    }

    @Test
    @DisplayName("Should throw an exception when save a book with a duplicated ISBN")
    public void createBookWithDuplicatedIsbn() throws Exception {
        BookDTO dto = createNewBookDTO();
        String json = new ObjectMapper().writeValueAsString(dto);
        String mensagemErro = "Isbn já cadastrado";

        given(service.save(Mockito.any(Book.class)))
                .willThrow(new BusinessException(mensagemErro));

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(BOOK_API)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json);

        mvc
                .perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors", Matchers.hasSize(1)))
                .andExpect(jsonPath("errors[0]").value(mensagemErro));
    }

    @Test
    @DisplayName("Should get book details info")
    public void getBookDetailsInfo() throws Exception {
        // given
        Long id = 1L;
        Book book = createNewBook(id);
        given(service.getById(id)).willReturn(Optional.of(book));

        // when
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get(BOOK_API.concat("/").concat(id.toString()))
                .accept(MediaType.APPLICATION_JSON);

        // then
        mvc
                .perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(id))
                .andExpect(jsonPath("title").value("As aventuras"))
                .andExpect(jsonPath("author").value("Fulano"))
                .andExpect(jsonPath("isbn").value("123"));
    }

    @Test
    @DisplayName("Return Not Found when book dont exists")
    public void returnNotFoundWhenBookDontExists() throws Exception {
        //given
        when(service.getById(Mockito.anyLong())).thenReturn(Optional.empty());

        //when
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get(BOOK_API.concat("/").concat("1"))
                .accept(MediaType.APPLICATION_JSON);

        //then
        mvc
                .perform(request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("errors[0]").value("Book not found"));
    }

    @Test
    @DisplayName("Should delete a book")
    public void shouldDeleteBook() throws Exception {
        //given
        Long id = 1L;
        given(service.getById(id)).willReturn(Optional.of(createNewBook(id)));

        //when
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .delete(BOOK_API.concat("/").concat(id.toString()));

        //then
        mvc
                .perform(request)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return Not Found when try to delete a book that not exists")
    public void returnNotFoundWhenDeleteNonExistingBook() throws Exception {
        //given
        Long id = 1L;
        given(service.getById(id)).willReturn(Optional.empty());

        //when
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .delete(BOOK_API.concat("/").concat(id.toString()));

        mvc
                .perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update a book")
    public void updateBook() throws Exception {
        //given
        Long id = 1L;
        BookDTO bookDTO = createNewBookDTO();
        String json = new ObjectMapper().writeValueAsString(bookDTO);

        Book entity = createNewBook(id);
        given(service.getById(id)).willReturn(Optional.of(entity));
        Book updatedBook = createNewBook(id);
        updatedBook.setTitle(bookDTO.getTitle());
        updatedBook.setAuthor(bookDTO.getAuthor());
        given(service.save(Mockito.any())).willReturn(updatedBook);

        //when
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .put(BOOK_API.concat("/").concat(id.toString()))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);

        mvc
                .perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(updatedBook.getId()))
                .andExpect(jsonPath("isbn").value(updatedBook.getIsbn()))
                .andExpect(jsonPath("author").value(updatedBook.getAuthor()))
                .andExpect(jsonPath("title").value(updatedBook.getTitle()));
    }

    @Test
    @DisplayName("Should return Not Found when try to update an inexisting Book")
    public void returnNotFoundWhenUpdatingNonExistingBook() throws Exception {
        //given
        Long id = 1L;
        BookDTO bookDTO = createNewBookDTO();
        bookDTO.setId(id);
        String json = new ObjectMapper().writeValueAsString(bookDTO);
        given(service.getById(id)).willReturn(Optional.empty());

        //when
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .put(BOOK_API.concat("/").concat(id.toString()))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);

        mvc
                .perform(request)
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("Should list Books by Filter")
    public void shouldListBooksByFilter() throws Exception {
        //given
        Book filter = createNewBook(1L);
        given(service.find(Mockito.any(Book.class), Mockito.any(Pageable.class)))
                .willReturn(new PageImpl<>(Collections.singletonList(filter), PageRequest.of(0, 100), 1));

        // when
        String queryString = String.format("?title=%s&author=%s&page=0&size=100",
                filter.getTitle(), filter.getAuthor());
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get(BOOK_API.concat(queryString))
                .accept(MediaType.APPLICATION_JSON);

        // then
        mvc
                .perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(1)))
                .andExpect(jsonPath("content[0].id").value(1))
                .andExpect(jsonPath("content[0].title").value("As aventuras"))
                .andExpect(jsonPath("content[0].author").value("Fulano"))
                .andExpect(jsonPath("content[0].isbn").value("123"))
                .andExpect(jsonPath("totalElements").value(1));
    }

    @Test
    @DisplayName("List all Book's loans")
    public void listAllBookLoans() throws Exception {
        // given
        long id = 1L;
        Book book = createNewBook(id);
        Loan loan = Loan.builder().id(1L).customer("Fulano").book(book).build();
        given(service.getById(anyLong())).willReturn(Optional.of(book));
        given(service.getLoansByBook(any(Book.class), any(Pageable.class)))
                .willReturn(
                        new PageImpl<>(Collections.singletonList(loan), PageRequest.of(0, 10), 1)
                );

        // when
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
                .get(BOOK_API.concat("/").concat(Long.toString(id)).concat("/").concat("loans"))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mvc
                .perform(req)
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(1)))
                .andExpect(jsonPath("content[0].id").value(1))
                .andExpect(jsonPath("content[0].bookIsbn").value("123"))
                .andExpect(jsonPath("content[0].customer").value("Fulano"))
                .andExpect(jsonPath("totalPages").value(1))
                .andExpect(jsonPath("totalElements").value(1));
    }

    @Test
    @DisplayName("Fails to list all book's loans when book doenst exist")
    public void failToListAllLoansWhenBookDoesntExist() throws Exception {
        // given
        long id = 1L;
        given(service.getById(anyLong())).willReturn(Optional.empty());

        // when
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders
                .get(BOOK_API.concat("/").concat(Long.toString(id)).concat("/").concat("loans"))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mvc
                .perform(req)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("errors", Matchers.hasSize(1)))
                .andExpect(jsonPath("errors[0]").value("Book not found"));
    }


    private Book createNewBook(Long id) {
        return Book.builder().id(id).isbn("123").title("As aventuras").author("Fulano").build();
    }


    private BookDTO createNewBookDTO() {
        return BookDTO.builder().author("Artur").title("As aventuras").isbn("001").build();
    }
}
