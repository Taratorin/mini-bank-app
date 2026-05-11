package contracts.accounts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return account by login"
    request {
        method GET()
        urlPath("/accounts/login") {
            queryParameters {
                parameter "login": "test"
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
                sum: 100.00,
                accounts: [],
                errors: [],
                info: null
        )
    }
}
