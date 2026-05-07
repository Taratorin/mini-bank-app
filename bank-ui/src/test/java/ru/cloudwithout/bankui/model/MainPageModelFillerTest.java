package ru.cloudwithout.bankui.model;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import ru.cloudwithout.bankui.model.dto.AccountDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MainPageModelFillerTest {

    private final MainPageModelFiller filler = new MainPageModelFiller();

    @Test
    void fillModelShouldPopulateAllMainAttributes() {
        CommonResponse response = new CommonResponse(List.of(new AccountDto("alex", "Алексей")));
        response.setFirstLastName("Иван Иванович");
        response.setBirthDate(LocalDate.of(1990, 1, 1));
        response.setSum(new BigDecimal("100.00"));

        var model = new ConcurrentModel();
        filler.fillModel(model, response, List.of("ошибка"), "инфо");

        assertThat(model.getAttribute("name")).isEqualTo("Иван Иванович");
        assertThat(model.getAttribute("birthdate")).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(model.getAttribute("sum")).isEqualTo(new BigDecimal("100.00"));
        assertThat(model.getAttribute("accounts")).isEqualTo(List.of(new AccountDto("alex", "Алексей")));
        assertThat(model.getAttribute("errors")).isEqualTo(List.of("ошибка"));
        assertThat(model.getAttribute("info")).isEqualTo("инфо");
    }
}