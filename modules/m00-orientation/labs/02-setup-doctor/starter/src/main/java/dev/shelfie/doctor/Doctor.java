package dev.shelfie.doctor;

import java.util.ArrayList;
import java.util.List;

/**
 * Lab 0.2: your own setup doctor. Read the lab README for the exact output format and checks.
 * Run it with:  ./mvnw -q compile exec:java   (or mvn, inside bin/course shell)
 */
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
        // TODO: check that `hurl` runs            (name: "hurl")
        // TODO: check that `jq` runs              (name: "jq")
        // TODO: check the Shelfie server is up    (name: "shelfie")
        // TODO: warn (do not fail) when GITHUB_TOKEN is missing  (name: "github-token")

        results.forEach(r -> System.out.println(r.line()));
        // TODO: exit with status 1 when any check FAILed, 0 otherwise
    }

    static Result checkJava() {
        int feature = Runtime.version().feature();
        return feature >= 25
                ? new Result(Status.OK, "java", "Java " + feature)
                : new Result(Status.FAIL, "java", "Java " + feature + " found, need 25+");
    }
}
