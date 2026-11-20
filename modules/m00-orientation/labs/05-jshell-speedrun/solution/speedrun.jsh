// Reference run: 2 lines. Par is 2; 1 is possible.
var r = java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("https://swapi-graphql.netlify.app/graphql")).header("Content-Type", "application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"query\":\"{film(filmID:1){title}}\"}")).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
System.out.println(r.body().replaceAll(".*\"title\":\"([^\"]+)\".*", "$1"))
/exit
