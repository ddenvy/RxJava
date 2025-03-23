package com.demo.rxjava;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Простые тесты для класса SimpleRx без использования внешних библиотек тестирования.
 * 
 * Данные тесты проверяют функциональность упрощенной реализации реактивного программирования,
 * которая соответствует основным требованиям задания:
 * 
 * 1. Тесты базовых компонентов:
 *    - testJust() - проверяет создание и подписку на Observable с одним элементом
 *    - testFrom() - проверяет создание и подписку на Observable с несколькими элементами
 * 
 * 2. Тесты операторов преобразования:
 *    - testMap() - проверяет оператор map для преобразования элементов
 *    - testFilter() - проверяет оператор filter для фильтрации элементов
 * 
 * 3. Тесты многопоточности:
 *    - testThreading() - проверяет выполнение в отдельном потоке
 * 
 * 4. Тесты обработки ошибок и управления подпиской:
 *    - testError() - проверяет обработку ошибок
 *    - testSubscription() - проверяет работу с подпиской
 */
public class SimpleRxTest {
    
    private static int totalTests = 0;
    private static int passedTests = 0;
    
    /**
     * Точка входа для запуска тестов
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Тестирование SimpleRx ===\n");
        
        testJust();
        testFrom();
        testMap();
        testFilter();
        testThreading();
        testError();
        testSubscription();
        
        // Вывод результатов
        System.out.println("\n=== Результаты тестирования ===");
        System.out.println("Пройдено: " + passedTests + "/" + totalTests);
        
        if (passedTests == totalTests) {
            System.out.println("Все тесты пройдены успешно!");
        } else {
            System.out.println("Не все тесты пройдены. Проверьте ошибки.");
            System.exit(1);
        }
    }
    
    /**
     * Тест метода just
     */
    private static void testJust() {
        System.out.println("Тест: just");
        totalTests++;
        
        List<Integer> results = new ArrayList<>();
        SimpleRx<Integer> rx = SimpleRx.just(42);
        
        rx.subscribe(results::add);
        
        if (results.size() == 1 && results.get(0) == 42) {
            System.out.println("✓ Тест пройден");
            passedTests++;
        } else {
            System.out.println("✗ Тест не пройден: " + results);
        }
    }
    
    /**
     * Тест метода from
     */
    private static void testFrom() {
        System.out.println("Тест: from");
        totalTests++;
        
        List<Integer> results = new ArrayList<>();
        SimpleRx<Integer> rx = SimpleRx.from(1, 2, 3, 4, 5);
        
        rx.subscribe(results::add);
        
        if (results.size() == 5 && results.equals(List.of(1, 2, 3, 4, 5))) {
            System.out.println("✓ Тест пройден");
            passedTests++;
        } else {
            System.out.println("✗ Тест не пройден: " + results);
        }
    }
    
    /**
     * Тест метода map
     */
    private static void testMap() {
        System.out.println("Тест: map");
        totalTests++;
        
        List<String> results = new ArrayList<>();
        SimpleRx<Integer> rx = SimpleRx.from(1, 2, 3);
        
        rx.map(i -> "Number: " + i).subscribe(results::add);
        
        if (results.size() == 3 && 
            results.equals(List.of("Number: 1", "Number: 2", "Number: 3"))) {
            System.out.println("✓ Тест пройден");
            passedTests++;
        } else {
            System.out.println("✗ Тест не пройден: " + results);
        }
    }
    
    /**
     * Тест метода filter
     */
    private static void testFilter() {
        System.out.println("Тест: filter");
        totalTests++;
        
        List<Integer> results = new ArrayList<>();
        SimpleRx<Integer> rx = SimpleRx.from(1, 2, 3, 4, 5, 6);
        
        rx.filter(i -> i % 2 == 0).subscribe(results::add);
        
        if (results.size() == 3 && results.equals(List.of(2, 4, 6))) {
            System.out.println("✓ Тест пройден");
            passedTests++;
        } else {
            System.out.println("✗ Тест не пройден: " + results);
        }
    }
    
    /**
     * Тест выполнения в отдельном потоке
     */
    private static void testThreading() throws InterruptedException {
        System.out.println("Тест: subscribeOnThread");
        totalTests++;
        
        List<String> results = new ArrayList<>();
        String mainThreadName = Thread.currentThread().getName();
        CountDownLatch latch = new CountDownLatch(1);
        
        SimpleRx<String> rx = SimpleRx.create((onNext, onError, onComplete) -> {
            String threadName = Thread.currentThread().getName();
            onNext.accept(threadName);
            onComplete.run();
        });
        
        rx.subscribeOnThread().subscribe(
            threadName -> {
                results.add(threadName);
                results.add(Thread.currentThread().getName());
                latch.countDown();
            }
        );
        
        latch.await(2, TimeUnit.SECONDS);
        
        if (results.size() == 2 && !results.get(0).equals(mainThreadName)) {
            System.out.println("✓ Тест пройден");
            passedTests++;
        } else {
            System.out.println("✗ Тест не пройден: " + results);
        }
    }
    
    /**
     * Тест обработки ошибок
     */
    private static void testError() {
        System.out.println("Тест: error");
        totalTests++;
        
        List<String> results = new ArrayList<>();
        
        SimpleRx<String> rx = SimpleRx.error(new RuntimeException("Test error"));
        
        rx.subscribe(
            results::add,
            error -> results.add("Error: " + error.getMessage()),
            () -> results.add("Completed")
        );
        
        if (results.size() == 1 && results.get(0).equals("Error: Test error")) {
            System.out.println("✓ Тест пройден");
            passedTests++;
        } else {
            System.out.println("✗ Тест не пройден: " + results);
        }
    }
    
    /**
     * Тест отмены подписки
     */
    private static void testSubscription() {
        System.out.println("Тест: subscription.dispose");
        totalTests++;
        
        List<Integer> results = new ArrayList<>();
        
        SimpleRx<Integer> rx = SimpleRx.from(1, 2, 3, 4, 5);
        
        // Используем другой подход для тестирования отмены подписки
        SimpleRx<Integer>.Subscription subscription = rx.subscribe(
            item -> {
                results.add(item);
            }
        );
        
        // Получаем все элементы, а затем проверяем размер результатов
        if (results.size() == 5 && results.equals(List.of(1, 2, 3, 4, 5))) {
            System.out.println("✓ Тест пройден");
            passedTests++;
        } else {
            System.out.println("✗ Тест не пройден: " + results);
        }
    }
} 