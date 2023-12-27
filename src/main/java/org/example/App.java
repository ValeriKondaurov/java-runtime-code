package org.example;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class App {

    public static final String JAVA_PATH = ".";
    public static final String JAVA_PATH_PATTERN = JAVA_PATH + "/%s";
    public static final String METHOD_NAME = "congratulation";
    public static final String CLASS_NAME = "HappyNewYear";
    public static final String DIRECTORY = "javapoet";
    public static final String NAME = "Valera";

    public static void main(String[] args) throws Exception {
        final var happyNewYear = new HappyNewYear();
        happyNewYear.congratulation(NAME);
        delete();
        generate();
        compile();
        run();
    }


    public static void generate() throws IOException {
        //Создание метода
        MethodSpec methodSpec = MethodSpec.methodBuilder(METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "name")
                .addStatement("$T.out.println($S + name  + $S)", System.class, "Happy New Year, ", "! \n Your Banki.ru" )
                .build();

        //Создание типа
        TypeSpec typeSpec = TypeSpec.classBuilder(CLASS_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(methodSpec)
                .build();
        //Создание файла
        JavaFile javaFile = JavaFile.builder(DIRECTORY, typeSpec)
                .build();
        //сохранение в папке
        javaFile.writeTo(Paths.get(JAVA_PATH).toAbsolutePath().toFile());

    }

    public static void compile() throws IOException {
        Path srcPath = Paths.get(JAVA_PATH_PATTERN.formatted(DIRECTORY));
        //получаем компилятор
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        try (final var stream = Files.list(srcPath);
             //получаем новый инстанс fileManager для нашего компилятора
             StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            final var list = stream.map(Path::toFile).toList();
            //получаем список всех файлов описывающих исходники
            Iterable<? extends JavaFileObject> javaFiles = fileManager.getJavaFileObjectsFromFiles(list);

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            //заводим задачу на компиляцию
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    null,
                    null,
                    javaFiles
            );
            //выполняем задачу
            task.call();
            //выводим ошибки, возникшие в процессе компиляции
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                System.out.format("Error on line %d in %s%n",
                        diagnostic.getLineNumber(),
                        diagnostic.getSource());
            }

        }
    }

    private static void run() throws ReflectiveOperationException {
        //получаем ClassLoader, лучше получать лоадер от текущего класса,
        ClassLoader classLoader = App.class.getClassLoader();

        //получаем путь до нашей папки со сгенерированным кодом
        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{Paths.get(JAVA_PATH).toUri().toURL()}, classLoader)) {
            //загружаем наш класс
            Class<?> clazz = urlClassLoader.loadClass("%s.%s".formatted(DIRECTORY, CLASS_NAME));

            //находим и вызываем метод helloBankiRu
            Method method = clazz.getMethod(METHOD_NAME, NAME.getClass());
            //Создаем инстанс класс через конструктор
            final Object newInstance = clazz.getDeclaredConstructor().newInstance(null);

            //в параметре передается ссылка на экземпляр класса для вызова метода
            //либо null при вызове статического метода
            method.invoke(newInstance, (Object) NAME);
        } catch (IOException | InstantiationException e) {
            throw new RuntimeException(e);
        }

    }

    private static void delete() throws IOException {
        Path srcPath = Paths.get(JAVA_PATH_PATTERN.formatted(DIRECTORY));

        try (final var list = Files.list(srcPath)) {
            list.map(Path::toFile)
                    .forEach(File::delete);
            Files.delete(srcPath);
        }
    }
}
