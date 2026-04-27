# ============================================
#  LIBRARY MANAGEMENT API - cURL REQUESTS
#  Base URL: http://localhost:8080
# ============================================


# ===================== BOOKS =====================

# 1. GET /api/books — Get all books
curl -s http://localhost:8080/api/books | json_pp

# 2. GET /api/books/{id} — Get book by ID
curl -s http://localhost:8080/api/books/1 | json_pp

# 3. GET /api/books/search — Search books by genre, author, title, available
curl -s "http://localhost:8080/api/books/search?genre=Fiction&author=&title=&available=true" | json_pp

# 4. POST /api/books — Create a new book
curl -s -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "The Great Gatsby",
    "author": "F. Scott Fitzgerald",
    "isbn": "978-0743273565",
    "genre": "Fiction",
    "totalCopies": 5,
    "availableCopies": 5
  }' | json_pp

# 5. PUT /api/books/{id} — Update a book
curl -s -X PUT http://localhost:8080/api/books/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "The Great Gatsby (Revised)",
    "author": "F. Scott Fitzgerald",
    "isbn": "978-0743273565",
    "genre": "Classic Fiction",
    "totalCopies": 10,
    "availableCopies": 10
  }' | json_pp

# 6. PATCH /api/books/{id}/copies — Update book copies
curl -s -X PATCH http://localhost:8080/api/books/1/copies \
  -H "Content-Type: application/json" \
  -d '{
    "copies": 3
  }' | json_pp

# 7. DELETE /api/books/{id} — Delete a book
curl -s -X DELETE http://localhost:8080/api/books/1 | json_pp


# ===================== MEMBERS =====================

# 8. POST /api/members — Create a new member
curl -s -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@email.com",
    "phone": "1234567890",
    "membershipType": "BASIC",
    "membershipDate": "2026-04-28"
  }' | json_pp

# 9. GET /api/members/{id} — Get member by ID
curl -s http://localhost:8080/api/members/1 | json_pp

# 10. GET /api/members?type=PREMIUM — Get members by membership type
curl -s "http://localhost:8080/api/members?type=PREMIUM" | json_pp

# 11. PUT /api/members/{id} — Update a member
curl -s -X PUT http://localhost:8080/api/members/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe Updated",
    "email": "john.updated@email.com",
    "phone": "9876543210",
    "membershipType": "BASIC",
    "membershipDate": "2026-04-28",
    "active": true
  }' | json_pp

# 12. PATCH /api/members/{id}/upgrade — Upgrade membership to PREMIUM
curl -s -X PATCH http://localhost:8080/api/members/1/upgrade | json_pp

# 13. DELETE /api/members/{id} — Soft delete (deactivate) a member
curl -s -X DELETE http://localhost:8080/api/members/1 | json_pp

# 19. GET /api/members/{id}/borrow-history — Get borrow history for a member
curl -s http://localhost:8080/api/members/1/borrow-history | json_pp

# 22. GET /api/members/{id}/fines — Get fines for a member
curl -s http://localhost:8080/api/members/1/fines | json_pp


# ===================== BORROW =====================

# 14. POST /api/borrow — Borrow a book
curl -s -X POST http://localhost:8080/api/borrow \
  -H "Content-Type: application/json" \
  -d '{
    "bookId": 1,
    "memberId": 1
  }' | json_pp

# 15. PATCH /api/borrow/{id}/return — Return a borrowed book
curl -s -X PATCH http://localhost:8080/api/borrow/1/return | json_pp

# 16. GET /api/borrow/overdue — Get all overdue borrow records
curl -s http://localhost:8080/api/borrow/overdue | json_pp

# 17. GET /api/borrow/active — Get all active borrow records
curl -s http://localhost:8080/api/borrow/active | json_pp

# 18. PATCH /api/borrow/{id}/extend — Extend borrow (PREMIUM only)
curl -s -X PATCH http://localhost:8080/api/borrow/1/extend | json_pp


# ===================== FINES =====================

# 20. GET /api/fines/unpaid — Get all unpaid fines
curl -s http://localhost:8080/api/fines/unpaid | json_pp

# 21. PATCH /api/fines/{id}/pay — Pay a fine
curl -s -X PATCH http://localhost:8080/api/fines/1/pay | json_pp


# ===================== STATS =====================

# 23. GET /api/stats/summary — Get library summary stats
curl -s http://localhost:8080/api/stats/summary | json_pp

# 24. GET /api/stats/top-books?limit=5 — Get top borrowed books
curl -s "http://localhost:8080/api/stats/top-books?limit=5" | json_pp

# 25. GET /api/stats/overdue-rate — Get overdue rate
curl -s http://localhost:8080/api/stats/overdue-rate | json_pp
