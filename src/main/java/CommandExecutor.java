import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public class CommandExecutor {


    public static void execute(String cmd, List<String> args) {
        execute(cmd, args, null);
    }

    public static void execute(String cmd, List<String> args, RedirectInfo redirect) {
        if (redirect == null) {
            runCommand(cmd, args, System.out);
            return;
        }

        boolean append = redirect.append();
        String filename = "out" + File.separator + redirect.filename();

        try (PrintStream out = new PrintStream(
                new FileOutputStream(filename, append),
                true,
                StandardCharsets.UTF_8)) {
            runCommand(cmd, args, out);
        } catch (IOException e) {
            System.out.println("redirect error: " + e.getMessage());
        }
    }

    private static void runCommand(String cmd, List<String> args, PrintStream out) {
        switch(cmd) {
            case "echo" -> echo(args, out);
            case "ls" -> ls(args, out);
            case "cat" -> cat(args, out);
            case "wc" -> wc(args, out);
            default -> out.println("Unknown command: " + cmd);
        }
    }

    private static void echo(List<String> args, PrintStream out) {
        out.println(String.join(" ", args));
    }

    private static void ls(List<String> args, PrintStream out) {
        Path dir;
        if (args.isEmpty()) {
            dir = Paths.get(".");
        } else {
            dir = Paths.get(args.getFirst());
        }

        try (var stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                out.println(p.getFileName().toString());
            }
        } catch (IOException e) {
            out.println("ls: " + e.getMessage());
        }
    }

    private static void cat(List<String> args, PrintStream out) {
        if(args.isEmpty()) {
            out.println("Usage: cat [filename]");
            return;
        }
        for(String arg : args) {
            try (BufferedReader br = new BufferedReader(new FileReader(arg))) {
                String line;
                while ((line = br.readLine()) != null) {
                    out.println(line);
                }
            } catch(IOException e) {
                out.println("Cannot open file: " + arg);
            }
        }
    }

    private static void wc(List<String> args, PrintStream out) {
        if(args.isEmpty()) {
            out.println("Usage: wc [filename]");
            return;
        }
        for(String arg : args) {
            try (BufferedReader br = new BufferedReader(new FileReader(arg))) {
                out.println(br.readAllLines().size());
            } catch(IOException e) {
                out.println("Cannot open file: " + arg);
            }
        }
    }
}
