# 4. Gapless document numbers

## Context

German tax law expects invoice numbers to be unique and sequential, so that a missing invoice is noticeable. Quotes, orders and supplier orders benefit from the same readable numbering, such as `RE-2026-0001`.

## Decision

Numbers come from a table with one counter per document type and year. Taking a number locks that row until the surrounding transaction ends (a pessimistic write lock) and happens in the same transaction that saves the document.

## Why

- A database sequence would be simpler, but a sequence value is lost when its transaction rolls back, which leaves gaps.
- With the counter inside the transaction, a failed save rolls the counter back too, so no number is skipped.
- The row lock makes two simultaneous requests wait for each other instead of receiving the same number.

## Consequences

- Creating documents of the same type is serialized: one at a time. For a kitchen studio's volume this does not matter.
- The counter restarts every year because the year is part of the counter's name.
- Document ids from the database and document numbers are different things; the number is what people see.
