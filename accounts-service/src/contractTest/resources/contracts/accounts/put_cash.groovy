package contracts.accounts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should put cash to account"
    request {
        method POST()
        urlPath("/accounts/cash") {
            queryParameters {
                parameter "login": "test"
                parameter "value": "100"
                parameter "action": "DEPOSIT"
            }
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
                login: "test",
                firstLastName: "Иван Иванович",
                birthDate: "1990-01-01",
                sum: 200.00,
                accounts: [],
                errors: [],
                info: "Положено руб.: 100"
        )
    }
}