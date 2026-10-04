package com.api.rizz.portfolio_api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.api.rizz.portfolio_api.config.WebConfig;
import com.api.rizz.portfolio_api.controller.*;
import com.api.rizz.portfolio_api.exception.GlobalExceptionHandler;
import com.api.rizz.portfolio_api.mapper.ProjectMapper;
import com.api.rizz.portfolio_api.service.*;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/** Run against a disposable PostgreSQL database using FILTER_TEST_DATABASE_URL. */
@SpringJUnitConfig(AdvancedFilteringIntegrationTests.Config.class)
@EnabledIfEnvironmentVariable(named = "FILTER_TEST_DATABASE_URL", matches = ".+")
@Transactional
@Sql("/filtering-fixtures.sql")
class AdvancedFilteringIntegrationTests {
  @Autowired ApplicationContext context;
  @Autowired CacheManager cacheManager;
  MockMvc mvc;

  @BeforeEach
  void setUp() {
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    mvc =
        MockMvcBuilders.standaloneSetup(
                new ProjectController(context.getBean(ProjectService.class)),
                new SkillController(context.getBean(SkillService.class)),
                new UseController(context.getBean(UseService.class)),
                new BlogController(context.getBean(BlogService.class)),
                new BlogAttachmentController(context.getBean(BlogAttachmentService.class)),
                new ExperienceController(context.getBean(ExperienceService.class)),
                new UserController(context.getBean(UserService.class)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setLocaleResolver(new WebConfig().localeResolver())
            .build();
  }

  @Test
  void combinesProjectEnumsJsonKeysAndSearch() throws Exception {
    mvc.perform(
            get("/projects")
                .header("Accept-Language", "en")
                .param("status", "ACTIVE,development")
                .param("projectTypes", "BACKEND,api", "frontend")
                .param("linkTypes", "github", "website")
                .param("techStack", "Spring,Vue")
                .param("search", "portfolio")
                .param("sortBy", "id")
                .param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("10", "20")));
  }

  @Test
  void matchesJsonArrayMembershipRatherThanEquality() throws Exception {
    mvc.perform(
            get("/projects")
                .param("status", "active")
                .param("projectTypes", "backend")
                .param("sortBy", "id")
                .param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("10", "30")));
  }

  @Test
  void supportsRepeatedStatusAndInclusiveTimestampBounds() throws Exception {
    mvc.perform(
            get("/projects")
                .param("status", "active", "development")
                .param("ids", "10,20,30")
                .param("createdFrom", "2026-01-01T00:00:00Z")
                .param("createdTo", "2026-01-02T00:00:00Z")
                .param("updatedFrom", "2026-02-01T00:00:00Z")
                .param("updatedTo", "2026-02-02T00:00:00Z")
                .param("sortBy", "id")
                .param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("10", "20")));
  }

  @Test
  void keepsCacheSeparateForDifferentFiltersAndLocales() throws Exception {
    mvc.perform(get("/projects").param("slug", "portfolio-api").header("Accept-Language", "en"))
        .andExpect(jsonPath("$.data[0].id").value("10"))
        .andExpect(jsonPath("$.data[0].name").value("Portfolio API"));
    mvc.perform(get("/projects").param("slug", "portfolio-ui").header("Accept-Language", "en"))
        .andExpect(jsonPath("$.data[0].id").value("20"));
    mvc.perform(get("/projects").param("slug", "portfolio-api").header("Accept-Language", "id"))
        .andExpect(jsonPath("$.data[0].name").value("API Portofolio"));
    mvc.perform(get("/projects").param("slug", "portfolio-api").header("Accept-Language", "en"))
        .andExpect(jsonPath("$.data[0].name").value("Portfolio API"));
  }

  @ParameterizedTest
  @CsvSource({"projects", "skills", "uses", "blogs", "blog-attachments", "experiences", "users"})
  void everyEndpointSupportsIdsAndCursorWithoutMutatingImmutableLists(String endpoint)
      throws Exception {
    mvc.perform(
            get("/" + endpoint).param("ids", "10,20,30").param("cursor", "40").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("30")))
        .andExpect(jsonPath("$.cursor.nextCursor").value("30"))
        .andExpect(jsonPath("$.cursor.hasNextPage").value(true));
    mvc.perform(
            get("/" + endpoint).param("ids", "10,20,30").param("cursor", "30").param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("20", "10")))
        .andExpect(jsonPath("$.cursor.hasNextPage").value(false));
  }

  @Test
  void paginatesAfterApplyingFilters() throws Exception {
    mvc.perform(
            get("/projects")
                .param("status", "active")
                .param("projectTypes", "backend")
                .param("page", "2")
                .param("size", "1")
                .param("sortBy", "id")
                .param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("30")))
        .andExpect(jsonPath("$.pagination.total").value(2));
  }

  @ParameterizedTest
  @CsvSource({"skills,programming_language,framework", "uses,software,hardware"})
  void filtersCategoriesAsTypedEnums(String endpoint, String first, String second)
      throws Exception {
    mvc.perform(
            get("/" + endpoint)
                .param("category", first.toUpperCase())
                .param("ids", "10,20")
                .param("sortBy", "id")
                .param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("10")));
    mvc.perform(
            get("/" + endpoint)
                .param("category", first, second)
                .param("ids", "10,20")
                .param("sortBy", "id")
                .param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("10", "20")));
  }

  @Test
  void filtersDraftBlogsAndViewsRangeAndSeparatesCache() throws Exception {
    mvc.perform(get("/blogs").param("isPublished", "true"))
        .andExpect(jsonPath("$.data[*].id", contains("10")));
    mvc.perform(
            get("/blogs")
                .param("isPublished", "false")
                .param("minViews", "5")
                .param("maxViews", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("20")));
    mvc.perform(get("/blogs").param("slug", "another-draft"))
        .andExpect(jsonPath("$.data[*].id", contains("30")));
  }

  @Test
  void filtersAttachmentsByParentTypeAndName() throws Exception {
    mvc.perform(
            get("/blog-attachments")
                .param("blogId", "10")
                .param("fileType", "IMAGE,document")
                .param("search", "cover"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("10")));
  }

  @Test
  void filtersPastExperienceAndOptionalBoolean() throws Exception {
    mvc.perform(
            get("/experiences")
                .param("companyName", "ACME")
                .param("sortBy", "id")
                .param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("10", "20")));
    mvc.perform(
            get("/experiences")
                .param("isCurrent", "false")
                .param("companyName", "acme")
                .param("position", "frontend")
                .param("startDate", "2021-01-01")
                .param("endDate", "2023-12-31")
                .header("Accept-Language", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("20")));
  }

  @Test
  void filtersUsersByCombinedEnumsAndBirthDates() throws Exception {
    mvc.perform(
            get("/users")
                .param("role", "admin,user")
                .param("provider", "local")
                .param("gender", "female,other")
                .param("dateOfBirthFrom", "1990-01-01")
                .param("dateOfBirthTo", "1995-01-01")
                .param("email", "ALICE@example.com"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("10")));
  }

  @ParameterizedTest
  @CsvSource({
    "projects,status,bogus",
    "projects,projectTypes,bogus",
    "projects,linkTypes,bogus",
    "skills,category,bogus",
    "uses,category,bogus",
    "users,role,bogus",
    "users,provider,bogus",
    "users,gender,bogus",
    "blog-attachments,fileType,bogus",
    "projects,size,0",
    "projects,page,0",
    "projects,size,101",
    "projects,sortBy,bogus",
    "projects,sortDir,bogus",
    "projects,createdFrom,invalid",
    "projects,ids,abc",
    "projects,ids,-1",
    "blogs,minViews,-1",
    "blog-attachments,blogId,-1"
  })
  void rejectsInvalidFilters(String endpoint, String parameter, String value) throws Exception {
    mvc.perform(get("/" + endpoint).param(parameter, value)).andExpect(status().isBadRequest());
  }

  @Test
  void rejectsReversedRangesAndReturnsEmptyForUnmatchedFilters() throws Exception {
    mvc.perform(
            get("/projects")
                .param("createdFrom", "2026-01-02T00:00:00Z")
                .param("createdTo", "2026-01-01T00:00:00Z"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/blogs").param("minViews", "100").param("maxViews", "10"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/projects").param("status", "archived").param("projectTypes", "backend"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", hasSize(0)));
  }

  @Test
  @Sql({"/filtering-fixtures.sql", "/sorting-fixtures.sql"})
  void ordersNamesInTheCurrentLanguageAndKeepsLocaleCachesSeparate() throws Exception {
    mvc.perform(
            get("/projects")
                .header("Accept-Language", "id")
                .param("sortBy", "name")
                .param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("30", "20", "50", "10")))
        .andExpect(jsonPath("$.data[*].resolvedLocale", contains("id", "id", "id", "id")))
        .andExpect(jsonPath("$.pagination.total").value(4));
    mvc.perform(
            get("/projects")
                .header("Accept-Language", "en")
                .param("sortBy", "name")
                .param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("30", "20", "40", "50", "10")))
        .andExpect(jsonPath("$.pagination.total").value(5));
    mvc.perform(
            get("/projects")
                .header("Accept-Language", "id")
                .param("sortBy", "name")
                .param("sortDir", "asc"))
        .andExpect(jsonPath("$.data[*].id", contains("30", "20", "50", "10")));
    mvc.perform(
            get("/projects")
                .header("Accept-Language", "fr")
                .param("sortBy", "name")
                .param("sortDir", "asc"))
        .andExpect(jsonPath("$.data[*].id", contains("30", "20", "40", "50", "10")));
  }

  @ParameterizedTest
  @CsvSource({
    "blogs,title",
    "blogs,content",
    "experiences,position",
    "experiences,description",
    "skills,description",
    "uses,reasons",
    "users,bio"
  })
  @Sql({"/filtering-fixtures.sql", "/sorting-fixtures.sql"})
  void supportsEveryOtherTranslatedTextFieldWithoutFallingBack(String endpoint, String field)
      throws Exception {
    mvc.perform(
            get("/" + endpoint)
                .header("Accept-Language", "id")
                .param("sortBy", field)
                .param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("20", "10")))
        .andExpect(jsonPath("$.data[*].resolvedLocale", contains("id", "id")))
        .andExpect(jsonPath("$.pagination.total").value(2));
    mvc.perform(
            get("/" + endpoint)
                .header("Accept-Language", "id")
                .param("sortBy", field)
                .param("sortDir", "desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("10", "20")));
  }

  @Test
  @Sql({"/filtering-fixtures.sql", "/sorting-fixtures.sql"})
  void keepsCurrentLanguageNullFieldsLastInsteadOfUsingEnglishValues() throws Exception {
    mvc.perform(
            get("/projects")
                .header("Accept-Language", "id")
                .param("sortBy", "description")
                .param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("50", "20", "10", "30")));
    mvc.perform(
            get("/projects")
                .header("Accept-Language", "id")
                .param("sortBy", "description")
                .param("sortDir", "desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("10", "20", "50", "30")));
  }

  @Test
  @Sql({"/filtering-fixtures.sql", "/sorting-fixtures.sql"})
  void paginatesTranslatedOrderingWithoutDuplicateResourcesOrWrongCounts() throws Exception {
    mvc.perform(
            get("/projects")
                .header("Accept-Language", "id")
                .param("sortBy", "name")
                .param("sortDir", "asc")
                .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("30", "20")))
        .andExpect(jsonPath("$.pagination.total").value(4))
        .andExpect(jsonPath("$.pagination.totalPages").value(2));
    mvc.perform(
            get("/projects")
                .header("Accept-Language", "id")
                .param("sortBy", "name")
                .param("sortDir", "asc")
                .param("size", "2")
                .param("page", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("50", "10")))
        .andExpect(jsonPath("$.pagination.total").value(4));
  }

  @Test
  @Sql({"/filtering-fixtures.sql", "/sorting-fixtures.sql"})
  void combinesTranslatedSortingSearchAndScalarFiltersOnOneLanguageRow() throws Exception {
    mvc.perform(
            get("/projects")
                .header("Accept-Language", "id")
                .param("search", "apel")
                .param("status", "active,development")
                .param("sortBy", "name")
                .param("sortDir", "asc")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("30")))
        .andExpect(jsonPath("$.pagination.total").value(2));
    mvc.perform(
            get("/projects")
                .header("Accept-Language", "id")
                .param("search", "apel")
                .param("sortBy", "name")
                .param("sortDir", "asc")
                .param("page", "2")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("20")))
        .andExpect(jsonPath("$.pagination.total").value(2));
  }

  @ParameterizedTest
  @CsvSource({
    "projects,Zulu,Zebra",
    "blogs,Zulu,Zebra",
    "blogs,English-only,Apel",
    "experiences,Backend,Zebra"
  })
  @Sql({"/filtering-fixtures.sql", "/sorting-fixtures.sql"})
  void searchesTranslationsOnlyInTheCurrentLanguage(
      String endpoint, String english, String indonesian) throws Exception {
    mvc.perform(get("/" + endpoint).header("Accept-Language", "id").param("search", english))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", hasSize(0)));
    mvc.perform(get("/" + endpoint).header("Accept-Language", "id").param("search", indonesian))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", hasSize(1)))
        .andExpect(jsonPath("$.data[0].resolvedLocale").value("id"));
    mvc.perform(get("/" + endpoint).header("Accept-Language", "en").param("search", english))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));
  }

  @Test
  @Sql({"/filtering-fixtures.sql", "/sorting-fixtures.sql"})
  void searchesCompanyWithoutRequiringTranslationButKeepsPositionLanguageStrict() throws Exception {
    mvc.perform(get("/experiences").header("Accept-Language", "id").param("search", "Other"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("30")));
    mvc.perform(get("/experiences").header("Accept-Language", "id").param("position", "Designer"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", hasSize(0)));
    mvc.perform(
            get("/experiences")
                .header("Accept-Language", "id")
                .param("search", "acme")
                .param("position", "apel")
                .param("sortBy", "position")
                .param("sortDir", "asc")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("20")))
        .andExpect(jsonPath("$.pagination.total").value(1));
  }

  @Test
  void translatedSortWithNoCurrentTranslationsReturnsAnEmptyPage() throws Exception {
    mvc.perform(get("/blogs").header("Accept-Language", "id").param("sortBy", "title"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", hasSize(0)))
        .andExpect(jsonPath("$.pagination.total").value(0));
  }

  @Test
  @Sql({"/filtering-fixtures.sql", "/sorting-fixtures.sql"})
  void sortsMainTableTextIgnoringCaseWithAnIdTieBreaker() throws Exception {
    mvc.perform(get("/skills").param("sortBy", "name").param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("30", "20", "10")));
  }

  @ParameterizedTest
  @CsvSource({"placeOfBirth", "address", "phoneNumber"})
  @Sql({"/filtering-fixtures.sql", "/sorting-fixtures.sql"})
  void sortsAdditionalUserFields(String field) throws Exception {
    mvc.perform(get("/users").param("sortBy", field).param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("20", "10", "30")));
  }

  @Test
  void sortsAttachmentsByTheirParentBlog() throws Exception {
    mvc.perform(get("/blog-attachments").param("sortBy", "blogId").param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("20", "10", "30")));
  }

  @Test
  void keepsNullableDatesLastInBothDirections() throws Exception {
    mvc.perform(get("/experiences").param("sortBy", "endDate").param("sortDir", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("30", "20", "10")));
    mvc.perform(get("/experiences").param("sortBy", "endDate").param("sortDir", "desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("20", "30", "10")));
  }

  @ParameterizedTest
  @CsvSource({
    "projects,name",
    "blogs,title",
    "experiences,position",
    "skills,name",
    "uses,itemName",
    "users,nickname",
    "blog-attachments,fileName"
  })
  void rejectsCustomCursorSortingInsteadOfIgnoringIt(String endpoint, String field)
      throws Exception {
    mvc.perform(get("/" + endpoint).param("cursor", "40").param("sortBy", field))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/" + endpoint).param("cursor", "40").param("sortBy", "id").param("sortDir", "asc"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/" + endpoint)
                .param("cursor", "40")
                .param("sortBy", "id")
                .param("sortDir", "desc")
                .param("ids", "10,20,30")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].id", contains("30")));
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "projects|name|invalid",
        "projects|name|asc,desc",
        "projects|name,name|asc",
        "experiences|jobdesks|asc",
        "projects|projectTypes|asc"
      },
      delimiter = '|')
  void rejectsInvalidOrUnsupportedSortRequests(String endpoint, String field, String direction)
      throws Exception {
    mvc.perform(get("/" + endpoint).param("sortBy", field).param("sortDir", direction))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @CsvSource({"projects", "blogs", "experiences", "skills", "uses", "users", "blog-attachments"})
  void everyEndpointRejectsMultipleSortFieldsAndDirections(String endpoint) throws Exception {
    String path = "/" + endpoint;
    mvc.perform(get(path).param("sortBy", "id,createdAt").param("sortDir", "asc"))
        .andExpect(status().isBadRequest());
    mvc.perform(get(path).param("sortBy", "id").param("sortDir", "asc,desc"))
        .andExpect(status().isBadRequest());
    mvc.perform(get(path).param("sortBy", "id", "createdAt")).andExpect(status().isBadRequest());
    mvc.perform(get(path).param("sortDir", "asc", "desc")).andExpect(status().isBadRequest());
    mvc.perform(get(path).param("sortBy", "id", "id")).andExpect(status().isBadRequest());
    mvc.perform(get(path).param("sortDir", "asc", "asc")).andExpect(status().isBadRequest());
  }

  @TestConfiguration
  @EnableJpaRepositories(basePackages = "com.api.rizz.portfolio_api.repository")
  @EnableTransactionManagement
  @EnableCaching
  @ComponentScan(basePackageClasses = ProjectMapper.class)
  @Import({
    ProjectService.class,
    SkillService.class,
    UseService.class,
    BlogService.class,
    BlogAttachmentService.class,
    ExperienceService.class,
    UserService.class
  })
  static class Config {
    @Bean
    DataSource dataSource() {
      return new DriverManagerDataSource(System.getenv("FILTER_TEST_DATABASE_URL"));
    }

    @Bean(initMethod = "migrate")
    Flyway flyway(DataSource dataSource) {
      return Flyway.configure().dataSource(dataSource).load();
    }

    @Bean
    @DependsOn("flyway")
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
      LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(dataSource);
      factory.setPackagesToScan("com.api.rizz.portfolio_api.entity");
      factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
      return factory;
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
      return new JpaTransactionManager(factory);
    }

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager();
    }

    @Bean
    SnowflakeGenerator snowflakeGenerator() {
      return new SnowflakeGenerator(1, 1);
    }

    @Bean
    FileUploadService fileUploadService() {
      return org.mockito.Mockito.mock(FileUploadService.class);
    }
  }
}
