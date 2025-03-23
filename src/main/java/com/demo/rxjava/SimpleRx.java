package com.demo.rxjava;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Упрощенная реализация реактивного программирования.
 * 
 * Данная реализация объединяет в одном классе функциональность нескольких компонентов,
 * которые обычно представлены отдельными интерфейсами и классами в полной реализации:
 * 
 * 1. Вместо интерфейса Observer используются функциональные интерфейсы:
 *    - Consumer<T> для onNext
 *    - Consumer<Throwable> для onError
 *    - Runnable для onComplete
 * 
 * 2. Класс включает функциональность Observable с поддержкой операторов:
 *    - map для преобразования элементов
 *    - filter для фильтрации элементов
 * 
 * 3. Вместо отдельных реализаций Scheduler используется метод subscribeOnThread(),
 *    реализующий выполнение в отдельном потоке через ExecutorService
 * 
 * 4. Вместо интерфейса Disposable используется внутренний класс Subscription
 *    для управления подпиской и её отмены
 * 
 * @param <T> тип данных в потоке
 */
public class  SimpleRx<T> {
    private final List<Consumer<T>> nextHandlers = new ArrayList<>();
    private final List<Consumer<Throwable>> errorHandlers = new ArrayList<>();
    private final List<Runnable> completeHandlers = new ArrayList<>();
    private final SourceFunction<T> source;
    private boolean completed = false;
    private boolean hasError = false;
    private ExecutorService executor;

    /**
     * Функциональный интерфейс для источника данных
     */
    @FunctionalInterface
    public interface SourceFunction<T> {
        void subscribe(Consumer<T> onNext, Consumer<Throwable> onError, Runnable onComplete);
    }

    /**
     * Конструктор
     * @param source источник данных
     */
    private SimpleRx(SourceFunction<T> source) {
        this.source = source;
    }

    /**
     * Создает новый SimpleRx из значения
     * @param value значение
     * @return новый SimpleRx
     */
    public static <T> SimpleRx<T> just(T value) {
        return new SimpleRx<>(
            (onNext, onError, onComplete) -> {
                onNext.accept(value);
                onComplete.run();
            }
        );
    }

    /**
     * Создает новый SimpleRx из нескольких значений
     * @param values значения
     * @return новый SimpleRx
     */
    @SafeVarargs
    public static <T> SimpleRx<T> from(T... values) {
        return new SimpleRx<>(
            (onNext, onError, onComplete) -> {
                for (T value : values) {
                    onNext.accept(value);
                }
                onComplete.run();
            }
        );
    }

    /**
     * Создает новый SimpleRx с ошибкой
     * @param error ошибка
     * @return новый SimpleRx
     */
    public static <T> SimpleRx<T> error(Throwable error) {
        return new SimpleRx<>(
            (onNext, onError, onComplete) -> {
                onError.accept(error);
            }
        );
    }

    /**
     * Создает SimpleRx с пользовательской логикой
     * @param source источник данных
     * @return новый SimpleRx
     */
    public static <T> SimpleRx<T> create(SourceFunction<T> source) {
        return new SimpleRx<>(source);
    }

    /**
     * Преобразует элементы потока
     * @param mapper функция преобразования
     * @return новый SimpleRx с преобразованными элементами
     */
    public <R> SimpleRx<R> map(Function<T, R> mapper) {
        return new SimpleRx<>((onNext, onError, onComplete) -> 
            subscribe(
                item -> onNext.accept(mapper.apply(item)),
                onError,
                onComplete
            )
        );
    }

    /**
     * Фильтрует элементы потока
     * @param predicate функция фильтрации
     * @return новый SimpleRx с отфильтрованными элементами
     */
    public SimpleRx<T> filter(Predicate<T> predicate) {
        return new SimpleRx<>((onNext, onError, onComplete) -> 
            subscribe(
                item -> {
                    if (predicate.test(item)) {
                        onNext.accept(item);
                    }
                },
                onError,
                onComplete
            )
        );
    }

    /**
     * Выполняет подписку в отдельном потоке
     * @return новый SimpleRx
     */
    public SimpleRx<T> subscribeOnThread() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        SimpleRx<T> result = new SimpleRx<>((onNext, onError, onComplete) -> {
            executor.execute(() -> subscribe(onNext, onError, () -> {
                onComplete.run();
                executor.shutdown();
            }));
        });
        result.executor = executor;
        return result;
    }

    /**
     * Подписывается на события потока
     * @param onNext обработчик элементов
     * @param onError обработчик ошибок
     * @param onComplete обработчик завершения
     * @return объект для отмены подписки
     */
    public Subscription subscribe(Consumer<T> onNext, Consumer<Throwable> onError, Runnable onComplete) {
        if (onNext != null) nextHandlers.add(onNext);
        if (onError != null) errorHandlers.add(onError);
        if (onComplete != null) completeHandlers.add(onComplete);
        
        source.subscribe(
            item -> {
                if (!completed && !hasError) {
                    for (Consumer<T> handler : nextHandlers) {
                        handler.accept(item);
                    }
                }
            },
            error -> {
                if (!completed && !hasError) {
                    hasError = true;
                    for (Consumer<Throwable> handler : errorHandlers) {
                        handler.accept(error);
                    }
                }
            },
            () -> {
                if (!completed && !hasError) {
                    completed = true;
                    for (Runnable handler : completeHandlers) {
                        handler.run();
                    }
                }
                
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdown();
                }
            }
        );
        
        return new Subscription();
    }

    /**
     * Подписывается только на новые элементы
     * @param onNext обработчик элементов
     * @return объект для отмены подписки
     */
    public Subscription subscribe(Consumer<T> onNext) {
        return subscribe(onNext, e -> e.printStackTrace(), () -> {});
    }

    /**
     * Класс для отмены подписки
     */
    public class Subscription {
        private boolean disposed = false;

        /**
         * Отменяет подписку
         */
        public void dispose() {
            if (!disposed) {
                disposed = true;
                completed = true;
                nextHandlers.clear();
                errorHandlers.clear();
                completeHandlers.clear();
                
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdown();
                }
            }
        }

        /**
         * Проверяет, отменена ли подписка
         * @return true, если подписка отменена
         */
        public boolean isDisposed() {
            return disposed;
        }
    }
} 