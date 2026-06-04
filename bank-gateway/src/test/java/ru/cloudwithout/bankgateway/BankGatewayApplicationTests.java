package ru.cloudwithout.bankgateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "ROUTES_ACCOUNTS_URI=http://localhost:8083",
        "ROUTES_CASH_URI=http://localhost:8085",
        "ROUTES_TRANSFER_URI=http://localhost:8086"
})
class BankGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
