import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.*;
import java.io.IOException;
import java.util.List;

public class CommandExecutor {

    public static void execute(String cmd, List<String> args) {
        switch (cmd) {
            case "echo" -> echo(args);
            case "ls" -> ls(args);
            case "cat" -> cat(args);
            default -> System.out.println("Unknown command: " + cmd);
        }
    }

    private static void echo(List<String> args) {
        System.out.println(String.join(" ", args));
    }

    private static void ls(List<String> args) {
        Path dir;
        if (args.isEmpty()) {
            dir = Paths.get(".");
        } else {
            dir = Paths.get(args.getFirst());
        }

        try (var stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                System.out.println(p.getFileName().toString());
            }
        } catch (IOException e) {
            System.out.println("ls: " + e.getMessage());
        }
    }

    private static void cat(List<String> args) {
        if(args.isEmpty()) {
            System.out.println("Usage: cat [filename]");
            return;
        }
        for(String arg : args) {
            try(BufferedReader bufferedReader = new BufferedReader(new FileReader(arg))) {
                System.out.println(String.join(" ", bufferedReader.readAllAsString()));
            } catch(IOException e) {
                System.out.println("Cannot open file: " + arg);
            }

        }
    }
}
