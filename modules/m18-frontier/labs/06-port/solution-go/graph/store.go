package graph

import (
	"encoding/json"
	"fmt"
	"os"
	"slices"
	"strconv"
	"sync"

	"shelfie/reviews/graph/model"
)

// Seed is the contract's seed.json, shared by every port.
type Seed struct {
	Users   []model.User    `json:"users"`
	Books   []string        `json:"books"`
	Reviews []model.Review  `json:"reviews"`
}

// Store holds the reviews in memory; Reset puts the seed back.
type Store struct {
	mu      sync.Mutex
	seed    Seed
	handles map[string]string
	books   map[string]bool
	reviews map[string]model.Review
	next    int
}

func LoadStore(path string) (*Store, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	s := &Store{handles: map[string]string{}, books: map[string]bool{}}
	if err := json.Unmarshal(b, &s.seed); err != nil {
		return nil, fmt.Errorf("%s: %w", path, err)
	}
	for _, u := range s.seed.Users {
		s.handles[u.ID] = u.Handle
	}
	for _, id := range s.seed.Books {
		s.books[id] = true
	}
	s.Reset()
	return s, nil
}

func (s *Store) Reset() {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.reviews = map[string]model.Review{}
	for _, r := range s.seed.Reviews {
		s.reviews[r.ID] = r
	}
	s.next = len(s.seed.Reviews) + 1
}

func number(id string) int { n, _ := strconv.Atoi(id[1:]); return n }

// InIDOrder lists the reviews r1, r2, ... r10 (as the Java subgraph does), filtered by keep.
func (s *Store) InIDOrder(keep func(model.Review) bool) []*model.Review {
	s.mu.Lock()
	defer s.mu.Unlock()
	out := []*model.Review{}
	for _, r := range s.reviews {
		if keep == nil || keep(r) {
			r := r
			out = append(out, &r)
		}
	}
	slices.SortFunc(out, func(a, b *model.Review) int { return number(a.ID) - number(b.ID) })
	return out
}

func (s *Store) Review(id string) *model.Review {
	s.mu.Lock()
	defer s.mu.Unlock()
	if r, ok := s.reviews[id]; ok {
		return &r
	}
	return nil
}

func (s *Store) Handle(userID string) (string, bool) { h, ok := s.handles[userID]; return h, ok }
func (s *Store) HasBook(id string) bool              { return s.books[id] }

func (s *Store) Add(bookID, authorID string, rating int, body string) *model.Review {
	s.mu.Lock()
	defer s.mu.Unlock()
	r := model.Review{ID: fmt.Sprintf("r%d", s.next), BookID: bookID, AuthorID: authorID, Rating: rating, Body: body}
	s.next++
	s.reviews[r.ID] = r
	return &r
}
