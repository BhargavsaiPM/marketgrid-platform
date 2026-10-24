# Bolt's Journal - Performance Optimizations

## 2026-10-24 - Fix JPA N+1 Query Overhead & Missing Foreign Key Indexes
**Learning:** JPA Lazy loading default on @OneToMany collections causes N+1 SQL queries when mapping Entities to DTOs in loop/stream mappings. Using `@EntityGraph` forces eager single-JOIN queries for targeted repository calls without changing lazy defaults globally. Adding `@Index` on foreign keys (`userId`, `vendorId`) significantly accelerates lookup queries under database load.
**Action:** Apply `@EntityGraph` on collection repository lookup methods and define `@Index` on foreign key columns across entities.
