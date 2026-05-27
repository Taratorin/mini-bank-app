package ru.cloudwithout.bankui.model;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

import java.util.List;

@Component
public class MainPageModelFiller {

    public void fillModel(
            Model model,
            @Nullable CommonResponse commonResponse,
            @Nullable List<String> errors,
            @Nullable String info
    ) {
        if (commonResponse != null) {
            model.addAttribute("name", commonResponse.getFirstLastName());
            model.addAttribute("birthdate", commonResponse.getBirthDate());
            model.addAttribute("sum", commonResponse.getSum());
            model.addAttribute("accounts", commonResponse.getAccounts());
        }
        model.addAttribute("errors", errors);
        model.addAttribute("info", info);
    }
}