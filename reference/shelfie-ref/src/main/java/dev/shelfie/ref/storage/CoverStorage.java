package dev.shelfie.ref.storage;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Pre-signed URLs to S3-compatible object storage (Module 4.6). The course runs SeaweedFS as
 * {@code storage:8333}; MinIO, the plan's choice, stopped publishing container images in 2025.
 *
 * <p>Signing is offline (no network call), so Shelfie starts even when storage is down; only
 * {@link #inspect} talks to storage, and it streams: it never holds more than {@link #BUFFER} bytes.
 */
@Component
public class CoverStorage {

    public static final int BUFFER = 8192;
    public record Pending(String bookId, String key, String contentType) {}
    public record Inspected(long bytes, String sha256) {}

    private final S3Presigner presigner;
    private final String bucket;
    private final String endpoint;
    private final HttpClient http = HttpClient.newHttpClient();
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final AtomicInteger seq = new AtomicInteger();

    public CoverStorage(@Value("${shelfie.storage.endpoint:http://storage:8333}") String endpoint,
                        @Value("${shelfie.storage.bucket:covers}") String bucket,
                        @Value("${shelfie.storage.access-key:shelfie}") String accessKey,
                        @Value("${shelfie.storage.secret-key:shelfie-secret}") String secretKey) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    /** Returns {uploadId, url}: a PUT URL valid for 10 minutes. */
    public Map.Entry<String, String> createUpload(String bookId, String contentType) {
        String uploadId = "up" + seq.incrementAndGet();
        String key = bookId + "/" + uploadId + (contentType.equals("image/png") ? ".png" : ".jpg");
        String url = presigner.presignPutObject(r -> r.signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(p -> p.bucket(bucket).key(key).contentType(contentType))).url().toString();
        pending.put(uploadId, new Pending(bookId, key, contentType));
        return Map.entry(uploadId, url);
    }

    public Optional<Pending> pending(String uploadId) { return Optional.ofNullable(pending.get(uploadId)); }

    public void forget(String uploadId) { pending.remove(uploadId); }

    public String objectUrl(Pending p) { return endpoint + "/" + bucket + "/" + p.key(); }

    /** Streams the object through SHA-256. Empty when it does not exist (yet). */
    public Optional<Inspected> inspect(Pending p) throws Exception {
        String url = presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(1))
                .getObjectRequest(g -> g.bucket(bucket).key(p.key()))).url().toString();
        HttpResponse<InputStream> res = http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = res.body()) {
            if (res.statusCode() == 404) return Optional.empty();
            if (res.statusCode() != 200) throw new IllegalStateException("storage answered " + res.statusCode());
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            long n = 0;
            byte[] buf = new byte[BUFFER];
            try (DigestInputStream d = new DigestInputStream(in, sha)) {
                for (int r; (r = d.read(buf)) > 0; ) n += r;
            }
            return Optional.of(new Inspected(n, HexFormat.of().formatHex(sha.digest())));
        }
    }
}
