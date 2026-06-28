<!-- Parent: ../../../../../../backend/AGENTS.md -->
<!-- Generated: 2026-06-28 | Updated: 2026-06-28 -->

# com.crisfin (Test Source Root)

## Purpose
Root test package mirroring the main source tree. Contains integration tests, unit tests, and slice tests for all CrisFin domains. Uses H2 in-memory DB for persistence tests and `@WebMvcTest` for controller-layer tests.

## For AI Agents

### Working In This Directory
- Mirror the production package structure: test classes for `com.crisfin.crisis.*` go in `com.crisfin.crisis.*` here
- Three test tiers:
  1. **Unit tests** — pure logic, no Spring context, mock dependencies with Mockito
  2. **Slice tests** — `@WebMvcTest` (controller) or `@DataJpaTest` (repository), partial Spring context
  3. **Integration tests** — `@SpringBootTest` with H2, tests the full stack
- Prefer unit and slice tests over full `@SpringBootTest` for speed
- H2 dialect is configured for test profile — never assume PostgreSQL-specific SQL in tests

### Testing Requirements
- Run all tests: `./gradlew test`
- JUnit 5 (`@Test`, `@ExtendWith(MockitoExtension.class)`)
- MockMvc for HTTP layer assertions
- `Assertions` from JUnit 5 + AssertJ via `assertThat()`

### Common Patterns
```java
// Controller slice test
@WebMvcTest(CrisisController.class)
class CrisisControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean CrisisService crisisService;
    ...
}

// Repository slice test
@DataJpaTest
class WelfareProgramRepositoryTest {
    @Autowired WelfareProgramRepository repo;
    ...
}
```

<!-- MANUAL: -->
