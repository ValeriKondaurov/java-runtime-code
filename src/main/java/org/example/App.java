package org.example;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;

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
    public static final String NAME_METHOD = "helloBankiRu";

    public static void main(String[] args) throws Exception {
        App app = new App();
        final var library = "javapoet";
//        final var library = "codemodel";

        app.delete(library);
        if (library.equals("javapoet")) {
            app.generate2();
        } else if (library.equals("codemodel")) {
            app.generate();
        } else {
            return;
        }
        app.compile(library);
        app.run(library);
        app.delete(library);
    }


    public void generate() throws JClassAlreadyExistsException, IOException {
        //создаем модель, это своего рода корень вашего дерева кода
        JCodeModel codeModel = new JCodeModel();

        //определяем наш класс BankiRu в пакете hello
        JDefinedClass testClass = codeModel._class("codemodel.HelloWorld");

        // определяем метод helloBankiRu
        JMethod method = testClass.method(JMod.PUBLIC + JMod.STATIC, codeModel.VOID, NAME_METHOD);
        method.param(String[].class, "args");
        // в теле метода выводим строку "Hello BankiRu!"
        method.body().directStatement("System.out.println(\"Hello BankiRu FROM CodeModel!\");");

        //собираем модель и пишем пакеты в currentDirectory
        codeModel.build(Paths.get(JAVA_PATH).toAbsolutePath().toFile());
    }


    public void generate2() throws IOException {
        //Создание метода
        MethodSpec main = MethodSpec.methodBuilder(NAME_METHOD)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(String[].class, "args")
                .addStatement("$T.out.println($S)", System.class, "Hello, BankiRu FROM JavaPoet!")
                .build();

        //Создание метода
        TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(main)
                .build();
        //Создание файла-класса
        JavaFile javaFile = JavaFile.builder("javapoet", helloWorld)
                .build();
        //сохранение в папке
        javaFile.writeTo(Paths.get(JAVA_PATH).toAbsolutePath().toFile());

    }

    public void compile(String packageName) throws IOException {
        Path srcPath = Paths.get(JAVA_PATH_PATTERN.formatted(packageName));
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

    private void run(String packageName) throws ClassNotFoundException, RuntimeException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //получаем ClassLoader, лучше получать лоадер от текущего класса,
        ClassLoader classLoader = App.class.getClassLoader();

        //получаем путь до нашей папки со сгенерированным кодом
        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{Paths.get(JAVA_PATH).toUri().toURL()}, classLoader)) {
            //загружаем наш класс
            Class<?> helloBankiRuClass = urlClassLoader.loadClass("%s.HelloWorld".formatted(packageName));

            final var args = new String[0];

            //находим и вызываем метод helloBankiRu
            Method methodHelloBankiRu = helloBankiRuClass.getMethod(NAME_METHOD, args.getClass());

            //в параметре передается ссылка на экземпляр класса для вызова метода
            //либо null при вызове статического метода
            methodHelloBankiRu.invoke(null, (Object) args);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

private void delete(String packageName) throws IOException {
    Path srcPath = Paths.get(JAVA_PATH_PATTERN.formatted(packageName));

    try (final var list = Files.list(srcPath)) {
        list.map(Path::toFile)
                .forEach(File::delete);
        Files.delete(srcPath);
    }

}
}
