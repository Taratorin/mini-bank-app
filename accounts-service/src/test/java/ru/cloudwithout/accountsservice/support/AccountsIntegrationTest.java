package ru.cloudwithout.accountsservice.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.cloudwithout.accountsservice.client.NotificationsClient;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AccountsIntegrationTest {

    @MockitoBean
    private NotificationsClient notificationsClient;
}