package org.example.achat.product.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class ProductSchemaMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Test
    void createsCatalogTablesWithNonCascadingCategoryReferenceAndAllowsDuplicateCodes() throws Exception {
        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO product_categories (id_category, category_code, label)
                    VALUES (1, 'CAT-A', 'Category A')
                    """);
            statement.executeUpdate("""
                    INSERT INTO products (id_product, code_product, label, price, category_id)
                    VALUES (1, 'DUPLICATE', 'Product A', 1.25, 1),
                           (2, 'DUPLICATE', 'Product B', 2.50, 1)
                    """);

            try (ResultSet products = statement.executeQuery(
                    "SELECT COUNT(*) FROM products WHERE code_product = 'DUPLICATE'")) {
                products.next();
                assertThat(products.getInt(1)).isEqualTo(2);
            }

            boolean categoryDeleteRejected = false;
            try {
                statement.executeUpdate("DELETE FROM product_categories WHERE id_category = 1");
            } catch (java.sql.SQLIntegrityConstraintViolationException expected) {
                categoryDeleteRejected = true;
            }
            assertThat(categoryDeleteRejected).isTrue();
        }
    }
}
