package com.demo.rxjava.example;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.demo.rxjava.SimpleRx;

/**
 * Простой пример использования SimpleRx
 * 
 * Данный пример демонстрирует упрощенную реализацию реактивного программирования,
 * которая объединяет несколько концепций из требований задания:
 * 
 * 1. Базовые компоненты:
 *    - Создание Observable через SimpleRx.from() и SimpleRx.create()
 *    - Подписка с обработчиками onNext, onError, onComplete
 * 
 * 2. Операторы преобразования:
 *    - map - преобразует элементы потока
 *    - filter - фильтрует элементы по условию
 * 
 * 3. Многопоточность:
 *    - subscribeOnThread - демонстрирует выполнение в отдельном потоке
 * 
 * 4. Обработка ошибок:
 *    - Демонстрация механизма передачи и обработки ошибок
 */
public class SimpleExample {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Демонстрация SimpleRx ===\n");
        
        basicExample();
        operatorsExample();
        threadingExample();
        errorHandlingExample();
        
        System.out.println("\nВсе примеры выполнены!");
    }
    
    /**
     * Базовый пример создания и подписки
     */
    private static void basicExample() {
        System.out.println("=== Базовый пример ===");
        
        SimpleRx<Integer> numbers = SimpleRx.from(1, 2, 3, 4, 5);
        
        numbers.subscribe(
            item -> System.out.println("Получено: " + item),
            error -> System.err.println("Ошибка: " + error.getMessage()),
            () -> System.out.println("Завершено")
        );
    }
    
    /**
     * Пример использования операторов
     */
    private static void operatorsExample() {
        System.out.println("\n=== Операторы ===");
        
        SimpleRx<Integer> numbers = SimpleRx.from(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        numbers
            .filter(n -> n % 2 == 0)  // Только четные числа
            .map(n -> "Четное число: " + n)  // Преобразуем в строки
            .subscribe(
                System.out::println,
                error -> System.err.println("Ошибка: " + error.getMessage()),
                () -> System.out.println("Обработка четных чисел завершена")
            );
    }
    
    /**
     * Пример многопоточности
     */
    private static void threadingExample() throws InterruptedException {
        System.out.println("\n=== Многопоточность ===");
        
        CountDownLatch latch = new CountDownLatch(1);
        
        System.out.println("Основной поток: " + Thread.currentThread().getName());
        
        SimpleRx<String> observable = SimpleRx.create((onNext, onError, onComplete) -> {
            onNext.accept("Сообщение из потока: " + Thread.currentThread().getName());
            onComplete.run();
        });
        
        observable
            .subscribeOnThread()
            .subscribe(
                message -> {
                    System.out.println(message);
                    System.out.println("Обработано в потоке: " + Thread.currentThread().getName());
                    latch.countDown();
                },
                error -> {
                    System.err.println("Ошибка: " + error.getMessage());
                    latch.countDown();
                },
                () -> System.out.println("Завершено в потоке: " + Thread.currentThread().getName())
            );
        
        latch.await(2, TimeUnit.SECONDS);
    }
    
    /**
     * Пример обработки ошибок
     */
    private static void errorHandlingExample() {
        System.out.println("\n=== Обработка ошибок ===");
        
        SimpleRx<Integer> observable = SimpleRx.create((onNext, onError, onComplete) -> {
            try {
                onNext.accept(1);
                onNext.accept(2);
                // Генерируем ошибку
                throw new RuntimeException("Демонстрационная ошибка");
                // До этого кода выполнение не дойдет
            } catch (Exception e) {
                onError.accept(e);
            }
        });
        
        observable.subscribe(
            item -> System.out.println("Получено: " + item),
            error -> System.out.println("Перехвачена ошибка: " + error.getMessage()),
            () -> System.out.println("Завершено (не должно быть вызвано)")
        );
    }
} 