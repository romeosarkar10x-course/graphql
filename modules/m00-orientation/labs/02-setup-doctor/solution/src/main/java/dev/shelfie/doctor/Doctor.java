package dev.shelfie.doctor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Checks the course toolchain. Exit code 1 when any required check fails. */
public class Doctor {

    enum Status { OK, WARN, FAIL }

    record Result(Status status, String name, String detail) {
        String line() {
            return switch (status) {
                case OK -> "ok   " + name + (detail.isEmpty() ? "" : " (" + detail + ")");
                case WARN -> "warn " + name + ": " + detail;
                case FAIL -> "FAIL " + name + ": " + detail;
            };
        }
    }

    public static void main(String[] args) {
        List<Result> results = new ArrayList<>();
        results.add(checkJava());
        results.add(checkCommand("hurl", "hurl", "--version"));
        results.add(checkCommand("jq", "jq", "--version"));
        results.add(checkShelfie());
        results.add(checkGithubToken());

        results.forEach(r -> System.out.println(r.line()));
        long failed = results.stream().filter(r -> r.status() == Status.FAIL).count();
        System.out.println(failed == 0 ? "doctor: all required checks passed" : "doctor: " + failed + " check(s) failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    static Result checkJava() {
        int feature = Runtime.version().feature();
        return feature >= 25
                ? new Result(Status.OK, "java", "Java " + feature)
                : new Result(Status.FAIL, "java", "Java " + feature + " found, need 25+; install JDK 25 or use bin/course");
    }

    static Result checkCommand(String name, String... command) {
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
            String first = new String(p.getInputStream().readAllBytes()).lines().findFirst().orElse("");
            if (!p.waitFor(5, TimeUnit.SECONDS) || p.exitValue() != 0) {
                return new Result(Status.FAIL, name, name + " exited with an error; reinstall it");
            }
            return new Result(Status.OK, name, first.trim());
        } catch (IOException e) {
            return new Result(Status.FAIL, name, name + " not found on PATH; install it or run inside bin/course shell");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(Status.FAIL, name, "interrupted");
        }
    }

    static Result checkShelfie() {
        String base = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080");
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            HttpResponse<String> res = http.send(
                    HttpRequest.newBuilder(URI.create(base + "/actuator/health")).timeout(Duration.ofSeconds(3)).build(),
                    HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200
                    ? new Result(Status.OK, "shelfie", base)
                    : new Result(Status.FAIL, "shelfie", base + " answered " + res.statusCode() + "; restart with bin/course ref up");
        } catch (IOException e) {
            return new Result(Status.FAIL, "shelfie", "cannot reach " + base + "; start it with bin/course ref up");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(Status.FAIL, "shelfie", "interrupted");
        }
    }

    static Result checkGithubToken() {
        String token = System.getenv("GITHUB_TOKEN");
        return token == null || token.isBlank()
                ? new Result(Status.WARN, "github-token", "GITHUB_TOKEN not set; only lab 0.3's GitHub part needs it")
                : new Result(Status.OK, "github-token", "set");
    }
}
