// Shelfie's reviews subgraph in Go: the same contract as Module 15's Java one
// (modules/m15-federation/contract/subgraphs/src/main/resources/graphql/reviews/reviews.graphqls), schema first
// with gqlgen and its federation plugin.
//   bin/course go run github.com/99designs/gqlgen generate     (after a schema change)
//   bin/course go build -o reviews .                            (a static Linux binary)
//   ./reviews [port]                                            (default 4003)
//   POST /graphql   GraphQL over HTTP
//   POST /_reset    put the reviews back to the seed (tests call it between checks)
package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"strings"

	"github.com/99designs/gqlgen/graphql/handler"
	"github.com/99designs/gqlgen/graphql/handler/extension"
	"github.com/99designs/gqlgen/graphql/handler/transport"

	"shelfie/reviews/graph"
)

func main() {
	port := "4003"
	if len(os.Args) > 1 {
		port = os.Args[1]
	}
	seed := os.Getenv("SHELFIE_SEED")
	if seed == "" {
		seed = "seed.json"
	}
	store, err := graph.LoadStore(seed)
	if err != nil {
		log.Fatal(err)
	}
	srv := handler.New(graph.NewExecutableSchema(graph.Config{Resolvers: &graph.Resolver{Store: store}}))
	srv.AddTransport(transport.POST{})
	srv.AddTransport(transport.GET{})
	// handler.New starts with introspection off, and gqlgen's federation answers _service only when it is on
	// ("federated introspection disabled"); the Java subgraph allows introspection too.
	srv.Use(extension.Introspection{})

	mux := http.NewServeMux()
	// The reader comes from X-Shelfie-User, as in Module 15 (the router forwards it).
	mux.Handle("/graphql", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Java's HttpClient asks to upgrade plain HTTP to HTTP/2 ("Upgrade: h2c"); Go's server ignores that, but
		// gqlgen's POST transport refuses any request with an Upgrade header ("transport not supported"), as it
		// leaves those to the WebSocket transport. Drop the h2c offer and answer in HTTP/1.1.
		if strings.EqualFold(r.Header.Get("Upgrade"), "h2c") {
			r.Header.Del("Upgrade")
		}
		ctx := context.WithValue(r.Context(), graph.UserKey, r.Header.Get("X-Shelfie-User"))
		srv.ServeHTTP(w, r.WithContext(ctx))
	}))
	mux.HandleFunc("POST /_reset", func(w http.ResponseWriter, r *http.Request) {
		store.Reset()
		w.WriteHeader(http.StatusOK)
	})
	log.Printf("reviews (Go) on :%s", port)
	log.Fatal(http.ListenAndServe(":"+port, mux))
}
