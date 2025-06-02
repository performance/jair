# API Contract Tests

This directory is intended for API contract tests.
The API specification is defined in `tests/api_spec.json` (relative to the root of the repository).

## Tools & Strategies:
1.  **Spring REST Docs:** Can be used to generate snippets from tests, which can then be used to verify or generate the OpenAPI spec. This is a good approach for producer contract testing.
2.  **Pact:** For consumer-driven contract testing if there are separate frontend/microservice consumers for this backend. This helps ensure that changes in the provider (this backend) do not break consumers.
3.  **REST Assured with JSON Schema Validation:** Write integration tests that call the API endpoints and validate responses against JSON schemas. These JSON schemas can be derived or manually created based on `api_spec.json` or the OpenAPI definition.
4.  **Postman/Newman:** API request collections can be created based on `api_spec.json`. These collections can be run using Newman in a CI/CD pipeline to continuously validate the API against its specification.
5.  **OpenAPI Validators:** Tools that can take an OpenAPI specification and automatically validate live API responses against it, or validate generated client/server code.

## Example (Conceptual - REST Assured with JSON Schema Validation):

```kotlin
// Example in a Spring Boot integration test class
// Ensure you have dependencies like rest-assured and json-schema-validator

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions. entonces
import io.restassured.module.kotlin.extensions. Cuando
import io.restassured.module.kotlin.extensions. Dado
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserRecommendationContractTests {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setup() {
        RestAssured.port = port
        RestAssured.basePath = "/api/v1" // Adjust if needed
    }

    // Helper function to obtain a valid token for testing
    fun getValidUserToken(): String {
        // Implement logic to call login endpoint and get a token
        // This is simplified; actual implementation would involve a login call
        return "dummy-jwt-token-for-contract-test" 
    }

    @Test
    fun `validate GET me_recommendations endpoint response schema`() {
        Dado {
            auth().oauth2(getValidUserToken()) // Or your specific auth mechanism
            contentType(ContentType.JSON)
        } Cuando {
            get("/me/recommendations")
        } entonces {
            statusCode(200) // Or other expected status for this scenario
            // Assuming you have a schema file: "schemas/userRecommendationListSchema.json"
            // This schema would define the expected structure of the list of RecommendationResponse DTOs.
            // body(matchesJsonSchemaInClasspath("schemas/userRecommendationListSchema.json"))
            // For now, just a placeholder check:
            body("size()", org.hamcrest.Matchers.greaterThanOrEqualTo(0))
        }
    }
}
```

This provides a starting point for how contract tests could be structured and implemented. The choice of tools would depend on the project's specific needs and existing testing infrastructure.
