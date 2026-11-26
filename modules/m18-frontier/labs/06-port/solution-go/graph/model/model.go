// Package model holds the reviews subgraph's own types; gqlgen maps the schema's types onto them (gqlgen.yml).
package model

// Review as stored: the book and the author by id. Book and User are other subgraphs' entities.
type Review struct {
	ID       string `json:"id"`
	BookID   string `json:"bookId"`
	AuthorID string `json:"authorId"`
	Rating   int    `json:"rating"`
	Body     string `json:"body"`
}

// Book is catalog's entity; reviews only adds fields to it.
type Book struct {
	ID string `json:"id"`
}

// User is accounts' entity; reviews keeps a copy of the handle (@provides).
type User struct {
	ID     string `json:"id"`
	Handle string `json:"handle"`
}

func (Book) IsEntity()   {}
func (User) IsEntity()   {}
func (Review) IsEntity() {}
