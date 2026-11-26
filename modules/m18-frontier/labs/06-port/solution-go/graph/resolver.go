package graph

// Resolver is gqlgen's root: every resolver reaches the store through it.
type Resolver struct {
	Store *Store
}

type ctxKey string

// UserKey holds the X-Shelfie-User header's value in the request context ("" when absent).
const UserKey ctxKey = "user"
