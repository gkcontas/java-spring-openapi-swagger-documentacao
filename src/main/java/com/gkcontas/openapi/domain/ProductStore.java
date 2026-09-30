package com.gkcontas.openapi.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * In memory on purpose.
 *
 * <p>This project is about the contract, and a database would add migrations, containers
 * and a slower test suite without changing a single line of the OpenAPI document. The
 * storage is the one part here that is genuinely an implementation detail.
 */
@Component
public class ProductStore {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public ProductStore() {
        seed();
    }

    /**
     * Restores the initial catalogue. Used by the test suite: every test class shares one
     * cached Spring context, so they also share this bean — without a reset, a product
     * created by one class changes the totals another class asserts, and the suite starts
     * failing depending on the order JUnit happens to pick.
     */
    public void reset() {
        products.clear();
        sequence.set(0);
        seed();
    }

    private void seed() {
        save(new Product(null, "Mechanical keyboard", "KB-001", new BigDecimal("450.00"), "BRL",
                List.of("peripherals", "input"), ProductKind.PHYSICAL, 1.2, null, false));
        save(new Product(null, "Ergonomic mouse", "MS-002", new BigDecimal("180.00"), "BRL",
                List.of("peripherals", "input"), ProductKind.PHYSICAL, 0.3, null, false));
        save(new Product(null, "Design toolkit licence", "SW-100", new BigDecimal("990.00"), "BRL",
                List.of("software"), ProductKind.DIGITAL, null, "https://example.com/download/sw-100", false));
        save(new Product(null, "Legacy docking station", "DK-009", new BigDecimal("620.00"), "BRL",
                List.of("peripherals"), ProductKind.PHYSICAL, 0.8, null, true));
    }

    public Product save(Product product) {
        long id = product.id() == null ? sequence.incrementAndGet() : product.id();
        Product stored = new Product(id, product.name(), product.sku(), product.price(), product.currency(),
                product.categories(), product.kind(), product.weightKg(), product.downloadUrl(),
                product.discontinued());
        products.put(id, stored);
        return stored;
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    public boolean deleteById(Long id) {
        return products.remove(id) != null;
    }

    public List<Product> findAll(String category, int page, int size) {
        return products.values().stream()
                .filter(product -> category == null || product.categories().contains(category))
                .sorted((left, right) -> Long.compare(left.id(), right.id()))
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    public long count(String category) {
        return products.values().stream()
                .filter(product -> category == null || product.categories().contains(category))
                .count();
    }
}
