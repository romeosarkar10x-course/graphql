// TODO lab 10.5: write the trusted-documents manifest for every file in the operations folder.
//   java BuildManifest.java <operations dir> <manifest.json>
// Format (Apollo's persisted query manifest):
//   {"format": "apollo-persisted-query-manifest", "version": 1,
//    "operations": [{"id": "<sha256 hex of body>", "name": "Home", "type": "query", "body": "<document>"}]}
// body is the file's text with surrounding whitespace trimmed; that exact string is what the client sends.
public class BuildManifest {
  public static void main(String[] args) {
    throw new UnsupportedOperationException("TODO lab 10.5");
  }
}
